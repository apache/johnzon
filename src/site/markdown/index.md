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
# Apache fleece

Apache Fleece is a project providing an implementation of JsonProcessing (aka jsr-353) and a set of useful extension
for this specification like an Object mapper and some JAX-RS providers.

## Get started

Fleece comes with three main modules.

### Core

<pre class="prettyprint linenums"><![CDATA[
<dependency>
  <groupId>org.apache.fleece</groupId>
  <artifactId>fleece-core</artifactId>
  <version>${fleece.version}</version>
</dependency>
]]></pre>

This is the implementation of the specification. You'll surely want to add the API as dependency too:

<pre class="prettyprint linenums"><![CDATA[
<dependency>
  <groupId>org.apache.geronimo.specs</groupId>
  <artifactId>geronimo-json_1.0_spec</artifactId>
  <version>${json-processing.version}</version>
  <scope>provided</scope> <!-- or compile if your environment doesn't provide it -->
</dependency>
]]></pre>

### Mapper

<pre class="prettyprint linenums"><![CDATA[
<dependency>
  <groupId>org.apache.fleece</groupId>
  <artifactId>fleece-mapper</artifactId>
  <version>${fleece.version}</version>
</dependency>
]]></pre>

The mapper module allows you to use the implementation you want of Json Processing specification to map
Json to Object and the opposite.

<pre class="prettyprint linenums"><![CDATA[
final MySuperObject object = createObject();

final Mapper mapper = new MapperBuilder().build();
mapper.writeObject(object, outputStream);

final MySuperObject otherObject = mapper.readObject(inputStream, MySuperObject.class);
]]></pre>

### JAX-RS

<pre class="prettyprint linenums"><![CDATA[
<dependency>
  <groupId>org.apache.fleece</groupId>
  <artifactId>fleece-jaxrs</artifactId>
  <version>${fleece.version}</version>
</dependency>
]]></pre>

JAX-RS module provides two providers (and underlying MessageBodyReaders and MessageBodyWriters):

* org.apache.fleece.jaxrs.FleeceProvider.FleeceProvider: use Fleece Mapper to map Object to Json and the opposite
* org.apache.fleece.jaxrs.JsrProvider: allows you to use JsrArray, JsrObject (more generally JsonStructure)

