# Bruk en passende baseimage for Python-applikasjonen din
FROM python:3.8-slim-buster

# Sett arbeidskatalogen til /app
WORKDIR /app

# Kopier requirements.txt-filen og installer avhengigheter
COPY ./requirements.txt requirements.txt
RUN pip3 install -r requirements.txt

# Kopier resten av prosjektfilene
COPY . .

# Start applikasjonen
CMD ["python3", "-u", "app.py"]
