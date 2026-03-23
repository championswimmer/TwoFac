type WebAuthnOperationStatus = "SUCCESS" | "CANCELLED" | "UNAVAILABLE" | "FAILED";

type WebAuthnCapabilitiesResult = {
  publicKeyCredentialAvailable: boolean;
  userVerifyingAuthenticatorAvailable: boolean;
  clientCapabilitiesAvailable: boolean;
  prfSupported: boolean;
};

type WebAuthnOperationResult = {
  status: WebAuthnOperationStatus;
  credentialId: string | null;
  extensionResults: string | null;
  prfFirstOutputBase64Url: string | null;
  message: string | null;
};

const PRF_SALT = new Uint8Array([116, 119, 111, 102, 97, 99]);

const bytesToBase64Url = (bytes: Uint8Array): string => {
  let binary = "";
  for (let index = 0; index < bytes.length; index++) {
    binary += String.fromCharCode(bytes[index]);
  }
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
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

const handleError = (
  error: unknown,
  credentialId: string | null,
  unavailableNames: string[],
): WebAuthnOperationResult => {
  const name = typeof error === "object" && error && "name" in error ? String((error as { name: unknown }).name) : "UnknownError";
  if (name === "AbortError" || name === "NotAllowedError") {
    return { status: "CANCELLED", credentialId, extensionResults: null, prfFirstOutputBase64Url: null, message: name };
  }
  if (unavailableNames.includes(name)) {
    return { status: "UNAVAILABLE", credentialId, extensionResults: null, prfFirstOutputBase64Url: null, message: name };
  }
  return { status: "FAILED", credentialId, extensionResults: null, prfFirstOutputBase64Url: null, message: name };
};

export const isWebAuthnSupported = (): boolean => {
  return (
    typeof window !== "undefined" &&
    window.isSecureContext === true &&
    typeof window.PublicKeyCredential !== "undefined" &&
    typeof navigator !== "undefined" &&
    navigator.credentials != null
  );
};

export const queryWebAuthnCapabilities = async (): Promise<WebAuthnCapabilitiesResult> => {
  const hasPublicKeyCredential = typeof window !== "undefined" && typeof window.PublicKeyCredential !== "undefined";
  if (!hasPublicKeyCredential) {
    return {
      publicKeyCredentialAvailable: false,
      userVerifyingAuthenticatorAvailable: false,
      clientCapabilitiesAvailable: false,
      prfSupported: false,
    };
  }

  const publicKeyCredential = window.PublicKeyCredential as typeof PublicKeyCredential & {
    getClientCapabilities?: () => Promise<{ prf?: boolean }> | { prf?: boolean };
  };
  const hasClientCapabilities = typeof publicKeyCredential.getClientCapabilities === "function";

  let supportsPrf = false;
  if (hasClientCapabilities) {
    try {
      const maybeCapabilities = (publicKeyCredential as { getClientCapabilities?: () => unknown }).getClientCapabilities?.();
      const capabilities =
        maybeCapabilities && typeof (maybeCapabilities as Promise<{ prf?: boolean }>).then === "function"
          ? await (maybeCapabilities as Promise<{ prf?: boolean }>)
          : maybeCapabilities;
      supportsPrf = !!(capabilities as { prf?: boolean } | undefined)?.prf;
    } catch {
      supportsPrf = false;
    }
  }

  if (typeof publicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable !== "function") {
    return {
      publicKeyCredentialAvailable: true,
      userVerifyingAuthenticatorAvailable: false,
      clientCapabilitiesAvailable: hasClientCapabilities,
      prfSupported: supportsPrf,
    };
  }

  try {
    const isAvailable = await publicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable();
    return {
      publicKeyCredentialAvailable: true,
      userVerifyingAuthenticatorAvailable: !!isAvailable,
      clientCapabilitiesAvailable: hasClientCapabilities,
      prfSupported: supportsPrf,
    };
  } catch {
    return {
      publicKeyCredentialAvailable: true,
      userVerifyingAuthenticatorAvailable: false,
      clientCapabilitiesAvailable: hasClientCapabilities,
      prfSupported: supportsPrf,
    };
  }
};

export const createWebAuthnCredential = async (): Promise<WebAuthnOperationResult> => {
  if (!isWebAuthnSupported()) {
    return {
      status: "UNAVAILABLE",
      credentialId: null,
      extensionResults: null,
      prfFirstOutputBase64Url: null,
      message: "WebAuthnUnavailable",
    };
  }

  try {
    const challenge = new Uint8Array(32);
    const userId = new Uint8Array(16);
    crypto.getRandomValues(challenge);
    crypto.getRandomValues(userId);

    const options: CredentialCreationOptions = {
      publicKey: {
        challenge,
        rp: { name: "TwoFac" },
        user: {
          id: userId,
          name: "twofac-user",
          displayName: "TwoFac User",
        },
        pubKeyCredParams: [
          { type: "public-key", alg: -7 },
          { type: "public-key", alg: -257 },
        ],
        authenticatorSelection: {
          userVerification: "required",
        },
        timeout: 60000,
        extensions: {
          prf: {
            eval: {
              first: PRF_SALT,
            },
          },
        },
      },
    };

    const credential = await navigator.credentials.create(options);
    const credentialRawId = credential && "rawId" in credential && credential.rawId ? new Uint8Array(credential.rawId as ArrayBuffer) : null;
    const credentialId = credentialRawId ? bytesToBase64Url(credentialRawId) : null;
    const extensionResultObject =
      credential && "getClientExtensionResults" in credential && typeof credential.getClientExtensionResults === "function"
        ? credential.getClientExtensionResults()
        : null;
    const extensionResults = extensionResultObject ? JSON.stringify(extensionResultObject) : null;

    let prfFirstOutputBase64Url: string | null = null;
    try {
      const prfFirstOutput = (extensionResultObject as { prf?: { results?: { first?: ArrayBuffer | ArrayBufferView } } } | null)?.prf?.results?.first;
      if (prfFirstOutput instanceof ArrayBuffer) {
        prfFirstOutputBase64Url = bytesToBase64Url(new Uint8Array(prfFirstOutput));
      } else if (typeof ArrayBuffer !== "undefined" && ArrayBuffer.isView(prfFirstOutput)) {
        const view = prfFirstOutput;
        prfFirstOutputBase64Url = bytesToBase64Url(new Uint8Array(view.buffer, view.byteOffset, view.byteLength));
      }
    } catch {
      prfFirstOutputBase64Url = null;
    }

    return {
      status: "SUCCESS",
      credentialId,
      extensionResults,
      prfFirstOutputBase64Url,
      message: null,
    };
  } catch (error) {
    return handleError(error, null, ["NotSupportedError", "SecurityError", "InvalidStateError"]);
  }
};

export const authenticateWebAuthnCredential = async (credentialId: string | null): Promise<WebAuthnOperationResult> => {
  if (!isWebAuthnSupported()) {
    return {
      status: "UNAVAILABLE",
      credentialId,
      extensionResults: null,
      prfFirstOutputBase64Url: null,
      message: "WebAuthnUnavailable",
    };
  }

  try {
    const challenge = new Uint8Array(32);
    crypto.getRandomValues(challenge);

    const publicKey: PublicKeyCredentialRequestOptions = {
      challenge,
      userVerification: "required",
      timeout: 60000,
      extensions: {
        prf: {
          eval: {
            first: PRF_SALT,
          },
        },
      },
    };

    if (credentialId) {
      try {
        publicKey.allowCredentials = [{ type: "public-key", id: base64UrlToBytes(credentialId) as unknown as BufferSource }];
      } catch {
        // Invalid credential ID format should not break the unlock attempt.
      }
    }

    const assertion = await navigator.credentials.get({ publicKey });
    const extensionResultObject =
      assertion && "getClientExtensionResults" in assertion && typeof assertion.getClientExtensionResults === "function"
        ? assertion.getClientExtensionResults()
        : null;
    const extensionResults = extensionResultObject ? JSON.stringify(extensionResultObject) : null;

    let prfFirstOutputBase64Url: string | null = null;
    try {
      const prfFirstOutput = (extensionResultObject as { prf?: { results?: { first?: ArrayBuffer | ArrayBufferView } } } | null)?.prf?.results?.first;
      if (prfFirstOutput instanceof ArrayBuffer) {
        prfFirstOutputBase64Url = bytesToBase64Url(new Uint8Array(prfFirstOutput));
      } else if (typeof ArrayBuffer !== "undefined" && ArrayBuffer.isView(prfFirstOutput)) {
        const view = prfFirstOutput;
        prfFirstOutputBase64Url = bytesToBase64Url(new Uint8Array(view.buffer, view.byteOffset, view.byteLength));
      }
    } catch {
      prfFirstOutputBase64Url = null;
    }

    return {
      status: "SUCCESS",
      credentialId,
      extensionResults,
      prfFirstOutputBase64Url,
      message: null,
    };
  } catch (error) {
    return handleError(error, credentialId, ["NotSupportedError", "SecurityError"]);
  }
};
