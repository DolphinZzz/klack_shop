from __future__ import annotations

import asyncio
import inspect
import shutil
import tempfile
from collections.abc import Awaitable, Callable
from pathlib import Path
from typing import Any
from uuid import UUID

import httpx
from mineru import DEFAULT_BASE_URL, DEFAULT_FLASH_BASE_URL, MinerU
from pypdf import PdfReader, PdfWriter

from app.config import Settings

VALID_MINERU_MODES = frozenset({"cloud_api", "local_http", "local_cli"})
DEFAULT_FLASH_MAX_PAGES = 20


def _split_pdf_pages(pdf_path: Path, max_pages: int, out_dir: Path) -> list[Path]:
    """当 PDF 页数超过 *max_pages* 时，按页数上限拆分为多个子 PDF。"""
    reader = PdfReader(str(pdf_path))
    total_pages = len(reader.pages)
    if total_pages == 0:
        raise ValueError(f"PDF has no pages: {pdf_path}")
    if total_pages <= max_pages:
        return [pdf_path]

    chunk_paths: list[Path] = []
    for part_idx, start in enumerate(range(0, total_pages, max_pages), start=1):
        writer = PdfWriter()
        end = min(start + max_pages, total_pages)
        for page_num in range(start, end):
            writer.add_page(reader.pages[page_num])
        chunk_path = out_dir / f"{pdf_path.stem}_part_{part_idx:03d}.pdf"
        with chunk_path.open("wb") as handle:
            writer.write(handle)
        chunk_paths.append(chunk_path)
    return chunk_paths


def _extract_http_markdown(payload: Any) -> str | None:
    """从常见的 MinerU 本地 HTTP JSON 响应结构中提取 Markdown 文本。"""
    if not isinstance(payload, dict):
        return None
    for key in ("md_content", "markdown", "content"):
        val = payload.get(key)
        if isinstance(val, str) and val.strip():
            return val

    data = payload.get("data")
    if isinstance(data, dict):
        extracted = _extract_http_markdown(data)
        if extracted is not None:
            return extracted

    results = payload.get("results")
    if isinstance(results, dict):
        for entry in results.values():
            extracted = _extract_http_markdown(entry)
            if extracted is not None:
                return extracted
    elif isinstance(results, list):
        for entry in results:
            extracted = _extract_http_markdown(entry)
            if extracted is not None:
                return extracted
    return None


