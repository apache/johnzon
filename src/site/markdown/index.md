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
# Apache Johnzon

Apache Johnzon is a project providing an implementation of JsonProcessing (aka JSR-353) and a set of useful extension
for this specification like an Object mapper, some JAX-RS providers and a WebSocket module provides a basic integration with Java WebSocket API (JSR-356).

## Status

Apache Johnzon is a Top Level Project at the Apache Software Foundation (ASF).
It fully implements the JSON-P_1.1 (JSR-353) and JSON-B_1.0 (JSR-367) specifications.

## Get started

Johnzon comes with four main modules.

### Core (stable)

<pre class="prettyprint linenums"><![CDATA[
<dependency>
  <groupId>org.apache.johnzon</groupId>
  <artifactId>johnzon-core</artifactId>
  <version>${johnzon.version}</version>
</dependency>
]]></pre>

This is the implementation of the JSON-P 1.1 specification. 
You'll surely want to add the API as dependency too:

<pre class="prettyprint linenums"><![CDATA[
<dependency>
  <groupId>org.apache.geronimo.specs</groupId>
  <artifactId>geronimo-json_1.1_spec</artifactId>
  <version>${jsonspecversion}</version>
  <scope>provided</scope> <!-- or compile if your environment doesn't provide it -->
</dependency>
]]></pre>

### Mapper (stable)

