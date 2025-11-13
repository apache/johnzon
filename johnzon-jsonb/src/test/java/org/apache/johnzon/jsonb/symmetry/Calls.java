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
package org.apache.johnzon.jsonb.symmetry;

import java.util.ArrayList;
import java.util.List;

public class Calls {

    private final List<String> calls = new ArrayList<>();

    public String called() {
        final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        if (stackTrace.length > 2) {
            final StackTraceElement caller = stackTrace[2];
            final String className = caller.getClassName();
            final String methodName = caller.getMethodName();

            final int lastDot = className.lastIndexOf('.');
            final int lastDollar = className.lastIndexOf('$');
            final String simpleName;
            if (lastDollar == 0 && lastDot == 0) {
                simpleName = className;
            } else {
                final int start = Math.max(lastDollar, lastDot) + 1;
                simpleName = className.substring(start);
            }

            final String result = simpleName + "." + methodName;
            calls.add(result);
            return result;
        }
        return null;
    }

    public String called(final Object instance) {
        final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        if (stackTrace.length > 2) {
            final StackTraceElement caller = stackTrace[2];
            final String simpleName = instance.getClass().getSimpleName();
            final String methodName = caller.getMethodName();


            final String result = simpleName + "." + methodName;
            calls.add(result);
            return result;
        }
        return null;
    }

    public List<String> list() {
        return new ArrayList<>(calls);
    }

    public String get() {
        final String result = String.join("\n", calls);
        calls.clear();
        return result;
    }

    public void reset() {
        calls.clear();
    }
}