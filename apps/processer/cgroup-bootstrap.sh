#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${1:-mediaprocessing}"
SLICE="${NAMESPACE}.slice"
SLICE_PATH="/etc/systemd/system/${SLICE}"

echo "== Installing systemd slice: $SLICE =="

# Create slice unit if missing
if [[ ! -f "$SLICE_PATH" ]]; then
  cat > "$SLICE_PATH" <<EOF
[Unit]
Description=${NAMESPACE} workload slice

[Slice]
CPUAccounting=true
MemoryAccounting=true
IOAccounting=true
TasksAccounting=true

Delegate=yes
EOF
  echo "[OK] Created slice unit"
else
  echo "[OK] Slice unit already exists"
fi

# Reload systemd units
systemctl daemon-reload

# Start slice (idempotent)
systemctl start "$SLICE" || true

# Find actual cgroup path
CGROUP_PATH="$(systemctl show -p ControlGroup --value "$SLICE")"
FULL="/sys/fs/cgroup${CGROUP_PATH}"

echo "[OK] Slice cgroup: $FULL"

echo
echo "[DONE] Governor root ready at: $FULL"
echo "Use this in docker-compose:"
echo "  cgroup_parent: ${SLICE}"
echo
echo "Mount this into the container:"
echo "  - ${FULL}:/cgroup"
