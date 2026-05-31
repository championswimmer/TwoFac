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

type BrowserStorageArea = {
  get: (keys?: string | string[] | Record<string, unknown>) => Promise<Record<string, unknown>>;
  set: (items: Record<string, unknown>) => Promise<void>;
  remove: (keys: string | string[]) => Promise<void>;
};

type ChromeStorageArea = {
  get: (keys: string | string[] | Record<string, unknown> | null | undefined, callback: (items: Record<string, unknown>) => void) => void;
  set: (items: Record<string, unknown>, callback: () => void) => void;
  remove: (keys: string | string[], callback: () => void) => void;
};

type ExtensionGlobals = typeof globalThis & {
  browser?: {
    storage?: {
      session?: BrowserStorageArea;
    };
  };
  chrome?: {
    runtime?: {
      lastError?: {
        message?: string;
      };
    };
    storage?: {
      session?: ChromeStorageArea;
    };
  };
};

const extensionGlobals = globalThis as ExtensionGlobals;

const getBrowserSessionStorage = (): BrowserStorageArea | undefined => {
  return extensionGlobals.browser?.storage?.session;
};

const getChromeSessionStorage = (): ChromeStorageArea | undefined => {
  return extensionGlobals.chrome?.storage?.session;
};

const getChromeRuntimeError = (): string | null => {
  return extensionGlobals.chrome?.runtime?.lastError?.message ?? null;
};

const chromeGet = (storageArea: ChromeStorageArea, key: string): Promise<Record<string, unknown>> => {
  return new Promise((resolve, reject) => {
    storageArea.get(key, (items) => {
      const errorMessage = getChromeRuntimeError();
      if (errorMessage) {
        reject(new Error(errorMessage));
        return;
      }
      resolve(items);
    });
  });
};

const chromeSet = (storageArea: ChromeStorageArea, items: Record<string, unknown>): Promise<void> => {
  return new Promise((resolve, reject) => {
    storageArea.set(items, () => {
      const errorMessage = getChromeRuntimeError();
      if (errorMessage) {
        reject(new Error(errorMessage));
        return;
      }
      resolve();
    });
  });
};

const chromeRemove = (storageArea: ChromeStorageArea, key: string): Promise<void> => {
  return new Promise((resolve, reject) => {
    storageArea.remove(key, () => {
      const errorMessage = getChromeRuntimeError();
      if (errorMessage) {
        reject(new Error(errorMessage));
        return;
      }
      resolve();
    });
  });
};

export const isExtensionSessionStorageAccessible = (): boolean => {
  return Boolean(getBrowserSessionStorage() ?? getChromeSessionStorage());
};

export const extensionSessionStorageGetItem = async (key: string): Promise<{ value: string | null }> => {
  try {
    const browserStorage = getBrowserSessionStorage();
    if (browserStorage) {
      const items = await browserStorage.get(key);
      const value = items[key];
      return { value: typeof value === "string" ? value : null };
    }

    const chromeStorage = getChromeSessionStorage();
    if (chromeStorage) {
      const items = await chromeGet(chromeStorage, key);
      const value = items[key];
      return { value: typeof value === "string" ? value : null };
    }
  } catch {
    return { value: null };
  }

  return { value: null };
};

export const extensionSessionStorageSetItem = async (key: string, value: string): Promise<{ success: boolean }> => {
  try {
    const browserStorage = getBrowserSessionStorage();
    if (browserStorage) {
      await browserStorage.set({ [key]: value });
      return { success: true };
    }

    const chromeStorage = getChromeSessionStorage();
    if (chromeStorage) {
      await chromeSet(chromeStorage, { [key]: value });
      return { success: true };
    }
  } catch {
    return { success: false };
  }

  return { success: false };
};

export const extensionSessionStorageRemoveItem = async (key: string): Promise<{ success: boolean }> => {
  try {
    const browserStorage = getBrowserSessionStorage();
    if (browserStorage) {
      await browserStorage.remove(key);
      return { success: true };
    }

    const chromeStorage = getChromeSessionStorage();
    if (chromeStorage) {
      await chromeRemove(chromeStorage, key);
      return { success: true };
    }
  } catch {
    return { success: false };
  }

  return { success: false };
};
