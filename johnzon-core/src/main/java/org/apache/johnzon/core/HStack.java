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

public class HStack<T> implements Serializable {

    private Node<T> topElement = null;
    private int size;


    private static final class Node<T> implements Serializable {
        private final Node<T> previous;
        private final T object;

        private Node(final Node<T> previous, final T object) {
            super();
            this.previous = previous;
            this.object = object;
        }
    }


    void push(T object) {
        topElement = new Node<T>(topElement, object);
        size++;
    }

    T pop() {

        if (topElement == null) {
            return null;
        }

        T tmp = topElement.object;
        topElement = topElement.previous;
        size--;
        return tmp;
    }

    T peek() {
        return topElement == null ? null : topElement.object;
    }

    int size() {
        return size;
    }

}
