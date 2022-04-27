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
package org.apache.johnzon.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public abstract class AbstractJsonFactory implements Serializable {
    public static final String ENCODING = "org.apache.johnzon.encoding";
    public static final String BUFFER_STRATEGY = "org.apache.johnzon.buffer-strategy";
    public static final BufferStrategy DEFAULT_BUFFER_STRATEGY = BufferStrategyFactory.valueOf(System.getProperty(BUFFER_STRATEGY, "QUEUE"));
    
    protected final Map<String, Object> internalConfig = new HashMap<String, Object>();
    
    protected AbstractJsonFactory(final Map<String, ?> config, Collection<String> supportedConfigKeys,
                                  final Collection<String> defaultSupportedConfigKeys) {
        if(config != null && !config.isEmpty()) {
            if(defaultSupportedConfigKeys != null) {
                supportedConfigKeys = new ArrayList<>(supportedConfigKeys);
                supportedConfigKeys.addAll(defaultSupportedConfigKeys);
            }
            
            for (String configKey : config.keySet()) {
                if(supportedConfigKeys.contains(configKey)) {
                    internalConfig.put(configKey, config.get(configKey));
                } else {
                    Logger.getLogger(this.getClass().getName()).warning(configKey + " is not supported by " + getClass().getName());
                }
            }
        }
    }

    protected BufferStrategy getBufferProvider() {
        final Object name = internalConfig.get(BUFFER_STRATEGY);
        if (name != null) {
            return BufferStrategyFactory.valueOf(name.toString());
        }
        return DEFAULT_BUFFER_STRATEGY;
    }

    protected int getInt(final String key, final int defaultValue) {
        final Object intValue = internalConfig.get(key);
        if (intValue == null) {
            return defaultValue;
        } else if (Number.class.isInstance(intValue)) {
            return Number.class.cast(intValue).intValue();
        }
        return Integer.parseInt(intValue.toString());
    }

    protected boolean getBool(final String key, final boolean defaultValue) {
        final Object boolValue = internalConfig.get(key);
        if (boolValue == null) {
            return defaultValue;
        } else if (Boolean.class.isInstance(boolValue)) {
            return Boolean.class.cast(boolValue);
        }
        return Boolean.parseBoolean(boolValue.toString());
    }

    protected String getString(final String key, final String defaultValue) {
        final Object value = internalConfig.get(key);
        if (value == null) {
            return defaultValue;
        } else if (String.class.isInstance(value)) {
            return String.class.cast(value);
        }
        return value.toString();
    }

}
