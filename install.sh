#!/usr/bin/env bash

set -euo pipefail

TWOFAC_REPO="${TWOFAC_REPO:-championswimmer/TwoFac}"
TWOFAC_RELEASE_TAG="${TWOFAC_RELEASE_TAG:-}"
TWOFAC_INSTALL_DIR="${TWOFAC_INSTALL_DIR:-}"

RELEASES_BASE_URL="https://github.com/${TWOFAC_REPO}/releases"
LATEST_RELEASE_URL="${RELEASES_BASE_URL}/latest"
WORK_DIR=""

log() {
  printf 'twofac installer: %s\n' "$*" >&2
}

fail() {
  log "$*"
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "Missing required command: $1"
}

detect_os() {
  case "$(uname -s)" in
    Darwin)
      printf 'macos\n'
      ;;
    Linux)
      printf 'linux\n'
      ;;
    *)
      fail "Unsupported operating system: $(uname -s). Use ${RELEASES_BASE_URL} for manual downloads."
      ;;
  esac
}

detect_arch() {
  case "$(uname -m)" in
    x86_64 | amd64)
      printf 'x64\n'
      ;;
    arm64 | aarch64)
      printf 'arm64\n'
      ;;
    *)
      fail "Unsupported CPU architecture: $(uname -m). Use ${RELEASES_BASE_URL} for manual downloads."
      ;;
  esac
}

asset_name_for_platform() {
  case "$1-$2" in
    macos-arm64)
      printf '2fac-macos-arm64.tar.gz\n'
      ;;
    macos-x64)
      printf '2fac-macos-x64.tar.gz\n'
      ;;
    linux-x64)
      printf '2fac-linux-x64.tar.gz\n'
      ;;
    linux-arm64)
      fail "Linux arm64 releases are not published yet. Use ${RELEASES_BASE_URL} for manual downloads."
      ;;
    *)
      fail "Unsupported platform: $1/$2. Use ${RELEASES_BASE_URL} for manual downloads."
      ;;
  esac
}

resolve_release_tag_from_redirect() {
  local effective_url
  effective_url="$(curl -fsSL -o /dev/null -w '%{url_effective}' "${LATEST_RELEASE_URL}" || true)"

  case "${effective_url}" in
    */releases/tag/*)
      printf '%s\n' "${effective_url##*/}"
      ;;
    *)
      return 1
      ;;
  esac
}

resolve_release_tag_from_api() {
  local api_response
  api_response="$(curl -fsSL -H 'Accept: application/vnd.github+json' "https://api.github.com/repos/${TWOFAC_REPO}/releases/latest" || true)"

  printf '%s\n' "${api_response}" | sed -n 's/.*"tag_name":[[:space:]]*"\([^"]*\)".*/\1/p' | head -n 1
}

resolve_release_tag() {
  local release_tag

  if [ -n "${TWOFAC_RELEASE_TAG}" ]; then
    printf '%s\n' "${TWOFAC_RELEASE_TAG}"
    return 0
  fi

  release_tag="$(resolve_release_tag_from_redirect || true)"
  if [ -n "${release_tag}" ]; then
    printf '%s\n' "${release_tag}"
    return 0
  fi

  release_tag="$(resolve_release_tag_from_api || true)"
  if [ -n "${release_tag}" ]; then
    printf '%s\n' "${release_tag}"
    return 0
  fi

  fail "Could not resolve the latest release tag from GitHub."
}

choose_install_dir() {
  if [ -n "${TWOFAC_INSTALL_DIR}" ]; then
    printf '%s\n' "${TWOFAC_INSTALL_DIR}"
    return 0
  fi

  if [ -d /usr/local/bin ] && [ -w /usr/local/bin ]; then
    printf '/usr/local/bin\n'
    return 0
  fi

  [ -n "${HOME:-}" ] || fail "HOME is not set, so a user install directory could not be determined."
  printf '%s\n' "${HOME}/.local/bin"
}

path_contains_dir() {
  case ":${PATH:-}:" in
    *":$1:"*)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

cleanup() {
  if [ -n "${WORK_DIR:-}" ] && [ -d "${WORK_DIR}" ]; then
    rm -rf "${WORK_DIR}"
  fi
}

main() {
  local os arch asset_name release_tag asset_url install_dir target_path target_tmp
  local archive_path extracted_dir extracted_binary staged_binary

  require_command curl
  require_command tar
  require_command mktemp
  require_command sed

  os="$(detect_os)"
  arch="$(detect_arch)"
  asset_name="$(asset_name_for_platform "${os}" "${arch}")"
  release_tag="$(resolve_release_tag)"
  asset_url="${RELEASES_BASE_URL}/download/${release_tag}/${asset_name}"
  install_dir="$(choose_install_dir)"

  WORK_DIR="$(mktemp -d)"
  trap cleanup EXIT

  archive_path="${WORK_DIR}/${asset_name}"
  extracted_dir="${WORK_DIR}/extract"
  staged_binary="${WORK_DIR}/2fac"

  log "Resolved ${release_tag} for ${os}/${arch}"
  log "Downloading ${asset_name}"

  if ! curl -fsSL "${asset_url}" -o "${archive_path}"; then
    fail "Download failed for ${asset_url}. Visit ${RELEASES_BASE_URL}/tag/${release_tag} for manual downloads."
  fi

  case "${asset_name}" in
    *.tar.gz)
      mkdir -p "${extracted_dir}"
      tar -xzf "${archive_path}" -C "${extracted_dir}"
      extracted_binary="${extracted_dir}/2fac.kexe"
      [ -f "${extracted_binary}" ] || fail "Archive did not contain the expected 2fac.kexe binary."
      ;;
    *)
      extracted_binary="${archive_path}"
      ;;
  esac

  mkdir -p "${install_dir}"
  [ -w "${install_dir}" ] || fail "Install directory is not writable: ${install_dir}. Set TWOFAC_INSTALL_DIR to a writable directory."

  cp "${extracted_binary}" "${staged_binary}"
  chmod +x "${staged_binary}"

  target_path="${install_dir}/2fac"
  target_tmp="${install_dir}/.2fac.tmp.$$"

  cp "${staged_binary}" "${target_tmp}"
  chmod +x "${target_tmp}"
  mv -f "${target_tmp}" "${target_path}"

  printf 'Installed TwoFac CLI %s to %s\n' "${release_tag}" "${target_path}"

  if ! path_contains_dir "${install_dir}"; then
    printf 'Add it to your PATH with:\n'
    printf '  export PATH="%s:$PATH"\n' "${install_dir}"
  fi

  printf 'Run `2fac --help` to get started.\n'
}

main "$@"
