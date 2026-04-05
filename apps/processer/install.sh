#!/usr/bin/env bash
set -euo pipefail

DEFAULT_NS="mediaprocessing"

# Hvis namespace ble gitt som argument → bruk det
if [[ $# -ge 1 ]]; then
  NAMESPACE="$1"
else
  # Ellers spør brukeren
  read -rp "Namespace [${DEFAULT_NS}]: " INPUT
  NAMESPACE="${INPUT:-$DEFAULT_NS}"
fi

echo "[INFO] Using namespace: $NAMESPACE"

# Last ned installer til /tmp
INSTALLER="/tmp/cgroup-bootstrap.sh"
curl -fsSL "https://raw.githubusercontent.com/iktdev-no/MediaProcessing/refs/heads/v5/apps/processer/cgroup-bootstrap.sh" -o "$INSTALLER"
chmod +x "$INSTALLER"

# Hvis ikke root → kjør installer som sudo
if [[ $EUID -ne 0 ]]; then
  echo "[INFO] Elevating with sudo..."
  exec sudo "$INSTALLER" "$NAMESPACE"
fi

# Hvis root → kjør installer direkte
exec "$INSTALLER" "$NAMESPACE"
