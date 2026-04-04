#!/usr/bin/env bash
set -euo pipefail

CGROUP_ROOT="/sys/fs/cgroup"
BASE_GROUP="$CGROUP_ROOT/mediaprocessing"

OWNER_UID="${1:-1000}"
OWNER_GID="${2:-1000}"

# --- Require root ------------------------------------------------------------

if [ "$(id -u)" -ne 0 ]; then
  echo "ERROR: Must run as root."
  exit 1
fi

echo "Setting up base cgroup: $BASE_GROUP"
echo "Target owner UID:GID = ${OWNER_UID}:${OWNER_GID}"

# --- Validate cgroup v2 ------------------------------------------------------

if [ ! -f "$CGROUP_ROOT/cgroup.controllers" ]; then
  echo "ERROR: cgroup v2 not detected at $CGROUP_ROOT."
  exit 1
fi

echo "Available controllers: $(cat "$CGROUP_ROOT/cgroup.controllers")"

# --- Create base group -------------------------------------------------------

if [ ! -d "$BASE_GROUP" ]; then
  echo "Creating base cgroup directory: $BASE_GROUP"
  mkdir -p "$BASE_GROUP"
else
  echo "Base cgroup directory already exists: $BASE_GROUP"
fi

# --- Enable controllers on BASE group (NOT root) -----------------------------

echo "Configuring subtree_control for mediaprocessing..."

CURRENT_SC="$(cat "$BASE_GROUP/cgroup.subtree_control" 2>/dev/null || true)"
ENABLE=""

[[ "$CURRENT_SC" != *"cpu"* ]] && ENABLE="$ENABLE +cpu"
[[ "$CURRENT_SC" != *"cpuset"* ]] && ENABLE="$ENABLE +cpuset"

if [ -n "$ENABLE" ]; then
  if echo "$ENABLE" > "$BASE_GROUP/cgroup.subtree_control" 2>/dev/null; then
    echo "Enabled controllers on mediaprocessing: $ENABLE"
  else
    echo "ERROR: Cannot enable controllers on $BASE_GROUP/cgroup.subtree_control"
    echo "Likely missing delegation or cpuset/cpu not available."
    exit 1
  fi
else
  echo "Controllers already enabled on mediaprocessing."
fi

echo "mediaprocessing subtree_control: $(cat "$BASE_GROUP/cgroup.subtree_control" 2>/dev/null || echo '<none>')"

# --- Initialize cpuset on BASE group (inherit from root if empty) -----------

if [ -f "$CGROUP_ROOT/cpuset.mems" ]; then
  ROOT_MEMS="$(cat "$CGROUP_ROOT/cpuset.mems")"
  BASE_MEMS="$(cat "$BASE_GROUP/cpuset.mems" 2>/dev/null || true)"

  if [ -z "$BASE_MEMS" ]; then
    echo "Initializing $BASE_GROUP/cpuset.mems = $ROOT_MEMS"
    echo "$ROOT_MEMS" > "$BASE_GROUP/cpuset.mems"
  else
    echo "$BASE_GROUP/cpuset.mems already set: $BASE_MEMS"
  fi
fi

if [ -f "$CGROUP_ROOT/cpuset.cpus" ]; then
  ROOT_CPUS="$(cat "$CGROUP_ROOT/cpuset.cpus")"
  BASE_CPUS="$(cat "$BASE_GROUP/cpuset.cpus" 2>/dev/null || true)"

  if [ -z "$BASE_CPUS" ]; then
    echo "Initializing $BASE_GROUP/cpuset.cpus = $ROOT_CPUS"
    echo "$ROOT_CPUS" > "$BASE_GROUP/cpuset.cpus"
  else
    echo "$BASE_GROUP/cpuset.cpus already set: $BASE_CPUS"
  fi
fi

# --- Ownership ---------------------------------------------------------------

CURRENT_OWNER="$(stat -c "%u:%g" "$BASE_GROUP")"
TARGET_OWNER="${OWNER_UID}:${OWNER_GID}"

if [ "$CURRENT_OWNER" != "$TARGET_OWNER" ]; then
  echo "Updating ownership of $BASE_GROUP to $TARGET_OWNER (was $CURRENT_OWNER)..."
  chown "$OWNER_UID:$OWNER_GID" "$BASE_GROUP"
else
  echo "Ownership already correct ($TARGET_OWNER)."
fi

# --- Final status ------------------------------------------------------------

echo "READY: mediaprocessing base cgroup initialized."
echo "  path: $BASE_GROUP"
echo "  subtree_control: $(cat "$BASE_GROUP/cgroup.subtree_control" 2>/dev/null || echo '<none>')"
echo "  cpuset.mems: $(cat "$BASE_GROUP/cpuset.mems" 2>/dev/null || echo '<none>')"
echo "  cpuset.cpus: $(cat "$BASE_GROUP/cpuset.cpus" 2>/dev/null || echo '<none>')"
