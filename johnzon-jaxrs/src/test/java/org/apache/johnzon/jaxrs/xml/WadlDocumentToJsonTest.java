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

import org.junit.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertEquals;

public class WadlDocumentToJsonTest {
    @Test
    public void xmlToJson() throws Exception {
        final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(new ByteArrayInputStream(("" +
                    "<application xmlns=\"http://wadl.dev.java.net/2009/02\">\n" +
                    "    <resources base=\"http://example.com/api\">\n" +
                    "        <resource path=\"books\">\n" +
                    "            <method name=\"GET\"/>\n" +
                    "            <resource path=\"{bookId}\">\n" +
                    "                <param required=\"true\" style=\"template\" name=\"bookId\"/>\n" +
                    "                <method name=\"GET\"/>\n" +
                    "                <method name=\"DELETE\"/>\n" +
                    "                <resource path=\"reviews\">\n" +
                    "                    <method name=\"GET\">\n" +
                    "                        <request>\n" +
                    "                            <param name=\"page\" required=\"false\" default=\"1\" style=\"query\"/>\n" +
                    "                            <param name=\"size\" required=\"false\" default=\"20\" style=\"query\"/>\n" +
                    "                        </request>\n" +
                    "                    </method>\n" +
                    "                </resource>\n" +
                    "            </resource>\n" +
                    "        </resource>\n" +
                    "        <resource path=\"readers\">\n" +
                    "            <method name=\"GET\"/>\n" +
                    "        </resource>\n" +
                    "    </resources>\n" +
                    "</application>").getBytes()));

        final String json = new WadlDocumentToJson().convert(doc);
        assertEquals("{\"application\":{\"xmlns\":\"http://wadl.dev.java.net/2009/02\",\"resources\":[{\"base\":\"http://example.com/api\""
                + ",\"resource\":[{\"path\":\"books\",\"method\":[{\"name\":\"GET\"}],\"resource\":[{\"path\":\"{bookId}\",\"param\":[{\"name\""
                + ":\"bookId\",\"required\":\"true\",\"style\":\"template\"}],\"method\":[{\"name\":\"GET\"},{\"name\":\"DELETE\"}],\"resource\""
                + ":[{\"path\":\"reviews\",\"method\":[{\"name\":\"GET\",\"request\":[{\"param\":[{\"default\":\"1\",\"name\":\"page\",\"required\""
                + ":\"false\",\"style\":\"query\"},{\"default\":\"20\",\"name\":\"size\",\"required\":\"false\",\"style\":\"query\"}]}]}]}]}]},{\"path\""
                + ":\"readers\",\"method\":[{\"name\":\"GET\"}]}]}]}}", json);
    }
}
