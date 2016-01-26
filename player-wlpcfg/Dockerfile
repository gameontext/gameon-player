FROM websphere-liberty:latest

MAINTAINER Ben Smith

ADD https://download.elastic.co/logstash-forwarder/binaries/logstash-forwarder_linux_amd64 /opt/forwarder
ADD ./servers/gameon-player /opt/ibm/wlp/usr/servers/defaultServer/
RUN apt-get update; apt-get install -y curl

COPY ./forwarder.conf /opt/forwarder.conf     

COPY ./startup.sh /opt/startup.sh

CMD ["/opt/startup.sh"]
