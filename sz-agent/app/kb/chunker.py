from __future__ import annotations

import re
from dataclasses import dataclass

_HEADING_RE = re.compile(r"^(#{1,6})\s+(.+?)\s*#*\s*$")
_LAW_ARTICLE_RE = re.compile(r"^(第[一二三四五六七八九十百千零〇0-9]+条(?:之[一二三四五六七八九十]+)?)")
_PAGE_MARKER_RE = re.compile(r"<!--\s*page\s*:\s*(\d+)\s*-->", re.IGNORECASE)
_HTML_COMMENT_RE = re.compile(r"<!--.*?-->", re.DOTALL)
_NOISE_LINE_RE = re.compile(
    r"^(?:[-—_=~]\s*\d+\s*[-—_=~]"
    r"|第\s*\d+\s*页(?:\s*/\s*共\s*\d+\s*页)?"
    r"|Page\s+\d+(?:\s+of\s+\d+)?"
    r"|\d+\s*/\s*\d+)$",
    re.IGNORECASE,
)
_SENTENCE_BREAKS = ("。", "；", "！", "？", ".", ";", "!", "?", "\n")


@dataclass(slots=True)
class KbChunk:
    """Structure-aware knowledge base chunk ready for embedding and Milvus storage."""

    chunk_id: str
    doc_id: str
    doc_title: str
    category: str
    section_path: str
    page_no: int
    scope: str
    content: str
    chunk_index: int = 0


@dataclass(slots=True)
class _AtomicUnit:
    section_path: str
    parent_path: str
    article_label: str | None
    page_no: int
    text: str
    is_table: bool = False


def _clean_lines(markdown_text: str) -> list[str]:
    """Normalize line endings and remove header/footer page noise outside code fences."""
    normalized = markdown_text.replace("\r\n", "\n").replace("\r", "\n")
    cleaned: list[str] = []
    in_fence = False
    for raw_line in normalized.split("\n"):
        stripped = raw_line.strip()
        if stripped.startswith("```"):
            in_fence = not in_fence
            cleaned.append(raw_line)
            continue
        if not in_fence and _NOISE_LINE_RE.match(stripped):
            continue
        cleaned.append(raw_line)
    return cleaned


def _split_blocks(body_lines: list[str], base_page_no: int) -> list[tuple[str, int, bool]]:
    """Group section lines into prose paragraphs, Markdown/HTML tables, and legal article blocks."""
    blocks: list[tuple[str, int, bool]] = []
    current_lines: list[str] = []
    current_page = base_page_no
    block_page = base_page_no
    i = 0
    n = len(body_lines)

    def _flush_prose() -> None:
        if not current_lines:
            return
        text = "\n".join(current_lines).strip()
        current_lines.clear()
        if text:
            blocks.append((text, block_page, False))

    while i < n:
        line = body_lines[i]
        marker = _PAGE_MARKER_RE.search(line)
        if marker:
            current_page = int(marker.group(1))
            line = _PAGE_MARKER_RE.sub("", line)
        line = _HTML_COMMENT_RE.sub("", line)
        stripped = line.strip()

        if not stripped:
            _flush_prose()
            i += 1
            continue

        # Preserve HTML <table>...</table> blocks intact
        if stripped.lower().startswith("<table"):
            _flush_prose()
            tbl_lines = [line]
            tbl_page = current_page
            if "</table>" not in stripped.lower():
                i += 1
                while i < n:
                    next_line = _HTML_COMMENT_RE.sub("", body_lines[i])
                    tbl_lines.append(next_line)
                    if "</table>" in next_line.lower():
                        break
                    i += 1
            blocks.append(("\n".join(tbl_lines).strip(), tbl_page, True))
            i += 1
            continue

        # Preserve Markdown pipe tables intact
        if stripped.startswith("|") and i + 1 < n and "|" in body_lines[i + 1]:
            sep_candidate = body_lines[i + 1].strip()
            if sep_candidate.startswith("|") and "-" in sep_candidate:
                _flush_prose()
                tbl_lines = [line]
                tbl_page = current_page
                i += 1
                while i < n:
                    cand = _HTML_COMMENT_RE.sub("", body_lines[i])
                    if not cand.strip().startswith("|"):
                        break
                    tbl_lines.append(cand)
                    i += 1
                blocks.append(("\n".join(tbl_lines).strip(), tbl_page, True))
                continue

        # Start a new unit on legal article boundary (e.g. "第二十五条 ...")
        if _LAW_ARTICLE_RE.match(stripped) and current_lines:
            _flush_prose()

        if not current_lines:
            block_page = current_page
        current_lines.append(line)
        i += 1

    _flush_prose()
    return blocks


