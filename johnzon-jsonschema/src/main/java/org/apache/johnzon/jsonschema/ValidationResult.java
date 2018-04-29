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
package org.apache.johnzon.jsonschema;

import java.util.Collection;

public class ValidationResult {
    private Collection<ValidationError> errors;

    public ValidationResult() {
        // no-op
    }

    ValidationResult(final Collection<ValidationError> errors) {
        this.errors = errors;
    }

    public boolean isSuccess() {
        return errors == null || errors.isEmpty();
    }

    public Collection<ValidationError> getErrors() {
        return errors;
    }

    public void setErrors(final Collection<ValidationError> errors) {
        this.errors = errors;
    }

    @Override
    public String toString() {
        return "ValidationResult{" +
                "errors=" + errors +
                '}';
    }

    public static class ValidationError {
        private String field;
        private String message;

        public ValidationError() {
            // no-op
        }

        public ValidationError(final String field, final String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public void setField(final String field) {
            this.field = field;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(final String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return "ValidationError{" +
                    "field='" + field + '\'' +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
}
