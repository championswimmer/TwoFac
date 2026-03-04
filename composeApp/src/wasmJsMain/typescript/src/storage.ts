export function localStorageGetItem(key: string): string | null {
  return window.localStorage.getItem(key);
}

export function localStorageSetItem(key: string, value: string): void {
  window.localStorage.setItem(key, value);
}

export function localStorageRemoveItem(key: string): void {
  window.localStorage.removeItem(key);
}

export function isLocalStorageAccessible(): boolean {
  try {
    window.localStorage.setItem("twofac_ls_test", "1");
    window.localStorage.removeItem("twofac_ls_test");
    return true;
  } catch (e) {
    return false;
  }
}

export function nowEpochMillis(): number {
  return Date.now();
}
