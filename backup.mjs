const bytesToBase64 = (bytes) => {
    let binary = "";
    const chunkSize = 0x8000;
    for (let i = 0; i < bytes.length; i += chunkSize) {
        const chunk = bytes.subarray(i, i + chunkSize);
        binary += String.fromCharCode(...chunk);
    }
    return btoa(binary);
};
const base64ToBytes = (base64) => {
    const binary = atob(base64);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i += 1) {
        bytes[i] = binary.charCodeAt(i);
    }
    return bytes;
};
const toArrayBuffer = (bytes) => {
    return bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength);
};
export const pickBackupFile = async (accept = ".json") => {
    return new Promise((resolve) => {
        const input = document.createElement("input");
        input.type = "file";
        input.accept = accept;
        input.style.display = "none";
        const cleanup = () => {
            input.remove();
        };
        input.onchange = async () => {
            try {
                const file = input.files?.item(0);
                if (!file) {
                    resolve(null);
                    return;
                }
                const buffer = await file.arrayBuffer();
                const bytes = new Uint8Array(buffer);
                resolve({
                    name: file.name,
                    contentBase64: bytesToBase64(bytes),
                });
            }
            catch {
                resolve(null);
            }
            finally {
                cleanup();
            }
        };
        input.oncancel = () => {
            cleanup();
            resolve(null);
        };
        document.body.appendChild(input);
        input.click();
    });
};
export const saveBackupFile = async (fileName, contentBase64) => {
    const bytes = base64ToBytes(contentBase64);
    const savePicker = window.showSaveFilePicker;
    if (typeof savePicker === "function") {
        try {
            const fileHandle = await savePicker({
                suggestedName: fileName,
                types: [
                    {
                        description: "TwoFac backup",
                        accept: {
                            "application/json": [".json"],
                        },
                    },
                ],
            });
            const writable = await fileHandle.createWritable();
            await writable.write(toArrayBuffer(bytes));
            await writable.close();
            return { success: true };
        }
        catch (error) {
            const errorName = error?.name;
            if (errorName === "AbortError") {
                return { success: false };
            }
            // fallback to download link below
        }
    }
    try {
        const blob = new Blob([toArrayBuffer(bytes)], { type: "application/json" });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = fileName;
        link.style.display = "none";
        document.body.appendChild(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(url);
        return { success: true };
    }
    catch {
        return { success: false };
    }
};
