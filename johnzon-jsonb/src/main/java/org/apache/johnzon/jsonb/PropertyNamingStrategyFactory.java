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
package org.apache.johnzon.jsonb;

import jakarta.json.bind.config.PropertyNamingStrategy;
import java.util.function.Function;

public class PropertyNamingStrategyFactory {
    private final Object value;

    public PropertyNamingStrategyFactory(final Object value) {
        this.value = value;
    }

    public PropertyNamingStrategy create() {
        if (String.class.isInstance(value)) {
            final String val = value.toString();
            switch (val) {
                case PropertyNamingStrategy.IDENTITY:
                    return propertyName -> propertyName;
                case PropertyNamingStrategy.LOWER_CASE_WITH_DASHES:
                    return new ConfigurableNamingStrategy(Character::toLowerCase, '-');
                case PropertyNamingStrategy.LOWER_CASE_WITH_UNDERSCORES:
                    return new ConfigurableNamingStrategy(Character::toLowerCase, '_');
                case PropertyNamingStrategy.UPPER_CAMEL_CASE:
                    return camelCaseStrategy();
                case PropertyNamingStrategy.UPPER_CAMEL_CASE_WITH_SPACES:
                    final PropertyNamingStrategy camelCase = camelCaseStrategy();
                    final PropertyNamingStrategy space = new ConfigurableNamingStrategy(Function.identity(), ' ');
                    return propertyName -> camelCase.translateName(space.translateName(propertyName));
                case PropertyNamingStrategy.CASE_INSENSITIVE:
                    return propertyName -> propertyName;
                default:
                    throw new IllegalArgumentException(val + " unknown as PropertyNamingStrategy");
            }
        }
        if (PropertyNamingStrategy.class.isInstance(value)) {
            return PropertyNamingStrategy.class.cast(value);
        }
        throw new IllegalArgumentException(value + " not supported as PropertyNamingStrategy");
    }

    private PropertyNamingStrategy camelCaseStrategy() {
        return propertyName -> Character.toUpperCase(propertyName.charAt(0)) + (propertyName.length() > 1 ? propertyName.substring(1) : "");
    }

    private static class ConfigurableNamingStrategy implements PropertyNamingStrategy {
        private final Function<Character, Character> converter;
        private final char separator;

        public ConfigurableNamingStrategy(final Function<Character, Character> wordConverter, final char sep) {
            this.converter = wordConverter;
            this.separator = sep;
        }

        @Override
        public String translateName(final String propertyName) {
            final StringBuilder global = new StringBuilder();

            final StringBuilder current = new StringBuilder();
            for (int i = 0; i < propertyName.length(); i++) {
                final char c = propertyName.charAt(i);
                if (Character.isUpperCase(c)) {
                    final char transformed = converter.apply(c);
                    if (current.length() > 0) {
                        global.append(current).append(separator);
                        current.setLength(0);
                    }
                    current.append(transformed);
                } else {
                    current.append(c);
                }
            }
            if (current.length() > 0) {
                global.append(current);
            } else {
                global.setLength(global.length() - 1); // remove last sep
            }
            return global.toString();
        }
    }
}
