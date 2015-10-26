FROM websphere-liberty:latest

MAINTAINER Ben Smith

RUN rm -rf /opt/ibm/wlp/usr/servers/defaultServer
ADD ./servers/gameon-player /opt/ibm/wlp/usr/servers/
COPY ./docker-env.xml /opt/ibm/wlp/usr/servers/gameon-player/configDropins/

RUN /opt/ibm/wlp/bin/featureManager install mongodb-2.0 --acceptLicense

CMD ["/opt/ibm/wlp/bin/server", "run", "gameon-player"]