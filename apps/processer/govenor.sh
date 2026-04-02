#!/usr/bin/env bash
set -euo pipefail

CGROUP_ROOT="/sys/fs/cgroup"
MEDIAGROUP="$CGROUP_ROOT/mediaprocessing"

OWNER_UID="${1:-1000}"
OWNER_GID="${2:-1000}"

# Use sudo only if not running as root
SUDO=""
if [ "$(id -u)" -ne 0 ]; then
  SUDO="sudo"
fi

echo "Using owner UID:GID = ${OWNER_UID}:${OWNER_GID}"
echo "Ensuring delegated cgroup exists at: ${MEDIAGROUP}"

# --- Validate environment ----------------------------------------------------

if [ ! -d "$CGROUP_ROOT" ]; then
  echo "ERROR: $CGROUP_ROOT does not exist. Are you on cgroup v2?"
  exit 1
fi

if [ ! -f "$CGROUP_ROOT/cgroup.controllers" ]; then
  echo "ERROR: $CGROUP_ROOT/cgroup.controllers missing. Not a unified cgroup v2 mount?"
  exit 1
fi

# --- Create directory if missing --------------------------------------------

if [ ! -d "$MEDIAGROUP" ]; then
  echo "Creating cgroup directory..."
  $SUDO mkdir "$MEDIAGROUP"
else
  echo "Cgroup directory already exists."
fi

# --- Ensure ownership --------------------------------------------------------

CURRENT_OWNER="$(stat -c "%u:%g" "$MEDIAGROUP")"
TARGET_OWNER="${OWNER_UID}:${OWNER_GID}"

if [ "$CURRENT_OWNER" != "$TARGET_OWNER" ]; then
  echo "Updating ownership to $TARGET_OWNER (was $CURRENT_OWNER)..."
  $SUDO chown -R "$OWNER_UID:$OWNER_GID" "$MEDIAGROUP"
else
  echo "Ownership already correct ($TARGET_OWNER)."
fi

# --- Final status ------------------------------------------------------------

echo "Delegated cgroup ready at $MEDIAGROUP"
echo "Available controllers: $(cat "$CGROUP_ROOT/cgroup.controllers")"