FROM postgres:9.4

# Install Java.
RUN \
  apt-get update && apt-get install -y software-properties-common && \
  echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && \
  echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" > /etc/apt/sources.list.d/webupd8team-java.list && \
  echo "deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" >> /etc/apt/sources.list.d/webupd8team-java.list && \
  apt-key adv --keyserver keyserver.ubuntu.com --recv-keys EEA14886 && \
  apt-get update && \
  apt-get install -y oracle-java8-installer && \
  rm -rf /var/lib/apt/lists/* && \
  rm -rf /var/cache/oracle-jdk8-installer

# Define commonly used JAVA_HOME variable
ENV JAVA_HOME /usr/lib/jvm/java-8-oracle

# install deps
RUN echo "deb http://http.debian.net/debian jessie-backports main" >> /etc/apt/sources.list
RUN apt-get update && apt-get install -y maven python-setuptools python-dev build-essential
RUN easy_install pip
RUN easy_install -U setuptools

# build app
COPY dockstore.yml /dockstore.yml
COPY docker-dockstore-entrypoint.sh /docker-dockstore-entrypoint.sh
COPY . /root/
WORKDIR /root
RUN pip install cwl-runner  cwltool==1.0.20160316150250 schema-salad==1.7.20160316150109 avro==1.7.7
# have to build with skip tests since some tests call out to docker which is difficult within docker
RUN mvn clean install -DskipTests
RUN cp dockstore-webservice/target/dockstore-webservice-*.jar /
#RUN wget https://seqwaremaven.oicr.on.ca/artifactory/collab-release/io/dockstore/dockstore-webservice/0.2.1/dockstore-webservice-0.2.1.jar
RUN chmod a+x /docker-dockstore-entrypoint.sh
EXPOSE 8080

# default command launches daemons
CMD /docker-dockstore-entrypoint.sh

