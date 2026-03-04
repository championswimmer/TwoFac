export const localStorageGetItem = (key) => window.localStorage.getItem(key);
export const localStorageSetItem = (key, value) => {
    window.localStorage.setItem(key, value);
};
export const localStorageRemoveItem = (key) => {
    window.localStorage.removeItem(key);
};
export const isLocalStorageAccessible = () => {
    try {
        window.localStorage.setItem("twofac_ls_test", "1");
        window.localStorage.removeItem("twofac_ls_test");
        return true;
    }
    catch {
        return false;
    }
};
