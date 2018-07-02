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
package org.apache.johnzon.jsonschema.regex;

import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;

import org.jcodings.specific.ASCIIEncoding;
import org.joni.Matcher;
import org.joni.Option;
import org.joni.Regex;
import org.joni.Syntax;

public class JoniRegex implements Predicate<CharSequence> {
    private final Regex regex;
    private final String toStr;

    public JoniRegex(final String regex) {
        if (regex.startsWith("/") && regex.length() > 1) {
            final int end = regex.lastIndexOf('/');
            if (end < 0) {
                this.regex = new Regex(regex);
            } else {
                final String optStr = regex.substring(end + 1);
                int option = Option.SINGLELINE;

                if (optStr.contains("i")) {
                    option |= Option.IGNORECASE;
                } else if (optStr.contains("m")) {
                    option &= ~Option.SINGLELINE;
                    option |= Option.NEGATE_SINGLELINE;
                }

                this.regex = new Regex(
                        regex.getBytes(StandardCharsets.US_ASCII), 1, end, option,
                        ASCIIEncoding.INSTANCE, Syntax.ECMAScript);
            }
        } else {
            this.regex = new Regex(regex);
        }
        this.toStr = "JoniRegex{" + regex + '}';
    }

    @Override
    public boolean test(final CharSequence string) {
        return regex.matcher(string.toString().getBytes(StandardCharsets.UTF_8))
             .search(0, string.length(), Option.NONE) > Matcher.FAILED;
    }

    @Override
    public String toString() {
        return toStr;
    }
}
