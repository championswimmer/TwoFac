(() => {
    const sidePanelApi = globalThis.chrome?.sidePanel;
    if (!sidePanelApi || typeof sidePanelApi.setPanelBehavior !== "function") {
        return;
    }

    const result = sidePanelApi.setPanelBehavior({ openPanelOnActionClick: false });
    if (result && typeof result.catch === "function") {
        result.catch(() => {
            // sidePanel is optional and unsupported on Firefox.
        });
    }
})();
