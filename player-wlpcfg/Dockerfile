FROM gameontext/docker-liberty-custom:master-29

ENV SERVERDIRNAME player

ADD https://raw.githubusercontent.com/gameontext/gameon/master/bin/gen-keystore.sh /opt/gen-keystore.sh
ADD https://raw.githubusercontent.com/gameontext/gameon/master/bin/init_couchdb.sh /opt/init_couchdb.sh
USER 0
RUN chmod g+rwx /opt/init_couchdb.sh /opt/gen-keystore.sh
COPY ./servers/gameon-player /opt/ol/wlp/usr/servers/defaultServer/
RUN mkdir -p /opt/ol/wlp/usr/servers/defaultServer/resources/security && chown -R 1001:0 /opt/ol/wlp/usr/servers/defaultServer && chmod -R g+rwx /opt/ol/wlp/usr/servers/defaultServer
USER 1001
COPY ./startup.sh /opt/startup.sh
COPY ./player.json /opt/player.json

CMD ["/opt/startup.sh"]

HEALTHCHECK \
  --timeout=10s \
  --start-period=40s \
  CMD wget -q -O /dev/null http://localhost:9080/players/v1/health
