(() => {
    if ("serviceWorker" in navigator) {
        window.addEventListener("load", () => {
            navigator.serviceWorker.register("serviceWorker.js")
                .then((registration) => {
                    console.log("Service Worker registerd!");
                    console.log(`scope: ${registration.scope}`);
                })
                .catch((error) => {
                    console.log("Service Worker register failed");
                    console.log(`error: ${error}`);
                });
        });
    }
})();