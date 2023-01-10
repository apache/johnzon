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
package org.apache.johnzon.jaxrs.xml;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObjectBuilder;
import javax.xml.stream.XMLStreamException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public class WadlDocumentToJson {
    private final JsonBuilderFactory builderFactory = Json.createBuilderFactory(Collections.<String, Object>emptyMap());

    public String convert(final Document doc) throws XMLStreamException {
        final JsonObjectBuilder builder = builderFactory.createObjectBuilder();
        if (doc.getChildNodes().getLength() != 1) {
            return "{}";
        }
        final Node item = doc.getChildNodes().item(0);
        return builder.add(item.getNodeName(), createNode(item)).build().toString();
    }

    private void addChildrens(/*final String nodeName, */final JsonObjectBuilder builder, final NodeList children) {
        final Map<String, Collection<Node>> nodesByName = new LinkedHashMap<String, Collection<Node>>();
        for (int i = 0; i < children.getLength(); i++) {
            final Node node = children.item(i);
            if ("#text".equals(node.getNodeName())) {
                continue;
            }

            final String name = node.getNodeName();
            Collection<Node> nodes = nodesByName.get(name);
            if (nodes == null) {
                nodes = new LinkedList<Node>();
                nodesByName.put(name, nodes);
            }
            nodes.add(node);
        }

        for (final Map.Entry<String, Collection<Node>> entry : nodesByName.entrySet()) {
            final JsonArrayBuilder arrayBuilder = builderFactory.createArrayBuilder();
            for (final Node n : entry.getValue()) {
                final JsonObjectBuilder jsonObjectBuilder = createNode(n);
                if (jsonObjectBuilder != null) {
                    arrayBuilder.add(jsonObjectBuilder);
                }
            }
            builder.add(entry.getKey(), arrayBuilder);
        }
    }

    private JsonObjectBuilder createNode(final Node node) {
        JsonObjectBuilder childBuilder = null;

        if (node.hasAttributes()) {
            childBuilder = builderFactory.createObjectBuilder();
            final NamedNodeMap attributes = node.getAttributes();
            for (int j = 0; j < attributes.getLength(); j++) {
                final Node namedItem = attributes.item(j);
                childBuilder.add(namedItem.getNodeName(), namedItem.getNodeValue());
            }
        }

        if (node.hasChildNodes()) {
            if (childBuilder == null) {
                childBuilder = builderFactory.createObjectBuilder();
            }
            addChildrens(/*node.getNodeName(),*/ childBuilder, node.getChildNodes());
        }
        return childBuilder;
    }
}
