<!---
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->
# Apache Johnzon Releases

This page contains download links to the latest Apache Johnzon releases.

All maven artifacts are available in the Maven.Central repository with the groupId ``org.apache.johnzon``. 
The dependencies you can use are listed at the bottom of this page: [Maven Dependencies](#Maven_Dependencies).


should be addressed to the [mailing list](http://johnzon.apache.org/mail-lists.html).

## KEYS for verifying Apache releases

Please use the Johnzon [KEYS](https://www.apache.org/dist/johnzon/KEYS) file to validate our releases.
Read more about [how we sign Apache Releases](http://www.apache.org/info/verification.html)


----------

## Johnzon-2.0.x

Apache Johnzon 2.0.x implements the JSON-P 2.1 and JSON-B 3.0 specifications which on a level of JavaEE 10. This version is not backward compatible due to the namespace change from `javax` to `jakarta`. 
Apache Johnzon does not implement Jakarta EE 9 per se, because there was no change in terms of APIs except the namespace change. 
So we decided to use Apache Johnzon 1.2.x bellow and publish a Jakarta compatible version using bytecode transformation. All artifacts can be used with the classifier `jakarta`.

#### Source

This version is currently only available as snapshot. 
We are still actively working on passing the TCK but so far most of the implementation is ready.

## Johnzon-1.2.x

Apache Johnzon 1.2.x implements the JSON-P 1.1 and JSON-B 1.0 specifications which on a level of JavaEE 8.

#### Source
Should you want to build any of the above binaries, this source bundle is the right one and covers them all.

* [johnzon-1.2.20-source-release.zip](https://www.apache.org/dyn/closer.lua/1.2.20/johnzon-1.2.20-source-release.zip)
* [johnzon-1.2.20-source-release.zip.sha512](https://www.apache.org/dist/johnzon/1.2.20/johnzon-1.2.20-source-release.zip.sha512)
* [johnzon-1.2.20-source-release.zip.asc](https://www.apache.org/dist/johnzon/1.2.20/johnzon-1.2.20-source-release.zip.asc)


## Johnzon-1.0.x

Apache Johnzon 1.0.x implements the JSON-P 1.0 specification and a preliminary version of the JSON-B 1.0.
This corresponds to JavaEE 7 level.

#### Binaries
The binary distribution contains all Johnzon modules.

* [apache-johnzon-1.0.2-bin.zip](https://www.apache.org/dyn/closer.lua/johnzon/johnzon-1.0.2/apache-johnzon-1.0.2-bin.zip)
* [apache-johnzon-1.0.2-bin.zip.sha256](https://www.apache.org/dist/johnzon/johnzon-1.0.2/apache-johnzon-1.0.2-bin.zip.sha256)
* [apache-johnzon-1.0.2-bin.zip.asc](https://www.apache.org/dist/johnzon/johnzon-1.0.2/apache-johnzon-1.0.2-bin.zip.asc)

#### Source
Should you want to build any of the above binaries, this source bundle is the right one and covers them all.

* [johnzon-1.0.2-source-release.zip](https://www.apache.org/dyn/closer.lua/johnzon/johnzon-1.0.2/apache-johnzon-1.0.2-source-release.zip)
* [johnzon-1.0.2-source-release.zip.sha256](https://www.apache.org/dist/johnzon/johnzon-1.0.2/apache-johnzon-1.0.2-source-release.zip.sha256)
* [johnzon-1.0.2-source-release.zip.asc](https://www.apache.org/dist/johnzon/johnzon-1.0.2/apache-johnzon-1.0.2-source-release.zip.asc)

-------

### Maven Dependencies

#### APIs for Johnzon-2.0.x (Jakarta EE 10)

Since Java EE got open sourced to become Jakarta EE, the APIs can be used without license restrictions, so we moved away from our Apache APIs.

    <dependency>
        <groupId>jakarta.json</groupId>
        <artifactId>jakarta.json-api</artifactId>
        <version>2.1.1</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>jakarta.json.bind</groupId>
        <artifactId>jakarta.json.bind-api</artifactId>
        <version>3.0</version>
        <scope>provided</scope>
    </dependency>

#### APIs for Johnzon-1.1.x (JavaEE 8)

    <dependency>
        <groupId>org.apache.geronimo.specs</groupId>
        <artifactId>geronimo-json_1.1_spec</artifactId>
        <version>1.0</version>
    </dependency>

    <dependency>
        <groupId>org.apache.geronimo.specs</groupId>
        <artifactId>geronimo-jsonb_1.0_spec</artifactId>
        <version>1.2</version>
    </dependency>

#### APIs for Johnzon-1.0.x (JavaEE 7)

    <dependency>
        <groupId>org.apache.geronimo.specs</groupId>
        <artifactId>geronimo-json_1.0_spec</artifactId>
        <version>1.0-alpha-1</version>
    </dependency>

    <dependency>
        <groupId>org.apache.geronimo.specs</groupId>
        <artifactId>geronimo-jsonb_1.0_spec</artifactId>
        <version>1.0</version>
    </dependency>

Note that you should set the scope of those dependencies to either `provided` or `compile` depending on whether your environment already provide them or not.
