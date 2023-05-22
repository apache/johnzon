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

#### Johnzon Factory Configurations

##### JsonGeneratorFactory

The generator factory supports the standard properties (pretty one for example) but also:

* `org.apache.johnzon.encoding`: encoding to use for the generator when converting an OutputStream to a Writer.
* `org.apache.johnzon.buffer-strategy`: how to get buffers (char buffer), default strategy is a queue/pool based one but you can switch it to a `THREAD_LOCAL` one. `BY_INSTANCE` (per call/prototype) and `SINGLETON` (single instance) are also supported but first one is generally slower and last one does not enable overflows.  
* `org.apache.johnzon.default-char-buffer-generator` (int): buffer size of the generator, it enables to work in memory to flush less often (for performances).
* `org.apache.johnzon.boundedoutputstreamwriter` (int): when converting an `OuputStream` to a `Writer` it defines the buffer size (if > 0) +- 2 charaters (for the encoding logic). It enables a faster flushing to the actual underlying output stream combined with `org.apache.johnzon.default-char-buffer-generator`.

### JSON-P Strict Compliance (stable)

<pre class="prettyprint linenums"><![CDATA[
<dependency>
  <groupId>org.apache.johnzon</groupId>
  <artifactId>johnzon-jsonp-strict</artifactId>
  <version>${johnzon.version}</version>
</dependency>
]]></pre>

This module enables to enforce a strict compliance of JsonPointer behavior on `/-` usage.
Johnzon default implementation enables an extended usage of it for replace/remove and get operations.
In that case, it will point to the last element of the array so it's easy to replace/remove or get the last element of the array.
For add operation, it remains the same, aka points to the element right after the last element of the array.

This module enforces Johnzon to be JSONP compliant and fail if `/-` is used for anything but add.

Note that you can even customize this behavior implementing your own `JsonPointerFactory` and changing the ordinal value to take a highest priority.

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

If you don't fully know your model but want to handle all keys you can use @JohnzonAny to capture/serialize them all:

