/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.johnzon.jsonschema.generator;

import org.junit.Test;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;

public class PojoGeneratorTest {
    private final PojoGenerator.PojoConfiguration configuration = new PojoGenerator.PojoConfiguration()
            .setAddAllArgsConstructor(false)
            .setClassName("TheClass")
            .setPackageName("org.test");

    @Test
    public void generate() {
        final Map<String, String> expected = new HashMap<>();
        expected.put("org/test/TheClass.java", root());
        expected.put("org/test/TheClassMetadata.java", metadata());
        expected.put("org/test/TheClassMetadataManagedFields.java", managedFields());
        expected.put("org/test/TheClassMetadataOwnerReferences.java", ownerRefs());
        final Map<String, String> generated = new PojoGenerator(configuration)
                .visitSchema(load("ConfigMap.json"))
                .generate();

        // actual assertion but since we want a better error message we split it in 2
        // assertEquals(expected, generated);

        assertEquals(expected.keySet(), generated.keySet());
        expected.forEach((k, v) -> assertEquals(v, generated.get(k)));
    }

    @Test
    public void generateEnums() {
        final Map<String, String> generated = new PojoGenerator(configuration)
                .visitSchema(load("Node.json"))
                .generate();
        assertEquals("" +
                        "package org.test;\n" +
                        "\n" +
                        "public enum TheClassSpecTaintsEffect {\n" +
                        "    PreferNoSchedule,\n" +
                        "    NoSchedule,\n" +
                        "    NoExecute\n" +
                        "}\n",
                generated.get("org/test/TheClassSpecTaintsEffect.java"));
        assertEquals("" +
                        "package org.test;\n" +
                        "\n" +
                        "import java.util.Objects;\n" +
                        "\n" +
                        "public class TheClassSpecTaints {\n" +
                        "    private TheClassSpecTaintsEffect effect;\n" +
                        "    private String key;\n" +
                        "    private String timeAdded;\n" +
                        "    private String value;\n" +
                        "\n" +
                        "    public TheClassSpecTaintsEffect getEffect() {\n" +
                        "        return effect;\n" +
                        "    }\n" +
                        "\n" +
                        "    public void setEffect(final TheClassSpecTaintsEffect effect) {\n" +
                        "        this.effect = effect;\n" +
                        "    }\n" +
                        "\n" +
                        "    public String getKey() {\n" +
                        "        return key;\n" +
                        "    }\n" +
                        "\n" +
                        "    public void setKey(final String key) {\n" +
                        "        this.key = key;\n" +
                        "    }\n" +
                        "\n" +
                        "    public String getTimeAdded() {\n" +
                        "        return timeAdded;\n" +
                        "    }\n" +
                        "\n" +
                        "    public void setTimeAdded(final String timeAdded) {\n" +
                        "        this.timeAdded = timeAdded;\n" +
                        "    }\n" +
                        "\n" +
                        "    public String getValue() {\n" +
                        "        return value;\n" +
                        "    }\n" +
                        "\n" +
                        "    public void setValue(final String value) {\n" +
                        "        this.value = value;\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public int hashCode() {\n" +
                        "        return Objects.hash(\n" +
                        "                effect,\n" +
                        "                key,\n" +
                        "                timeAdded,\n" +
                        "                value);\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public boolean equals(final Object __other) {\n" +
                        "        if (!(__other instanceof TheClassSpecTaints)) {\n" +
                        "            return false;\n" +
                        "        }\n" +
                        "        final TheClassSpecTaints __otherCasted = (TheClassSpecTaints) __other;\n" +
                        "        return Objects.equals(effect, __otherCasted.effect) &&\n" +
                        "            Objects.equals(key, __otherCasted.key) &&\n" +
                        "            Objects.equals(timeAdded, __otherCasted.timeAdded) &&\n" +
                        "            Objects.equals(value, __otherCasted.value);\n" +
                        "    }\n" +
                        "}\n",
                generated.get("org/test/TheClassSpecTaints.java"));
    }