def _parse_atomic_units(markdown_text: str, doc_title: str) -> list[_AtomicUnit]:
    """Parse Markdown heading hierarchy and legal article boundaries into atomic units."""
    lines = _clean_lines(markdown_text)
    heading_stack: list[tuple[int, str]] = []
    current_page = 0
    section_body: list[str] = []
    section_page = 0
    in_fence = False
    units: list[_AtomicUnit] = []

    def _current_paths() -> tuple[str, str]:
        if not heading_stack:
            return doc_title, doc_title
        full = " > ".join(title for _, title in heading_stack)
        parent = (
            " > ".join(title for _, title in heading_stack[:-1])
            if len(heading_stack) > 1
            else full
        )
        return full, parent

    def _flush_section(leaf_heading: str | None) -> None:
        if not section_body and not leaf_heading:
            return
        full_path, parent_path = _current_paths()
        blocks = _split_blocks(section_body, section_page)
        section_body.clear()
        if not blocks:
            return

        for idx, (block_text, blk_page, is_table) in enumerate(blocks):
            article_match = _LAW_ARTICLE_RE.match(block_text.lstrip())
            article_label = article_match.group(1) if article_match else None

            if idx == 0 and leaf_heading is not None:
                text = f"{leaf_heading}\n{block_text}"
                leaf_art = _LAW_ARTICLE_RE.match(heading_stack[-1][1]) if heading_stack else None
                if leaf_art:
                    article_label = leaf_art.group(1)
                    unit_path = full_path
                    unit_parent = parent_path
                elif article_label:
                    unit_path = f"{full_path} > {article_label}" if full_path else article_label
                    unit_parent = full_path
                else:
                    unit_path = full_path
                    unit_parent = parent_path
            else:
                text = block_text
                if article_label and (
                    not heading_stack or not heading_stack[-1][1].startswith(article_label)
                ):
                    unit_path = f"{full_path} > {article_label}" if full_path else article_label
                    unit_parent = full_path
                else:
                    unit_path = full_path
                    unit_parent = parent_path

            units.append(
                _AtomicUnit(
                    section_path=unit_path,
                    parent_path=unit_parent,
                    article_label=article_label,
                    page_no=blk_page,
                    text=text,
                    is_table=is_table,
                )
            )

    active_leaf_heading: str | None = None
    for raw_line in lines:
        marker = _PAGE_MARKER_RE.search(raw_line)
        if marker:
            current_page = int(marker.group(1))
        stripped = raw_line.strip()
        if stripped.startswith("```"):
            in_fence = not in_fence
            section_body.append(raw_line)
            continue

        if not in_fence:
            heading_match = _HEADING_RE.match(stripped)
            if heading_match:
                _flush_section(active_leaf_heading)
                level = len(heading_match.group(1))
                title = heading_match.group(2).strip()
                while heading_stack and heading_stack[-1][0] >= level:
                    heading_stack.pop()
                heading_stack.append((level, title))
                active_leaf_heading = stripped
                section_page = current_page
                continue

        if not section_body and active_leaf_heading is None:
            section_page = current_page
        section_body.append(raw_line)

    _flush_section(active_leaf_heading)
    return units


def _split_markdown_table(text: str, max_chars: int) -> list[str]:
    """Split an oversized Markdown pipe table by rows while repeating its header."""
    lines = [line for line in text.splitlines() if line.strip()]
    hdr_idx = -1
    for idx in range(len(lines) - 1):
        if lines[idx].lstrip().startswith("|") and lines[idx + 1].lstrip().startswith("|") and "-" in lines[idx + 1]:
            hdr_idx = idx
            break
    if hdr_idx == -1 or len(lines) - hdr_idx <= 3:
        return []

    prefix = "\n".join(lines[:hdr_idx]).strip()
    header = "\n".join(lines[hdr_idx : hdr_idx + 2])
    if len(header) >= max_chars:
        return []

    chunks: list[str] = []
    current_rows: list[str] = []
    first_chunk_header = f"{prefix}\n{header}" if prefix and len(prefix) + 1 + len(header) < max_chars else header
    active_header = first_chunk_header
    current_len = len(active_header)

    for row in lines[hdr_idx + 2 :]:
        row_cost = 1 + len(row)
        if current_rows and current_len + row_cost > max_chars:
            chunks.append(active_header + "\n" + "\n".join(current_rows))
            active_header = header
            current_rows = [row]
            current_len = len(active_header) + row_cost
        elif not current_rows and len(active_header) + row_cost > max_chars:
            return []
        else:
            current_rows.append(row)
            current_len += row_cost

    if current_rows:
        chunks.append(active_header + "\n" + "\n".join(current_rows))
    return chunks


