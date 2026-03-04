const textEncoder = new TextEncoder();
const textDecoder = new TextDecoder();

function base64UrlToBytes(value: string): Uint8Array {
  const base64 = value.replace(/-/g, "+").replace(/_/g, "/");
  const padded = base64 + "=".repeat((4 - (base64.length % 4)) % 4);
  const binary = atob(padded);
  const bytes = new Uint8Array(binary.length);
  for (let index = 0; index < binary.length; index++) {
    bytes[index] = binary.charCodeAt(index);
  }
  return bytes;
}

function bytesToBase64Url(bytes: Uint8Array): string {
  let binary = "";
  for (let index = 0; index < bytes.length; index++) {
    binary += String.fromCharCode(bytes[index]);
  }
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

function deriveAesGcmKey(
  saltBytes: Uint8Array,
  prfFirstOutputBase64Url: string,
  context: string,
): Promise<CryptoKey> {
  const inputKeyMaterial = base64UrlToBytes(prfFirstOutputBase64Url);
  return crypto.subtle
    .importKey("raw", inputKeyMaterial, "HKDF", false, ["deriveKey"])
    .then((baseKey) => {
      return crypto.subtle.deriveKey(
        {
          name: "HKDF",
          hash: "SHA-256",
          salt: saltBytes,
          info: textEncoder.encode(context),
        },
        baseKey,
        { name: "AES-GCM", length: 256 },
        false,
        ["encrypt", "decrypt"],
      );
    });
}

export function encryptPasskeyWithWebCrypto(
  plaintext: string,
  prfFirstOutputBase64Url: string,
  context: string,
  onResult: (
    status: string,
    salt: string | null,
    nonce: string | null,
    ciphertext: string | null,
    error: string | null,
  ) => void,
): void {
  if (
    typeof crypto === "undefined" ||
    crypto == null ||
    typeof crypto.subtle === "undefined" ||
    crypto.subtle == null
  ) {
    onResult("UNAVAILABLE", null, null, null, "WebCryptoUnavailable");
    return;
  }

  const saltBytes = crypto.getRandomValues(new Uint8Array(16));
  const nonceBytes = crypto.getRandomValues(new Uint8Array(12));

  deriveAesGcmKey(saltBytes, prfFirstOutputBase64Url, context)
    .then((derivedKey) => {
      return crypto.subtle.encrypt(
        {
          name: "AES-GCM",
          iv: nonceBytes,
          additionalData: textEncoder.encode(context),
          tagLength: 128,
        },
        derivedKey,
        textEncoder.encode(plaintext),
      );
    })
    .then((cipherBuffer) => {
      const ciphertextBytes = new Uint8Array(cipherBuffer);
      onResult(
        "SUCCESS",
        bytesToBase64Url(saltBytes),
        bytesToBase64Url(nonceBytes),
        bytesToBase64Url(ciphertextBytes),
        null,
      );
    })
    .catch((error: unknown) => {
      const name = (error instanceof Error && error.name) || "CryptoError";
      onResult("FAILED", null, null, null, name);
    });
}

export function decryptPasskeyWithWebCrypto(
  saltBase64Url: string,
  nonceBase64Url: string,
  ciphertextBase64Url: string,
  prfFirstOutputBase64Url: string,
  context: string,
  onResult: (status: string, plaintext: string | null, error: string | null) => void,
): void {
  if (
    typeof crypto === "undefined" ||
    crypto == null ||
    typeof crypto.subtle === "undefined" ||
    crypto.subtle == null
  ) {
    onResult("UNAVAILABLE", null, "WebCryptoUnavailable");
    return;
  }

  let saltBytes: Uint8Array;
  let nonceBytes: Uint8Array;
  let ciphertextBytes: Uint8Array;
  try {
    saltBytes = base64UrlToBytes(saltBase64Url);
    nonceBytes = base64UrlToBytes(nonceBase64Url);
    ciphertextBytes = base64UrlToBytes(ciphertextBase64Url);
  } catch (_) {
    onResult("FAILED", null, "DecodeError");
    return;
  }

  deriveAesGcmKey(saltBytes, prfFirstOutputBase64Url, context)
    .then((derivedKey) => {
      return crypto.subtle.decrypt(
        {
          name: "AES-GCM",
          iv: nonceBytes,
          additionalData: textEncoder.encode(context),
          tagLength: 128,
        },
        derivedKey,
        ciphertextBytes,
      );
    })
    .then((plaintextBuffer) => {
      const plaintext = textDecoder.decode(new Uint8Array(plaintextBuffer));
      onResult("SUCCESS", plaintext, null);
    })
    .catch((error: unknown) => {
      const name = (error instanceof Error && error.name) || "CryptoError";
      onResult("FAILED", null, name);
    });
}
