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

<!-- BEGIN:setup-script -->
```bash
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
```
<!-- END:setup-script -->

You can copy the script manually, or run the latest version directly from the repository:

```bash
bash -c "$(wget -qO- https://raw.githubusercontent.com/iktdev-no/MediaProcessing/refs/heads/v5/apps/processer/govenor.sh)"
```

---

## 2. Docker Compose – mount the delegated cgroup and use a private namespace

For safe CPU limiting, the Processer container must:

- mount only the delegated cgroup subtree
- use cgroupns: private so it cannot see host cgroups
- run as the same UID/GID that owns the delegated cgroup

Example:

```yaml
services:
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

    volumes:
      - ${MEDIA_INBOX}:${CONTAINER_INBOX}
      - ${MEDIA_OUTBOX}:${CONTAINER_OUTBOX}
      - ${MEDIA_SCRATCH}:${CONTAINER_SCRATCH}
      - ${MEDIA_INTERMEDIATE}:${CONTAINER_INTERMEDIATE}

      # Delegert cgroup mount – dette er kritisk
      - /sys/fs/cgroup/mediaprocessing:/sys/fs/cgroup:rw

      - ./data/processer/logs:/data/logs

    # Viktig: isolerer containerens cgroup-namespace
    # Noen IDEer kan gi en falsk advarsel om at nøkkelen ikke er kjent.
    # Docker støtter dette fullt ut.
    cgroupns: private

    # Må matche UID/GID som eier cgroup-grenen
    user: "1000:1000"

    healthcheck:
      test: [ "CMD", "curl", "-f", "http://localhost:8080/system/ready" ]
      interval: 10s
      timeout: 3s
      retries: 20
      start_period: 30s
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
