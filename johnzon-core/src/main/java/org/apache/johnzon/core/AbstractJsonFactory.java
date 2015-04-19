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
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public abstract class AbstractJsonFactory implements Serializable{

    protected final Logger logger = Logger.getLogger(this.getClass().getName());
    
    public static final String BUFFER_STRATEGY = "org.apache.johnzon.buffer-strategy";
    public static final BufferStrategy DEFAULT_BUFFER_STRATEGY = BufferStrategy.QUEUE;
    
    protected final Map<String, Object> internalConfig = new HashMap<String, Object>();
    
    protected AbstractJsonFactory(final Map<String, ?> config, Collection<String> supportedConfigKeys, Collection<String> defaultSupportedConfigKeys) {
        if(config != null && config.size() > 0) {
            
            if(defaultSupportedConfigKeys != null) {
                supportedConfigKeys = new ArrayList<String>(supportedConfigKeys);
                supportedConfigKeys.addAll(defaultSupportedConfigKeys);
            }
            
            for (String configKey : config.keySet()) {
                if(supportedConfigKeys.contains(configKey)) {
                    internalConfig.put(configKey, config.get(configKey));
                } else {
                    logger.warning(configKey + " is not supported by " + getClass().getName());
                }
            }
        }
    }

    protected BufferStrategy getBufferProvider() {
        final Object name = internalConfig.get(BUFFER_STRATEGY);
        if (name != null) {
            return BufferStrategy.valueOf(name.toString().toUpperCase(Locale.ENGLISH));
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

}