    private static String root() {
        return "" +
                "package org.test;\n" +
                "\n" +
                "import java.util.Map;\n" +
                "import java.util.Objects;\n" +
                "\n" +
                "public class TheClass {\n" +
                "    private String apiVersion;\n" +
                "    private Map<String, String> binaryData;\n" +
                "    private Map<String, String> data;\n" +
                "    private Boolean immutable;\n" +
                "    private String kind;\n" +
                "    private TheClassMetadata metadata;\n" +
                "\n" +
                "    public String getApiVersion() {\n" +
                "        return apiVersion;\n" +
                "    }\n" +
                "\n" +
                "    public void setApiVersion(final String apiVersion) {\n" +
                "        this.apiVersion = apiVersion;\n" +
                "    }\n" +
                "\n" +
                "    public Map<String, String> getBinaryData() {\n" +
                "        return binaryData;\n" +
                "    }\n" +
                "\n" +
                "    public void setBinaryData(final Map<String, String> binaryData) {\n" +
                "        this.binaryData = binaryData;\n" +
                "    }\n" +
                "\n" +
                "    public Map<String, String> getData() {\n" +
                "        return data;\n" +
                "    }\n" +
                "\n" +
                "    public void setData(final Map<String, String> data) {\n" +
                "        this.data = data;\n" +
                "    }\n" +
                "\n" +
                "    public Boolean getImmutable() {\n" +
                "        return immutable;\n" +
                "    }\n" +
                "\n" +
                "    public void setImmutable(final Boolean immutable) {\n" +
                "        this.immutable = immutable;\n" +
                "    }\n" +
                "\n" +
                "    public String getKind() {\n" +
                "        return kind;\n" +
                "    }\n" +
                "\n" +
                "    public void setKind(final String kind) {\n" +
                "        this.kind = kind;\n" +
                "    }\n" +
                "\n" +
                "    public TheClassMetadata getMetadata() {\n" +
                "        return metadata;\n" +
                "    }\n" +
                "\n" +
                "    public void setMetadata(final TheClassMetadata metadata) {\n" +
                "        this.metadata = metadata;\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public int hashCode() {\n" +
                "        return Objects.hash(\n" +
                "                apiVersion,\n" +
                "                binaryData,\n" +
                "                data,\n" +
                "                immutable,\n" +
                "                kind,\n" +
                "                metadata);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public boolean equals(final Object __other) {\n" +
                "        if (!(__other instanceof TheClass)) {\n" +
                "            return false;\n" +
                "        }\n" +
                "        final TheClass __otherCasted = (TheClass) __other;\n" +
                "        return Objects.equals(apiVersion, __otherCasted.apiVersion) &&\n" +
                "            Objects.equals(binaryData, __otherCasted.binaryData) &&\n" +
                "            Objects.equals(data, __otherCasted.data) &&\n" +
                "            Objects.equals(immutable, __otherCasted.immutable) &&\n" +
                "            Objects.equals(kind, __otherCasted.kind) &&\n" +
                "            Objects.equals(metadata, __otherCasted.metadata);\n" +
                "    }\n" +
                "}\n";
    }

