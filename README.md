Apache Johnzon
==============

[![Maven](https://github.com/apache/johnzon/actions/workflows/maven.yml/badge.svg)](https://github.com/apache/johnzon/actions/workflows/maven.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.johnzon/johnzon-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.apache.johnzon/johnzon-core)
[![Javadocs](https://www.javadoc.io/badge/org.apache.johnzon/johnzon-core.svg)](https://www.javadoc.io/doc/org.apache.johnzon/johnzon-core)

Apache Johnzon is a project providing an implementation of JsonProcessing (aka JSR-353) and a set of useful extension for this specification like an Object mapper, some JAX-RS providers and a WebSocket module provides a basic integration with Java WebSocket API (JSR-356).

See the main website: [johnzon.apache.org](https://johnzon.apache.org)

The project Jira: [issues.apache.org/jira/projects/JOHNZON](https://issues.apache.org/jira/projects/JOHNZON).

Artifacts are published to [maven central](https://central.sonatype.dev/publisher/org.apache.johnzon).

```xml
    <dependency>
        <groupId>org.apache.johnzon</groupId>
        <artifactId>johnzon-core</artifactId>
        <version>${johnzon.version}</version>
    </dependency>
    <dependency>
        <groupId>org.apache.johnzon</groupId>
        <artifactId>johnzon-jsonp-strict</artifactId>
        <version>${johnzon.version}</version>
    </dependency>
```