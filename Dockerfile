FROM nexus-docker-registry.apps.cicd2.mdtu-ddm.projects.epam.com/maven:3.8.1-jdk-11-slim
WORKDIR /app
COPY pom.xml settings.xml ./
COPY src src
RUN mvn org.apache.maven.plugins:maven-dependency-plugin:2.3:get -DgroupId=org.springframework.boot -DartifactId=spring-boot-maven-plugin -Dversion=2.3.5.RELEASE -DrepoUrl=repo1.maven.org
RUN mvn install --settings settings.xml -DskipTests=true -Dartifactory.baseUrl=https://nexus-public-mdtu-ddm-edp-cicd.apps.cicd2.mdtu-ddm.projects.epam.com -Dartifactory.groupPath=edp-maven-group -Dartifactory.releasePath=edp-maven-releases -Dartifactory.snapshotsPath=edp-maven-snapshots