    private static String metadata() {
        return "" +
                "package org.test;\n" +
                "\n" +
                "import java.util.List;\n" +
                "import java.util.Map;\n" +
                "import java.util.Objects;\n" +
                "\n" +
                "public class TheClassMetadata {\n" +
                "    private Map<String, String> annotations;\n" +
                "    private String clusterName;\n" +
                "    private String creationTimestamp;\n" +
                "    private Integer deletionGracePeriodSeconds;\n" +
                "    private String deletionTimestamp;\n" +
                "    private List<String> finalizers;\n" +
                "    private String generateName;\n" +
                "    private Integer generation;\n" +
                "    private Map<String, String> labels;\n" +
                "    private List<TheClassMetadataManagedFields> managedFields;\n" +
                "    private String name;\n" +
                "    private String namespace;\n" +
                "    private List<TheClassMetadataOwnerReferences> ownerReferences;\n" +
                "    private String resourceVersion;\n" +
                "    private String selfLink;\n" +
                "    private String uid;\n" +
                "\n" +
                "    public Map<String, String> getAnnotations() {\n" +
                "        return annotations;\n" +
                "    }\n" +
                "\n" +
                "    public void setAnnotations(final Map<String, String> annotations) {\n" +
                "        this.annotations = annotations;\n" +
                "    }\n" +
                "\n" +
                "    public String getClusterName() {\n" +
                "        return clusterName;\n" +
                "    }\n" +
                "\n" +
                "    public void setClusterName(final String clusterName) {\n" +
                "        this.clusterName = clusterName;\n" +
                "    }\n" +
                "\n" +
                "    public String getCreationTimestamp() {\n" +
                "        return creationTimestamp;\n" +
                "    }\n" +
                "\n" +
                "    public void setCreationTimestamp(final String creationTimestamp) {\n" +
                "        this.creationTimestamp = creationTimestamp;\n" +
                "    }\n" +
                "\n" +
                "    public Integer getDeletionGracePeriodSeconds() {\n" +
                "        return deletionGracePeriodSeconds;\n" +
                "    }\n" +
                "\n" +
                "    public void setDeletionGracePeriodSeconds(final Integer deletionGracePeriodSeconds) {\n" +
                "        this.deletionGracePeriodSeconds = deletionGracePeriodSeconds;\n" +
                "    }\n" +
                "\n" +
                "    public String getDeletionTimestamp() {\n" +
                "        return deletionTimestamp;\n" +
                "    }\n" +
                "\n" +
                "    public void setDeletionTimestamp(final String deletionTimestamp) {\n" +
                "        this.deletionTimestamp = deletionTimestamp;\n" +
                "    }\n" +
                "\n" +
                "    public List<String> getFinalizers() {\n" +
                "        return finalizers;\n" +
                "    }\n" +
                "\n" +
                "    public void setFinalizers(final List<String> finalizers) {\n" +
                "        this.finalizers = finalizers;\n" +
                "    }\n" +
                "\n" +
                "    public String getGenerateName() {\n" +
                "        return generateName;\n" +
                "    }\n" +
                "\n" +
                "    public void setGenerateName(final String generateName) {\n" +
                "        this.generateName = generateName;\n" +
                "    }\n" +
                "\n" +
                "    public Integer getGeneration() {\n" +
                "        return generation;\n" +
                "    }\n" +
                "\n" +
                "    public void setGeneration(final Integer generation) {\n" +
                "        this.generation = generation;\n" +
                "    }\n" +
                "\n" +
                "    public Map<String, String> getLabels() {\n" +
                "        return labels;\n" +
                "    }\n" +
                "\n" +
                "    public void setLabels(final Map<String, String> labels) {\n" +
                "        this.labels = labels;\n" +
                "    }\n" +
                "\n" +
                "    public List<TheClassMetadataManagedFields> getManagedFields() {\n" +
                "        return managedFields;\n" +
                "    }\n" +
                "\n" +
                "    public void setManagedFields(final List<TheClassMetadataManagedFields> managedFields) {\n" +
                "        this.managedFields = managedFields;\n" +
                "    }\n" +
                "\n" +
                "    public String getName() {\n" +
                "        return name;\n" +
                "    }\n" +
                "\n" +
                "    public void setName(final String name) {\n" +
                "        this.name = name;\n" +
                "    }\n" +
                "\n" +
                "    public String getNamespace() {\n" +
                "        return namespace;\n" +
                "    }\n" +
                "\n" +
                "    public void setNamespace(final String namespace) {\n" +
                "        this.namespace = namespace;\n" +
                "    }\n" +
                "\n" +
                "    public List<TheClassMetadataOwnerReferences> getOwnerReferences() {\n" +
                "        return ownerReferences;\n" +
                "    }\n" +
                "\n" +
                "    public void setOwnerReferences(final List<TheClassMetadataOwnerReferences> ownerReferences) {\n" +
                "        this.ownerReferences = ownerReferences;\n" +
                "    }\n" +
                "\n" +
                "    public String getResourceVersion() {\n" +
                "        return resourceVersion;\n" +
                "    }\n" +
                "\n" +
                "    public void setResourceVersion(final String resourceVersion) {\n" +
                "        this.resourceVersion = resourceVersion;\n" +
                "    }\n" +
                "\n" +
                "    public String getSelfLink() {\n" +
                "        return selfLink;\n" +
                "    }\n" +
                "\n" +
                "    public void setSelfLink(final String selfLink) {\n" +
                "        this.selfLink = selfLink;\n" +
                "    }\n" +
                "\n" +
                "    public String getUid() {\n" +
                "        return uid;\n" +
                "    }\n" +
                "\n" +
                "    public void setUid(final String uid) {\n" +
                "        this.uid = uid;\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public int hashCode() {\n" +
                "        return Objects.hash(\n" +
                "                annotations,\n" +
                "                clusterName,\n" +
                "                creationTimestamp,\n" +
                "                deletionGracePeriodSeconds,\n" +
                "                deletionTimestamp,\n" +
                "                finalizers,\n" +
                "                generateName,\n" +
                "                generation,\n" +
                "                labels,\n" +
                "                managedFields,\n" +
                "                name,\n" +
                "                namespace,\n" +
                "                ownerReferences,\n" +
                "                resourceVersion,\n" +
                "                selfLink,\n" +
                "                uid);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public boolean equals(final Object __other) {\n" +
                "        if (!(__other instanceof TheClassMetadata)) {\n" +
                "            return false;\n" +
                "        }\n" +
                "        final TheClassMetadata __otherCasted = (TheClassMetadata) __other;\n" +
                "        return Objects.equals(annotations, __otherCasted.annotations) &&\n" +
                "            Objects.equals(clusterName, __otherCasted.clusterName) &&\n" +
                "            Objects.equals(creationTimestamp, __otherCasted.creationTimestamp) &&\n" +
                "            Objects.equals(deletionGracePeriodSeconds, __otherCasted.deletionGracePeriodSeconds) &&\n" +
                "            Objects.equals(deletionTimestamp, __otherCasted.deletionTimestamp) &&\n" +
                "            Objects.equals(finalizers, __otherCasted.finalizers) &&\n" +
                "            Objects.equals(generateName, __otherCasted.generateName) &&\n" +
                "            Objects.equals(generation, __otherCasted.generation) &&\n" +
                "            Objects.equals(labels, __otherCasted.labels) &&\n" +
                "            Objects.equals(managedFields, __otherCasted.managedFields) &&\n" +
                "            Objects.equals(name, __otherCasted.name) &&\n" +
                "            Objects.equals(namespace, __otherCasted.namespace) &&\n" +
                "            Objects.equals(ownerReferences, __otherCasted.ownerReferences) &&\n" +
                "            Objects.equals(resourceVersion, __otherCasted.resourceVersion) &&\n" +
                "            Objects.equals(selfLink, __otherCasted.selfLink) &&\n" +
                "            Objects.equals(uid, __otherCasted.uid);\n" +
                "    }\n" +
                "}\n";
    }

