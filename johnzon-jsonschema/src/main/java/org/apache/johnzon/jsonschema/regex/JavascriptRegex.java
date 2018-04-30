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

import java.util.function.Predicate;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class JavascriptRegex implements Predicate<CharSequence> {

    private static final ScriptEngine ENGINE;

    static {
        ENGINE = new ScriptEngineManager().getEngineByName("javascript");
    }

    private final String regex;

    private final String indicators;

    public JavascriptRegex(final String regex) {
        if (regex.startsWith("/") && regex.length() > 1) {
            final int end = regex.lastIndexOf('/');
            if (end < 0) {
                this.regex = regex;
                this.indicators = "";
            } else {
                this.regex = regex.substring(1, end);
                this.indicators = regex.substring(end + 1);
            }
        } else {
            this.regex = regex;
            this.indicators = "";
        }
    }

    @Override
    public boolean test(final CharSequence string) {
        final Bindings bindings = ENGINE.createBindings();
        bindings.put("text", string);
        bindings.put("regex", regex);
        bindings.put("indicators", indicators);
        try {
            return Boolean.class.cast(ENGINE.eval("new RegExp(regex, indicators).test(text)", bindings));
        } catch (final ScriptException e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return "JavascriptRegex{/" + regex + "/" + indicators + '}';
    }
}
