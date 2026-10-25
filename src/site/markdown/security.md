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
# Reporting New Security Problems with Apache Johnzon

The Apache Software Foundation takes a very active stance in eliminating security problems and denial of service attacks against Apache projects.

We strongly encourage folks to report such problems to the [private security mailing list](http://www.apache.org/security/) first, before disclosing them in a public forum.

Please note that the security mailing list should only be used for reporting undisclosed security vulnerabilities in Apache projects and managing the process of fixing such vulnerabilities. 
We cannot accept regular bug reports or other queries at this address. All mail sent to this address that does not relate to an undisclosed security problem will be ignored.

If you need to report a bug that isn't an undisclosed security vulnerability, please use the [bug reporting](https://issues.apache.org/jira/browse/JOHNZON) system.

###Questions about:

* how to configure Johnzon securely
* if a vulnerability applies to your particular application
* obtaining further information on a published vulnerability
* availability of patches and/or new releases

should be addressed to the [mailing list](http://johnzon.apache.org/mail-lists.html).

The private security mailing address is: security (at) apache (dot) org

Known vulnerabilities are listed on the [vulnerabilities](./vulnerabilities.html) page.

<a id="BigInteger_and_Java"></a>
## BigDecimal and BigInteger

JSON-P, Mapper, and JSON-B can represent JSON numbers as `BigDecimal` or `BigInteger`.
Converting a `BigDecimal` with a large absolute scale to `BigInteger` or plain decimal notation can take substantial time and memory.

Johnzon's built-in numeric conversions check the absolute `BigDecimal` scale before these operations.
The default maximum is 1000. The `johnzon.max-big-decimal-scale` system property changes this default;
`JsonProviderImpl#setMaxBigDecimalScale`, `MapperBuilder#setMaxBigDecimalScale`, and the JSON-B
`johnzon.max-big-decimal-scale` property configure their respective components. On built-in paths,
JSON numbers converted to `BigDecimal` or `BigInteger` through a separately configured Johnzon JSON-P
provider must satisfy both the provider's limit and the Mapper's limit.

The scale limit does not restrict the number of digits. JSON numbers have a separate length limit of 1000 characters
by default. The count includes a sign, decimal point, and exponent. Set the
`org.apache.johnzon.max-number-length` system property or pass the key in a Johnzon JSON-P parser or reader
factory configuration. `MapperBuilder#setMaxNumberLength(int)` and the JSON-B property with
the same name configure their default `BigDecimal`/`BigInteger` string adapters and Johnzon reader factories.
A supplied reader factory or explicitly registered converter keeps its own limit. With a Johnzon reader factory,
quoted numeric values must also fit its `org.apache.johnzon.max-string-length` limit.

Neither limit restricts the total JSON input size. Apply suitable input-size limits and validate numeric range
and precision for your application. Custom adapters are responsible for validating numbers they construct or
format. If you do not need large numeric types, prefer primitive types or their wrappers.

## JSON Schema validation

If you validate untrusted payloads with johnzon-jsonschema, prefer setting `JsonSchemaValidatorFactory#setFailFast(boolean)` to `true`.

By default all keywords of the schema are evaluated and all errors are aggregated. With fail fast enabled the first failing
keyword stops the validation: since the cheap structural keywords (`type`, `minItems`/`maxItems`, `minLength`/`maxLength`,
`pattern`, ...) are evaluated before the expensive ones (`uniqueItems`, nested `properties`, ...), a hostile payload is
rejected without paying for the whole schema. The valid/invalid verdict is identical in both modes, only the number of
reported errors changes.

See the JSON Schema section of the [index](./index.html) page for more details.

