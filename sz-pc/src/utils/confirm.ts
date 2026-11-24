import { ElMessageBox, type ElMessageBoxOptions } from 'element-plus';

const DEFAULT_CONFIRM_CLASS = 'app-confirm-dialog';

function mergeDialogClass(customClass?: string) {
  return [DEFAULT_CONFIRM_CLASS, customClass].filter(Boolean).join(' ');
}

export function showConfirmDialog(
  message: string,
  title = '提示',
  options: Partial<ElMessageBoxOptions> = {}
) {
  const customClass = typeof options.customClass === 'string' ? options.customClass : undefined;

  return ElMessageBox.confirm(message, title, {
    type: 'warning',
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    closeOnClickModal: false,
    closeOnPressEscape: true,
    autofocus: false,
    customClass: mergeDialogClass(customClass),
    ...options
  });
}