def _split_long_text(text: str, max_chars: int, overlap: int) -> list[str]:
    """Split oversized prose into overlapping slices bounded by *max_chars*."""
    if len(text) <= max_chars:
        return [text]

    effective_overlap = min(max(0, overlap), max_chars // 2)
    slices: list[str] = []
    start = 0
    total = len(text)

    while start < total:
        end = min(start + max_chars, total)
        if end < total:
            search_floor = start + max(1, (max_chars - effective_overlap) // 2)
            best_split = -1
            for sep in _SENTENCE_BREAKS:
                pos = text.rfind(sep, search_floor, end)
                if pos > best_split:
                    best_split = pos + len(sep)
            if best_split > start:
                end = best_split

        piece = text[start:end].strip()
        if piece:
            slices.append(piece)

        if end >= total:
            break
        next_start = max(start + 1, end - effective_overlap)
        start = next_start

    return slices


def _expand_oversized_units(
    units: list[_AtomicUnit], max_chars: int, overlap: int
) -> list[_AtomicUnit]:
    """Split any unit exceeding *max_chars* into bounded sub-units."""
    expanded: list[_AtomicUnit] = []
    for unit in units:
        if len(unit.text) <= max_chars:
            expanded.append(unit)
            continue

        parts = _split_markdown_table(unit.text, max_chars) if unit.is_table else []
        if not parts:
            parts = _split_long_text(unit.text, max_chars=max_chars, overlap=overlap)

        for part in parts:
            expanded.append(
                _AtomicUnit(
                    section_path=unit.section_path,
                    parent_path=unit.parent_path,
                    article_label=unit.article_label,
                    page_no=unit.page_no,
                    text=part,
                    is_table=unit.is_table,
                )
            )
    return expanded


def _merge_section_path(units: list[_AtomicUnit]) -> str:
    """Compute a concise citation path for one or more merged atomic units."""
    if len(units) == 1:
        return units[0].section_path

    first = units[0]
    last = units[-1]
    if all(u.section_path == first.section_path for u in units):
        return first.section_path

    common_parent = first.parent_path
    if all(u.parent_path == common_parent for u in units) and first.article_label and last.article_label:
        if first.article_label == last.article_label:
            return f"{common_parent} > {first.article_label}"
        return f"{common_parent} > {first.article_label}~{last.article_label}"

    split_paths = [u.section_path.split(" > ") for u in units]
    prefix: list[str] = []
    for parts in zip(*split_paths):
        if len(set(parts)) == 1:
            prefix.append(parts[0])
        else:
            break
    return " > ".join(prefix) if prefix else first.section_path


def _can_merge(buf: list[_AtomicUnit], buf_len: int, nxt: _AtomicUnit, min_chars: int, max_chars: int) -> bool:
    if not buf:
        return False
    if buf_len >= min_chars:
        return False
    if buf_len + 2 + len(nxt.text) > max_chars:
        return False
    if buf[-1].is_table or nxt.is_table:
        return False
    buf_root = buf[0].section_path.split(" > ")[0]
    nxt_root = nxt.section_path.split(" > ")[0]
    return buf_root == nxt_root


def chunk_markdown(
    markdown_text: str,
    doc_id: str,
    doc_title: str,
    category: str,
    min_chars: int = 500,
    max_chars: int = 800,
    overlap: int = 90,
    scope: str = "public",
) -> list[KbChunk]:
    """Split Markdown into structure-aware chunks preserving headings and Chinese legal articles."""
    if min_chars < 1 or max_chars < min_chars:
        raise ValueError("Require 1 <= min_chars <= max_chars")
    if not markdown_text or not markdown_text.strip():
        return []

    units = _parse_atomic_units(markdown_text, doc_title=doc_title)
    if not units:
        return []

    bounded_units = _expand_oversized_units(units, max_chars=max_chars, overlap=overlap)
    groups: list[list[_AtomicUnit]] = []
    buf: list[_AtomicUnit] = []
    buf_len = 0

    for unit in bounded_units:
        if _can_merge(buf, buf_len, unit, min_chars=min_chars, max_chars=max_chars):
            buf.append(unit)
            buf_len += 2 + len(unit.text)
        else:
            if buf:
                groups.append(buf)
            buf = [unit]
            buf_len = len(unit.text)

    if buf:
        groups.append(buf)

    chunks: list[KbChunk] = []
    for group in groups:
        content = "\n\n".join(u.text for u in group).strip()
        if not content:
            continue
        chunks.append(
            KbChunk(
                chunk_id=f"{doc_id}:{len(chunks)}",
                doc_id=doc_id,
                doc_title=doc_title,
                category=category,
                section_path=_merge_section_path(group),
                page_no=group[0].page_no,
                scope=scope,
                content=content,
                chunk_index=len(chunks),
            )
        )
    return chunks
