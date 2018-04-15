/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.johnzon.mapper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.json.JsonObject;
import java.beans.ConstructorProperties;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

@RunWith(value = Parameterized.class)
public class ObjectConverterWithAnnotationTest {

    private static final String MANUFACTURER_ID = "manufacturerId";
    private static final String TYPE_INDEX = "typeIndex";


    @Parameterized.Parameter
    public String accessMode;


    @Parameterized.Parameters()
    public static Iterable<String> accessModes() {
        return Arrays.asList("field", "method", "both", "strict-method");
    }


    @Test
    public void testSerializeWithObjectConverter() {

        Mapper mapper = new MapperBuilder().setAccessModeName(accessMode)
                                           .setAttributeOrder(String.CASE_INSENSITIVE_ORDER)
                                           .build();

        Cyclist cyclist = new Cyclist("Peter Sagan", new Bike("Specialized / S-Works", BikeType.ROAD));

        String json = mapper.writeObjectAsString(cyclist);
        Assert.assertNotNull(json);
        Assert.assertEquals("{" +
                              "\"bike\":{" +
                                "\"" + MANUFACTURER_ID + "\":0," +
                                "\"" + TYPE_INDEX + "\":0" +
                              "}," +
                              "\"name\":\"Peter Sagan\"" +
                            "}", json);

    }

    @Test
    public void testDeserializeWithObjectConverter() {

        Mapper mapper = new MapperBuilder().setAccessModeName(accessMode)
                                           .build();

        String json = "{" +
                        "\"name\":\"Jan Frodeno\"," +
                        "\"bike\":{" +
                          "\"" + MANUFACTURER_ID + "\":1," +
                          "\"" + TYPE_INDEX + "\":2" +
                        "}" +
                      "}";

        Cyclist expected = new Cyclist("Jan Frodeno", new Bike("Canyon", BikeType.TRIATHLON));

        Object cyclist = mapper.readObject(json, Cyclist.class);
        Assert.assertNotNull(cyclist);
        Assert.assertEquals(expected, cyclist);
    }


    @Test
    public void testSerializeObjectWithCollectionAndObjectConverter() {

        Mapper mapper = new MapperBuilder().setAccessModeName(accessMode)
                                           .setAttributeOrder(String.CASE_INSENSITIVE_ORDER)
                                           .build();

        CycleRace tourDeFrance = new CycleRace(false, true,
                                               Arrays.asList(new Cyclist("Christopher Froome", new Bike("Specialized / S-Works", BikeType.ROAD)),
                                                             new Cyclist("Alejandro Valverde", new Bike("Canyon", BikeType.ROAD)),
                                                             new Cyclist("Andr\u00e9 Greipel", new Bike("Trek", BikeType.ROAD))));// i know they don't have Trek bikes ;)

        String json = mapper.writeObjectAsString(tourDeFrance);
        Assert.assertNotNull(json);
        Assert.assertEquals("{" +
                              "\"cyclists\":[" +
                                "{" +
                                  "\"bike\":{" +
                                    "\"" + MANUFACTURER_ID + "\":0," +
                                    "\"" + TYPE_INDEX + "\":0" +
                                  "}," +
                                  "\"name\":\"Christopher Froome\"" +
                                "}," +
                                "{" +
                                  "\"bike\":{" +
                                    "\"" + MANUFACTURER_ID + "\":1," +
                                    "\"" + TYPE_INDEX + "\":0" +
                                  "}," +
                                  "\"name\":\"Alejandro Valverde\"" +
                                "}," +
                                "{" +
                                  "\"bike\":{" +
                                    "\"" + MANUFACTURER_ID + "\":2," +
                                    "\"" + TYPE_INDEX + "\":0" +
                                  "}," +
                                  "\"name\":\"Andr\u00e9 Greipel\"" +
                                "}" +
                              "]," +
                              "\"monument\":false," +
                              "\"tour\":true" +
                            "}", json);
    }

    @Test
    public void testDeserializeObjectWithCollectionAndObjectConverter() {

        CycleRace expected = new CycleRace(true, false,
                                           Arrays.asList(new Cyclist("Alexander Kristoff", new Bike("Canyon", BikeType.ROAD))));

        Mapper mapper = new MapperBuilder().setAccessModeName(accessMode)
                                           .build();

        String json = "{" +
                        "\"cyclists\":[" +
                          "{" +
                            "\"bike\": {" +
                              "\"" + MANUFACTURER_ID + "\":1," +
                              "\"" + TYPE_INDEX +"\":0" +
                            "}," +
                            "\"name\":\"Alexander Kristoff\"" +
                          "}" +
                        "]," +
                        "\"monument\":true," +
                        "\"tour\":false" +
                      "}";

        Object tourDeFlanderen = mapper.readObject(json, CycleRace.class);
        Assert.assertNotNull(tourDeFlanderen);
        Assert.assertEquals(expected, tourDeFlanderen);
    }

