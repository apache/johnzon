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
# Apache Johnzon Security Vulnerabilities

This page lists the security vulnerabilities fixed in released versions of Apache Johnzon.
To report a new vulnerability, see [Report vulnerability](./security.html).

## [CVE-2026-71253](https://www.cve.org/CVERecord?id=CVE-2026-71253): ClassConverter loads & static-initializes an attacker-named class from untrusted JSON

* **Severity:** Low
* **Affected:** `org.apache.johnzon:johnzon-mapper` 1.0.0 before 1.3.1 and 2.0.0 before 2.2.0
* **Fixed in:** 1.3.1 (javax namespace), 2.2.0 (jakarta namespace)
* **Credit:** n0mi1k Security (finder)
* **Announcement:** [dev@johnzon.apache.org](https://lists.apache.org/thread/3vgm1yo2sn9yhkvvs9lc4ro01vl3zt68)

Unsafe reflection: `ClassConverter` resolves and statically initializes a class named in untrusted JSON.
Exploitation needs a mapped POJO with a `Class` attribute and a class with a harmful static initializer on the classpath.
