FROM bskjon/azuljava:17
EXPOSE 8080

ARG MODULE_NAME
COPY ./apps/${MODULE_NAME}/build/libs/app.jar /usr/share/app/app.jar