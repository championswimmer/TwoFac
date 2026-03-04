(() => {
    if ("serviceWorker" in navigator) {
        let refreshing = false;
        navigator.serviceWorker.addEventListener('controllerchange', () => {
            if (!refreshing) {
                window.location.reload();
                refreshing = true;
            }
        });

        window.addEventListener("load", () => {
            navigator.serviceWorker.register("serviceWorker.js")
                .then((registration) => {
                    console.log("Service Worker registered!");
                    console.log(`scope: ${registration.scope}`);
                })
                .catch((error) => {
                    console.log("Service Worker register failed");
                    console.log(`error: ${error}`);
                });
        });
    }
})();