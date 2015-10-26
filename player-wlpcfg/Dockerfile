FROM websphere-liberty:latest

MAINTAINER Ben Smith

ADD ./servers/gameon-player /opt/ibm/wlp/usr/servers/defaultServer/
COPY ./docker-env.xml /opt/ibm/wlp/usr/servers/defaultServer/configDropins/overrides/
COPY ./oauth-credentials.xml /opt/ibm/wlp/usr/servers/defaultServer/configDropins/overrides/

#RUN /opt/ibm/wlp/bin/featureManager install mongodb-2.0 --acceptLicense
ADD https://oss.sonatype.org/content/repositories/releases/org/mongodb/mongo-java-driver/2.13.3/mongo-java-driver-2.13.3.jar \
	/opt/ibm/wlp/usr/servers/defaultServer/lib/

CMD ["/opt/ibm/wlp/bin/server", "run", "defaultServer"]