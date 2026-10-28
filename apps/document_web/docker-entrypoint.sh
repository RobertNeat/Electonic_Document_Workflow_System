#!/bin/sh
set -eu

: "${KEYCLOAK_PUBLIC_URL:=http://localhost:10681}"
: "${DOCUMENT_API_PUBLIC_URL:=http://localhost:10620}"
export KEYCLOAK_PUBLIC_URL DOCUMENT_API_PUBLIC_URL

envsubst '${KEYCLOAK_PUBLIC_URL} ${DOCUMENT_API_PUBLIC_URL}' \
  < /etc/document-web/config.template.json \
  > /usr/share/nginx/html/config.json
