/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.johnzon.jsonb.symmetry.adapter.array;

import jakarta.json.bind.adapter.JsonbAdapter;
import jakarta.json.bind.annotation.JsonbTypeAdapter;
import org.apache.johnzon.jsonb.symmetry.Calls;
import org.apache.johnzon.jsonb.symmetry.SymmetryTest;
import org.junit.Before;

public abstract class ArrayAdapterOnClassTest extends SymmetryTest {

    protected static final Calls CALLS = new Calls();

    @Before
    public void reset() {
        CALLS.reset();
    }

    public static String calls() {
        return CALLS.get();
    }

    @JsonbTypeAdapter(Adapter.EmailClass.class)
    public static class Email {
        final String user;
        final String domain;
        final String call;

        public Email(final String user, final String domain) {
            this(user, domain, null);
        }

        public Email(final String user, final String domain, final String call) {
            this.user = user;
            this.domain = domain;
            this.call = call;
        }

        @Override
        public String toString() {
            if (call == null) {
                return user + "@" + domain;
            } else {
                return user + "@" + domain + ":" + call;
            }
        }
    }

    public abstract static class Adapter implements JsonbAdapter<Email, String> {

        @Override
        public String adaptToJson(final Email obj) {
            return obj.user + "@" + obj.domain + ":" + CALLS.called(this);
        }

        @Override
        public Email adaptFromJson(final String obj) {
            final String[] parts = obj.split("[@:]");
            return new Email(parts[0], parts[1], CALLS.called(this));
        }

        public static final class Getter extends Adapter {
        }

        public static final class Setter extends Adapter {
        }

        public static final class Field extends Adapter {
        }

        public static final class Constructor extends Adapter {
        }

        public static final class Config extends Adapter {
        }

        public static final class EmailClass extends Adapter {
        }
    }
}
