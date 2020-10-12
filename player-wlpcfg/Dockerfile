FROM gameontext/docker-liberty-custom:master-29

ENV SERVERDIRNAME player

ADD https://raw.githubusercontent.com/gameontext/gameon/master/bin/init_couchdb.sh /opt/init_couchdb.sh
USER 0
RUN chmod g+rwx /opt/init_couchdb.sh
USER 1001
COPY ./startup.sh /opt/startup.sh
COPY ./servers/gameon-player /opt/ol/wlp/usr/servers/defaultServer/

COPY ./player.json /opt/player.json

CMD ["/opt/startup.sh"]

HEALTHCHECK \
  --timeout=10s \
  --start-period=40s \
  CMD wget -q -O /dev/null http://localhost:9080/players/v1/health
