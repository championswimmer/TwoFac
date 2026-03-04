export function isWebAuthnSupported(): boolean {
  return (
    typeof window !== "undefined" &&
    window.isSecureContext === true &&
    typeof window.PublicKeyCredential !== "undefined" &&
    typeof navigator !== "undefined" &&
    navigator.credentials != null
  );
}

export function queryWebAuthnCapabilities(
  onResult: (
    hasPublicKeyCredential: boolean,
    hasUvAuthenticator: boolean,
    hasClientCapabilities: boolean,
    supportsPrf: boolean,
  ) => void,
): void {
  const hasPublicKeyCredential =
    typeof window !== "undefined" &&
    typeof window.PublicKeyCredential !== "undefined";
  if (!hasPublicKeyCredential) {
    onResult(false, false, false, false);
    return;
  }

  const publicKeyCredential = window.PublicKeyCredential;
  const hasClientCapabilities =
    typeof (publicKeyCredential as any).getClientCapabilities === "function";

  const resolveWithPrf = (supportsPrf: boolean) => {
    const hasUvpaCheck =
      typeof publicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable === "function";
    if (!hasUvpaCheck) {
      onResult(true, false, hasClientCapabilities, !!supportsPrf);
      return;
    }

    publicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable()
      .then((isAvailable: boolean) => {
        onResult(true, !!isAvailable, hasClientCapabilities, !!supportsPrf);
      })
      .catch(() => {
        onResult(true, false, hasClientCapabilities, !!supportsPrf);
      });
  };

  if (!hasClientCapabilities) {
    resolveWithPrf(false);
    return;
  }

  try {
    const maybeCapabilities = (publicKeyCredential as any).getClientCapabilities();
    if (maybeCapabilities && typeof maybeCapabilities.then === "function") {
      maybeCapabilities
        .then((capabilities: any) => resolveWithPrf(!!(capabilities && capabilities.prf)))
        .catch(() => resolveWithPrf(false));
    } else {
      resolveWithPrf(!!(maybeCapabilities && maybeCapabilities.prf));
    }
  } catch (_) {
    resolveWithPrf(false);
  }
}

function bytesToBase64Url(bytes: Uint8Array): string {
  let binary = "";
  for (let index = 0; index < bytes.length; index++) {
    binary += String.fromCharCode(bytes[index]);
  }
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

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

export function createWebAuthnCredential(
  onResult: (
    status: string,
    credentialId: string | null,
    extensionResults: string | null,
    prfFirstOutputBase64Url: string | null,
    message: string | null,
  ) => void,
): void {
  const handleError = (error: unknown) => {
    const name = (error instanceof Error && error.name) || "UnknownError";
    if (name === "AbortError" || name === "NotAllowedError") {
      onResult("CANCELLED", null, null, null, name);
      return;
    }
    if (name === "NotSupportedError" || name === "SecurityError" || name === "InvalidStateError") {
      onResult("UNAVAILABLE", null, null, null, name);
      return;
    }
    onResult("FAILED", null, null, null, name);
  };

  if (
    typeof navigator === "undefined" ||
    navigator.credentials == null ||
    typeof window === "undefined" ||
    typeof window.PublicKeyCredential === "undefined" ||
    !window.isSecureContext
  ) {
    onResult("UNAVAILABLE", null, null, null, "WebAuthnUnavailable");
    return;
  }

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
            first: new Uint8Array([116, 119, 111, 102, 97, 99]),
          },
        },
      } as any,
    },
  };

  navigator.credentials.create(options)
    .then((credential) => {
      const pkCredential = credential as PublicKeyCredential | null;
      const credentialRawId = pkCredential && pkCredential.rawId ? new Uint8Array(pkCredential.rawId) : null;
      let credentialId: string | null = null;
      if (credentialRawId != null) {
        credentialId = bytesToBase64Url(credentialRawId);
      }
      const extensionResults =
        pkCredential && typeof pkCredential.getClientExtensionResults === "function"
          ? JSON.stringify(pkCredential.getClientExtensionResults())
          : null;
      onResult("SUCCESS", credentialId, extensionResults, null, null);
    })
    .catch(handleError);
}

export function authenticateWebAuthnCredential(
  credentialId: string | null,
  onResult: (
    status: string,
    returnedCredentialId: string | null,
    extensionResults: string | null,
    prfFirstOutputBase64Url: string | null,
    message: string | null,
  ) => void,
): void {
  const handleError = (error: unknown) => {
    const name = (error instanceof Error && error.name) || "UnknownError";
    if (name === "AbortError" || name === "NotAllowedError") {
      onResult("CANCELLED", credentialId || null, null, null, name);
      return;
    }
    if (name === "NotSupportedError" || name === "SecurityError") {
      onResult("UNAVAILABLE", credentialId || null, null, null, name);
      return;
    }
    onResult("FAILED", credentialId || null, null, null, name);
  };

  if (
    typeof navigator === "undefined" ||
    navigator.credentials == null ||
    typeof window === "undefined" ||
    typeof window.PublicKeyCredential === "undefined" ||
    !window.isSecureContext
  ) {
    onResult("UNAVAILABLE", credentialId || null, null, null, "WebAuthnUnavailable");
    return;
  }

  const challenge = new Uint8Array(32);
  crypto.getRandomValues(challenge);

  const publicKey: PublicKeyCredentialRequestOptions = {
    challenge,
    userVerification: "required",
    timeout: 60000,
    extensions: {
      prf: {
        eval: {
          first: new Uint8Array([116, 119, 111, 102, 97, 99]),
        },
      },
    } as any,
  };

  if (credentialId) {
    try {
      const bytes = base64UrlToBytes(credentialId);
      (publicKey as any).allowCredentials = [{ type: "public-key", id: bytes }];
    } catch (_) {
      // Invalid credential ID format should not break the unlock attempt.
    }
  }

  navigator.credentials.get({ publicKey })
    .then((assertion) => {
      const pkAssertion = assertion as PublicKeyCredential | null;
      const extensionResultObject =
        pkAssertion && typeof pkAssertion.getClientExtensionResults === "function"
          ? pkAssertion.getClientExtensionResults()
          : null;
      const extensionResults = extensionResultObject ? JSON.stringify(extensionResultObject) : null;
      let prfFirstOutputBase64Url: string | null = null;
      try {
        const prfFirstOutput =
          extensionResultObject &&
          (extensionResultObject as any).prf &&
          (extensionResultObject as any).prf.results
            ? (extensionResultObject as any).prf.results.first
            : null;
        if (prfFirstOutput instanceof ArrayBuffer) {
          const prfBytes = new Uint8Array(prfFirstOutput);
          prfFirstOutputBase64Url = bytesToBase64Url(prfBytes);
        } else if (typeof ArrayBuffer !== "undefined" && ArrayBuffer.isView(prfFirstOutput)) {
          const view = prfFirstOutput as ArrayBufferView;
          const prfBytes = new Uint8Array(view.buffer, view.byteOffset, view.byteLength);
          prfFirstOutputBase64Url = bytesToBase64Url(prfBytes);
        }
      } catch (_) {
        prfFirstOutputBase64Url = null;
      }
      onResult("SUCCESS", credentialId || null, extensionResults, prfFirstOutputBase64Url, null);
    })
    .catch(handleError);
}
