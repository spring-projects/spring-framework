package org.springframework.ui.format;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.ui.format.number.IntegerFormatter;

public class GenericFormatterRegistryTests {

	private GenericFormatterRegistry registry;
	
	@Before
	public void setUp() {
		registry = new GenericFormatterRegistry();
	}

	@Test
	@Ignore
	public void testAdd() {
		registry.add(new IntegerFormatter());
		Formatter formatter = registry.getFormatter(typeDescriptor(Long.class));
		String formatted = formatter.format(new Long(3), Locale.US);
		assertEquals("3", formatted);
	}

	@Test
	@Ignore
	public void testAddByOtherObjectType() {
		registry.add(Integer.class, new IntegerFormatter());
		Formatter formatter = registry.getFormatter(typeDescriptor(Integer.class));
		String formatted = formatter.format(new Integer(3), Locale.US);
		assertEquals("3", formatted);
	}

	@Test
	@Ignore
	public void testAddAnnotationFormatterFactory() {
	}

	private static TypeDescriptor typeDescriptor(Class<?> clazz) {
		return TypeDescriptor.valueOf(clazz);
	}
}
