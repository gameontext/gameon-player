#!/bin/bash

if [ "$SSL_CERT" != "" ]; then
  echo Found an SSL cert to use.
  cd /opt/ibm/wlp/usr/servers/defaultServer/resources/
  echo -e $SSL_CERT > cert.pem
  openssl pkcs12 -passin pass:keystore -passout pass:keystore -export -out cert.pkcs12 -in cert.pem
  keytool -import -v -trustcacerts -alias default -file cert.pem -storepass truststore -keypass keystore -noprompt -keystore security/truststore.jks
  keytool -genkey -storepass testOnlyKeystore -keypass wefwef -keyalg RSA -alias endeca -keystore security/key.jks -dname CN=rsssl,OU=unknown,O=unknown,L=unknown,ST=unknown,C=CA
  keytool -delete -storepass testOnlyKeystore -alias endeca -keystore security/key.jks
  keytool -v -importkeystore -srcalias 1 -alias 1 -destalias default -noprompt -srcstorepass keystore -deststorepass testOnlyKeystore -srckeypass keystore -destkeypass testOnlyKeystore -srckeystore cert.pkcs12 -srcstoretype PKCS12 -destkeystore security/key.jks -deststoretype JKS
fi

if [ "$ETCDCTL_ENDPOINT" != "" ]; then
  echo Setting up etcd...
  wget https://github.com/coreos/etcd/releases/download/v2.2.2/etcd-v2.2.2-linux-amd64.tar.gz -q
  tar xzf etcd-v2.2.2-linux-amd64.tar.gz etcd-v2.2.2-linux-amd64/etcdctl --strip-components=1
  rm etcd-v2.2.2-linux-amd64.tar.gz
  mv etcdctl /usr/local/bin/etcdctl
  
  cd /opt/ibm/wlp/usr/servers/defaultServer/resources/
  etcdctl get /proxy/third-party-ssl-cert > cert.pem
  openssl pkcs12 -passin pass:keystore -passout pass:keystore -export -out cert.pkcs12 -in cert.pem
  keytool -import -v -trustcacerts -alias default -file cert.pem -storepass truststore -keypass keystore -noprompt -keystore security/truststore.jks
  keytool -genkey -storepass testOnlyKeystore -keypass wefwef -keyalg RSA -alias endeca -keystore security/key.jks -dname CN=rsssl,OU=unknown,O=unknown,L=unknown,ST=unknown,C=CA
  keytool -delete -storepass testOnlyKeystore -alias endeca -keystore security/key.jks
  keytool -v -importkeystore -srcalias 1 -alias 1 -destalias default -noprompt -srcstorepass keystore -deststorepass testOnlyKeystore -srckeypass keystore -destkeypass testOnlyKeystore -srckeystore cert.pkcs12 -srcstoretype PKCS12 -destkeystore security/key.jks -deststoretype JKS

  export COUCHDB_URL=$(etcdctl get /couchdb/url)
  export COUCHDB_USER=$(etcdctl get /couchdb/user)
  export COUCHDB_PASSWORD=$(etcdctl get /passwords/couchdb)
  export CONCIERGE_URL=$(etcdctl get /concierge/url)
  export CONCIERGE_KEY=$(etcdctl get /passwords/concierge-key)
  export PLAYER_URL=$(etcdctl get /player/url)
  export MONGO_HOST=$(etcdctl get /mongo/host)
  export MONGO_PORT=$(etcdctl get /mongo/port)
  export TWITTER_CONSUMER_KEY=$(etcdctl get /player/twitter/id)
  export TWITTER_CONSUMER_SECRET=$(etcdctl get /player/twitter/secret)
  export FACEBOOK_APP_ID=$(etcdctl get /player/facebook/id)
  export FACEBOOK_APP_SECRET=$(etcdctl get /player/facebook/secret)
  export GOOGLE_APP_ID=$(etcdctl get /player/google/id)
  export GOOGLE_APP_SECRET=$(etcdctl get /player/google/secret)
  export SUCCESS_CALLBACK=$(etcdctl get /player/callback)
  
  /opt/ibm/wlp/bin/server start defaultServer
  echo Starting the logstash forwarder...
  sed -i s/PLACEHOLDER_LOGHOST/$(etcdctl get /logstash/endpoint)/g /opt/forwarder.conf
  cd /opt
  chmod +x ./forwarder
  etcdctl get /logstash/cert > logstash-forwarder.crt
  etcdctl get /logstash/key > logstash-forwarder.key
  sleep 0.5
  ./forwarder --config ./forwarder.conf
else
  # LOCAL DEVELOPMENT!
  # We do not want to ruin the cloudant admin party, but our code is written to expect
  # that creds are required, so we should make sure the required user/password exist
  export AUTH_HOST="http://${COUCHDB_USER}:${COUCHDB_PASSWORD}@couchdb:5984"
  curl --fail -v -X GET ${AUTH_HOST}/_config/admins/${COUCHDB_USER}
  if [ $? -eq 22 ]; then
      curl -X PUT ${COUCHDB_URL}/_config/admins/${COUCHDB_USER} -d \"${COUCHDB_PASSWORD}\"
  fi
  
  echo Have setup couchdb with user ${COUCHDB_USER}

  /opt/ibm/wlp/bin/server run defaultServer
fi
