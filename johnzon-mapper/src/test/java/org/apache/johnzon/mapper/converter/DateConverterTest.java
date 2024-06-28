package org.apache.johnzon.mapper.converter;

import org.junit.Test;

import static org.junit.Assert.assertThrows;

public class DateConverterTest {
    @Test
    public void rejectNullPattern() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            final String pattern = null;
            new DateConverter(pattern);
        });
    }
}