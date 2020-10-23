#!/bin/bash
export CONTAINER_NAME=player

SERVER_PATH=/opt/ol/wlp/usr/servers/defaultServer
ssl_path=${SERVER_PATH}/resources/security


if [ "$ETCDCTL_ENDPOINT" != "" ]; then
  echo Setting up etcd...
  echo "** Testing etcd is accessible"
  etcdctl --debug ls
  RC=$?

  while [ $RC -ne 0 ]; do
    sleep 15

    # recheck condition
    echo "** Re-testing etcd connection"
    etcdctl --debug ls
    RC=$?
  done
  echo "etcdctl returned sucessfully, continuing"

  etcdctl get /proxy/third-party-ssl-cert > ${ssl_path}/cert.pem

  export MAP_KEY=$(etcdctl get /passwords/map-key)

  export COUCHDB_SERVICE_URL=$(etcdctl get /couchdb/url)
  export COUCHDB_USER=$(etcdctl get /couchdb/user)
  export COUCHDB_PASSWORD=$(etcdctl get /passwords/couchdb)

  export SYSTEM_ID=$(etcdctl get /global/system_id)

  GAMEON_MODE=$(etcdctl get /global/mode)
  export GAMEON_MODE=${GAMEON_MODE:-production}
  export TARGET_PLATFORM=$(etcdctl get /global/targetPlatform)

  export KAFKA_SERVICE_URL=$(etcdctl get /kafka/url)
fi
if [ -f /etc/cert/cert.pem ]; then
  cp /etc/cert/cert.pem ${ssl_path}/cert.pem
fi

# Make sure keystores are present or are generated
/opt/gen-keystore.sh ${ssl_path} ${ssl_path}

# Make sure couchdb / cloudant is around
. /opt/init_couchdb.sh 30

if [ "$GAMEON_MODE" == "development" ]; then
  # LOCAL DEVELOPMENT!
  # We do not want to ruin the cloudant admin party, but our code is written to expect
  # that creds are required, so we should make sure the required user/password exist

  echo "** Checking playerdb"
  ensure_exists playerdb

  echo "** Checking design documents"
  ensure_exists playerdb/_design/players --data-binary @/opt/player.json
else
  activeUrl=${AUTH_URL}
  assert_exists playerdb
fi

exec /opt/ol/wlp/bin/server run defaultServer
