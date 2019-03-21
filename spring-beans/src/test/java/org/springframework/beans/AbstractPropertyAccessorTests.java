/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans;

import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.logging.LogFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.beans.support.DerivedFromProtectedBaseBean;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.tests.Assume;
import org.springframework.tests.TestGroup;
import org.springframework.tests.sample.beans.BooleanTestBean;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.IndexedTestBean;
import org.springframework.tests.sample.beans.NumberTestBean;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;

/**
 * Shared tests for property accessors.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Alef Arendsen
 * @author Arjen Poutsma
 * @author Chris Beams
 * @author Dave Syer
 * @author Stephane Nicoll
 */
public abstract class AbstractPropertyAccessorTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();


	protected abstract AbstractPropertyAccessor createAccessor(Object target);


	@Test
	public void createWithNullTarget() {
		try {
			createAccessor(null);
			fail("Must throw an exception when constructed with null object");
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	@Test
	public void isReadableProperty() {
		AbstractPropertyAccessor accessor = createAccessor(new Simple("John", 2));

		assertThat(accessor.isReadableProperty("name"), is(true));
	}

	@Test
	public void isReadablePropertyNotReadable() {
		AbstractPropertyAccessor accessor = createAccessor(new NoRead());

		assertFalse(accessor.isReadableProperty("age"));
	}

	/**
	 * Shouldn't throw an exception: should just return false
	 */
	@Test
	public void isReadablePropertyNoSuchProperty() {
		AbstractPropertyAccessor accessor = createAccessor(new NoRead());

		assertFalse(accessor.isReadableProperty("xxxxx"));
	}

	@Test
	public void isReadablePropertyNull() {
		AbstractPropertyAccessor accessor = createAccessor(new NoRead());

		thrown.expect(IllegalArgumentException.class);
		accessor.isReadableProperty(null);
	}

	@Test
	public void isWritableProperty() {
		AbstractPropertyAccessor accessor = createAccessor(new Simple("John", 2));

		assertThat(accessor.isWritableProperty("name"), is(true));
	}

	@Test
	public void isWritablePropertyNull() {
		AbstractPropertyAccessor accessor = createAccessor(new NoRead());

		thrown.expect(IllegalArgumentException.class);
		accessor.isWritableProperty(null);
	}

	@Test
	public void isWritablePropertyNoSuchProperty() {
		AbstractPropertyAccessor accessor = createAccessor(new NoRead());

		assertFalse(accessor.isWritableProperty("xxxxx"));
	}

	@Test
	public void isReadableWritableForIndexedProperties() {
		IndexedTestBean target = new IndexedTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);

		assertTrue(accessor.isReadableProperty("array"));
		assertTrue(accessor.isReadableProperty("list"));
		assertTrue(accessor.isReadableProperty("set"));
		assertTrue(accessor.isReadableProperty("map"));
		assertFalse(accessor.isReadableProperty("xxx"));

		assertTrue(accessor.isWritableProperty("array"));
		assertTrue(accessor.isWritableProperty("list"));
		assertTrue(accessor.isWritableProperty("set"));
		assertTrue(accessor.isWritableProperty("map"));
		assertFalse(accessor.isWritableProperty("xxx"));

		assertTrue(accessor.isReadableProperty("array[0]"));
		assertTrue(accessor.isReadableProperty("array[0].name"));
		assertTrue(accessor.isReadableProperty("list[0]"));
		assertTrue(accessor.isReadableProperty("list[0].name"));
		assertTrue(accessor.isReadableProperty("set[0]"));
		assertTrue(accessor.isReadableProperty("set[0].name"));
		assertTrue(accessor.isReadableProperty("map[key1]"));
		assertTrue(accessor.isReadableProperty("map[key1].name"));
		assertTrue(accessor.isReadableProperty("map[key4][0]"));
		assertTrue(accessor.isReadableProperty("map[key4][0].name"));
		assertTrue(accessor.isReadableProperty("map[key4][1]"));
		assertTrue(accessor.isReadableProperty("map[key4][1].name"));
		assertFalse(accessor.isReadableProperty("array[key1]"));

		assertTrue(accessor.isWritableProperty("array[0]"));
		assertTrue(accessor.isWritableProperty("array[0].name"));
		assertTrue(accessor.isWritableProperty("list[0]"));
		assertTrue(accessor.isWritableProperty("list[0].name"));
		assertTrue(accessor.isWritableProperty("set[0]"));
		assertTrue(accessor.isWritableProperty("set[0].name"));
		assertTrue(accessor.isWritableProperty("map[key1]"));
		assertTrue(accessor.isWritableProperty("map[key1].name"));
		assertTrue(accessor.isWritableProperty("map[key4][0]"));
		assertTrue(accessor.isWritableProperty("map[key4][0].name"));
		assertTrue(accessor.isWritableProperty("map[key4][1]"));
		assertTrue(accessor.isWritableProperty("map[key4][1].name"));
		assertFalse(accessor.isWritableProperty("array[key1]"));
	}

	@Test
	public void getSimpleProperty() {
		Simple target = new Simple("John", 2);
		AbstractPropertyAccessor accessor = createAccessor(target);
		assertThat(accessor.getPropertyValue("name"), is("John"));
	}

	@Test
	public void getNestedProperty() {
		Person target = createPerson("John", "London", "UK");
		AbstractPropertyAccessor accessor = createAccessor(target);
		assertThat(accessor.getPropertyValue("address.city"), is("London"));
	}

	@Test
	public void getNestedDeepProperty() {
		Person target = createPerson("John", "London", "UK");
		AbstractPropertyAccessor accessor = createAccessor(target);
		assertThat(accessor.getPropertyValue("address.country.name"), is("UK"));
	}

	@Test
	public void getAnotherNestedDeepProperty() {
		ITestBean target = new TestBean("rod", 31);
		ITestBean kerry = new TestBean("kerry", 35);
		target.setSpouse(kerry);
		kerry.setSpouse(target);
		AbstractPropertyAccessor accessor = createAccessor(target);
		Integer KA = (Integer) accessor.getPropertyValue("spouse.age");
		assertTrue("kerry is 35", KA == 35);
		Integer RA = (Integer) accessor.getPropertyValue("spouse.spouse.age");
		assertTrue("rod is 31, not" + RA, RA == 31);
		ITestBean spousesSpouse = (ITestBean) accessor.getPropertyValue("spouse.spouse");
		assertTrue("spousesSpouse = initial point", target == spousesSpouse);
	}

	@Test
	public void getPropertyIntermediatePropertyIsNull() {
		Person target = createPerson("John", "London", "UK");
		target.address = null;
		AbstractPropertyAccessor accessor = createAccessor(target);

		try {
			accessor.getPropertyValue("address.country.name");
			fail("Should have failed to get value with null intermediate path");
		}
		catch (NullValueInNestedPathException e) {
			assertEquals("address", e.getPropertyName());
			assertEquals(Person.class, e.getBeanClass());
		}
	}

	@Test
	public void getPropertyIntermediatePropertyIsNullWithAutoGrow() {
		Person target = createPerson("John", "London", "UK");
		target.address = null;
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setAutoGrowNestedPaths(true);

		assertEquals("DefaultCountry", accessor.getPropertyValue("address.country.name"));
	}

	@Test
	public void getPropertyIntermediateMapEntryIsNullWithAutoGrow() {
		Foo target = new Foo();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setConversionService(new DefaultConversionService());
		accessor.setAutoGrowNestedPaths(true);
		accessor.setPropertyValue("listOfMaps[0]['luckyNumber']", "9");
		assertEquals("9", target.listOfMaps.get(0).get("luckyNumber"));
	}

	@Test
	public void getUnknownProperty() {
		Simple target = new Simple("John", 2);
		AbstractPropertyAccessor accessor = createAccessor(target);

		try {
			accessor.getPropertyValue("foo");
			fail("Should have failed to get an unknown property.");
		}
		catch (NotReadablePropertyException e) {
			assertEquals(Simple.class, e.getBeanClass());
			assertEquals("foo", e.getPropertyName());
		}
	}

	@Test
	public void getUnknownNestedProperty() {
		Person target = createPerson("John", "London", "UK");
		AbstractPropertyAccessor accessor = createAccessor(target);

		thrown.expect(NotReadablePropertyException.class);
		accessor.getPropertyValue("address.bar");
	}

	@Test
	public void setSimpleProperty() {
		Simple target = new Simple("John", 2);
		AbstractPropertyAccessor accessor = createAccessor(target);

		accessor.setPropertyValue("name", "SomeValue");

		assertThat(target.name, is("SomeValue"));
		assertThat(target.getName(), is("SomeValue"));
	}

	@Test
	public void setNestedProperty() {
		Person target = createPerson("John", "Paris", "FR");
		AbstractPropertyAccessor accessor = createAccessor(target);

		accessor.setPropertyValue("address.city", "London");
		assertThat(target.address.city, is("London"));
	}

	@Test
	public void setNestedPropertyPolymorphic() throws Exception {
		ITestBean target = new TestBean("rod", 31);
		ITestBean kerry = new Employee();

		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setPropertyValue("spouse", kerry);
		accessor.setPropertyValue("spouse.age", new Integer(35));
		accessor.setPropertyValue("spouse.name", "Kerry");
		accessor.setPropertyValue("spouse.company", "Lewisham");
		assertTrue("kerry name is Kerry", kerry.getName().equals("Kerry"));

		assertTrue("nested set worked", target.getSpouse() == kerry);
		assertTrue("no back relation", kerry.getSpouse() == null);
		accessor.setPropertyValue(new PropertyValue("spouse.spouse", target));
		assertTrue("nested set worked", kerry.getSpouse() == target);

		AbstractPropertyAccessor kerryAccessor = createAccessor(kerry);
		assertTrue("spouse.spouse.spouse.spouse.company=Lewisham",
				"Lewisham".equals(kerryAccessor.getPropertyValue("spouse.spouse.spouse.spouse.company")));
	}

	@Test
	public void setAnotherNestedProperty() throws Exception {
		ITestBean target = new TestBean("rod", 31);
		ITestBean kerry = new TestBean("kerry", 0);

		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setPropertyValue("spouse", kerry);

		assertTrue("nested set worked", target.getSpouse() == kerry);
		assertTrue("no back relation", kerry.getSpouse() == null);
		accessor.setPropertyValue(new PropertyValue("spouse.spouse", target));
		assertTrue("nested set worked", kerry.getSpouse() == target);
		assertTrue("kerry age not set", kerry.getAge() == 0);
		accessor.setPropertyValue(new PropertyValue("spouse.age", 35));
		assertTrue("Set primitive on spouse", kerry.getAge() == 35);

		assertEquals(kerry, accessor.getPropertyValue("spouse"));
		assertEquals(target, accessor.getPropertyValue("spouse.spouse"));
	}

	@Test
	public void setYetAnotherNestedProperties() {
		String doctorCompany = "";
		String lawyerCompany = "Dr. Sueem";
		TestBean target = new TestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setPropertyValue("doctor.company", doctorCompany);
		accessor.setPropertyValue("lawyer.company", lawyerCompany);
		assertEquals(doctorCompany, target.getDoctor().getCompany());
		assertEquals(lawyerCompany, target.getLawyer().getCompany());
	}

	@Test
	public void setNestedDeepProperty() {
		Person target = createPerson("John", "Paris", "FR");
		AbstractPropertyAccessor accessor = createAccessor(target);

		accessor.setPropertyValue("address.country.name", "UK");
		assertThat(target.address.country.name, is("UK"));
	}

	@Test
	public void testErrorMessageOfNestedProperty() {
		ITestBean target = new TestBean();
		ITestBean child = new DifferentTestBean();
		child.setName("test");
		target.setSpouse(child);
		AbstractPropertyAccessor accessor = createAccessor(target);
		try {
			accessor.getPropertyValue("spouse.bla");
		}
		catch (NotReadablePropertyException ex) {
			assertTrue(ex.getMessage().contains(TestBean.class.getName()));
		}
	}

	@Test
	public void setPropertyIntermediatePropertyIsNull() {
		Person target = createPerson("John", "Paris", "FR");
		target.address.country = null;
		AbstractPropertyAccessor accessor = createAccessor(target);

		try {
			accessor.setPropertyValue("address.country.name", "UK");
			fail("Should have failed to set value with intermediate null value");
		}
		catch (NullValueInNestedPathException e) {
			assertEquals("address.country", e.getPropertyName());
			assertEquals(Person.class, e.getBeanClass());
		}
		assertThat(target.address.country, is(nullValue())); // Not touched
	}

	@Test
	public void setAnotherPropertyIntermediatePropertyIsNull() throws Exception {
		ITestBean target = new TestBean("rod", 31);
		AbstractPropertyAccessor accessor = createAccessor(target);
		try {
			accessor.setPropertyValue("spouse.age", new Integer(31));
			fail("Shouldn't have succeeded with null path");
		}
		catch (NullValueInNestedPathException ex) {
			// expected
			assertTrue("it was the spouse property that was null, not " + ex.getPropertyName(),
					ex.getPropertyName().equals("spouse"));
		}
	}

	@Test
	public void setPropertyIntermediatePropertyIsNullWithAutoGrow() {
		Person target = createPerson("John", "Paris", "FR");
		target.address.country = null;
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setAutoGrowNestedPaths(true);

		accessor.setPropertyValue("address.country.name", "UK");
		assertThat(target.address.country.name, is("UK"));
	}

	@SuppressWarnings("AssertEqualsBetweenInconvertibleTypes")
	@Test
	public void setPropertyIntermediateListIsNullWithAutoGrow() {
		Foo target = new Foo();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setConversionService(new DefaultConversionService());
		accessor.setAutoGrowNestedPaths(true);
		Map<String, String> map = new HashMap<String, String>();
		map.put("favoriteNumber", "9");
		accessor.setPropertyValue("list[0]", map);
		assertEquals(map, target.list.get(0));
	}

	@Test
	public void setPropertyIntermediateListIsNullWithNoConversionService() {
		Foo target = new Foo();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setAutoGrowNestedPaths(true);
		accessor.setPropertyValue("listOfMaps[0]['luckyNumber']", "9");
		assertEquals("9", target.listOfMaps.get(0).get("luckyNumber"));
	}

	@Test
	public void setPropertyIntermediateListIsNullWithBadConversionService() {
		Foo target = new Foo();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setConversionService(new GenericConversionService() {
			@Override
			public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
				throw new ConversionFailedException(sourceType, targetType, source, null);
			}
		});
		accessor.setAutoGrowNestedPaths(true);
		accessor.setPropertyValue("listOfMaps[0]['luckyNumber']", "9");
		assertEquals("9", target.listOfMaps.get(0).get("luckyNumber"));
	}


	@Test
	public void setEmptyPropertyValues() {
		TestBean target = new TestBean();
		int age = 50;
		String name = "Tony";
		target.setAge(age);
		target.setName(name);
		try {
			AbstractPropertyAccessor accessor = createAccessor(target);
			assertTrue("age is OK", target.getAge() == age);
			assertTrue("name is OK", name.equals(target.getName()));
			accessor.setPropertyValues(new MutablePropertyValues());
			// Check its unchanged
			assertTrue("age is OK", target.getAge() == age);
			assertTrue("name is OK", name.equals(target.getName()));
		}
		catch (BeansException ex) {
			fail("Shouldn't throw exception when everything is valid");
		}
	}


	@Test
	public void setValidPropertyValues() {
		TestBean target = new TestBean();
		String newName = "tony";
		int newAge = 65;
		String newTouchy = "valid";
		try {
			AbstractPropertyAccessor accessor = createAccessor(target);
			MutablePropertyValues pvs = new MutablePropertyValues();
			pvs.addPropertyValue(new PropertyValue("age", newAge));
			pvs.addPropertyValue(new PropertyValue("name", newName));
			pvs.addPropertyValue(new PropertyValue("touchy", newTouchy));
			accessor.setPropertyValues(pvs);
			assertTrue("Name property should have changed", target.getName().equals(newName));
			assertTrue("Touchy property should have changed", target.getTouchy().equals(newTouchy));
			assertTrue("Age property should have changed", target.getAge() == newAge);
		}
		catch (BeansException ex) {
			fail("Shouldn't throw exception when everything is valid");
		}
	}

	@Test
	public void setIndividualValidPropertyValues() {
		TestBean target = new TestBean();
		String newName = "tony";
		int newAge = 65;
		String newTouchy = "valid";
		try {
			AbstractPropertyAccessor accessor = createAccessor(target);
			accessor.setPropertyValue("age", new Integer(newAge));
			accessor.setPropertyValue(new PropertyValue("name", newName));
			accessor.setPropertyValue(new PropertyValue("touchy", newTouchy));
			assertTrue("Name property should have changed", target.getName().equals(newName));
			assertTrue("Touchy property should have changed", target.getTouchy().equals(newTouchy));
			assertTrue("Age property should have changed", target.getAge() == newAge);
		}
		catch (BeansException ex) {
			fail("Shouldn't throw exception when everything is valid");
		}
	}

	@Test
	public void setPropertyIsReflectedImmediately() {
		TestBean target = new TestBean();
		int newAge = 33;
		try {
			AbstractPropertyAccessor accessor = createAccessor(target);
			target.setAge(newAge);
			Object bwAge = accessor.getPropertyValue("age");
			assertTrue("Age is an integer", bwAge instanceof Integer);
			assertTrue("Bean wrapper must pick up changes", (int) bwAge == newAge);
		}
		catch (Exception ex) {
			fail("Shouldn't throw exception when everything is valid");
		}
	}

	@Test
	public void setPropertyToNull() {
		TestBean target = new TestBean();
		target.setName("Frank");    // we need to change it back
		target.setSpouse(target);
		AbstractPropertyAccessor accessor = createAccessor(target);
		assertTrue("name is not null to start off", target.getName() != null);
		accessor.setPropertyValue("name", null);
		assertTrue("name is now null", target.getName() == null);
		// now test with non-string
		assertTrue("spouse is not null to start off", target.getSpouse() != null);
		accessor.setPropertyValue("spouse", null);
		assertTrue("spouse is now null", target.getSpouse() == null);
	}


	@Test
	public void setIndexedPropertyIgnored() {
		MutablePropertyValues values = new MutablePropertyValues();
		values.add("toBeIgnored[0]", 42);
		AbstractPropertyAccessor accessor = createAccessor(new Object());
		accessor.setPropertyValues(values, true);
	}

	@Test
	public void setPropertyWithPrimitiveConversion() {
		MutablePropertyValues values = new MutablePropertyValues();
		values.add("name", 42);
		TestBean target = new TestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setPropertyValues(values);
		assertEquals("42", target.getName());
	}

	@Test
	public void setPropertyWithCustomEditor() {
		MutablePropertyValues values = new MutablePropertyValues();
		values.add("name", Integer.class);
		TestBean target = new TestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.registerCustomEditor(String.class, new PropertyEditorSupport() {
			@Override
			public void setValue(Object value) {
				super.setValue(value.toString());
			}
		});
		accessor.setPropertyValues(values);
		assertEquals(Integer.class.toString(), target.getName());
	}

	@Test
	public void setStringPropertyWithCustomEditor() throws Exception {
		TestBean target = new TestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.registerCustomEditor(String.class, "name", new PropertyEditorSupport() {
			@Override
			public void setValue(Object value) {
				if (value instanceof String[]) {
					setValue(StringUtils.arrayToDelimitedString(((String[]) value), "-"));
				}
				else {
					super.setValue(value != null ? value : "");
				}
			}
		});
		accessor.setPropertyValue("name", new String[] {});
		assertEquals("", target.getName());
		accessor.setPropertyValue("name", new String[] {"a1", "b2"});
		assertEquals("a1-b2", target.getName());
		accessor.setPropertyValue("name", null);
		assertEquals("", target.getName());
	}

	@Test
	public void setBooleanProperty() {
		BooleanTestBean target = new BooleanTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);

		accessor.setPropertyValue("bool2", "true");
		assertTrue("Correct bool2 value", Boolean.TRUE.equals(accessor.getPropertyValue("bool2")));
		assertTrue("Correct bool2 value", target.getBool2());

		accessor.setPropertyValue("bool2", "false");
		assertTrue("Correct bool2 value", Boolean.FALSE.equals(accessor.getPropertyValue("bool2")));
		assertTrue("Correct bool2 value", !target.getBool2());
	}

	@Test
	public void setNumberProperties() {
		NumberTestBean target = new NumberTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);

		try {
			accessor.setPropertyValue("short2", "2");
			accessor.setPropertyValue("int2", "8");
			accessor.setPropertyValue("long2", "6");
			accessor.setPropertyValue("bigInteger", "3");
			accessor.setPropertyValue("float2", "8.1");
			accessor.setPropertyValue("double2", "6.1");
			accessor.setPropertyValue("bigDecimal", "4.0");
		}
		catch (BeansException ex) {
			fail("Should not throw BeansException: " + ex.getMessage());
		}

		assertTrue("Correct short2 value", new Short("2").equals(accessor.getPropertyValue("short2")));
		assertTrue("Correct short2 value", new Short("2").equals(target.getShort2()));
		assertTrue("Correct int2 value", new Integer("8").equals(accessor.getPropertyValue("int2")));
		assertTrue("Correct int2 value", new Integer("8").equals(target.getInt2()));
		assertTrue("Correct long2 value", new Long("6").equals(accessor.getPropertyValue("long2")));
		assertTrue("Correct long2 value", new Long("6").equals(target.getLong2()));
		assertTrue("Correct bigInteger value", new BigInteger("3").equals(accessor.getPropertyValue("bigInteger")));
		assertTrue("Correct bigInteger value", new BigInteger("3").equals(target.getBigInteger()));
		assertTrue("Correct float2 value", new Float("8.1").equals(accessor.getPropertyValue("float2")));
		assertTrue("Correct float2 value", new Float("8.1").equals(target.getFloat2()));
		assertTrue("Correct double2 value", new Double("6.1").equals(accessor.getPropertyValue("double2")));
		assertTrue("Correct double2 value", new Double("6.1").equals(target.getDouble2()));
		assertTrue("Correct bigDecimal value", new BigDecimal("4.0").equals(accessor.getPropertyValue("bigDecimal")));
		assertTrue("Correct bigDecimal value", new BigDecimal("4.0").equals(target.getBigDecimal()));
	}

	@Test
	public void setNumberPropertiesWithCoercion() {
		NumberTestBean target = new NumberTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);

		try {
			accessor.setPropertyValue("short2", new Integer(2));
			accessor.setPropertyValue("int2", new Long(8));
			accessor.setPropertyValue("long2", new BigInteger("6"));
			accessor.setPropertyValue("bigInteger", new Integer(3));
			accessor.setPropertyValue("float2", new Double(8.1));
			accessor.setPropertyValue("double2", new BigDecimal(6.1));
			accessor.setPropertyValue("bigDecimal", new Float(4.0));
		}
		catch (BeansException ex) {
			fail("Should not throw BeansException: " + ex.getMessage());
		}

		assertTrue("Correct short2 value", new Short("2").equals(accessor.getPropertyValue("short2")));
		assertTrue("Correct short2 value", new Short("2").equals(target.getShort2()));
		assertTrue("Correct int2 value", new Integer("8").equals(accessor.getPropertyValue("int2")));
		assertTrue("Correct int2 value", new Integer("8").equals(target.getInt2()));
		assertTrue("Correct long2 value", new Long("6").equals(accessor.getPropertyValue("long2")));
		assertTrue("Correct long2 value", new Long("6").equals(target.getLong2()));
		assertTrue("Correct bigInteger value", new BigInteger("3").equals(accessor.getPropertyValue("bigInteger")));
		assertTrue("Correct bigInteger value", new BigInteger("3").equals(target.getBigInteger()));
		assertTrue("Correct float2 value", new Float("8.1").equals(accessor.getPropertyValue("float2")));
		assertTrue("Correct float2 value", new Float("8.1").equals(target.getFloat2()));
		assertTrue("Correct double2 value", new Double("6.1").equals(accessor.getPropertyValue("double2")));
		assertTrue("Correct double2 value", new Double("6.1").equals(target.getDouble2()));
		assertTrue("Correct bigDecimal value", new BigDecimal("4.0").equals(accessor.getPropertyValue("bigDecimal")));
		assertTrue("Correct bigDecimal value", new BigDecimal("4.0").equals(target.getBigDecimal()));
	}

	@Test
	public void setPrimitiveProperties() {
		NumberPropertyBean target = new NumberPropertyBean();
		AbstractPropertyAccessor accessor = createAccessor(target);

		String byteValue = " " + Byte.MAX_VALUE + " ";
		String shortValue = " " + Short.MAX_VALUE + " ";
		String intValue = " " + Integer.MAX_VALUE + " ";
		String longValue = " " + Long.MAX_VALUE + " ";
		String floatValue = " " + Float.MAX_VALUE + " ";
		String doubleValue = " " + Double.MAX_VALUE + " ";

		accessor.setPropertyValue("myPrimitiveByte", byteValue);
		accessor.setPropertyValue("myByte", byteValue);

		accessor.setPropertyValue("myPrimitiveShort", shortValue);
		accessor.setPropertyValue("myShort", shortValue);

		accessor.setPropertyValue("myPrimitiveInt", intValue);
		accessor.setPropertyValue("myInteger", intValue);

		accessor.setPropertyValue("myPrimitiveLong", longValue);
		accessor.setPropertyValue("myLong", longValue);

		accessor.setPropertyValue("myPrimitiveFloat", floatValue);
		accessor.setPropertyValue("myFloat", floatValue);

		accessor.setPropertyValue("myPrimitiveDouble", doubleValue);
		accessor.setPropertyValue("myDouble", doubleValue);

		assertEquals(Byte.MAX_VALUE, target.getMyPrimitiveByte());
		assertEquals(Byte.MAX_VALUE, target.getMyByte().byteValue());

		assertEquals(Short.MAX_VALUE, target.getMyPrimitiveShort());
		assertEquals(Short.MAX_VALUE, target.getMyShort().shortValue());

		assertEquals(Integer.MAX_VALUE, target.getMyPrimitiveInt());
		assertEquals(Integer.MAX_VALUE, target.getMyInteger().intValue());

		assertEquals(Long.MAX_VALUE, target.getMyPrimitiveLong());
		assertEquals(Long.MAX_VALUE, target.getMyLong().longValue());

		assertEquals(Float.MAX_VALUE, target.getMyPrimitiveFloat(), 0.001);
		assertEquals(Float.MAX_VALUE, target.getMyFloat().floatValue(), 0.001);

		assertEquals(Double.MAX_VALUE, target.getMyPrimitiveDouble(), 0.001);
		assertEquals(Double.MAX_VALUE, target.getMyDouble().doubleValue(), 0.001);

	}

	@Test
	public void setEnumProperty() {
		EnumTester target = new EnumTester();
		AbstractPropertyAccessor accessor = createAccessor(target);

		accessor.setPropertyValue("autowire", "BY_NAME");
		assertEquals(Autowire.BY_NAME, target.getAutowire());

		accessor.setPropertyValue("autowire", "  BY_TYPE ");
		assertEquals(Autowire.BY_TYPE, target.getAutowire());

		try {
			accessor.setPropertyValue("autowire", "NHERITED");
			fail("Should have thrown TypeMismatchException");
		}
		catch (TypeMismatchException ex) {
			// expected
		}
	}

	@Test
	public void setGenericEnumProperty() {
		EnumConsumer target = new EnumConsumer();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setPropertyValue("enumValue", TestEnum.class.getName() + ".TEST_VALUE");
		assertEquals(TestEnum.TEST_VALUE, target.getEnumValue());
	}

	@Test
	public void setWildcardEnumProperty() {
		WildcardEnumConsumer target = new WildcardEnumConsumer();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setPropertyValue("enumValue", TestEnum.class.getName() + ".TEST_VALUE");
		assertEquals(TestEnum.TEST_VALUE, target.getEnumValue());
	}

	@Test
	public void setPropertiesProperty() throws Exception {
		PropsTester target = new PropsTester();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setPropertyValue("name", "ptest");

		// Note format...
		String ps = "peace=war\nfreedom=slavery";
		accessor.setPropertyValue("properties", ps);

		assertTrue("name was set", target.name.equals("ptest"));
		assertTrue("properties non null", target.properties != null);
		String freedomVal = target.properties.getProperty("freedom");
		String peaceVal = target.properties.getProperty("peace");
		assertTrue("peace==war", peaceVal.equals("war"));
		assertTrue("Freedom==slavery", freedomVal.equals("slavery"));
	}

	@Test
	public void setStringArrayProperty() throws Exception {
		PropsTester target = new PropsTester();
		AbstractPropertyAccessor accessor = createAccessor(target);

		accessor.setPropertyValue("stringArray", new String[] {"foo", "fi", "fi", "fum"});
		assertTrue("stringArray length = 4", target.stringArray.length == 4);
		assertTrue("correct values", target.stringArray[0].equals("foo") && target.stringArray[1].equals("fi") &&
				target.stringArray[2].equals("fi") && target.stringArray[3].equals("fum"));

		List<String> list = new ArrayList<String>();
		list.add("foo");
		list.add("fi");
		list.add("fi");
		list.add("fum");
		accessor.setPropertyValue("stringArray", list);
		assertTrue("stringArray length = 4", target.stringArray.length == 4);
		assertTrue("correct values", target.stringArray[0].equals("foo") && target.stringArray[1].equals("fi") &&
				target.stringArray[2].equals("fi") && target.stringArray[3].equals("fum"));

		Set<String> set = new HashSet<String>();
		set.add("foo");
		set.add("fi");
		set.add("fum");
		accessor.setPropertyValue("stringArray", set);
		assertTrue("stringArray length = 3", target.stringArray.length == 3);
		List<String> result = Arrays.asList(target.stringArray);
		assertTrue("correct values", result.contains("foo") && result.contains("fi") && result.contains("fum"));

		accessor.setPropertyValue("stringArray", "one");
		assertTrue("stringArray length = 1", target.stringArray.length == 1);
		assertTrue("stringArray elt is ok", target.stringArray[0].equals("one"));

		accessor.setPropertyValue("stringArray", null);
		assertTrue("stringArray is null", target.stringArray == null);
	}

	@Test
	public void setStringArrayPropertyWithCustomStringEditor() throws Exception {
		PropsTester target = new PropsTester();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.registerCustomEditor(String.class, "stringArray", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) {
				setValue(text.substring(1));
			}
		});

		accessor.setPropertyValue("stringArray", new String[] {"4foo", "7fi", "6fi", "5fum"});
		assertTrue("stringArray length = 4", target.stringArray.length == 4);
		assertTrue("correct values", target.stringArray[0].equals("foo") && target.stringArray[1].equals("fi") &&
				target.stringArray[2].equals("fi") && target.stringArray[3].equals("fum"));

		List<String> list = new ArrayList<String>();
		list.add("4foo");
		list.add("7fi");
		list.add("6fi");
		list.add("5fum");
		accessor.setPropertyValue("stringArray", list);
		assertTrue("stringArray length = 4", target.stringArray.length == 4);
		assertTrue("correct values", target.stringArray[0].equals("foo") && target.stringArray[1].equals("fi") &&
				target.stringArray[2].equals("fi") && target.stringArray[3].equals("fum"));

		Set<String> set = new HashSet<String>();
		set.add("4foo");
		set.add("7fi");
		set.add("6fum");
		accessor.setPropertyValue("stringArray", set);
		assertTrue("stringArray length = 3", target.stringArray.length == 3);
		List<String> result = Arrays.asList(target.stringArray);
		assertTrue("correct values", result.contains("foo") && result.contains("fi") && result.contains("fum"));

		accessor.setPropertyValue("stringArray", "8one");
		assertTrue("stringArray length = 1", target.stringArray.length == 1);
		assertTrue("correct values", target.stringArray[0].equals("one"));
	}

	@Test
	public void setStringArrayPropertyWithStringSplitting() throws Exception {
		PropsTester target = new PropsTester();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.useConfigValueEditors();
		accessor.setPropertyValue("stringArray", "a1,b2");
		assertTrue("stringArray length = 2", target.stringArray.length == 2);
		assertTrue("correct values", target.stringArray[0].equals("a1") && target.stringArray[1].equals("b2"));
	}

	@Test
	public void setStringArrayPropertyWithCustomStringDelimiter() throws Exception {
		PropsTester target = new PropsTester();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.registerCustomEditor(String[].class, "stringArray", new StringArrayPropertyEditor("-"));
		accessor.setPropertyValue("stringArray", "a1-b2");
		assertTrue("stringArray length = 2", target.stringArray.length == 2);
		assertTrue("correct values", target.stringArray[0].equals("a1") && target.stringArray[1].equals("b2"));
	}

	@Test
	public void setStringArrayWithAutoGrow() throws Exception {
		StringArrayBean target = new StringArrayBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setAutoGrowNestedPaths(true);

		accessor.setPropertyValue("array[0]", "Test0");
		assertEquals(1, target.getArray().length);

		accessor.setPropertyValue("array[2]", "Test2");
		assertEquals(3, target.getArray().length);
		assertTrue("correct values", target.getArray()[0].equals("Test0") && target.getArray()[1] == null &&
				target.getArray()[2].equals("Test2"));
	}

	@Test
	public void setIntArrayProperty() {
		PropsTester target = new PropsTester();
		AbstractPropertyAccessor accessor = createAccessor(target);

		accessor.setPropertyValue("intArray", new int[] {4, 5, 2, 3});
		assertTrue("intArray length = 4", target.intArray.length == 4);
		assertTrue("correct values", target.intArray[0] == 4 && target.intArray[1] == 5 &&
				target.intArray[2] == 2 && target.intArray[3] == 3);

		accessor.setPropertyValue("intArray", new String[] {"4", "5", "2", "3"});
		assertTrue("intArray length = 4", target.intArray.length == 4);
		assertTrue("correct values", target.intArray[0] == 4 && target.intArray[1] == 5 &&
				target.intArray[2] == 2 && target.intArray[3] == 3);

		List<Object> list = new ArrayList<>();
		list.add(4);
		list.add("5");
		list.add(2);
		list.add("3");
		accessor.setPropertyValue("intArray", list);
		assertTrue("intArray length = 4", target.intArray.length == 4);
		assertTrue("correct values", target.intArray[0] == 4 && target.intArray[1] == 5 &&
				target.intArray[2] == 2 && target.intArray[3] == 3);

		Set<Object> set = new HashSet<>();
		set.add("4");
		set.add(5);
		set.add("3");
		accessor.setPropertyValue("intArray", set);
		assertTrue("intArray length = 3", target.intArray.length == 3);
		List<Integer> result = new ArrayList<>();
		result.add(target.intArray[0]);
		result.add(target.intArray[1]);
		result.add(target.intArray[2]);
		assertTrue("correct values", result.contains(new Integer(4)) && result.contains(new Integer(5)) &&
				result.contains(new Integer(3)));

		accessor.setPropertyValue("intArray", new Integer[] {1});
		assertTrue("intArray length = 4", target.intArray.length == 1);
		assertTrue("correct values", target.intArray[0] == 1);

		accessor.setPropertyValue("intArray", new Integer(1));
		assertTrue("intArray length = 4", target.intArray.length == 1);
		assertTrue("correct values", target.intArray[0] == 1);

		accessor.setPropertyValue("intArray", new String[] {"1"});
		assertTrue("intArray length = 4", target.intArray.length == 1);
		assertTrue("correct values", target.intArray[0] == 1);

		accessor.setPropertyValue("intArray", "1");
		assertTrue("intArray length = 4", target.intArray.length == 1);
		assertTrue("correct values", target.intArray[0] == 1);
	}

	@Test
	public void setIntArrayPropertyWithCustomEditor() {
		PropsTester target = new PropsTester();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.registerCustomEditor(int.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) {
				setValue(new Integer(Integer.parseInt(text) + 1));
			}
		});

		accessor.setPropertyValue("intArray", new int[] {4, 5, 2, 3});
		assertTrue("intArray length = 4", target.intArray.length == 4);
		assertTrue("correct values", target.intArray[0] == 4 && target.intArray[1] == 5 &&
				target.intArray[2] == 2 && target.intArray[3] == 3);

		accessor.setPropertyValue("intArray", new String[] {"3", "4", "1", "2"});
		assertTrue("intArray length = 4", target.intArray.length == 4);
		assertTrue("correct values", target.intArray[0] == 4 && target.intArray[1] == 5 &&
				target.intArray[2] == 2 && target.intArray[3] == 3);

		accessor.setPropertyValue("intArray", new Integer(1));
		assertTrue("intArray length = 4", target.intArray.length == 1);
		assertTrue("correct values", target.intArray[0] == 1);

		accessor.setPropertyValue("intArray", new String[] {"0"});
		assertTrue("intArray length = 4", target.intArray.length == 1);
		assertTrue("correct values", target.intArray[0] == 1);

		accessor.setPropertyValue("intArray", "0");
		assertTrue("intArray length = 4", target.intArray.length == 1);
		assertTrue("correct values", target.intArray[0] == 1);
	}

	@Test
	public void setIntArrayPropertyWithStringSplitting() throws Exception {
		PropsTester target = new PropsTester();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.useConfigValueEditors();
		accessor.setPropertyValue("intArray", "4,5");
		assertTrue("intArray length = 2", target.intArray.length == 2);
		assertTrue("correct values", target.intArray[0] == 4 && target.intArray[1] == 5);
	}

	@Test
	public void setPrimitiveArrayProperty() {
		PrimitiveArrayBean target = new PrimitiveArrayBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setPropertyValue("array", new String[] {"1", "2"});
		assertEquals(2, target.getArray().length);
		assertEquals(1, target.getArray()[0]);
		assertEquals(2, target.getArray()[1]);
	}

	@Test
	public void setPrimitiveArrayPropertyLargeMatching() {
		Assume.group(TestGroup.PERFORMANCE);
		Assume.notLogging(LogFactory.getLog(AbstractPropertyAccessorTests.class));

		PrimitiveArrayBean target = new PrimitiveArrayBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		int[] input = new int[1024];
		StopWatch sw = new StopWatch();
		sw.start("array1");
		for (int i = 0; i < 1000; i++) {
			accessor.setPropertyValue("array", input);
		}
		sw.stop();
		assertEquals(1024, target.getArray().length);
		assertEquals(0, target.getArray()[0]);
		long time1 = sw.getLastTaskTimeMillis();
		assertTrue("Took too long", sw.getLastTaskTimeMillis() < 100);

		accessor.registerCustomEditor(String.class, new StringTrimmerEditor(false));
		sw.start("array2");
		for (int i = 0; i < 1000; i++) {
			accessor.setPropertyValue("array", input);
		}
		sw.stop();
		assertTrue("Took too long", sw.getLastTaskTimeMillis() < 125);

		accessor.registerCustomEditor(int.class, "array.somePath", new CustomNumberEditor(Integer.class, false));
		sw.start("array3");
		for (int i = 0; i < 1000; i++) {
			accessor.setPropertyValue("array", input);
		}
		sw.stop();
		assertTrue("Took too long", sw.getLastTaskTimeMillis() < 100);

		accessor.registerCustomEditor(int.class, "array[0].somePath", new CustomNumberEditor(Integer.class, false));
		sw.start("array3");
		for (int i = 0; i < 1000; i++) {
			accessor.setPropertyValue("array", input);
		}
		sw.stop();
		assertTrue("Took too long", sw.getLastTaskTimeMillis() < 100);

		accessor.registerCustomEditor(int.class, new CustomNumberEditor(Integer.class, false));
		sw.start("array4");
		for (int i = 0; i < 100; i++) {
			accessor.setPropertyValue("array", input);
		}
		sw.stop();
		assertEquals(1024, target.getArray().length);
		assertEquals(0, target.getArray()[0]);
		assertTrue("Took too long", sw.getLastTaskTimeMillis() > time1);
	}

	@Test
	public void setPrimitiveArrayPropertyLargeMatchingWithSpecificEditor() {
		PrimitiveArrayBean target = new PrimitiveArrayBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.registerCustomEditor(int.class, "array", new PropertyEditorSupport() {
			@Override
			public void setValue(Object value) {
				if (value instanceof Integer) {
					super.setValue(new Integer((Integer) value + 1));
				}
			}
		});
		int[] input = new int[1024];
		accessor.setPropertyValue("array", input);
		assertEquals(1024, target.getArray().length);
		assertEquals(1, target.getArray()[0]);
		assertEquals(1, target.getArray()[1]);
	}

	@Test
	public void setPrimitiveArrayPropertyLargeMatchingWithIndexSpecificEditor() {
		PrimitiveArrayBean target = new PrimitiveArrayBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.registerCustomEditor(int.class, "array[1]", new PropertyEditorSupport() {
			@Override
			public void setValue(Object value) {
				if (value instanceof Integer) {
					super.setValue(new Integer((Integer) value + 1));
				}
			}
		});
		int[] input = new int[1024];
		accessor.setPropertyValue("array", input);
		assertEquals(1024, target.getArray().length);
		assertEquals(0, target.getArray()[0]);
		assertEquals(1, target.getArray()[1]);
	}

	@Test
	public void setPrimitiveArrayPropertyWithAutoGrow() throws Exception {
		PrimitiveArrayBean target = new PrimitiveArrayBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setAutoGrowNestedPaths(true);

		accessor.setPropertyValue("array[0]", 1);
		assertEquals(1, target.getArray().length);

		accessor.setPropertyValue("array[2]", 3);
		assertEquals(3, target.getArray().length);
		assertTrue("correct values", target.getArray()[0] == 1 && target.getArray()[1] == 0 &&
				target.getArray()[2] == 3);
	}

	@Test
	public void setGenericArrayProperty() {
		SkipReaderStub target = new SkipReaderStub();
		AbstractPropertyAccessor accessor = createAccessor(target);
		List<String> values = new LinkedList<String>();
		values.add("1");
		values.add("2");
		values.add("3");
		values.add("4");
		accessor.setPropertyValue("items", values);
		Object[] result = target.items;
		assertEquals(4, result.length);
		assertEquals("1", result[0]);
		assertEquals("2", result[1]);
		assertEquals("3", result[2]);
		assertEquals("4", result[3]);
	}

	@Test
	public void setArrayPropertyToObject() {
		ArrayToObject target = new ArrayToObject();
		AbstractPropertyAccessor accessor = createAccessor(target);

		Object[] array = new Object[] {"1", "2"};
		accessor.setPropertyValue("object", array);
		assertThat(target.getObject(), equalTo((Object) array));

		array = new Object[] {"1"};
		accessor.setPropertyValue("object", array);
		assertThat(target.getObject(), equalTo((Object) array));
	}


	@Test
	public void setCollectionProperty() {
		IndexedTestBean target = new IndexedTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		Collection<String> coll = new HashSet<String>();
		coll.add("coll1");
		accessor.setPropertyValue("collection", coll);
		Set<String> set = new HashSet<String>();
		set.add("set1");
		accessor.setPropertyValue("set", set);
		SortedSet<String> sortedSet = new TreeSet<String>();
		sortedSet.add("sortedSet1");
		accessor.setPropertyValue("sortedSet", sortedSet);
		List<String> list = new LinkedList<String>();
		list.add("list1");
		accessor.setPropertyValue("list", list);
		assertSame(coll, target.getCollection());
		assertSame(set, target.getSet());
		assertSame(sortedSet, target.getSortedSet());
		assertSame(list, target.getList());
	}

	@SuppressWarnings("unchecked") // list cannot be properly parameterized as it breaks other tests
	@Test
	public void setCollectionPropertyNonMatchingType() {
		IndexedTestBean target = new IndexedTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		Collection<String> coll = new ArrayList<String>();
		coll.add("coll1");
		accessor.setPropertyValue("collection", coll);
		List<String> set = new LinkedList<String>();
		set.add("set1");
		accessor.setPropertyValue("set", set);
		List<String> sortedSet = new ArrayList<String>();
		sortedSet.add("sortedSet1");
		accessor.setPropertyValue("sortedSet", sortedSet);
		Set<String> list = new HashSet<String>();
		list.add("list1");
		accessor.setPropertyValue("list", list);
		assertEquals(1, target.getCollection().size());
		assertTrue(target.getCollection().containsAll(coll));
		assertEquals(1, target.getSet().size());
		assertTrue(target.getSet().containsAll(set));
		assertEquals(1, target.getSortedSet().size());
		assertTrue(target.getSortedSet().containsAll(sortedSet));
		assertEquals(1, target.getList().size());
		assertTrue(target.getList().containsAll(list));
	}

	@SuppressWarnings("unchecked") // list cannot be properly parameterized as it breaks other tests
	@Test
	public void setCollectionPropertyWithArrayValue() {
		IndexedTestBean target = new IndexedTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		Collection<String> coll = new HashSet<String>();
		coll.add("coll1");
		accessor.setPropertyValue("collection", coll.toArray());
		List<String> set = new LinkedList<String>();
		set.add("set1");
		accessor.setPropertyValue("set", set.toArray());
		List<String> sortedSet = new ArrayList<String>();
		sortedSet.add("sortedSet1");
		accessor.setPropertyValue("sortedSet", sortedSet.toArray());
		Set<String> list = new HashSet<String>();
		list.add("list1");
		accessor.setPropertyValue("list", list.toArray());
		assertEquals(1, target.getCollection().size());
		assertTrue(target.getCollection().containsAll(coll));
		assertEquals(1, target.getSet().size());
		assertTrue(target.getSet().containsAll(set));
		assertEquals(1, target.getSortedSet().size());
		assertTrue(target.getSortedSet().containsAll(sortedSet));
		assertEquals(1, target.getList().size());
		assertTrue(target.getList().containsAll(list));
	}

	@SuppressWarnings("unchecked") // list cannot be properly parameterized as it breaks other tests
	@Test
	public void setCollectionPropertyWithIntArrayValue() {
		IndexedTestBean target = new IndexedTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		Collection<Integer> coll = new HashSet<Integer>();
		coll.add(0);
		accessor.setPropertyValue("collection", new int[] {0});
		List<Integer> set = new LinkedList<Integer>();
		set.add(1);
		accessor.setPropertyValue("set", new int[] {1});
		List<Integer> sortedSet = new ArrayList<Integer>();
		sortedSet.add(2);
		accessor.setPropertyValue("sortedSet", new int[] {2});
		Set<Integer> list = new HashSet<Integer>();
		list.add(3);
		accessor.setPropertyValue("list", new int[] {3});
		assertEquals(1, target.getCollection().size());
		assertTrue(target.getCollection().containsAll(coll));
		assertEquals(1, target.getSet().size());
		assertTrue(target.getSet().containsAll(set));
		assertEquals(1, target.getSortedSet().size());
		assertTrue(target.getSortedSet().containsAll(sortedSet));
		assertEquals(1, target.getList().size());
		assertTrue(target.getList().containsAll(list));
	}

	@SuppressWarnings("unchecked") // list cannot be properly parameterized as it breaks other tests
	@Test
	public void setCollectionPropertyWithIntegerValue() {
		IndexedTestBean target = new IndexedTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		Collection<Integer> coll = new HashSet<Integer>();
		coll.add(0);
		accessor.setPropertyValue("collection", new Integer(0));
		List<Integer> set = new LinkedList<Integer>();
		set.add(1);
		accessor.setPropertyValue("set", new Integer(1));
		List<Integer> sortedSet = new ArrayList<Integer>();
		sortedSet.add(2);
		accessor.setPropertyValue("sortedSet", new Integer(2));
		Set<Integer> list = new HashSet<Integer>();
		list.add(3);
		accessor.setPropertyValue("list", new Integer(3));
		assertEquals(1, target.getCollection().size());
		assertTrue(target.getCollection().containsAll(coll));
		assertEquals(1, target.getSet().size());
		assertTrue(target.getSet().containsAll(set));
		assertEquals(1, target.getSortedSet().size());
		assertTrue(target.getSortedSet().containsAll(sortedSet));
		assertEquals(1, target.getList().size());
		assertTrue(target.getList().containsAll(list));
	}

	@SuppressWarnings("unchecked") // list cannot be properly parameterized as it breaks other tests
	@Test
	public void setCollectionPropertyWithStringValue() {
		IndexedTestBean target = new IndexedTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		List<String> set = new LinkedList<String>();
		set.add("set1");
		accessor.setPropertyValue("set", "set1");
		List<String> sortedSet = new ArrayList<String>();
		sortedSet.add("sortedSet1");
		accessor.setPropertyValue("sortedSet", "sortedSet1");
		Set<String> list = new HashSet<String>();
		list.add("list1");
		accessor.setPropertyValue("list", "list1");
		assertEquals(1, target.getSet().size());
		assertTrue(target.getSet().containsAll(set));
		assertEquals(1, target.getSortedSet().size());
		assertTrue(target.getSortedSet().containsAll(sortedSet));
		assertEquals(1, target.getList().size());
		assertTrue(target.getList().containsAll(list));
	}

	@Test
	public void setCollectionPropertyWithStringValueAndCustomEditor() {
		IndexedTestBean target = new IndexedTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.registerCustomEditor(String.class, "set", new StringTrimmerEditor(false));
		accessor.registerCustomEditor(String.class, "list", new StringTrimmerEditor(false));

		accessor.setPropertyValue("set", "set1 ");
		accessor.setPropertyValue("sortedSet", "sortedSet1");
		accessor.setPropertyValue("list", "list1 ");
		assertEquals(1, target.getSet().size());
		assertTrue(target.getSet().contains("set1"));
		assertEquals(1, target.getSortedSet().size());
		assertTrue(target.getSortedSet().contains("sortedSet1"));
		assertEquals(1, target.getList().size());
		assertTrue(target.getList().contains("list1"));

		accessor.setPropertyValue("list", Collections.singletonList("list1 "));
		assertTrue(target.getList().contains("list1"));
	}

	@Test
	public void setMapProperty() {
		IndexedTestBean target = new IndexedTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		Map<String, String> map = new HashMap<String, String>();
		map.put("key", "value");
		accessor.setPropertyValue("map", map);
		SortedMap<?, ?> sortedMap = new TreeMap<>();
		map.put("sortedKey", "sortedValue");
		accessor.setPropertyValue("sortedMap", sortedMap);
		assertSame(map, target.getMap());
		assertSame(sortedMap, target.getSortedMap());
	}

	@Test
	public void setMapPropertyNonMatchingType() {
		IndexedTestBean target = new IndexedTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		Map<String, String> map = new TreeMap<String, String>();
		map.put("key", "value");
		accessor.setPropertyValue("map", map);
		Map<String, String> sortedMap = new TreeMap<String, String>();
		sortedMap.put("sortedKey", "sortedValue");
		accessor.setPropertyValue("sortedMap", sortedMap);
		assertEquals(1, target.getMap().size());
		assertEquals("value", target.getMap().get("key"));
		assertEquals(1, target.getSortedMap().size());
		assertEquals("sortedValue", target.getSortedMap().get("sortedKey"));
	}

	@Test
	public void setMapPropertyWithTypeConversion() {
		IndexedTestBean target = new IndexedTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.registerCustomEditor(TestBean.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				if (!StringUtils.hasLength(text)) {
					throw new IllegalArgumentException();
				}
				setValue(new TestBean(text));
			}
		});

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("map[key1]", "rod");
		pvs.add("map[key2]", "rob");
		accessor.setPropertyValues(pvs);
		assertEquals("rod", ((TestBean) target.getMap().get("key1")).getName());
		assertEquals("rob", ((TestBean) target.getMap().get("key2")).getName());

		pvs = new MutablePropertyValues();
		pvs.add("map[key1]", "rod");
		pvs.add("map[key2]", "");
		try {
			accessor.setPropertyValues(pvs);
			fail("Should have thrown TypeMismatchException");
		}
		catch (PropertyBatchUpdateException ex) {
			PropertyAccessException pae = ex.getPropertyAccessException("map[key2]");
			assertTrue(pae instanceof TypeMismatchException);
		}
	}

	@Test
	public void setMapPropertyWithUnmodifiableMap() {
		IndexedTestBean target = new IndexedTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.registerCustomEditor(TestBean.class, "map", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				if (!StringUtils.hasLength(text)) {
					throw new IllegalArgumentException();
				}
				setValue(new TestBean(text));
			}
		});

		Map<Integer, String> inputMap = new HashMap<Integer, String>();
		inputMap.put(1, "rod");
		inputMap.put(2, "rob");
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("map", Collections.unmodifiableMap(inputMap));
		accessor.setPropertyValues(pvs);
		assertEquals("rod", ((TestBean) target.getMap().get(1)).getName());
		assertEquals("rob", ((TestBean) target.getMap().get(2)).getName());
	}

	@Test
	public void setMapPropertyWithCustomUnmodifiableMap() {
		IndexedTestBean target = new IndexedTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.registerCustomEditor(TestBean.class, "map", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				if (!StringUtils.hasLength(text)) {
					throw new IllegalArgumentException();
				}
				setValue(new TestBean(text));
			}
		});

		Map<Object, Object> inputMap = new HashMap<Object, Object>();
		inputMap.put(1, "rod");
		inputMap.put(2, "rob");
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("map", new ReadOnlyMap<>(inputMap));
		accessor.setPropertyValues(pvs);
		assertEquals("rod", ((TestBean) target.getMap().get(1)).getName());
		assertEquals("rob", ((TestBean) target.getMap().get(2)).getName());
	}

	@SuppressWarnings("unchecked") // must work with raw map in this test
	@Test
	public void setRawMapPropertyWithNoEditorRegistered() {
		IndexedTestBean target = new IndexedTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		Map inputMap = new HashMap();
		inputMap.put(1, "rod");
		inputMap.put(2, "rob");
		ReadOnlyMap readOnlyMap = new ReadOnlyMap(inputMap);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("map", readOnlyMap);
		accessor.setPropertyValues(pvs);
		assertSame(readOnlyMap, target.getMap());
		assertFalse(readOnlyMap.isAccessed());
	}

	@Test
	public void setUnknownProperty() {
		Simple target = new Simple("John", 2);
		AbstractPropertyAccessor accessor = createAccessor(target);

		try {
			accessor.setPropertyValue("name1", "value");
			fail("Should have failed to set an unknown property.");
		}
		catch (NotWritablePropertyException e) {
			assertEquals(Simple.class, e.getBeanClass());
			assertEquals("name1", e.getPropertyName());
			assertEquals("Invalid number of possible matches", 1, e.getPossibleMatches().length);
			assertEquals("name", e.getPossibleMatches()[0]);
		}
	}

	@Test
	public void setUnknownPropertyWithPossibleMatches() {
		Simple target = new Simple("John", 2);
		AbstractPropertyAccessor accessor = createAccessor(target);

		try {
			accessor.setPropertyValue("foo", "value");
			fail("Should have failed to set an unknown property.");
		}
		catch (NotWritablePropertyException e) {
			assertEquals(Simple.class, e.getBeanClass());
			assertEquals("foo", e.getPropertyName());
		}
	}

	@Test
	public void setUnknownOptionalProperty() {
		Simple target = new Simple("John", 2);
		AbstractPropertyAccessor accessor = createAccessor(target);

		try {
			PropertyValue value = new PropertyValue("foo", "value");
			value.setOptional(true);
			accessor.setPropertyValue(value);
		}
		catch (NotWritablePropertyException e) {
			fail("Should not have failed to set an unknown optional property.");
		}
	}

	@Test
	public void setPropertyInProtectedBaseBean() {
		DerivedFromProtectedBaseBean target = new DerivedFromProtectedBaseBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setPropertyValue("someProperty", "someValue");
		assertEquals("someValue", accessor.getPropertyValue("someProperty"));
		assertEquals("someValue", target.getSomeProperty());
	}

	@Test
	public void setPropertyTypeMismatch() {
		TestBean target = new TestBean();
		try {
			AbstractPropertyAccessor accessor = createAccessor(target);
			accessor.setPropertyValue("age", "foobar");
			fail("Should throw exception on type mismatch");
		}
		catch (TypeMismatchException ex) {
			// expected
		}
	}

	@Test
	public void setEmptyValueForPrimitiveProperty() {
		TestBean target = new TestBean();
		try {
			AbstractPropertyAccessor accessor = createAccessor(target);
			accessor.setPropertyValue("age", "");
			fail("Should throw exception on type mismatch");
		}
		catch (TypeMismatchException ex) {
			// expected
		}
		catch (Exception ex) {
			fail("Shouldn't throw exception other than Type mismatch");
		}
	}

	@Test
	public void setUnknownNestedProperty() {
		Person target = createPerson("John", "Paris", "FR");
		AbstractPropertyAccessor accessor = createAccessor(target);

		thrown.expect(NotWritablePropertyException.class);
		accessor.setPropertyValue("address.bar", "value");
	}

	@Test
	public void setPropertyValuesIgnoresInvalidNestedOnRequest() {
		ITestBean target = new TestBean();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue(new PropertyValue("name", "rod"));
		pvs.addPropertyValue(new PropertyValue("graceful.rubbish", "tony"));
		pvs.addPropertyValue(new PropertyValue("more.garbage", new Object()));
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setPropertyValues(pvs, true);
		assertTrue("Set valid and ignored invalid", target.getName().equals("rod"));
		try {
			// Don't ignore: should fail
			accessor.setPropertyValues(pvs, false);
			fail("Shouldn't have ignored invalid updates");
		}
		catch (NotWritablePropertyException ex) {
			// OK: but which exception??
		}
	}

	@Test
	public void getAndSetIndexedProperties() {
		IndexedTestBean target = new IndexedTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		TestBean tb0 = target.getArray()[0];
		TestBean tb1 = target.getArray()[1];
		TestBean tb2 = ((TestBean) target.getList().get(0));
		TestBean tb3 = ((TestBean) target.getList().get(1));
		TestBean tb6 = ((TestBean) target.getSet().toArray()[0]);
		TestBean tb7 = ((TestBean) target.getSet().toArray()[1]);
		TestBean tb4 = ((TestBean) target.getMap().get("key1"));
		TestBean tb5 = ((TestBean) target.getMap().get("key.3"));
		assertEquals("name0", tb0.getName());
		assertEquals("name1", tb1.getName());
		assertEquals("name2", tb2.getName());
		assertEquals("name3", tb3.getName());
		assertEquals("name6", tb6.getName());
		assertEquals("name7", tb7.getName());
		assertEquals("name4", tb4.getName());
		assertEquals("name5", tb5.getName());
		assertEquals("name0", accessor.getPropertyValue("array[0].name"));
		assertEquals("name1", accessor.getPropertyValue("array[1].name"));
		assertEquals("name2", accessor.getPropertyValue("list[0].name"));
		assertEquals("name3", accessor.getPropertyValue("list[1].name"));
		assertEquals("name6", accessor.getPropertyValue("set[0].name"));
		assertEquals("name7", accessor.getPropertyValue("set[1].name"));
		assertEquals("name4", accessor.getPropertyValue("map[key1].name"));
		assertEquals("name5", accessor.getPropertyValue("map[key.3].name"));
		assertEquals("name4", accessor.getPropertyValue("map['key1'].name"));
		assertEquals("name5", accessor.getPropertyValue("map[\"key.3\"].name"));
		assertEquals("nameX", accessor.getPropertyValue("map[key4][0].name"));
		assertEquals("nameY", accessor.getPropertyValue("map[key4][1].name"));

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("array[0].name", "name5");
		pvs.add("array[1].name", "name4");
		pvs.add("list[0].name", "name3");
		pvs.add("list[1].name", "name2");
		pvs.add("set[0].name", "name8");
		pvs.add("set[1].name", "name9");
		pvs.add("map[key1].name", "name1");
		pvs.add("map['key.3'].name", "name0");
		pvs.add("map[key4][0].name", "nameA");
		pvs.add("map[key4][1].name", "nameB");
		accessor.setPropertyValues(pvs);
		assertEquals("name5", tb0.getName());
		assertEquals("name4", tb1.getName());
		assertEquals("name3", tb2.getName());
		assertEquals("name2", tb3.getName());
		assertEquals("name1", tb4.getName());
		assertEquals("name0", tb5.getName());
		assertEquals("name5", accessor.getPropertyValue("array[0].name"));
		assertEquals("name4", accessor.getPropertyValue("array[1].name"));
		assertEquals("name3", accessor.getPropertyValue("list[0].name"));
		assertEquals("name2", accessor.getPropertyValue("list[1].name"));
		assertEquals("name8", accessor.getPropertyValue("set[0].name"));
		assertEquals("name9", accessor.getPropertyValue("set[1].name"));
		assertEquals("name1", accessor.getPropertyValue("map[\"key1\"].name"));
		assertEquals("name0", accessor.getPropertyValue("map['key.3'].name"));
		assertEquals("nameA", accessor.getPropertyValue("map[key4][0].name"));
		assertEquals("nameB", accessor.getPropertyValue("map[key4][1].name"));
	}

	@Test
	public void getAndSetIndexedPropertiesWithDirectAccess() {
		IndexedTestBean target = new IndexedTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		TestBean tb0 = target.getArray()[0];
		TestBean tb1 = target.getArray()[1];
		TestBean tb2 = ((TestBean) target.getList().get(0));
		TestBean tb3 = ((TestBean) target.getList().get(1));
		TestBean tb6 = ((TestBean) target.getSet().toArray()[0]);
		TestBean tb7 = ((TestBean) target.getSet().toArray()[1]);
		TestBean tb4 = ((TestBean) target.getMap().get("key1"));
		TestBean tb5 = ((TestBean) target.getMap().get("key2"));
		assertEquals(tb0, accessor.getPropertyValue("array[0]"));
		assertEquals(tb1, accessor.getPropertyValue("array[1]"));
		assertEquals(tb2, accessor.getPropertyValue("list[0]"));
		assertEquals(tb3, accessor.getPropertyValue("list[1]"));
		assertEquals(tb6, accessor.getPropertyValue("set[0]"));
		assertEquals(tb7, accessor.getPropertyValue("set[1]"));
		assertEquals(tb4, accessor.getPropertyValue("map[key1]"));
		assertEquals(tb5, accessor.getPropertyValue("map[key2]"));
		assertEquals(tb4, accessor.getPropertyValue("map['key1']"));
		assertEquals(tb5, accessor.getPropertyValue("map[\"key2\"]"));

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("array[0]", tb5);
		pvs.add("array[1]", tb4);
		pvs.add("list[0]", tb3);
		pvs.add("list[1]", tb2);
		pvs.add("list[2]", tb0);
		pvs.add("list[4]", tb1);
		pvs.add("map[key1]", tb1);
		pvs.add("map['key2']", tb0);
		pvs.add("map[key5]", tb4);
		pvs.add("map['key9']", tb5);
		accessor.setPropertyValues(pvs);
		assertEquals(tb5, target.getArray()[0]);
		assertEquals(tb4, target.getArray()[1]);
		assertEquals(tb3, (target.getList().get(0)));
		assertEquals(tb2, (target.getList().get(1)));
		assertEquals(tb0, (target.getList().get(2)));
		assertEquals(null, (target.getList().get(3)));
		assertEquals(tb1, (target.getList().get(4)));
		assertEquals(tb1, (target.getMap().get("key1")));
		assertEquals(tb0, (target.getMap().get("key2")));
		assertEquals(tb4, (target.getMap().get("key5")));
		assertEquals(tb5, (target.getMap().get("key9")));
		assertEquals(tb5, accessor.getPropertyValue("array[0]"));
		assertEquals(tb4, accessor.getPropertyValue("array[1]"));
		assertEquals(tb3, accessor.getPropertyValue("list[0]"));
		assertEquals(tb2, accessor.getPropertyValue("list[1]"));
		assertEquals(tb0, accessor.getPropertyValue("list[2]"));
		assertEquals(null, accessor.getPropertyValue("list[3]"));
		assertEquals(tb1, accessor.getPropertyValue("list[4]"));
		assertEquals(tb1, accessor.getPropertyValue("map[\"key1\"]"));
		assertEquals(tb0, accessor.getPropertyValue("map['key2']"));
		assertEquals(tb4, accessor.getPropertyValue("map[\"key5\"]"));
		assertEquals(tb5, accessor.getPropertyValue("map['key9']"));
	}

	@Test
	public void propertyType() {
		Person target = createPerson("John", "Paris", "FR");
		AbstractPropertyAccessor accessor = createAccessor(target);

		assertEquals(String.class, accessor.getPropertyType("address.city"));
	}

	@Test
	public void propertyTypeUnknownProperty() {
		Simple target = new Simple("John", 2);
		AbstractPropertyAccessor accessor = createAccessor(target);

		assertThat(accessor.getPropertyType("foo"), is(nullValue()));
	}

	@Test
	public void propertyTypeDescriptor() {
		Person target = createPerson("John", "Paris", "FR");
		AbstractPropertyAccessor accessor = createAccessor(target);

		assertThat(accessor.getPropertyTypeDescriptor("address.city"), is(notNullValue()));
	}

	@Test
	public void propertyTypeDescriptorUnknownProperty() {
		Simple target = new Simple("John", 2);
		AbstractPropertyAccessor accessor = createAccessor(target);

		assertThat(accessor.getPropertyTypeDescriptor("foo"), is(nullValue()));
	}

	@Test
	public void propertyTypeIndexedProperty() {
		IndexedTestBean target = new IndexedTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		assertEquals(null, accessor.getPropertyType("map[key0]"));

		accessor = createAccessor(target);
		accessor.setPropertyValue("map[key0]", "my String");
		assertEquals(String.class, accessor.getPropertyType("map[key0]"));

		accessor = createAccessor(target);
		accessor.registerCustomEditor(String.class, "map[key0]", new StringTrimmerEditor(false));
		assertEquals(String.class, accessor.getPropertyType("map[key0]"));
	}

	@Test
	public void cornerSpr10115() {
		Spr10115Bean target = new Spr10115Bean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setPropertyValue("prop1", "val1");
		assertEquals("val1", Spr10115Bean.prop1);
	}

	@Test
	public void cornerSpr13837() {
		Spr13837Bean target = new Spr13837Bean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setPropertyValue("something", 42);
		assertEquals(Integer.valueOf(42), target.something);
	}


	private Person createPerson(String name, String city, String country) {
		return new Person(name, new Address(city, country));
	}


	private static class Simple {

		private String name;

		private Integer integer;

		private Simple(String name, Integer integer) {
			this.name = name;
			this.integer = integer;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Integer getInteger() {
			return integer;
		}

		public void setInteger(Integer integer) {
			this.integer = integer;
		}
	}

	private static class Person {
		private String name;

		private Address address;

		private Person(String name, Address address) {
			this.name = name;
			this.address = address;
		}

		public Person() {
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Address getAddress() {
			return address;
		}

		public void setAddress(Address address) {
			this.address = address;
		}
	}

	private static class Address {
		private String city;

		private Country country;

		private Address(String city, String country) {
			this.city = city;
			this.country = new Country(country);
		}

		public Address() {
			this("DefaultCity", "DefaultCountry");
		}

		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}

		public Country getCountry() {
			return country;
		}

		public void setCountry(Country country) {
			this.country = country;
		}
	}

	private static class Country {
		private String name;

		public Country(String name) {
			this.name = name;
		}

		public Country() {
			this(null);
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}


	@SuppressWarnings("unused")
	static class NoRead {

		public void setAge(int age) {
		}
	}

	@SuppressWarnings("unused")
	private static class Foo {

		private List list;

		private List<Map> listOfMaps;

		public List getList() {
			return list;
		}

		public void setList(List list) {
			this.list = list;
		}

		public List<Map> getListOfMaps() {
			return listOfMaps;
		}

		public void setListOfMaps(List<Map> listOfMaps) {
			this.listOfMaps = listOfMaps;
		}
	}


	@SuppressWarnings("unused")
	private static class EnumTester {

		private Autowire autowire;

		public void setAutowire(Autowire autowire) {
			this.autowire = autowire;
		}

		public Autowire getAutowire() {
			return autowire;
		}
	}

	@SuppressWarnings("unused")
	private static class PropsTester {

		private Properties properties;

		private String name;

		private String[] stringArray;

		private int[] intArray;

		public void setProperties(Properties p) {
			properties = p;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setStringArray(String[] sa) {
			this.stringArray = sa;
		}

		public void setIntArray(int[] intArray) {
			this.intArray = intArray;
		}
	}

	@SuppressWarnings("unused")
	private static class StringArrayBean {

		private String[] array;

		public String[] getArray() {
			return array;
		}

		public void setArray(String[] array) {
			this.array = array;
		}
	}


	@SuppressWarnings("unused")
	private static class PrimitiveArrayBean {

		private int[] array;

		public int[] getArray() {
			return array;
		}

		public void setArray(int[] array) {
			this.array = array;
		}
	}

	@SuppressWarnings("unused")
	private static class Employee extends TestBean {

		private String company;

		public String getCompany() {
			return company;
		}

		public void setCompany(String co) {
			this.company = co;
		}
	}

	@SuppressWarnings("unused")
	private static class DifferentTestBean extends TestBean {
		// class to test naming of beans in a error message
	}


	@SuppressWarnings("unused")
	private static class NumberPropertyBean {

		private byte myPrimitiveByte;

		private Byte myByte;

		private short myPrimitiveShort;

		private Short myShort;

		private int myPrimitiveInt;

		private Integer myInteger;

		private long myPrimitiveLong;

		private Long myLong;

		private float myPrimitiveFloat;

		private Float myFloat;

		private double myPrimitiveDouble;

		private Double myDouble;

		public byte getMyPrimitiveByte() {
			return myPrimitiveByte;
		}

		public void setMyPrimitiveByte(byte myPrimitiveByte) {
			this.myPrimitiveByte = myPrimitiveByte;
		}

		public Byte getMyByte() {
			return myByte;
		}

		public void setMyByte(Byte myByte) {
			this.myByte = myByte;
		}

		public short getMyPrimitiveShort() {
			return myPrimitiveShort;
		}

		public void setMyPrimitiveShort(short myPrimitiveShort) {
			this.myPrimitiveShort = myPrimitiveShort;
		}

		public Short getMyShort() {
			return myShort;
		}

		public void setMyShort(Short myShort) {
			this.myShort = myShort;
		}

		public int getMyPrimitiveInt() {
			return myPrimitiveInt;
		}

		public void setMyPrimitiveInt(int myPrimitiveInt) {
			this.myPrimitiveInt = myPrimitiveInt;
		}

		public Integer getMyInteger() {
			return myInteger;
		}

		public void setMyInteger(Integer myInteger) {
			this.myInteger = myInteger;
		}

		public long getMyPrimitiveLong() {
			return myPrimitiveLong;
		}

		public void setMyPrimitiveLong(long myPrimitiveLong) {
			this.myPrimitiveLong = myPrimitiveLong;
		}

		public Long getMyLong() {
			return myLong;
		}

		public void setMyLong(Long myLong) {
			this.myLong = myLong;
		}

		public float getMyPrimitiveFloat() {
			return myPrimitiveFloat;
		}

		public void setMyPrimitiveFloat(float myPrimitiveFloat) {
			this.myPrimitiveFloat = myPrimitiveFloat;
		}

		public Float getMyFloat() {
			return myFloat;
		}

		public void setMyFloat(Float myFloat) {
			this.myFloat = myFloat;
		}

		public double getMyPrimitiveDouble() {
			return myPrimitiveDouble;
		}

		public void setMyPrimitiveDouble(double myPrimitiveDouble) {
			this.myPrimitiveDouble = myPrimitiveDouble;
		}

		public Double getMyDouble() {
			return myDouble;
		}

		public void setMyDouble(Double myDouble) {
			this.myDouble = myDouble;
		}
	}


	public static class EnumConsumer {

		private Enum<TestEnum> enumValue;

		public Enum<TestEnum> getEnumValue() {
			return enumValue;
		}

		public void setEnumValue(Enum<TestEnum> enumValue) {
			this.enumValue = enumValue;
		}
	}


	public static class WildcardEnumConsumer {

		private Enum<?> enumValue;

		public Enum<?> getEnumValue() {
			return enumValue;
		}

		public void setEnumValue(Enum<?> enumValue) {
			this.enumValue = enumValue;
		}
	}


	public enum TestEnum {

		TEST_VALUE
	}


	public static class ArrayToObject {

		private Object object;

		public void setObject(Object object) {
			this.object = object;
		}

		public Object getObject() {
			return object;
		}
	}


	public static class SkipReaderStub<T> {

		public T[] items;

		public SkipReaderStub() {
		}

		public SkipReaderStub(T... items) {
			this.items = items;
		}

		public void setItems(T... items) {
			this.items = items;
		}
	}


	static class Spr10115Bean {

		private static String prop1;

		public static void setProp1(String prop1) {
			Spr10115Bean.prop1 = prop1;
		}
	}

	interface Spr13837 {

		Integer getSomething();

		<T extends Spr13837> T setSomething(Integer something);

	}

	static class Spr13837Bean implements Spr13837 {

		protected Integer something;

		@Override
		public Integer getSomething() {
			return this.something;
		}

		@Override
		public Spr13837Bean setSomething(final Integer something) {
			this.something = something;
			return this;
		}
	}

	@SuppressWarnings("serial")
	public static class ReadOnlyMap<K, V> extends HashMap<K, V> {

		private boolean frozen = false;

		private boolean accessed = false;

		public ReadOnlyMap() {
			this.frozen = true;
		}

		public ReadOnlyMap(Map<? extends K, ? extends V> map) {
			super(map);
			this.frozen = true;
		}

		@Override
		public V put(K key, V value) {
			if (this.frozen) {
				throw new UnsupportedOperationException();
			}
			else {
				return super.put(key, value);
			}
		}

		@Override
		public Set<Map.Entry<K, V>> entrySet() {
			this.accessed = true;
			return super.entrySet();
		}

		@Override
		public Set<K> keySet() {
			this.accessed = true;
			return super.keySet();
		}

		@Override
		public int size() {
			this.accessed = true;
			return super.size();
		}

		public boolean isAccessed() {
			return this.accessed;
		}
	}

}
