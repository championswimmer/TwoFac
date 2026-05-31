export const localStorageGetItem = (key: string): string | null => window.localStorage.getItem(key);

export const localStorageSetItem = (key: string, value: string): void => {
  window.localStorage.setItem(key, value);
};

export const localStorageRemoveItem = (key: string): void => {
  window.localStorage.removeItem(key);
};

export const isLocalStorageAccessible = (): boolean => {
  try {
    window.localStorage.setItem("twofac_ls_test", "1");
    window.localStorage.removeItem("twofac_ls_test");
    return true;
  } catch {
    return false;
  }
};