    private static String managedFields() {
        return "" +
                "package org.test;\n" +
                "\n" +
                "import jakarta.json.JsonObject;\n" +
                "import java.util.Objects;\n" +
                "\n" +
                "public class TheClassMetadataManagedFields {\n" +
                "    private String apiVersion;\n" +
                "    private String fieldsType;\n" +
                "    private JsonObject fieldsV1;\n" +
                "    private String manager;\n" +
                "    private String operation;\n" +
                "    private String subresource;\n" +
                "    private String time;\n" +
                "\n" +
                "    public String getApiVersion() {\n" +
                "        return apiVersion;\n" +
                "    }\n" +
                "\n" +
                "    public void setApiVersion(final String apiVersion) {\n" +
                "        this.apiVersion = apiVersion;\n" +
                "    }\n" +
                "\n" +
                "    public String getFieldsType() {\n" +
                "        return fieldsType;\n" +
                "    }\n" +
                "\n" +
                "    public void setFieldsType(final String fieldsType) {\n" +
                "        this.fieldsType = fieldsType;\n" +
                "    }\n" +
                "\n" +
                "    public JsonObject getFieldsV1() {\n" +
                "        return fieldsV1;\n" +
                "    }\n" +
                "\n" +
                "    public void setFieldsV1(final JsonObject fieldsV1) {\n" +
                "        this.fieldsV1 = fieldsV1;\n" +
                "    }\n" +
                "\n" +
                "    public String getManager() {\n" +
                "        return manager;\n" +
                "    }\n" +
                "\n" +
                "    public void setManager(final String manager) {\n" +
                "        this.manager = manager;\n" +
                "    }\n" +
                "\n" +
                "    public String getOperation() {\n" +
                "        return operation;\n" +
                "    }\n" +
                "\n" +
                "    public void setOperation(final String operation) {\n" +
                "        this.operation = operation;\n" +
                "    }\n" +
                "\n" +
                "    public String getSubresource() {\n" +
                "        return subresource;\n" +
                "    }\n" +
                "\n" +
                "    public void setSubresource(final String subresource) {\n" +
                "        this.subresource = subresource;\n" +
                "    }\n" +
                "\n" +
                "    public String getTime() {\n" +
                "        return time;\n" +
                "    }\n" +
                "\n" +
                "    public void setTime(final String time) {\n" +
                "        this.time = time;\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public int hashCode() {\n" +
                "        return Objects.hash(\n" +
                "                apiVersion,\n" +
                "                fieldsType,\n" +
                "                fieldsV1,\n" +
                "                manager,\n" +
                "                operation,\n" +
                "                subresource,\n" +
                "                time);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public boolean equals(final Object __other) {\n" +
                "        if (!(__other instanceof TheClassMetadataManagedFields)) {\n" +
                "            return false;\n" +
                "        }\n" +
                "        final TheClassMetadataManagedFields __otherCasted = (TheClassMetadataManagedFields) __other;\n" +
                "        return Objects.equals(apiVersion, __otherCasted.apiVersion) &&\n" +
                "            Objects.equals(fieldsType, __otherCasted.fieldsType) &&\n" +
                "            Objects.equals(fieldsV1, __otherCasted.fieldsV1) &&\n" +
                "            Objects.equals(manager, __otherCasted.manager) &&\n" +
                "            Objects.equals(operation, __otherCasted.operation) &&\n" +
                "            Objects.equals(subresource, __otherCasted.subresource) &&\n" +
                "            Objects.equals(time, __otherCasted.time);\n" +
                "    }\n" +
                "}\n";
    }

