# Processer – Cgroup Delegation for CPU Limiting

The Processer module supports CPU limiting and kernel‑level resource control using cgroup v2.
To make this work safely and predictably inside Docker, a few explicit preparations must be done on the host.

This is required because:

- Docker does not create cgroup directories automatically
- The container must only access its own delegated cgroup subtree
- The container must not see or affect host cgroups
- We want no --privileged, no --cap-add=SYS_ADMIN, and minimal exposure

Below is the correct and recommended setup.

---

## 1. Create a delegated cgroup subtree on the host

This script creates a dedicated cgroup directory for the Processer module and assigns ownership to the user running the container (typically UID 1000):

<!-- GOVENOR_SH_START -->
```bash
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

```
<!-- GOVENOR_SH_END -->

You can copy the script manually, or run the latest version directly from the repository:

```bash
bash -c "$(wget -qO- https://raw.githubusercontent.com/iktdev-no/MediaProcessing/refs/heads/v5/apps/processer/install.sh)"
```

---

## 2. Docker Compose – mount the delegated cgroup and use a private namespace

For safe CPU limiting, the Processer container must:

- mount only the delegated cgroup subtree
- use cgroupns: private so it cannot see host cgroups
- run as the same UID/GID that owns the delegated cgroup

Example:

```yaml
  processer:
    hostname: processer
    restart: always
    container_name: mediaprocessing.processerV5
    image: bskjon/mediaprocessing-processer:v5
    networks:
      - mediaprocessing
      - services_service
    ports:
      - "192.168.2.250:6082:8080"
    environment:
      TZ: ${TIME_ZONE}
      DATABASE_NAME: ${DATABASE_NAME}
      DATABASE_ADDRESS: ${DATABASE_ADDRESS}
      DATABASE_PORT: ${DATABASE_PORT}
      DATABASE_USERNAME: ${DATABASE_USERNAME}
      DATABASE_PASSWORD: ${DATABASE_PASSWORD}
      #FullLogging: true
    volumes:
      - ${MEDIA_INBOX}:${CONTAINER_INBOX}
      - ${MEDIA_OUTBOX}:${CONTAINER_OUTBOX}
      - ${MEDIA_SCRATCH}:${CONTAINER_SCRATCH}
      - ${MEDIA_INTERMEDIATE}:${CONTAINER_INTERMEDIATE}
      - ./data/processer/config:/data/config/
      - ./data/processer/logs:/data/logs
      - /sys/fs/cgroup/mediaprocessing.slice:/cgroup:rw
    #      - ./docker-entrypoint.d/:/docker-entrypoint.d/
    security_opt:
      - no-new-privileges:true
    cgroup_parent: mediaprocessing.slice
    cgroup: host
    healthcheck:
      test: [ "CMD", "curl", "-f", "http://localhost:8080/system/ready" ]
      interval: 10s
      timeout: 3s
      retries: 20
      start_period: 30s
  #    depends_on:
  #      coordinator:
  #        condition: service_healthy

```

---

## 3. Why this is required

This setup ensures:

- The Processer module can safely limit FFmpeg CPU usage
- The container has full control over its own cgroup subtree
- The container cannot affect host cgroups
- No --privileged or --cap-add=SYS_ADMIN is needed
- Minimal exposure of host resources
- A deterministic and isolated cgroup environment

---

## 4. Summary

This setup is mandatory for safe and correct CPU limiting. It provides:

- correct cgroup delegation
- proper isolation
- strong security
- correct namespace configuration
- safe access for the limiter

This is the professional way to implement cgroup‑based CPU control in Docker.