<pre class="prettyprint linenums"><![CDATA[
<dependency>
  <groupId>org.apache.johnzon</groupId>
  <artifactId>johnzon-mapper</artifactId>
  <version>${johnzon.version}</version>
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

The mapper uses a direct java to json representation.

For instance this java bean:

<pre class="prettyprint linenums"><![CDATA[
public class MyModel {
  private int id;
  private String name;
  
  // getters/setters
}
]]></pre>

will be mapped to:

<pre class="prettyprint linenums"><![CDATA[
{
  "id": 1234,
  "name": "Johnzon doc"
}
]]></pre>

Note that Johnzon supports several customization either directly on the MapperBuilder of through annotations.

#### @JohnzonIgnore

@JohnzonIgnore is used to ignore a field. You can optionally say you ignore the field until some version
if the mapper has a version:

<pre class="prettyprint linenums"><![CDATA[
public class MyModel {
  @JohnzonIgnore
  private String name;
  
  // getters/setters
}
]]></pre>

Or to support name for version 3, 4, ... but ignore it for 1 and 2:


<pre class="prettyprint linenums"><![CDATA[
public class MyModel {
  @JohnzonIgnore(minVersion = 3)
  private String name;
  
  // getters/setters
}
]]></pre>

#### @JohnzonConverter

Converters are used for advanced mapping between java and json.

There are several converter types:

1. Converter: map java to json and the opposite based on the string representation
2. Adapter: a converter not limited to String
3. ObjectConverter.Reader: to converter from json to java at low level
4. ObjectConverter.Writer: to converter from java to json at low level
4. ObjectConverter.Codec: a Reader and Writer

The most common is to customize date format but they all take. For that simple case we often use a Converter:

<pre class="prettyprint linenums"><![CDATA[
public class LocalDateConverter implements Converter<LocalDate> {
    @Override
    public String toString(final LocalDate instance) {
        return instance.toString();
    }

    @Override
    public LocalDate fromString(final String text) {
        return LocalDate.parse(text);
    }
}
]]></pre>

If you need a more advanced use case and modify the structure of the json (wrapping the value for instance)
you will likely need Reader/Writer or a Codec.

Then once your converter developed you can either register globally on the MapperBuilder or simply decorate
the field you want to convert with @JohnzonConverter:

<pre class="prettyprint linenums"><![CDATA[
public class MyModel {
  @JohnzonConverter(LocalDateConverter.class)
  private LocalDate date;
  
  // getters/setters
}
]]></pre>

#### @JohnzonProperty

Sometimes the json name is not java friendly (_foo or foo-bar or even 200 for instance). For that cases
@JohnzonProperty allows to customize the name used:

<pre class="prettyprint linenums"><![CDATA[
public class MyModel {
  @JohnzonProperty("__date")
  private LocalDate date;
  
  // getters/setters
}
]]></pre>

#### @JohnzonAny

If you don't fully know you model but want to handle all keys you can use @JohnzonAny to capture/serialize them all:

<pre class="prettyprint linenums"><![CDATA[
public class AnyMe {
    @JohnzonAny // ignore normal serialization of this field
    private String name; // known
    private Map<String, Object> any = new TreeMap<String, Object>(); // unknown

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Any
    public Map<String, Object> getAny() {
        return any;
    }

    @Any
    public void handle(final String key, final Object val) {
        any.put(key, val);
    }
}
]]></pre>

#### AccessMode

On MapperBuilder you have several AccessMode available by default but you can also create your own one.

The default available names are:

* field: to use fields model and ignore getters/setters
* method: use getters/setters (means if you have a getter but no setter you will serialize the property but not read it)
* strict-method (default based on Pojo convention): same as method but getters for collections are not used to write
* both: field and method accessors are merged together

You can use these names with setAccessModeName().

### JAX-RS (stable)

<pre class="prettyprint linenums"><![CDATA[
<dependency>
  <groupId>org.apache.johnzon</groupId>
  <artifactId>johnzon-jaxrs</artifactId>
  <version>${johnzon.version}</version>
</dependency>
]]></pre>

JAX-RS module provides two providers (and underlying MessageBodyReaders and MessageBodyWriters):

* org.apache.johnzon.jaxrs.[Wildcard]JohnzonProvider: use Johnzon Mapper to map Object to Json and the opposite
* org.apache.johnzon.jaxrs.[Wildcard]ConfigurableJohnzonProvider: same as JohnzonProvider but with setters to ease the configuration of the provider in most servers/containers
* org.apache.johnzon.jaxrs.[Wildcard]JsrProvider: allows you to use JsrArray, JsrObject (more generally JsonStructure)

Note: Wildcard providers are basically the same as other provider but instead of application/json they support */json, */*+json, */x-json, */javascript, */x-javascript. This
split makes it easier to mix json and other MediaType in the same resource (like text/plain, xml etc since JAX-RS API always matches as true wildcard type in some version whatever the subtype is).

Tip: ConfigurableJohnzonProvider maps most of MapperBuilder configuration letting you configure it through any IoC including not programming language based formats.

### TomEE Configuration

TomEE uses by default Johnzon as JAX-RS provider for versions 7.x. If you want however to customize it you need to follow this procedure:

1. Create a WEB-INF/openejb-jar.xml:

<pre class="prettyprint linenums"><![CDATA[
<?xml version="1.0" encoding="UTF-8"?>
<openejb-jar>
  <pojo-deployment class-name="jaxrs-application">
    <properties>
      # optional but requires to skip scanned providers if set to true
      cxf.jaxrs.skip-provider-scanning = true
      # list of providers we want
      cxf.jaxrs.providers = johnzon,org.apache.openejb.server.cxf.rs.EJBAccessExceptionMapper
    </properties>
  </pojo-deployment>
</openejb-jar>
]]></pre>

2. Create a WEB-INF/resources.xml to define johnzon service which will be use to instantiate the provider

<pre class="prettyprint linenums"><![CDATA[
<?xml version="1.0" encoding="UTF-8"?>
<resources>
  <Service id="johnzon" class-name="org.apache.johnzon.jaxrs.ConfigurableJohnzonProvider">
    # 1M
    maxSize = 1048576
    bufferSize = 1048576

    # ordered attributes
    attributeOrder = $order

    # Additional types to ignore
    ignores = org.apache.cxf.jaxrs.ext.multipart.MultipartBody
  </Service>

  <Service id="order" class-name="com.company.MyAttributeSorter" />

</resources>
]]></pre>

Note: as you can see you mainly just need to define a service with the id johnzon (same as in openejb-jar.xml)
and you can reference other instances using $id for services and @id for resources.

### JSON-B (JSON-B 1.0 compliant)

Johnzon provides a module johnzon-jsonb implementing JSON-B standard based on Johnzon Mapper.

It fully reuses the JSON-B as API.

### Websocket (beta)

<pre class="prettyprint linenums"><![CDATA[
<dependency>
  <groupId>org.apache.johnzon</groupId>
  <artifactId>johnzon-websocket</artifactId>
  <version>${johnzon.version}</version>
</dependency>
]]></pre>

WebSocket module provides a basic integration with Java WebSocket API (JSR 356).

Integration is at codec level (encoder/decoder). There are two families of codecs:

* The ones based on JSON-P (JsonObject, JsonArray, JsonStructure)
* The ones based on Johnzon Mapper

#### JSON-P integration

Encoders:

*  `org.apache.johnzon.websocket.jsr.JsrObjectEncoder`
*  `org.apache.johnzon.websocket.jsr.JsrArrayEncoder`
*  `org.apache.johnzon.websocket.jsr.JsrStructureEncoder`

Decoders:

*  `org.apache.johnzon.websocket.jsr.JsrObjectDecoder`
*  `org.apache.johnzon.websocket.jsr.JsrArrayDecoder`
*  `org.apache.johnzon.websocket.jsr.JsrStructureDecoder`

#### Mapper integration

Encoder:

*  `org.apache.johnzon.websocket.mapper.JohnzonTextEncoder`

Decoder:

*  `org.apache.johnzon.websocket.mapper.JohnzonTextDecoder`

#### Sample

##### JSON-P Samples

On server and client side configuration is easy: just provide the `encoders` and `decoders` parameters to `@[Server|Client]Endpoint`
(or `EndpointConfig` if you use programmatic API)):

    @ClientEndpoint(encoders = JsrObjectEncoder.class, decoders = JsrObjectDecoder.class)
    public class JsrClientEndpointImpl {
        @OnMessage
        public void on(final JsonObject message) {
            // ...
        }
    }

    @ServerEndpoint(value = "/my-server", encoders = JsrObjectEncoder.class, decoders = JsrObjectDecoder.class)
    public class JsrClientEndpointImpl {
        @OnMessage
        public void on(final JsonObject message) {
            // ...
        }
    }

##### WebSocket Samples

Server configuration is as simple as providing `encoders` and `decoders` parameters to `@ServerEndpoint`:

    @ServerEndpoint(value = "/server", encoders = JohnzonTextEncoder.class, decoders = JohnzonTextDecoder.class)
    public class ServerEndpointImpl {
        @OnMessage
        public void on(final Session session, final Message message) {
            // ...
        }
    }

Client configuration is almost the same excepted in this case it is not possible for Johnzon
to guess the type you expect so you'll need to provide it. In next sample it is done just extending `JohnzonTextDecoder`
in `MessageDecoder`.

    @ClientEndpoint(encoders = JohnzonTextEncoder.class, decoders = ClientEndpointImpl.MessageDecoder.class)
    public class ClientEndpointImpl {
        @OnMessage
        public void on(final Message message) {
            // ...
        }
    
        public static class MessageDecoder extends JohnzonTextDecoder {
            public MessageDecoder() {
                super(Message.class);
            }
        }
    }



### JSON-B Extra

<pre class="prettyprint linenums"><![CDATA[
<dependency>
  <groupId>org.apache.johnzon</groupId>
  <artifactId>johnzon-jsonb-extras</artifactId>
  <version>${johnzon.version}</version>
</dependency>
]]></pre>

This module provides some extension to JSON-B.

#### Polymorphism

This extension provides a way to handle polymorphism:

For the deserialization side you have to list the potential children
on the root class:

    @Polymorphic.JsonChildren({
            Child1.class,
            Child2.class
    })
    public abstract class Root {
        public String name;
    }

Then on children you bind an "id" for each of them (note that if you don't give one, the simple name is used):

    @Polymorphic.JsonId("first")
    public class Child1 extends Root {
        public String type;
    }

Finally on the field using the root type (polymorphic type) you can
bind the corresponding serializer and/or deserializer:
    
    public class Wrapper {
        @JsonbTypeSerializer(Polymorphic.Serializer.class)
        @JsonbTypeDeserializer(Polymorphic.DeSerializer.class)
        public Root root;
    
        @JsonbTypeSerializer(Polymorphic.Serializer.class)
        @JsonbTypeDeserializer(Polymorphic.DeSerializer.class)
        public List<Root> roots;
    }


### JSON Schema

<pre class="prettyprint linenums"><![CDATA[
<dependency>
  <groupId>org.apache.johnzon</groupId>
  <artifactId>johnzon-jsonschema</artifactId>
  <version>${johnzon.version}</version>
</dependency>
]]></pre>

This module provides a way to validate an instance against a [JSON Schema](http://json-schema.org/).


<pre class="prettyprint linenums"><![CDATA[
// long live instances (@ApplicationScoped/@Singleton)
JsonObject schema = getJsonSchema();
JsonSchemaValidatorFactory factory = new JsonSchemaValidatorFactory();
JsonSchemaValidator validator = factory.newInstance(schema);

// runtime starts here
JsonObject objectToValidateAgainstSchema = getObject();
ValidatinResult result = validator.apply(objectToValidateAgainstSchema);
// if result.isSuccess, result.getErrors etc...

// end of runtime
validator.close();
factory.close();
]]></pre>

Known limitations are (feel free to do a PR on github to add these missing features):

* Doesn't support references in the schema
* Doesn't support: dependencies, propertyNames, if/then/else, allOf/anyOf/oneOf/not, format validations
