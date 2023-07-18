FROM bskjon/azuljava:17
EXPOSE 8080

RUN mkdir -p /src/input
RUN apt update -y
RUN apt install -y libav-tools

COPY ./build/libs/reader.jar /usr/share/app/app.jar