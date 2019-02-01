FROM tomcat:8.5-jre8-slim

ENV CR_BASE=/var/local/cr
ENV CR_HOME=${CR_BASE}/apphome
ENV MAVEN_VERSION=3.3.9-4

RUN apt-get update \
 && apt-get install -y --no-install-recommends git maven="$MAVEN_VERSION" \
            openjdk-8-jdk \
           ca-certificates-java \
 && mkdir -p $CR_HOME \
             $CR_HOME/acl \
             $CR_HOME/filestore \
             $CR_HOME/staging \
             $CR_HOME/tmp 

COPY . $CR_BASE/build

RUN cd $CR_BASE/build \
 && cp docker.properties local.properties \
 && sed -i "/^\s*application.homeDir/c\application.homeDir\=${CR_HOME}" local.properties \
 && mvn clean install -Dmaven.test.skip=true \
 && cp -pr target/cr-das $CATALINA_HOME/webapps/data \
 && cp $CR_BASE/build/docker-entrypoint.sh /docker-entrypoint.sh 

VOLUME  ${CR_HOME}

ENTRYPOINT ["/docker-entrypoint.sh"]


CMD ["catalina.sh", "run"]


