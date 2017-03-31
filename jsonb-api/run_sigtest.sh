#!/bin/sh

#    Licensed to the Apache Software Foundation (ASF) under one
#    or more contributor license agreements.  See the NOTICE file
#    distributed with this work for additional information
#    regarding copyright ownership.  The ASF licenses this file
#    to you under the Apache License, Version 2.0 (the
#    "License"); you may not use this file except in compliance
#    with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing,
#    software distributed under the License is distributed on an
#    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#    KIND, either express or implied.  See the License for the
#    specific language governing permissions and limitations
#    under the License.

# file to run the JPA signature tests


# HOWTO:
#
# Download sigtestdev.jar from https://wiki.openjdk.java.net/display/CodeTools/SigTest
# Copy to a local folder and set SIGTEST_HOME to it.


# needed, because we have deps to other specs in JSONB
mvn dependency:copy-dependencies

# generate the SIG for the RI
# there is currently no official jar yet. If availablen, then: curl  http://repo1.maven.org/maven2/javax/json/... > ./target/javax.json-1.1.jar
java -jar ${SIGTEST_HOME}/lib/sigtestdev.jar Setup -classpath ${JAVA_HOME}/jre/lib/rt.jar:./javax.json.bind-api.jar:./target/dependency/geronimo-json_1.1_spec-1.0-SNAPSHOT.jar -Package javax.json  -FileName target/javax.json.bind-api.sig -static

# this generates the signature for our own jpa api
java -jar ${SIGTEST_HOME}/lib/sigtestdev.jar Setup -classpath ${JAVA_HOME}/jre/lib/rt.jar:./target/dependency/geronimo-json_1.1_spec-1.0-SNAPSHOT.jar:./target/jsonb-api-1.1.0-SNAPSHOT.jar -Package javax.json  -FileName target/jsonb-api-1.1.0-SNAPSHOT.sig -static

# then open the 2 generated sig files in a diff browser and the only difference should be some internal variables.
