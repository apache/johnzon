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

## Johnzon-1.1.x

Apache Johnzon 1.1.x implements the JSON-P 1.1 and JSON-B 1.0 specifications which on a level of JavaEE 8.

#### Binaries
The binary distribution contains all Johnzon modules.

* [apache-johnzon-1.1.8-bin.zip](https://www.apache.org/dyn/closer.lua/johnzon/johnzon-1.1.8/apache-johnzon-1.1.8-bin.zip)
* [apache-johnzon-1.1.8-bin.zip.sha1](https://www.apache.org/dist/johnzon/johnzon-1.1.8/apache-johnzon-1.1.8-bin.zip.sha1)
* [apache-johnzon-1.1.8-bin.zip.asc](https://www.apache.org/dist/johnzon/johnzon-1.1.8/apache-johnzon-1.1.8-bin.zip.asc)

#### Source
Should you want to build any of the above binaries, this source bundle is the right one and covers them all.

* [johnzon-1.1.8-source-release.zip](https://www.apache.org/dyn/closer.lua/johnzon/johnzon-1.1.8/johnzon-1.1.8-source-release.zip)
* [johnzon-1.1.8-source-release.zip.sha1](https://www.apache.org/dist/johnzon/johnzon-1.1.8/johnzon-1.1.8-source-release.zip.sha1)
* [johnzon-1.1.8-source-release.zip.asc](https://www.apache.org/dist/johnzon/johnzon-1.1.8/johnzon-1.1.8-source-release.zip.asc)


## Johnzon-1.0.x

Apache Johnzon 1.0.x implements the JSON-P 1.0 specification and a preliminary version of the JSON-B 1.0.
This corresponds to JavaEE 7 level.

#### Binaries
The binary distribution contains all Johnzon modules.

* [apache-johnzon-1.0.1-bin.zip](https://www.apache.org/dyn/closer.lua/johnzon/johnzon-1.0.1/apache-johnzon-1.0.1-bin.zip)
* [apache-johnzon-1.0.1-bin.zip.sha1](https://www.apache.org/dist/johnzon/johnzon-1.0.1/apache-johnzon-1.0.1-bin.zip.sha1)
* [apache-johnzon-1.0.1-bin.zip.asc](https://www.apache.org/dist/johnzon/johnzon-1.0.1/apache-johnzon-1.0.1-bin.zip.asc)

#### Source
Should you want to build any of the above binaries, this source bundle is the right one and covers them all.

* [johnzon-1.0.1-source-release.zip](https://www.apache.org/dyn/closer.lua/johnzon/johnzon-1.0.1/johnzon-1.0.1-source-release.zip)
* [johnzon-1.0.1-source-release.zip.sha1](https://www.apache.org/dist/johnzon/johnzon-1.0.1/johnzon-1.0.1-source-release.zip.sha1)
* [johnzon-1.0.1-source-release.zip.asc](https://www.apache.org/dist/johnzon/johnzon-1.0.1/johnzon-1.0.1-source-release.zip.asc)

-------

### Maven Dependencies

#### APIs for Johnzon-1.1.x (JavaEE 8)

    <dependency>
        <groupId>org.apache.geronimo.specs</groupId>
        <artifactId>geronimo-json_1.1_spec</artifactId>
        <version>1.0</version>
    </dependency>

    <dependency>
        <groupId>org.apache.geronimo.specs</groupId>
        <artifactId>geronimo-jsonb_1.0_spec</artifactId>
        <version>1.0</version>
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