<pre class="prettyprint linenums"><![CDATA[
public class AnyMe {
    private String name; // Regular serialization for the known 'name' field

    /* This example uses a TreeMap to store and retrieve the other unknown
       fields for the @JohnzonAny annotated methods, but you can choose
       anything you want. Use @JohnzonIgnore to avoid exposing this as
       an actual 'unknownFields' property in JSON.
    */
    @JohnzonIgnore
    private Map<String, Object> unknownFields = new TreeMap<String, Object>();

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @JohnzonAny
    public Map<String, Object> getAny() {
        return unknownFields;
    }

    @JohnzonAny
    public void handle(final String key, final Object val) {
        this.unknownFields.put(key, val);
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

IMPORTANT: when used with `johnzon-core`, `NoContentException` is not thrown in case of an empty incoming input stream by these providers except `JsrProvider` to limit the breaking changes.

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

However it supports some specific properties to wire to the native johnzon configuration - see `JohnzonBuilder` for details.
One example is `johnzon.interfaceImplementationMapping` which will support a `Map<Class,Class>` to map interfaces to implementations
to use for deserialization.

JsonbConfig specific properties:

* johnzon.use-big-decimal-for-object: true to use BigDecimal for numbers not typed (Object), false to adjust the type to the number size, true by default.
* johnzon.support-enum-container-deserialization: prevent EnumMap/EnumSet instantiation, true by default.
* johnzon.attributeOrder: Comparator instance to sort properties by name.
* johnzon.deduplicateObjects: should instances be deduplicated.
* johnzon.supportsPrivateAccess: should private constructors/methods with `@JsonbCreator` be used too.
* johnzon.fail-on-unknown-properties: should unmapped properties fail the mapping. Similar to `jsonb.fail-on-unknown-properties`.
* johnzon.readAttributeBeforeWrite: should collection be read before being written, it enables to have an "append" mode.
* johnzon.autoAdjustBuffer: should internal read buffers be autoadjusted to stay fixed.
* johnzon.serialize-value-filter: enable to set a filter to not serialize some values.
* johnzon.cdi.activated: should cdi support be active.
* johnzon.accessMode: custom access mode, note that it can disable some JSON-B feature (annotations support).
* johnzon.accessModeDelegate: delegate access mode used by JsonbAccessModel. Enables to enrich default access mode.
* johnzon.failOnMissingCreatorValues: should the mapping fail when a `@JsonbCreator` misses some values.

TIP: more in JohnzonBuilder class.

A JAX-RS provider based on JSON-B is provided in the module as well. It is `org.apache.johnzon.jaxrs.jsonb.jaxrs.JsonbJaxrsProvider`.

IMPORTANT: in JAX-RS 1.0 the provider can throw any exception he wants for an empty incoming stream on reader side. This had been broken in JAX-RS 2.x where it must throw a `jakarta.ws.rs.core.NoContentException`.
To ensure you can pick the implementation you can and limit the breaking changes, you can set ̀throwNoContentExceptionOnEmptyStreams` on the provider to switch between both behaviors.
Default will be picked from the current available API. Finally, this behavior only works with `johnzon-core`.


#### Integration with `JsonValue`

You can use some optimization to map a `JsonObject` to a POJO using Johnzon `JsonValueReader` - or any implementation of  `Reader` implementing `Supplier<JsonStructure>` - and `JsonValueWriter` - or any implementation of  `Writer` implementing `Consumer<JsonValue>` -:

<pre class="prettyprint linenums"><![CDATA[
final JsonValueReader<Simple> reader = new JsonValueReader<>(Json.createObjectBuilder().add("value", "simple").build());
final Jsonb jsonb = getJohnzonJsonb();
final Simple simple = jsonb.fromJson(reader, SomeModel.class);
]]></pre>

<pre class="prettyprint linenums"><![CDATA[
final JsonValueWriter writer = new JsonValueWriter();
final Jsonb jsonb = getJohnzonJsonb();
jsonb.toJson(object, writer);
final JsonObject jsonObject = writer.getObject();
]]></pre>

These two example will not use any IO and directly map the `JsonValue` to/from a POJO.

Also note that, as an experimental extension and pre-available feature of the next specification version, `org.apache.johnzon.jsonb.api.experimental.JsonbExtension` enables
to map POJO to `JsonValue` and the opposite.

### Websocket

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

Note that if you want to control the Mapper or JSON-B instance used by decoders you can set up the associated servlet listeners:

* org.apache.johnzon.websocket.internal.mapper.MapperLocator for johnzon-mapper
* org.apache.johnzon.websocket.jsonb.JsonbLocator for JSON-B

if you write in the servlet context an attribute named `org.apache.johnzon.websocket.internal.mapper.MapperLocator.mapper` (it is a `Supplier<Mapper>`) or `org.apache.johnzon.websocket.jsonb.JsonbLocator.jsonb` (depending the implementation you use) it will be used instead of the default instance.

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

#### JSON-B integration

Encoder:

*  `org.apache.johnzon.websocket.jsonb.JsonbTextEncoder`

Decoder:

*  `org.apache.johnzon.websocket.jsonb.JsonbTextDecoder`

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

This extension shouldn't be used anymore if you don't absolutely rely on the JSON format it generates/parses.
Use JSON-B 3 polymorphism instead. It provides a way to handle polymorphism:


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

Binding the polymophic serializer and/or deserializer *must not* be done using `JsonbConfig.withSerializers` / `JsonbConfig.withDeserializers`, as it is designed to work *only* with binding performed *using annotations*.


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

### JSON Logic

<pre class="prettyprint linenums"><![CDATA[
<dependency>
  <groupId>org.apache.johnzon</groupId>
  <artifactId>johnzon-jsonlogic</artifactId>
  <version>${johnzon.version}</version>
</dependency>
<dependency> <!-- requires an implementation of JSON-P -->
  <groupId>org.apache.johnzon</groupId>
  <artifactId>johnzon-core</artifactId>
  <version>${johnzon.version}</version>
</dependency>
]]></pre>

This module provides a way to execute any [JSON Logic](http://jsonlogic.com/) expression.

<pre class="prettyprint linenums"><![CDATA[
final JohnzonJsonLogic jsonLogic = new JohnzonJsonLogic();
final JsonValue result = jsonLogic.apply(
        builderFactory.createObjectBuilder()
                .add("merge", builderFactory.createArrayBuilder()
                        .add(builderFactory.createArrayBuilder()
                                .add(1)
                                .add(2))
                        .add(3)
                        .add("4"))
                .build(),
        JsonValue.EMPTY_JSON_ARRAY);
]]></pre>

Default operators are supported - except "log" one to let you pick the logger (impl + name) you want.

To register a custom operator just do it on your json logic instance:

<pre class="prettyprint linenums"><![CDATA[
final JohnzonJsonLogic jsonLogic = new JohnzonJsonLogic();
jsonLogic.registerOperator(
  "log",
  (jsonLogic, config, args) -> log.info(String.valueOf(jsonLogic.apply(config, args)));
]]></pre>

Note that by default the set of standard JSON Logic operators is enriched with JSON-P jsonpatch, json merge diff and json merge patch operators.

### OSGi JAX-RS Whiteboard

Though Johnzon artifacts are OSGi bundles to begin with, this module provides further integration with the [OSGi JAX-RS Whiteboard](https://osgi.org/specification/osgi.cmpn/7.0.0/service.jaxrs.html) and [OSGi CDI Integration](https://osgi.org/specification/osgi.enterprise/7.0.0/service.cdi.html) specifications.

##### JAX-RS JSON-B

This module provides `MessageBodyWriter` and `MessageBodyReader` extensions for the media type `application/json` (by default) to whiteboard JAX-RS Applications.

Configuration of this extension is managed via Configuration Admin using the **pid** `org.apache.johnzon.jaxrs.jsonb` and defines a Metatype schema with the following properties:

|  Property    | Synopsis     | Type | Default |
| ---- | ------------- | -- | -- |
| `ignores` | List of fully qualified class names to ignore | String[] | empty |
| `osgi.jaxrs.application.select` | Filter expression used to match the extension to JAX-RS Whiteboard Applications | String | `(!(johnzon.jsonb=false))` *(which is a convention allowing the extension to bind to all applications unless the application is configured with `johnzon.jsonb=false`)* |
| `osgi.jaxrs.media.type` | List of media types handled by the extension | String[] | `application/json` |
| `throw.no.content.exception.on.empty.streams` | | boolean | `false` |
| `fail.on.unknown.properties` | | boolean | `false` |
| `use.js.range` | | boolean | `false` |
| `other.properties` | | String | empty |
| `ijson` | | boolean | `false` |
| `encoding` | | String | empty |
| `binary.datastrategy` | | String | empty |
| `property.naming.strategy` | | String | empty |
| `property.order.strategy` | | String | empty |
| `null.values` | | boolean | `false` |
| `pretty` | | boolean | `false` |
| `fail.on.missing.creator.values` | | boolean | `false` |
| `polymorphic.serialization.predicate` | | String | empty |
| `polymorphic.deserialization.predicate` | | String | empty |
| `polymorphic.discriminator` | | String | empty |

##### CDI

Since JSON-B specification provides an integration with the CDI specification to handle caching, this module also provides such integration for OSGi CDI Integration specification by providing an `jakarta.enterprise.inject.spi.Extension` service with the required service property `osgi.cdi.extension` with the value `JavaJSONB`.

##### Implicit Extensions

In order to reduce the burden of configuration Apache Aries CDI (the OSGi CDI Integration RI) provides a feature of implicit extensions. These are extensions which the developer doesn't have to configure a requirement for in their CDI bundle. The Johnzon JSON-B CDI extension is such an extension and as such when running in Aries CDI does not need to be required.

This is achieve using the service property `aries.cdi.extension.mode=implicit`.