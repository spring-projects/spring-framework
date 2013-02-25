package org.springframework.core.convert.support;

import org.junit.Test;
import org.springframework.core.convert.converter.Converter;

import static org.junit.Assert.assertEquals;

/**
 * @author Ståle Undheim <su@ums.no>
 */
public class StringToEnumConverterFactoryTest {

enum EnumWithSubclass {
    VALUE1 {
        String get() { return "XXX"; }
    },
    VALUE2 {
        String get() { return "YYY"; }
    };

    abstract String get();
}

    @Test
    public void testConvertToEnumWithSubclass() throws Exception {
        Converter<String,? extends EnumWithSubclass> converter =
                new StringToEnumConverterFactory().getConverter(EnumWithSubclass.VALUE1.getClass());
        EnumWithSubclass enumValue = converter.convert("VALUE1");
        assertEquals(enumValue, EnumWithSubclass.VALUE1);
    }
}
