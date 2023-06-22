FROM adoptopenjdk/openjdk11:alpine

ENV USER_UID=1001 \
    USER_NAME=rest-api \
    MAVEN_VERSION=3.9.3
ENV MAVEN_HOME=/opt/apache-maven-$MAVEN_VERSION
ENV PATH=$PATH:$MAVEN_HOME/bin

RUN apk add --no-cache curl
RUN curl -O https://downloads.apache.org/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz \
    && tar xf apache-maven-$MAVEN_VERSION-bin.tar.gz \
    && rm apache-maven-$MAVEN_VERSION-bin.tar.gz \
    && mv apache-maven-$MAVEN_VERSION $MAVEN_HOME \
    && ln -s $MAVEN_HOME/bin/mvn /usr/local/bin/mvn

RUN apk add --no-cache openrc \
    && apk add --no-cache --virtual .build-deps wget \
    && wget https://dlcdn.apache.org/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz \
    && tar -xvzf apache-maven-$MAVEN_VERSION-bin.tar.gz \
    && mv apache-maven-$MAVEN_VERSION /usr/lib/mvn \
    && ln -s /usr/lib/mvn/bin/mvn /usr/bin/mvn \
    && apk del .build-deps \
    && rm -rf /var/cache/apk/*

RUN addgroup --gid ${USER_UID} ${USER_NAME} \
    && adduser --disabled-password --uid ${USER_UID} --ingroup ${USER_NAME} ${USER_NAME}
ARG NEXUS_URL
WORKDIR /app
COPY pom.xml settings.xml ./
COPY src src
RUN mvn install --settings settings.xml -DskipTests=true -Dartifactory.baseUrl=$NEXUS_URL -Dartifactory.groupPath=edp-maven-group -Dartifactory.releasePath=edp-maven-releases -Dartifactory.snapshotsPath=edp-maven-snapshots
RUN find ~/.m2 -name "_remote.repositories" -exec rm -f {} \;
USER ${USER_NAME}
