const textEncoder = new TextEncoder();
const textDecoder = new TextDecoder();

type EncryptResult = {
  status: "SUCCESS" | "FAILED" | "UNAVAILABLE";
  salt: string | null;
  nonce: string | null;
  ciphertext: string | null;
};

type DecryptResult = {
  status: "SUCCESS" | "FAILED" | "UNAVAILABLE";
  plaintext: string | null;
};

const base64UrlToBytes = (value: string): Uint8Array => {
  const base64 = value.replace(/-/g, "+").replace(/_/g, "/");
  const padded = base64 + "=".repeat((4 - (base64.length % 4)) % 4);
  const binary = atob(padded);
  const bytes = new Uint8Array(binary.length);
  for (let index = 0; index < binary.length; index++) {
    bytes[index] = binary.charCodeAt(index);
  }
  return bytes;
};

const bytesToBase64Url = (bytes: Uint8Array): string => {
  let binary = "";
  for (let index = 0; index < bytes.length; index++) {
    binary += String.fromCharCode(bytes[index]);
  }
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
};

const deriveAesGcmKey = async (
  prfFirstOutputBase64Url: string,
  context: string,
  saltBytes: Uint8Array,
): Promise<CryptoKey> => {
  const inputKeyMaterial = base64UrlToBytes(prfFirstOutputBase64Url);
  const baseKey = await crypto.subtle.importKey("raw", inputKeyMaterial as unknown as BufferSource, "HKDF", false, ["deriveKey"]);
  return crypto.subtle.deriveKey(
    {
      name: "HKDF",
      hash: "SHA-256",
      salt: saltBytes as unknown as BufferSource,
      info: textEncoder.encode(context),
    },
    baseKey,
    { name: "AES-GCM", length: 256 },
    false,
    ["encrypt", "decrypt"],
  );
};

export const encryptPasskeyWithWebCrypto = async (
  plaintext: string,
  prfFirstOutputBase64Url: string,
  context: string,
): Promise<EncryptResult> => {
  if (typeof crypto === "undefined" || crypto == null || typeof crypto.subtle === "undefined" || crypto.subtle == null) {
    return { status: "UNAVAILABLE", salt: null, nonce: null, ciphertext: null };
  }

  try {
    const saltBytes = crypto.getRandomValues(new Uint8Array(16));
    const nonceBytes = crypto.getRandomValues(new Uint8Array(12));
    const derivedKey = await deriveAesGcmKey(prfFirstOutputBase64Url, context, saltBytes);
    const cipherBuffer = await crypto.subtle.encrypt(
      {
        name: "AES-GCM",
        iv: nonceBytes as unknown as BufferSource,
        additionalData: textEncoder.encode(context) as unknown as BufferSource,
        tagLength: 128,
      },
      derivedKey,
      textEncoder.encode(plaintext) as unknown as BufferSource,
    );
    const ciphertextBytes = new Uint8Array(cipherBuffer);
    return {
      status: "SUCCESS",
      salt: bytesToBase64Url(saltBytes),
      nonce: bytesToBase64Url(nonceBytes),
      ciphertext: bytesToBase64Url(ciphertextBytes),
    };
  } catch {
    return { status: "FAILED", salt: null, nonce: null, ciphertext: null };
  }
};

export const decryptPasskeyWithWebCrypto = async (
  saltBase64Url: string,
  nonceBase64Url: string,
  ciphertextBase64Url: string,
  prfFirstOutputBase64Url: string,
  context: string,
): Promise<DecryptResult> => {
  if (typeof crypto === "undefined" || crypto == null || typeof crypto.subtle === "undefined" || crypto.subtle == null) {
    return { status: "UNAVAILABLE", plaintext: null };
  }

  try {
    const saltBytes = base64UrlToBytes(saltBase64Url);
    const nonceBytes = base64UrlToBytes(nonceBase64Url);
    const ciphertextBytes = base64UrlToBytes(ciphertextBase64Url);
    const derivedKey = await deriveAesGcmKey(prfFirstOutputBase64Url, context, saltBytes);
    const plaintextBuffer = await crypto.subtle.decrypt(
      {
        name: "AES-GCM",
        iv: nonceBytes as unknown as BufferSource,
        additionalData: textEncoder.encode(context) as unknown as BufferSource,
        tagLength: 128,
      },
      derivedKey,
      ciphertextBytes as unknown as BufferSource,
    );
    const plaintext = textDecoder.decode(new Uint8Array(plaintextBuffer));
    return { status: "SUCCESS", plaintext };
  } catch {
    return { status: "FAILED", plaintext: null };
  }
};