    @Test
    public void testDeserializeObjectWithAnnotatedConsturctorParameter() {

        String json = "{" +
                        "\"bike\": {" +
                          "\"" + MANUFACTURER_ID + "\":1," +
                          "\"" + TYPE_INDEX + "\":0" +
                        "}" +
                      "}";

        Mapper mapper = new MapperBuilder().setAccessModeName(accessMode)
                                           .setReadAttributeBeforeWrite(true)
                                           .build();

        BikeWrapper bikeWrapper = mapper.readObject(json, BikeWrapper.class);
        Assert.assertNotNull(bikeWrapper);
        Assert.assertEquals(bikeWrapper.getBike(), new Bike("Canyon", BikeType.ROAD));
    }

    public static class BikeWrapper {
        private final Bike bike;

        @ConstructorProperties(value = "bike")
        BikeWrapper(@JohnzonConverter(value = BikeConverter.class) Bike bike) {
            this.bike = bike;
        }

        Bike getBike() {
            return bike;
        }
    }


    public static class CycleRace {
        private boolean monument;
        private boolean tour;

        private List<Cyclist> cyclists;

        public CycleRace() {
        }

        public CycleRace(boolean monument, boolean tour, List<Cyclist> cyclists) {
            this.monument = monument;
            this.tour = tour;
            this.cyclists = cyclists;
        }

        public boolean isMonument() {
            return monument;
        }

        public void setMonument(boolean monument) {
            this.monument = monument;
        }

        public boolean isTour() {
            return tour;
        }

        public void setTour(boolean tour) {
            this.tour = tour;
        }

        public List<Cyclist> getCyclists() {
            return cyclists;
        }

        public void setCyclists(List<Cyclist> cyclists) {
            this.cyclists = cyclists;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            CycleRace cycleRace = (CycleRace) o;

            if (monument != cycleRace.monument) {
                return false;
            }
            if (tour != cycleRace.tour) {
                return false;
            }
            return cyclists != null ? cyclists.equals(cycleRace.cyclists) : cycleRace.cyclists == null;

        }

        @Override
        public int hashCode() {
            int result = (monument ? 1 : 0);
            result = 31 * result + (tour ? 1 : 0);
            result = 31 * result + (cyclists != null ? cyclists.hashCode() : 0);
            return result;
        }
    }

    public static class Cyclist {

        private String name;

        @JohnzonConverter(value = BikeConverter.class)
        private Bike bike;

        public Cyclist() {
        }

        public Cyclist(String name, Bike bike) {
            this.name = name;
            this.bike = bike;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @JohnzonConverter(value = BikeConverter.class)
        public Bike getBike() {
            return bike;
        }

        @JohnzonConverter(value = BikeConverter.class)
        public void setBike(Bike bike) {
            this.bike = bike;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Cyclist cyclist = (Cyclist) o;

            if (name != null ? !name.equals(cyclist.name) : cyclist.name != null) {
                return false;
            }
            return bike != null ? bike.equals(cyclist.bike) : cyclist.bike == null;

        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (bike != null ? bike.hashCode() : 0);
            return result;
        }
    }

    public static class Bike {
        private String manufacturer;
        private BikeType type;

        public Bike() {
        }

        public Bike(String manufacturer, BikeType type) {
            this.manufacturer = manufacturer;
            this.type = type;
        }

        public String getManufacturer() {
            return manufacturer;
        }

        public void setManufacturer(String manufacturer) {
            this.manufacturer = manufacturer;
        }

        public BikeType getType() {
            return type;
        }

        public void setType(BikeType type) {
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Bike bike = (Bike) o;

            if (manufacturer != null ? !manufacturer.equals(bike.manufacturer) : bike.manufacturer != null) {
                return false;
            }
            return type == bike.type;

        }

        @Override
        public int hashCode() {
            int result = manufacturer != null ? manufacturer.hashCode() : 0;
            result = 31 * result + (type != null ? type.hashCode() : 0);
            return result;
        }
    }

    public enum BikeType {
        ROAD,
        TIME_TRIAL,
        TRIATHLON
    }

    public static class BikeConverter implements ObjectConverter.Codec<Bike> {

        public static final List<String> MANUFACTURERS = Arrays.asList("Specialized / S-Works", "Canyon", "Trek", "Scott");


        @Override
        public void writeJson(Bike instance, MappingGenerator jsonbGenerator) {
            jsonbGenerator.getJsonGenerator().write(MANUFACTURER_ID, MANUFACTURERS.indexOf(instance.getManufacturer()));

            // i know you should never use this in production but its good for our sample ;)
            jsonbGenerator.getJsonGenerator().write(TYPE_INDEX, instance.getType().ordinal());
        }

        @Override
        public Bike fromJson(JsonObject jsonObject, Type targetType, MappingParser parser) {
            return new Bike(MANUFACTURERS.get(jsonObject.getInt(MANUFACTURER_ID)),
                            BikeType.values()[jsonObject.getInt(TYPE_INDEX)]);
        }
    }
}
