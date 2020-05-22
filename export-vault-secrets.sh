#!/usr/bin/env sh

if test -f /var/run/secrets/nais.io/srvskanmotreferanse/username;
then
    echo "Setting SKANMOTREFERANSENR_SERVICEUSER_USERNAME"
    export SKANMOTREFERANSENR_SERVICEUSER_USERNAME=$(cat /var/run/secrets/nais.io/srvskanmotreferanse/username)
fi
if test -f /var/run/secrets/nais.io/srvskanmotreferanse/password;
then
    echo "Setting SKANMOTREFERANSENR_SERVICEUSER_PASSWORD"
    export SKANMOTREFERANSENR_SERVICEUSER_PASSWORD=$(cat /var/run/secrets/nais.io/srvskanmotreferanse/password)
fi