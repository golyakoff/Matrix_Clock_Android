#!/usr/bin/env bash
#
# Publishes a signed APK to RuStore via the public API.
# https://www.rustore.ru/help/work-with-rustore-api
#
# Required env:
#   RUSTORE_KEY_ID       key identifier from RuStore Console
#   RUSTORE_PRIVATE_KEY  base64 PKCS#8 private key from RuStore Console
#   PACKAGE_NAME         application id, e.g. net.agolyakov.tetrisclockble
#   APK_PATH             path to the signed release APK
# Optional env:
#   WHATS_NEW            release notes shown in the store
#
set -euo pipefail

API="https://public-api.rustore.ru"

: "${RUSTORE_KEY_ID:?}"
: "${RUSTORE_PRIVATE_KEY:?}"
: "${PACKAGE_NAME:?}"
: "${APK_PATH:?}"
WHATS_NEW="${WHATS_NEW:-}"

[ -f "$APK_PATH" ] || { echo "APK not found: $APK_PATH" >&2; exit 1; }

# Accept the RuStore private key in either form:
#  - a full PEM block (-----BEGIN [RSA] PRIVATE KEY----- ... -----END-----), which is the natural
#    thing to paste into a secret: used as-is, only normalizing CRLF and any literal "\n"; or
#  - the bare base64 PKCS#8 DER the console shows (no headers): wrapped in PEM framing.
# The old code always did the second, so a PEM-in-secret got its headers mangled by `tr -d`, which
# is the "Could not read private key" openssl failure.
KEY_PEM="$(mktemp)"
trap 'rm -f "$KEY_PEM"' EXIT
if printf '%s' "$RUSTORE_PRIVATE_KEY" | grep -q -- "-----BEGIN"; then
  printf '%s' "$RUSTORE_PRIVATE_KEY" | sed 's/\\n/\n/g' | tr -d '\r' > "$KEY_PEM"
else
  {
    echo "-----BEGIN PRIVATE KEY-----"
    printf '%s' "$RUSTORE_PRIVATE_KEY" | tr -d ' \t\n\r' | fold -w 64
    echo "-----END PRIVATE KEY-----"
  } > "$KEY_PEM"
fi

# Fail early with an actionable message instead of the opaque openssl error from the signing step.
if ! openssl pkey -in "$KEY_PEM" -noout 2>/dev/null; then
  echo "RuStore private key could not be parsed from the RUSTORE_..._PRIVATE_KEY secret." >&2
  echo "Store either a full PEM private key block, or the bare base64 PKCS#8 key from the RuStore console." >&2
  exit 1
fi

# --- 1. auth: signature is SHA512withRSA over (keyId + timestamp), base64 ---
TIMESTAMP="$(date +%Y-%m-%dT%H:%M:%S.%3N%:z)"
SIGNATURE="$(printf '%s' "${RUSTORE_KEY_ID}${TIMESTAMP}" \
  | openssl dgst -sha512 -sign "$KEY_PEM" -binary \
  | base64 -w0)"

AUTH_RESPONSE="$(curl -sS -X POST "$API/public/auth/" \
  -H 'Content-Type: application/json' \
  -d "$(jq -nc --arg k "$RUSTORE_KEY_ID" --arg t "$TIMESTAMP" --arg s "$SIGNATURE" \
        '{keyId:$k, timestamp:$t, signature:$s}')")"

if [ "$(jq -r '.code' <<<"$AUTH_RESPONSE")" != "OK" ]; then
  echo "RuStore auth failed: $(jq -r '.message // .' <<<"$AUTH_RESPONSE")" >&2
  exit 1
fi
TOKEN="$(jq -r '.body.jwe' <<<"$AUTH_RESPONSE")"
echo "::add-mask::$TOKEN"
echo "Authenticated with RuStore."

# Every later call returns the same {code,message,...} envelope.
check() { # <step name> <response>
  if [ "$(jq -r '.code' <<<"$2")" != "OK" ]; then
    echo "RuStore $1 failed: $(jq -r '.message // .' <<<"$2")" >&2
    exit 1
  fi
}

# --- 2. create draft version (only one draft per app is allowed) ---
DRAFT_BODY="$(jq -nc --arg w "$WHATS_NEW" 'if $w == "" then {} else {whatsNew:$w} end')"
DRAFT_RESPONSE="$(curl -sS -X POST "$API/public/v1/application/$PACKAGE_NAME/version" \
  -H 'Content-Type: application/json' \
  -H "Public-Token: $TOKEN" \
  -d "$DRAFT_BODY")"
check "draft creation" "$DRAFT_RESPONSE"
VERSION_ID="$(jq -r '.body' <<<"$DRAFT_RESPONSE")"
echo "Created draft version $VERSION_ID."

# --- 3. upload the APK ---
UPLOAD_RESPONSE="$(curl -sS -X POST \
  "$API/public/v1/application/$PACKAGE_NAME/version/$VERSION_ID/apk?isMainApk=true&servicesType=Unknown" \
  -H "Public-Token: $TOKEN" \
  -F "file=@$APK_PATH")"
check "APK upload" "$UPLOAD_RESPONSE"
echo "Uploaded $(basename "$APK_PATH")."

# --- 4. send for moderation ---
COMMIT_RESPONSE="$(curl -sS -X POST \
  "$API/public/v1/application/$PACKAGE_NAME/version/$VERSION_ID/commit?priorityUpdate=0" \
  -H "Public-Token: $TOKEN" \
  -d '')"
check "commit" "$COMMIT_RESPONSE"
echo "Version $VERSION_ID submitted for moderation."
