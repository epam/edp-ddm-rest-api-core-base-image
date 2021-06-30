FROM nexus-docker-registry.apps.cicd2.mdtu-ddm.projects.epam.com/mdtu-ddm-edp-cicd/maven:3.8.6-jdk-11-slim
ARG NEXUS_URL
WORKDIR /app
COPY pom.xml settings.xml ./
COPY src src
RUN mvn install --settings settings.xml -DskipTests=true -Dartifactory.baseUrl=$NEXUS_URL -Dartifactory.groupPath=edp-maven-group -Dartifactory.releasePath=edp-maven-releases -Dartifactory.snapshotsPath=edp-maven-snapshots
RUN find ~/.m2 -name "_remote.repositories" -exec rm -f {} \;
