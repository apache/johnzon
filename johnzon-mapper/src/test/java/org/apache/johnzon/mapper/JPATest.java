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
package org.apache.johnzon.mapper;

import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import java.io.StringReader;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;

import static jakarta.persistence.TemporalType.DATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class JPATest {
    private EntityManagerFactory emf;
    private EntityManager em;
    private long id;

    @Before
    public void createEm() {
        emf = OpenJPAPersistence.createEntityManagerFactory("johnzon", JPATest.class.getSimpleName() + ".xml", new HashMap() {{
            put("openjpa.jdbc.SynchronizeMappings", "buildSchema(ForeignKeys=true)");
        }});
        em = emf.createEntityManager();

        final AnEntity entity = new AnEntity();
        final Date date = new Date(0);
        entity.setDate(date); // fixed date for testing
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        id = entity.getId();
        em.clear();
    }

    @After
    public void clearEm() {
        em.close();
        emf.close();
    }

    @Test
    public void ensureStateIsIgnoredAndDateIsCorrect() {
        final AnEntity entity = em.find(AnEntity.class, id);
        final Mapper mapper = new MapperBuilder().setAttributeOrder(new Comparator<String>() {
            @Override
            public int compare(final String o1, final String o2) {
                return o1.compareTo(o2);
            }
        }).build();
        final String asString = mapper.writeObjectAsString(entity);
        final AnEntity entity1 = mapper.readObject(new StringReader(asString), AnEntity.class);
        // issue with date is the timezone of the test runner, make it simple for now
        assertEquals(entity.getDate(), entity1.getDate());
        assertNotSame(entity.getDate(), entity1.getDate());
        assertTrue(asString.contains("\"id\":" + id));
    }

    @Entity
    public static class AnEntity {
        @Id
        @GeneratedValue
        private long id;

        @Temporal(DATE)
        private Date date;

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(final Date date) {
            this.date = date;
        }
    }
}
