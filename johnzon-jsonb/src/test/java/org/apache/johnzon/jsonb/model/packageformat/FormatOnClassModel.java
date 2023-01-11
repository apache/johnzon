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
package org.apache.johnzon.jsonb.model.packageformat;

import java.util.Date;

import jakarta.json.bind.annotation.JsonbDateFormat;

import org.apache.johnzon.jsonb.model.Holder;

@JsonbDateFormat(value = "E DD MMM yyyy HH:mm:ss", locale = "de")
public class FormatOnClassModel implements Holder<Date> {
    private Date instance;

    @Override
    public Date getInstance() {
        return instance;
    }

    @Override
    public void setInstance(final Date instance) {
        this.instance = instance;
    }
}