class MinerUParser:
    """支持 MinerU 云端 API、本地 HTTP 与本地 CLI 模式的统一 PDF 转 Markdown 适配器。"""

    def __init__(
        self,
        mode: str = "cloud_api",
        api_token: str = "",
        base_url: str = "",
        *,
        flash_base_url: str = "",
        flash_max_pages: int = DEFAULT_FLASH_MAX_PAGES,
        storage_dir: str | Path | None = None,
        language: str = "ch",
        timeout: float = 300.0,
        flash_runner: Callable[[Path], str | Awaitable[str]] | None = None,
        precision_runner: Callable[[Path], str | Awaitable[str]] | None = None,
        http_client: httpx.AsyncClient | None = None,
        cli_command: str | None = None,
        cli_runner: Callable[[list[str]], Awaitable[tuple[int, str, str]]] | None = None,
    ) -> None:
        if mode not in VALID_MINERU_MODES:
            raise ValueError(f"Unsupported MinerU mode: {mode!r}")
        if flash_max_pages < 1:
            raise ValueError("flash_max_pages must be >= 1")

        self.mode = mode
        self.api_token = api_token.strip()
        self.base_url = base_url.strip()
        self.flash_base_url = flash_base_url.strip()
        self.flash_max_pages = flash_max_pages
        self.storage_dir = Path(storage_dir) if storage_dir is not None else None
        self.language = language
        self.timeout = timeout
        self._flash_runner = flash_runner
        self._precision_runner = precision_runner
        self._http_client = http_client
        self._cli_command = cli_command
        self._cli_runner = cli_runner

    @classmethod
    def from_settings(cls, settings: Settings, **overrides: Any) -> MinerUParser:
        """根据运行时应用配置实例化 MinerUParser。"""
        kwargs: dict[str, Any] = {
            "mode": settings.mineru_mode,
            "api_token": settings.mineru_api_token,
            "base_url": settings.mineru_base_url,
            "storage_dir": settings.kb_storage_dir,
        }
        kwargs.update(overrides)
        return cls(**kwargs)

    async def parse_pdf(
        self,
        pdf_path: Path | str,
        *,
        doc_id: str | UUID | None = None,
        cache_path: Path | None = None,
    ) -> str:
        """将本地 PDF 文件解析为 Markdown，并按需持久化原始 Markdown 缓存。"""
        path = Path(pdf_path)
        if not path.exists():
            raise FileNotFoundError(f"PDF file not found: {path}")

        if self.mode == "cloud_api":
            markdown = await self._parse_cloud_api(path)
        elif self.mode == "local_http":
            markdown = await self._parse_local_http(path)
        else:
            markdown = await self._parse_local_cli(path)

        target_cache: Path | None = cache_path
        if target_cache is None and doc_id is not None and self.storage_dir is not None:
            target_cache = self.storage_dir / "markdown" / f"{doc_id}.md"

        if target_cache is not None:
            target_cache.parent.mkdir(parents=True, exist_ok=True)
            target_cache.write_text(markdown, encoding="utf-8")

        return markdown

    async def _parse_cloud_api(self, pdf_path: Path) -> str:
        if self.api_token:
            if self._precision_runner is not None:
                res = self._precision_runner(pdf_path)
                return await res if inspect.isawaitable(res) else res
            return await asyncio.to_thread(self._extract_precision_sync, pdf_path)

        with tempfile.TemporaryDirectory(prefix="mineru_flash_") as tmp_dir:
            chunks = await asyncio.to_thread(
                _split_pdf_pages, pdf_path, self.flash_max_pages, Path(tmp_dir)
            )
            parts: list[str] = []
            for chunk_path in chunks:
                if self._flash_runner is not None:
                    res = self._flash_runner(chunk_path)
                    part_md = await res if inspect.isawaitable(res) else res
                else:
                    part_md = await asyncio.to_thread(self._extract_flash_sync, chunk_path)
                if part_md and part_md.strip():
                    parts.append(part_md.strip())
            return "\n\n".join(parts)

    def _extract_precision_sync(self, pdf_path: Path) -> str:
        client = MinerU(
            token=self.api_token,
            base_url=self.base_url or DEFAULT_BASE_URL,
        )
        try:
            result = client.extract(str(pdf_path), timeout=int(self.timeout))
        finally:
            client.close()
        if result.state != "done" or result.markdown is None:
            raise RuntimeError(
                f"MinerU precision extract failed: state={result.state}, "
                f"code={result.err_code}, error={result.error}"
            )
        return result.markdown

    def _extract_flash_sync(self, pdf_path: Path) -> str:
        client = MinerU(
            flash_base_url=self.flash_base_url or DEFAULT_FLASH_BASE_URL,
        )
        try:
            result = client.flash_extract(
                str(pdf_path),
                language=self.language,
                timeout=int(self.timeout),
            )
        finally:
            client.close()
        if result.state != "done" or result.markdown is None:
            raise RuntimeError(
                f"MinerU flash extract failed: state={result.state}, "
                f"code={result.err_code}, error={result.error}"
            )
        return result.markdown

    async def _parse_local_http(self, pdf_path: Path) -> str:
        if not self.base_url:
            raise ValueError("MINERU_BASE_URL is required for local_http mode")

        base = self.base_url.rstrip("/")
        url = base if base.endswith("/file_parse") else f"{base}/file_parse"
        pdf_bytes = pdf_path.read_bytes()

        async def _send(client: httpx.AsyncClient) -> httpx.Response:
            return await client.post(
                url,
                files={"files": (pdf_path.name, pdf_bytes, "application/pdf")},
                data={"return_md": "true", "lang_list": self.language},
            )

        if self._http_client is not None:
            resp = await _send(self._http_client)
        else:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                resp = await _send(client)

        resp.raise_for_status()
        content_type = resp.headers.get("content-type", "").lower()
        if "application/json" in content_type or resp.text.lstrip().startswith("{"):
            md = _extract_http_markdown(resp.json())
            if md is not None:
                return md
            raise RuntimeError("MinerU local_http JSON response contained no markdown content")
        if resp.text.strip():
            return resp.text
        raise RuntimeError("MinerU local_http response was empty")

    async def _parse_local_cli(self, pdf_path: Path) -> str:
        cmd_bin = self._cli_command or shutil.which("mineru") or shutil.which("magic-pdf")
        if not cmd_bin and self._cli_runner is not None:
            cmd_bin = "mineru"
        if not cmd_bin:
            raise FileNotFoundError(
                "Neither 'mineru' nor 'magic-pdf' CLI executable was found in PATH"
            )

        with tempfile.TemporaryDirectory(prefix="mineru_cli_") as tmp_dir:
            out_dir = Path(tmp_dir)
            cmd = [cmd_bin, "-p", str(pdf_path), "-o", str(out_dir)]
            if self._cli_runner is not None:
                returncode, _stdout, stderr = await self._cli_runner(cmd)
            else:
                proc = await asyncio.create_subprocess_exec(
                    *cmd,
                    stdout=asyncio.subprocess.PIPE,
                    stderr=asyncio.subprocess.PIPE,
                )
                try:
                    _, stderr_b = await asyncio.wait_for(
                        proc.communicate(), timeout=self.timeout
                    )
                except BaseException:
                    if proc.returncode is None:
                        try:
                            proc.kill()
                        except ProcessLookupError:
                            pass
                        await proc.wait()
                    raise
                returncode = proc.returncode or 0
                stderr = stderr_b.decode("utf-8", errors="replace")

            if returncode != 0:
                raise RuntimeError(
                    f"MinerU local_cli exited with code {returncode}: {stderr.strip()}"
                )

            md_files = sorted(out_dir.rglob("*.md"))
            preferred = [p for p in md_files if p.stem == pdf_path.stem]
            for candidate in preferred + md_files:
                content = candidate.read_text(encoding="utf-8")
                if content.strip():
                    return content
            raise RuntimeError(f"MinerU local_cli produced no markdown files for {pdf_path.name}")
