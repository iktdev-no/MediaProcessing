# Processer – Cgroup Delegation for CPU Limiting

Prosesser‑modulen støtter CPU‑begrensning og kjernetruffe ressurskontroller via cgroup v2.
For at dette skal fungere sikkert og forutsigbart i Docker, må vi gjøre noen eksplisitte forberedelser på hosten.

Dette er nødvendig fordi:
- Docker oppretter ikke cgroup‑mapper automatisk
- Containeren skal kun ha tilgang til sin egen cgroup‑gren
- Containeren skal ikke ha tilgang til hostens cgroups
- Vi ønsker ingen privileged‑flagg, ingen SYS_ADMIN, og minimal eksponering

Nedenfor følger den korrekte og anbefalte måten å sette opp dette på.

---

## 1. Opprett en delegert cgroup‑gren på hosten

Dette scriptet oppretter en egen cgroup‑mappe for prosesser‑modulen og gir eierskap til brukeren som kjører containeren (typisk UID 1000):

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

Du kan kopiere scriptet over manuelt, eller kjøre siste versjon direkte fra repoet:
```bash
bash -c "$(wget -qO- https://raw.githubusercontent.com/iktdev-no/MediaProcessing/refs/heads/v5/apps/processer/governor.sh)"
```


---

## 2. Docker Compose – monter delegert cgroup og bruk privat namespace
For at prosesser‑modulen skal kunne styre CPU‑begrensning trygt, må vi:
- montere kun den delegert cgroup‑grenen
- bruke cgroupns: private slik at containeren ikke ser hostens cgroups
- kjøre containeren som samme UID/GID som eier cgroup‑grenen

Eksempel:

In addition to create a cgroup folder, we will also need to mount this into the docker container.
An example will be given of a docker-compose service definition for the processer, which includes the necessary cgroup mount and configuration to use it.
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
      - /sys/fs/cgroup:/sys/fs/cgroup:ro

      - ./data/processer/logs:/data/logs

    # Viktig: isolerer containerens cgroup-namespace
    # Noen IDEer kan gi en falsk advarsel om at nøkkelen ikke er kjent.
    # Docker støtter dette fullt ut.
    cgroupns: host

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

## 3. Hvorfor dette er nødvendig
Dette oppsettet sikrer at:
Prosesser‑modulen kan CPU‑begrense FFmpeg‑jobber

Den får full kontroll over sin egen cgroup‑gren.
Containeren kan ikke påvirke hostens cgroups

Den ser kun det du mountet inn.
Ingen --privileged eller --cap-add=SYS_ADMIN

Dette er ikke bare unødvendig — det er en sikkerhetsrisiko.
Minimal eksponering

Containeren får akkurat det den trenger, og ingenting mer.
Forutsigbar og trygg drift

Cgroup‑miljøet er deterministisk og isolert.

---

## 4. Oppsummering

For prosesser‑modulen er dette oppsettet obligatorisk for at CPU‑limiting skal fungere sikkert og korrekt.
Det gir:
- riktig cgroup‑delegasjon
- riktig isolasjon
- riktig sikkerhetsnivå
- riktig namespace‑oppsett
- riktig tilgang for limiteren

Dette er den profesjonelle måten å gjøre cgroup‑basert CPU‑styring i Docker.