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

Du kan kopiere scriptet over manuelt, eller kjøre siste versjon direkte fra repoet:
```bash
bash -c "$(wget -qO- https://raw.githubusercontent.com/iktdev-no/MediaProcessing/refs/heads/v5/apps/processer/govenor.sh)"
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