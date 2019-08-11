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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;

@RunWith(Parameterized.class)
public class AdvancedGenericsTest {

    @Parameterized.Parameter
    public String accessMode;

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<String> modes() {
        return asList("field", "method", "both", "strict-method");
    }

    @Test
    public void testSerializeHierarchyOne() {
        Mapper mapper = new MapperBuilder().setAccessModeName(accessMode)
                                           .build();

        {
            String customerAsString = mapper.writeObjectAsString(new Customer("Bruce", "Wayne"));

            Assert.assertTrue("Serialized String must contain \"firstName\":\"Bruce\"", customerAsString.contains("\"firstName\":\"Bruce\""));
            Assert.assertTrue("Serialized String must contain \"lastName\":\"Wayne\"", customerAsString.contains("\"lastName\":\"Wayne\""));
            Assert.assertFalse("Serialized String must not contain \"id\": " + customerAsString, customerAsString.contains("\"id\""));
            Assert.assertFalse("Serialized String must not contain \"version\"", customerAsString.contains("\"version\""));
        }

        {
            String customerAsString = mapper.writeObjectAsString(new Customer(160784L, 35, "Clark", "Kent"));

            Assert.assertTrue("Serialized String must contain \"firstName\":\"Bruce\"", customerAsString.contains("\"firstName\":\"Clark\""));
            Assert.assertTrue("Serialized String must contain \"lastName\":\"Wayne\"", customerAsString.contains("\"lastName\":\"Kent\""));
            Assert.assertTrue("Serialized String must contain \"id\":160784", customerAsString.contains("\"id\":160784"));
            Assert.assertTrue("Serialized String must contain \"version\":35", customerAsString.contains("\"version\":35"));
        }
    }

    @Test
    public void testDeserializeHierarchyOne() {
        Mapper mapper = new MapperBuilder().setAccessModeName(accessMode)
                                           .build();

        {
            Customer customer = mapper.readObject("{ \"lastName\":\"Odinson\", \"firstName\":\"Thor\" }", Customer.class);

            Assert.assertNotNull(customer);
            Assert.assertNull(customer.getId());
            Assert.assertNull(customer.getVersion());
            Assert.assertEquals("Thor", customer.getFirstName());
            Assert.assertEquals("Odinson", customer.getLastName());
        }

        {
            // id as JsonString
            Customer customer = mapper.readObject("{ \"firstName\":\"Loki\", \"lastName\":\"Laufeyson\", \"id\":\"160883\" }", Customer.class);

            Assert.assertNotNull(customer);
            Assert.assertEquals(160883L, customer.getId().longValue());
            Assert.assertNull(customer.getVersion());
            Assert.assertEquals("Loki", customer.getFirstName());
            Assert.assertEquals("Laufeyson", customer.getLastName());
        }

        {
            // id as JsonNumber, version as JsonString
            Customer customer = mapper.readObject("{ \"lastName\":\"Banner\", \"firstName\":\"Bruce\", \"id\":7579, \"version\":\"74\" }", Customer.class);

            Assert.assertNotNull(customer);
            Assert.assertEquals(7579L,  customer.getId().longValue());
            Assert.assertEquals(74, customer.getVersion().intValue());
            Assert.assertEquals("Bruce", customer.firstName);
            Assert.assertEquals("Banner", customer.lastName);

        }
    }

    @Test
    public void testSerializeHierarchyTwo() {
        Mapper mapper = new MapperBuilder().setAccessModeName(accessMode)
                                           .build();

        String vipCustomerAsString = mapper.writeObjectAsString(new VIPCustomer(new Customer(15L, 37, "Lois", "Lane"), 12.5));

        Assert.assertNotNull(vipCustomerAsString);
        Assert.assertTrue("Serialized String must contain \"firstName\":\"Lois\"", vipCustomerAsString.contains("\"firstName\":\"Lois\""));
        Assert.assertTrue("Serialized String must contain \"lastName\":\"Lane\"", vipCustomerAsString.contains("\"lastName\":\"Lane\""));
        Assert.assertTrue("Serialized String must contain \"id\":15", vipCustomerAsString.contains("\"id\":15"));
        Assert.assertTrue("Serialized String must contain \"version\":37", vipCustomerAsString.contains("\"version\":37"));
        Assert.assertTrue("Serialized String must contain \"discount\":12.5", vipCustomerAsString.contains("\"discount\":12.5"));
    }