    private static String ownerRefs() {
        return "" +
                "package org.test;\n" +
                "\n" +
                "import java.util.Objects;\n" +
                "\n" +
                "public class TheClassMetadataOwnerReferences {\n" +
                "    private String apiVersion;\n" +
                "    private Boolean blockOwnerDeletion;\n" +
                "    private Boolean controller;\n" +
                "    private String kind;\n" +
                "    private String name;\n" +
                "    private String uid;\n" +
                "\n" +
                "    public String getApiVersion() {\n" +
                "        return apiVersion;\n" +
                "    }\n" +
                "\n" +
                "    public void setApiVersion(final String apiVersion) {\n" +
                "        this.apiVersion = apiVersion;\n" +
                "    }\n" +
                "\n" +
                "    public Boolean getBlockOwnerDeletion() {\n" +
                "        return blockOwnerDeletion;\n" +
                "    }\n" +
                "\n" +
                "    public void setBlockOwnerDeletion(final Boolean blockOwnerDeletion) {\n" +
                "        this.blockOwnerDeletion = blockOwnerDeletion;\n" +
                "    }\n" +
                "\n" +
                "    public Boolean getController() {\n" +
                "        return controller;\n" +
                "    }\n" +
                "\n" +
                "    public void setController(final Boolean controller) {\n" +
                "        this.controller = controller;\n" +
                "    }\n" +
                "\n" +
                "    public String getKind() {\n" +
                "        return kind;\n" +
                "    }\n" +
                "\n" +
                "    public void setKind(final String kind) {\n" +
                "        this.kind = kind;\n" +
                "    }\n" +
                "\n" +
                "    public String getName() {\n" +
                "        return name;\n" +
                "    }\n" +
                "\n" +
                "    public void setName(final String name) {\n" +
                "        this.name = name;\n" +
                "    }\n" +
                "\n" +
                "    public String getUid() {\n" +
                "        return uid;\n" +
                "    }\n" +
                "\n" +
                "    public void setUid(final String uid) {\n" +
                "        this.uid = uid;\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public int hashCode() {\n" +
                "        return Objects.hash(\n" +
                "                apiVersion,\n" +
                "                blockOwnerDeletion,\n" +
                "                controller,\n" +
                "                kind,\n" +
                "                name,\n" +
                "                uid);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public boolean equals(final Object __other) {\n" +
                "        if (!(__other instanceof TheClassMetadataOwnerReferences)) {\n" +
                "            return false;\n" +
                "        }\n" +
                "        final TheClassMetadataOwnerReferences __otherCasted = (TheClassMetadataOwnerReferences) __other;\n" +
                "        return Objects.equals(apiVersion, __otherCasted.apiVersion) &&\n" +
                "            Objects.equals(blockOwnerDeletion, __otherCasted.blockOwnerDeletion) &&\n" +
                "            Objects.equals(controller, __otherCasted.controller) &&\n" +
                "            Objects.equals(kind, __otherCasted.kind) &&\n" +
                "            Objects.equals(name, __otherCasted.name) &&\n" +
                "            Objects.equals(uid, __otherCasted.uid);\n" +
                "    }\n" +
                "}\n";
    }

    private JsonObject load(final String resource) {
        try (final JsonReader reader = Json.createReader(requireNonNull(Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(resource)))) {
            return reader.readObject();
        }
    }
}
