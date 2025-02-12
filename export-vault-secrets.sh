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

if test -f /var/run/secrets/nais.io/srvjiradokdistavstemming/username;
then
    echo "Setting skanmotovrig_jira_username"
    export skanmotovrig_jira_username=$(cat /var/run/secrets/nais.io/srvjiradokdistavstemming/username)
fi

if test -f /var/run/secrets/nais.io/srvjiradokdistavstemming/password;
then
    echo "Setting skanmotovrig_jira_password"
    export skanmotovrig_jira_password=$(cat /var/run/secrets/nais.io/srvjiradokdistavstemming/password)
fi