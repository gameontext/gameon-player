FROM gameontext/docker-liberty-custom:master-12

ENV SERVERDIRNAME player

COPY ./startup.sh /opt/startup.sh
ADD ./servers/gameon-player /opt/ol/wlp/usr/servers/defaultServer/

COPY ./player.json /opt/player.json

CMD ["/opt/startup.sh"]

HEALTHCHECK \
  --timeout=10s \
  --start-period=40s \
  CMD wget -q -O /dev/null http://localhost:9080/health