    @Test
    public void testDeserializeHierarchyTwo() {
        Mapper mapper = new MapperBuilder().setAccessModeName(accessMode)
                                           .build();

        {
            // id as JsonString
            VIPCustomer customer = mapper.readObject("{ \"discount\":5, \"id\":\"888\", \"lastName\":\"Ross\", \"firstName\":\"Betty\", \"version\":\"5555\" }", VIPCustomer.class);

            Assert.assertNotNull(customer);
            Assert.assertEquals(888L, customer.getId().longValue());
            Assert.assertEquals(5555, customer.getVersion().intValue());
            Assert.assertEquals("Betty", customer.getFirstName());
            Assert.assertEquals("Ross", customer.getLastName());
            Assert.assertEquals(5.0, customer.getDiscount(), 0);
        }

        {
            // id as JsonNumber
            VIPCustomer customer = mapper.readObject("{ \"discount\":25.5, \"id\":478965, \"firstName\":\"Selina\", \"version\":\"3\", \"lastName\":\"Kyle\" }",
                                                     VIPCustomer.class);

            Assert.assertNotNull(customer);
            Assert.assertEquals(478965L, customer.getId().longValue());
            Assert.assertEquals(3, customer.getVersion().intValue());
            Assert.assertEquals("Selina", customer.getFirstName());
            Assert.assertEquals("Kyle", customer.getLastName());
            Assert.assertEquals(25.5, customer.getDiscount(), 0);
        }
    }

    @Test
    public void testSerializeHierarchyThree() {
        Mapper mapper = new MapperBuilder().setAccessModeName(accessMode)
                                           .build();

        String vipCustomerAsString = mapper.writeObjectAsString(new GoldCustomer(new VIPCustomer(new Customer(6547L, 497, "Peter", "Parker"), 4.2), 1));

        Assert.assertNotNull(vipCustomerAsString);
        Assert.assertTrue("Serialized String must contain \"firstName\":\"Lois\"", vipCustomerAsString.contains("\"firstName\":\"Peter\""));
        Assert.assertTrue("Serialized String must contain \"lastName\":\"Lane\"", vipCustomerAsString.contains("\"lastName\":\"Parker\""));
        Assert.assertTrue("Serialized String must contain \"id\":6547", vipCustomerAsString.contains("\"id\":6547"));
        Assert.assertTrue("Serialized String must contain \"version\":497", vipCustomerAsString.contains("\"version\":497"));
        Assert.assertTrue("Serialized String must contain \"discount\":4.2", vipCustomerAsString.contains("\"discount\":4.2"));
        Assert.assertTrue("Serialized String must contain \"rating\":1", vipCustomerAsString.contains("\"rating\":1"));
    }

    @Test
    public void testDeserializeHierarchyThree() {
        Mapper mapper = new MapperBuilder().setAccessModeName(accessMode)
                                           .build();

        // id as JsonString, without rating
        GoldCustomer customer = mapper.readObject("{ \"discount\":5, \"id\":\"8745321\", \"lastName\":\"Watson\", \"firstName\":\"Mary Jane\", \"version\":\"821\" }",
                                                  GoldCustomer.class);

        Assert.assertNotNull(customer);
        Assert.assertEquals(8745321L, customer.getId().longValue());
        Assert.assertEquals(821, customer.getVersion().intValue());
        Assert.assertEquals("Mary Jane", customer.getFirstName());
        Assert.assertEquals("Watson", customer.getLastName());
        Assert.assertEquals(5.0, customer.getDiscount(), 0);
        Assert.assertEquals(0, customer.getRating());
    }


    private static abstract class Versioned<T> {

        private T id;
        private Integer version;

        public Versioned() {
        }

        public Versioned(T id, Integer version) {
            this.id = id;
            this.version = version;
        }

        public T getId() {
            return id;
        }

        public void setId(T id) {
            this.id = id;
        }

        public Integer getVersion() {
            return version;
        }

        public void setVersion(Integer version) {
            this.version = version;
        }
    }

    private static class Customer extends Versioned<Long> {

        private String firstName;
        private String lastName;

        public Customer() {
            this(null, null);
        }

        private Customer(String firstName, String lastName) {
            this(null, null, firstName, lastName);
        }

        private Customer(Long id, Integer version, String firstName, String lastName) {
            super(id, version);
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
    }

    private static class VIPCustomer extends Customer {

        private Double discount;

        private VIPCustomer() {

        }

        public VIPCustomer(String firstName, String lastName, Double discount) {
            super(null, null, firstName, lastName);
            this.discount = discount;
        }

        public VIPCustomer(Customer customer, Double discount) {
            super(customer.getId(), customer.getVersion(), customer.getFirstName(), customer.getLastName());
            this.discount = discount;
        }

        public Double getDiscount() {
            return discount;
        }

        public void setDiscount(Double discount) {
            this.discount = discount;
        }
    }

    private static class GoldCustomer extends VIPCustomer {
        private int rating;

        private GoldCustomer() {
        }

        private GoldCustomer(VIPCustomer customer, int rating) {
            super(customer, customer.getDiscount());
            this.rating = rating;
        }

        public int getRating() {
            return rating;
        }

        public void setRating(int rating) {
            this.rating = rating;
        }
    }

}
