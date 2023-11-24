/*
 * Copyright 2002-2023 the original author or authors.
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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.beans.support.DerivedFromProtectedBaseBean;
import org.springframework.beans.testfixture.beans.BooleanTestBean;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.IndexedTestBean;
import org.springframework.beans.testfixture.beans.NumberTestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

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
abstract class AbstractPropertyAccessorTests {

	protected abstract AbstractPropertyAccessor createAccessor(Object target);


	@Test
	void createWithNullTarget() {
		assertThatIllegalArgumentException().isThrownBy(() -> createAccessor(null));
	}

	@Test
	void isReadableProperty() {
		AbstractPropertyAccessor accessor = createAccessor(new Simple("John", 2));

		assertThat(accessor.isReadableProperty("name")).isTrue();
	}

	@Test
	void isReadablePropertyNotReadable() {
		AbstractPropertyAccessor accessor = createAccessor(new NoRead());

		assertThat(accessor.isReadableProperty("age")).isFalse();
	}

	/**
	 * Shouldn't throw an exception: should just return false
	 */
	@Test
	void isReadablePropertyNoSuchProperty() {
		AbstractPropertyAccessor accessor = createAccessor(new NoRead());

		assertThat(accessor.isReadableProperty("xxxxx")).isFalse();
	}

	@Test
	void isReadablePropertyNull() {
		AbstractPropertyAccessor accessor = createAccessor(new NoRead());

		assertThatIllegalArgumentException().isThrownBy(() -> accessor.isReadableProperty(null));
	}

	@Test
	void isWritableProperty() {
		AbstractPropertyAccessor accessor = createAccessor(new Simple("John", 2));

		assertThat(accessor.isWritableProperty("name")).isTrue();
	}

	@Test
	void isWritablePropertyNull() {
		AbstractPropertyAccessor accessor = createAccessor(new NoRead());

		assertThatIllegalArgumentException().isThrownBy(() -> accessor.isWritableProperty(null));
	}

	@Test
	void isWritablePropertyNoSuchProperty() {
		AbstractPropertyAccessor accessor = createAccessor(new NoRead());

		assertThat(accessor.isWritableProperty("xxxxx")).isFalse();
	}

	@Test
	void isReadableWritableForIndexedProperties() {
		IndexedTestBean target = new IndexedTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);

		assertThat(accessor.isReadableProperty("array")).isTrue();
		assertThat(accessor.isReadableProperty("list")).isTrue();
		assertThat(accessor.isReadableProperty("set")).isTrue();
		assertThat(accessor.isReadableProperty("map")).isTrue();
		assertThat(accessor.isReadableProperty("myTestBeans")).isTrue();
		assertThat(accessor.isReadableProperty("xxx")).isFalse();

		assertThat(accessor.isWritableProperty("array")).isTrue();
		assertThat(accessor.isWritableProperty("list")).isTrue();
		assertThat(accessor.isWritableProperty("set")).isTrue();
		assertThat(accessor.isWritableProperty("map")).isTrue();
		assertThat(accessor.isWritableProperty("myTestBeans")).isTrue();
		assertThat(accessor.isWritableProperty("xxx")).isFalse();

		assertThat(accessor.isReadableProperty("array[0]")).isTrue();
		assertThat(accessor.isReadableProperty("array[0].name")).isTrue();
		assertThat(accessor.isReadableProperty("list[0]")).isTrue();
		assertThat(accessor.isReadableProperty("list[0].name")).isTrue();
		assertThat(accessor.isReadableProperty("set[0]")).isTrue();
		assertThat(accessor.isReadableProperty("set[0].name")).isTrue();
		assertThat(accessor.isReadableProperty("map[key1]")).isTrue();
		assertThat(accessor.isReadableProperty("map[key1].name")).isTrue();
		assertThat(accessor.isReadableProperty("map[key4][0]")).isTrue();
		assertThat(accessor.isReadableProperty("map[key4][0].name")).isTrue();
		assertThat(accessor.isReadableProperty("map[key4][1]")).isTrue();
		assertThat(accessor.isReadableProperty("map[key4][1].name")).isTrue();
		assertThat(accessor.isReadableProperty("myTestBeans[0]")).isTrue();
		assertThat(accessor.isReadableProperty("myTestBeans[1]")).isFalse();
		assertThat(accessor.isReadableProperty("array[key1]")).isFalse();

		assertThat(accessor.isWritableProperty("array[0]")).isTrue();
		assertThat(accessor.isWritableProperty("array[0].name")).isTrue();
		assertThat(accessor.isWritableProperty("list[0]")).isTrue();
		assertThat(accessor.isWritableProperty("list[0].name")).isTrue();
		assertThat(accessor.isWritableProperty("set[0]")).isTrue();
		assertThat(accessor.isWritableProperty("set[0].name")).isTrue();
		assertThat(accessor.isWritableProperty("map[key1]")).isTrue();
		assertThat(accessor.isWritableProperty("map[key1].name")).isTrue();
		assertThat(accessor.isWritableProperty("map[key4][0]")).isTrue();
		assertThat(accessor.isWritableProperty("map[key4][0].name")).isTrue();
		assertThat(accessor.isWritableProperty("map[key4][1]")).isTrue();
		assertThat(accessor.isWritableProperty("map[key4][1].name")).isTrue();
		assertThat(accessor.isReadableProperty("myTestBeans[0]")).isTrue();
		assertThat(accessor.isReadableProperty("myTestBeans[1]")).isFalse();
		assertThat(accessor.isWritableProperty("array[key1]")).isFalse();
	}

	@Test
	void getSimpleProperty() {
		Simple target = new Simple("John", 2);
		AbstractPropertyAccessor accessor = createAccessor(target);
		assertThat(accessor.getPropertyValue("name")).isEqualTo("John");
	}

	@Test
	void getNestedProperty() {
		Person target = createPerson("John", "London", "UK");
		AbstractPropertyAccessor accessor = createAccessor(target);
		assertThat(accessor.getPropertyValue("address.city")).isEqualTo("London");
	}

	@Test
	void getNestedDeepProperty() {
		Person target = createPerson("John", "London", "UK");
		AbstractPropertyAccessor accessor = createAccessor(target);
		assertThat(accessor.getPropertyValue("address.country.name")).isEqualTo("UK");
	}

	@Test
	void getAnotherNestedDeepProperty() {
		ITestBean target = new TestBean("rod", 31);
		ITestBean kerry = new TestBean("kerry", 35);
		target.setSpouse(kerry);
		kerry.setSpouse(target);
		AbstractPropertyAccessor accessor = createAccessor(target);
		Integer KA = (Integer) accessor.getPropertyValue("spouse.age");
		assertThat(KA).as("kerry is 35").isEqualTo(35);
		Integer RA = (Integer) accessor.getPropertyValue("spouse.spouse.age");
		assertThat(RA).as("rod is 31, not" + RA).isEqualTo(31);
		ITestBean spousesSpouse = (ITestBean) accessor.getPropertyValue("spouse.spouse");
		assertThat(target).as("spousesSpouse = initial point").isSameAs(spousesSpouse);
	}

	@Test
	void getPropertyIntermediatePropertyIsNull() {
		Person target = createPerson("John", "London", "UK");
		target.address = null;
		AbstractPropertyAccessor accessor = createAccessor(target);
		assertThatExceptionOfType(NullValueInNestedPathException.class).isThrownBy(() ->
				accessor.getPropertyValue("address.country.name"))
			.satisfies(ex -> {
				assertThat(ex.getPropertyName()).isEqualTo("address");
				assertThat(ex.getBeanClass()).isEqualTo(Person.class);
			});
	}

	@Test
	void getPropertyIntermediatePropertyIsNullWithAutoGrow() {
		Person target = createPerson("John", "London", "UK");
		target.address = null;
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setAutoGrowNestedPaths(true);

		assertThat(accessor.getPropertyValue("address.country.name")).isEqualTo("DefaultCountry");
	}

	@Test
	@SuppressWarnings("unchecked")
	void getPropertyIntermediateMapEntryIsNullWithAutoGrow() {
		Foo target = new Foo();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setConversionService(new DefaultConversionService());
		accessor.setAutoGrowNestedPaths(true);
		accessor.setPropertyValue("listOfMaps[0]['luckyNumber']", "9");
		assertThat(target.listOfMaps.get(0)).containsEntry("luckyNumber", "9");
	}

	@Test
	void getUnknownProperty() {
		Simple target = new Simple("John", 2);
		AbstractPropertyAccessor accessor = createAccessor(target);
		assertThatExceptionOfType(NotReadablePropertyException.class).isThrownBy(() ->
				accessor.getPropertyValue("foo"))
			.satisfies(ex -> {
				assertThat(ex.getBeanClass()).isEqualTo(Simple.class);
				assertThat(ex.getPropertyName()).isEqualTo("foo");
			});
	}

	@Test
	void getUnknownNestedProperty() {
		Person target = createPerson("John", "London", "UK");
		AbstractPropertyAccessor accessor = createAccessor(target);

		assertThatExceptionOfType(NotReadablePropertyException.class).isThrownBy(() ->
				accessor.getPropertyValue("address.bar"));
	}

	@Test
	void setSimpleProperty() {
		Simple target = new Simple("John", 2);
		AbstractPropertyAccessor accessor = createAccessor(target);

		accessor.setPropertyValue("name", "SomeValue");

		assertThat(target.name).isEqualTo("SomeValue");
		assertThat(target.getName()).isEqualTo("SomeValue");
	}

	@Test
	void setNestedProperty() {
		Person target = createPerson("John", "Paris", "FR");
		AbstractPropertyAccessor accessor = createAccessor(target);

		accessor.setPropertyValue("address.city", "London");
		assertThat(target.address.city).isEqualTo("London");
	}

	@Test
	void setNestedPropertyPolymorphic() throws Exception {
		ITestBean target = new TestBean("rod", 31);
		ITestBean kerry = new Employee();

		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setPropertyValue("spouse", kerry);
		accessor.setPropertyValue("spouse.age", 35);
		accessor.setPropertyValue("spouse.name", "Kerry");
		accessor.setPropertyValue("spouse.company", "Lewisham");
		assertThat(kerry.getName()).as("kerry name is Kerry").isEqualTo("Kerry");

		assertThat(target.getSpouse()).as("nested set worked").isSameAs(kerry);
		assertThat(kerry.getSpouse()).as("no back relation").isNull();
		accessor.setPropertyValue(new PropertyValue("spouse.spouse", target));
		assertThat(kerry.getSpouse()).as("nested set worked").isSameAs(target);

		AbstractPropertyAccessor kerryAccessor = createAccessor(kerry);
		assertThat(kerryAccessor.getPropertyValue("spouse.spouse.spouse.spouse.company")).as("spouse.spouse.spouse.spouse.company=Lewisham")
				.isEqualTo("Lewisham");
	}

	@Test
	void setAnotherNestedProperty() throws Exception {
		ITestBean target = new TestBean("rod", 31);
		ITestBean kerry = new TestBean("kerry", 0);

		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setPropertyValue("spouse", kerry);

		assertThat(target.getSpouse()).as("nested set worked").isSameAs(kerry);
		assertThat(kerry.getSpouse()).as("no back relation").isNull();
		accessor.setPropertyValue(new PropertyValue("spouse.spouse", target));
		assertThat(kerry.getSpouse()).as("nested set worked").isSameAs(target);
		assertThat(kerry.getAge()).as("kerry age not set").isEqualTo(0);
		accessor.setPropertyValue(new PropertyValue("spouse.age", 35));
		assertThat(kerry.getAge()).as("Set primitive on spouse").isEqualTo(35);

		assertThat(accessor.getPropertyValue("spouse")).isEqualTo(kerry);
		assertThat(accessor.getPropertyValue("spouse.spouse")).isEqualTo(target);
	}

	@Test
	void setYetAnotherNestedProperties() {
		String doctorCompany = "";
		String lawyerCompany = "Dr. Sueem";
		TestBean target = new TestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setPropertyValue("doctor.company", doctorCompany);
		accessor.setPropertyValue("lawyer.company", lawyerCompany);
		assertThat(target.getDoctor().getCompany()).isEqualTo(doctorCompany);
		assertThat(target.getLawyer().getCompany()).isEqualTo(lawyerCompany);
	}

	@Test
	void setNestedDeepProperty() {
		Person target = createPerson("John", "Paris", "FR");
		AbstractPropertyAccessor accessor = createAccessor(target);

		accessor.setPropertyValue("address.country.name", "UK");
		assertThat(target.address.country.name).isEqualTo("UK");
	}

	@Test
	void testErrorMessageOfNestedProperty() {
		ITestBean target = new TestBean();
		ITestBean child = new DifferentTestBean();
		child.setName("test");
		target.setSpouse(child);
		AbstractPropertyAccessor accessor = createAccessor(target);
		try {
			accessor.getPropertyValue("spouse.bla");
		}
		catch (NotReadablePropertyException ex) {
			assertThat(ex.getMessage()).contains(TestBean.class.getName());
		}
	}

	@Test
	void setPropertyIntermediatePropertyIsNull() {
		Person target = createPerson("John", "Paris", "FR");
		target.address.country = null;
		AbstractPropertyAccessor accessor = createAccessor(target);
		assertThatExceptionOfType(NullValueInNestedPathException.class).isThrownBy(() ->
				accessor.setPropertyValue("address.country.name", "UK"))
			.satisfies(ex -> {
				assertThat(ex.getPropertyName()).isEqualTo("address.country");
				assertThat(ex.getBeanClass()).isEqualTo(Person.class);
			});
		assertThat(target.address.country).isNull(); // Not touched
	}

	@Test
	void setAnotherPropertyIntermediatePropertyIsNull() throws Exception {
		ITestBean target = new TestBean("rod", 31);
		AbstractPropertyAccessor accessor = createAccessor(target);
		assertThatExceptionOfType(NullValueInNestedPathException.class).isThrownBy(() ->
				accessor.setPropertyValue("spouse.age", 31))
			.satisfies(ex -> assertThat(ex.getPropertyName()).isEqualTo("spouse"));
	}

	@Test
	void setPropertyIntermediatePropertyIsNullWithAutoGrow() {
		Person target = createPerson("John", "Paris", "FR");
		target.address.country = null;
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setAutoGrowNestedPaths(true);

		accessor.setPropertyValue("address.country.name", "UK");
		assertThat(target.address.country.name).isEqualTo("UK");
	}

	@Test
	void setPropertyIntermediateListIsNullWithAutoGrow() {
		Foo target = new Foo();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setConversionService(new DefaultConversionService());
		accessor.setAutoGrowNestedPaths(true);
		Map<String, String> map = new HashMap<>();
		map.put("favoriteNumber", "9");
		accessor.setPropertyValue("list[0]", map);
		assertThat(target.list.get(0)).isEqualTo(map);
	}

	@Test
	void setPropertyIntermediateListIsNullWithNoConversionService() {
		Foo target = new Foo();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setAutoGrowNestedPaths(true);
		accessor.setPropertyValue("listOfMaps[0]['luckyNumber']", "9");
		assertThat(target.listOfMaps.get(0).get("luckyNumber")).isEqualTo("9");
	}

	@Test
	void setPropertyIntermediateListIsNullWithBadConversionService() {
		Foo target = new Foo();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setConversionService(new GenericConversionService() {
			@Override
			public Object convert(@Nullable Object source, @Nullable TypeDescriptor sourceType, TypeDescriptor targetType) {
				throw new ConversionFailedException(sourceType, targetType, source, null);
			}
		});
		accessor.setAutoGrowNestedPaths(true);
		accessor.setPropertyValue("listOfMaps[0]['luckyNumber']", "9");
		assertThat(target.listOfMaps.get(0).get("luckyNumber")).isEqualTo("9");
	}


	@Test
	void setEmptyPropertyValues() {
		TestBean target = new TestBean();
		int age = 50;
		String name = "Tony";
		target.setAge(age);
		target.setName(name);
		AbstractPropertyAccessor accessor = createAccessor(target);
		assertThat(target.getAge()).as("age is OK").isEqualTo(age);
		assertThat(name).as("name is OK").isEqualTo(target.getName());
		accessor.setPropertyValues(new MutablePropertyValues());
		// Check its unchanged
		assertThat(target.getAge()).as("age is OK").isEqualTo(age);
		assertThat(name).as("name is OK").isEqualTo(target.getName());
	}


	@Test
	void setValidPropertyValues() {
		TestBean target = new TestBean();
		String newName = "tony";
		int newAge = 65;
		String newTouchy = "valid";
		AbstractPropertyAccessor accessor = createAccessor(target);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue(new PropertyValue("age", newAge));
		pvs.addPropertyValue(new PropertyValue("name", newName));
		pvs.addPropertyValue(new PropertyValue("touchy", newTouchy));
		accessor.setPropertyValues(pvs);
		assertThat(target.getName()).as("Name property should have changed").isEqualTo(newName);
		assertThat(target.getTouchy()).as("Touchy property should have changed").isEqualTo(newTouchy);
		assertThat(target.getAge()).as("Age property should have changed").isEqualTo(newAge);
	}

	@Test
	void setIndividualValidPropertyValues() {
		TestBean target = new TestBean();
		String newName = "tony";
		int newAge = 65;
		String newTouchy = "valid";
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setPropertyValue("age", newAge);
		accessor.setPropertyValue(new PropertyValue("name", newName));
		accessor.setPropertyValue(new PropertyValue("touchy", newTouchy));
		assertThat(target.getName()).as("Name property should have changed").isEqualTo(newName);
		assertThat(target.getTouchy()).as("Touchy property should have changed").isEqualTo(newTouchy);
		assertThat(target.getAge()).as("Age property should have changed").isEqualTo(newAge);
	}

	@Test
	void setPropertyIsReflectedImmediately() {
		TestBean target = new TestBean();
		int newAge = 33;
		AbstractPropertyAccessor accessor = createAccessor(target);
		target.setAge(newAge);
		Object bwAge = accessor.getPropertyValue("age");
		assertThat(bwAge).as("Age is an integer").isInstanceOf(Integer.class);
		assertThat(bwAge).as("Bean wrapper must pick up changes").isEqualTo(newAge);
	}

	@Test
	void setPropertyToNull() {
		TestBean target = new TestBean();
		target.setName("Frank");    // we need to change it back
		target.setSpouse(target);
		AbstractPropertyAccessor accessor = createAccessor(target);
		assertThat(target.getName()).as("name is not null to start off").isNotNull();
		accessor.setPropertyValue("name", null);
		assertThat(target.getName()).as("name is now null").isNull();
		// now test with non-string
		assertThat(target.getSpouse()).as("spouse is not null to start off").isNotNull();
		accessor.setPropertyValue("spouse", null);
		assertThat(target.getSpouse()).as("spouse is now null").isNull();
	}

	@Test
	void setIndexedPropertyIgnored() {
		MutablePropertyValues values = new MutablePropertyValues();
		values.add("toBeIgnored[0]", 42);
		AbstractPropertyAccessor accessor = createAccessor(new Object());
		accessor.setPropertyValues(values, true);
	}

	@Test
	void setPropertyWithPrimitiveConversion() {
		MutablePropertyValues values = new MutablePropertyValues();
		values.add("name", 42);
		TestBean target = new TestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setPropertyValues(values);
		assertThat(target.getName()).isEqualTo("42");
	}

	@Test
	void setPropertyWithCustomEditor() {
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
		assertThat(target.getName()).isEqualTo(Integer.class.toString());
	}

	@Test
	void setStringPropertyWithCustomEditor() throws Exception {
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
		assertThat(target.getName()).isEmpty();
		accessor.setPropertyValue("name", new String[] {"a1", "b2"});
		assertThat(target.getName()).isEqualTo("a1-b2");
		accessor.setPropertyValue("name", null);
		assertThat(target.getName()).isEmpty();
	}

	@Test
	void setBooleanProperty() {
		BooleanTestBean target = new BooleanTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);

		accessor.setPropertyValue("bool2", "true");
		assertThat(Boolean.TRUE.equals(accessor.getPropertyValue("bool2"))).as("Correct bool2 value").isTrue();
		assertThat(target.getBool2()).as("Correct bool2 value").isTrue();

		accessor.setPropertyValue("bool2", "false");
		assertThat(Boolean.FALSE.equals(accessor.getPropertyValue("bool2"))).as("Correct bool2 value").isTrue();
		assertThat(target.getBool2()).as("Correct bool2 value").isFalse();
	}

	@Test
	void setNumberProperties() {
		NumberTestBean target = new NumberTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setPropertyValue("short2", "2");
		accessor.setPropertyValue("int2", "8");
		accessor.setPropertyValue("long2", "6");
		accessor.setPropertyValue("bigInteger", "3");
		accessor.setPropertyValue("float2", "8.1");
		accessor.setPropertyValue("double2", "6.1");
		accessor.setPropertyValue("bigDecimal", "4.0");
		assertThat(Short.valueOf("2")).as("Correct short2 value").isEqualTo(accessor.getPropertyValue("short2"));
		assertThat(Short.valueOf("2")).as("Correct short2 value").isEqualTo(target.getShort2());
		assertThat(Integer.valueOf("8")).as("Correct int2 value").isEqualTo(accessor.getPropertyValue("int2"));
		assertThat(Integer.valueOf("8")).as("Correct int2 value").isEqualTo(target.getInt2());
		assertThat(Long.valueOf("6")).as("Correct long2 value").isEqualTo(accessor.getPropertyValue("long2"));
		assertThat(Long.valueOf("6")).as("Correct long2 value").isEqualTo(target.getLong2());
		assertThat(new BigInteger("3")).as("Correct bigInteger value").isEqualTo(accessor.getPropertyValue("bigInteger"));
		assertThat(new BigInteger("3")).as("Correct bigInteger value").isEqualTo(target.getBigInteger());
		assertThat(Float.valueOf("8.1")).as("Correct float2 value").isEqualTo(accessor.getPropertyValue("float2"));
		assertThat(Float.valueOf("8.1")).as("Correct float2 value").isEqualTo(target.getFloat2());
		assertThat(Double.valueOf("6.1").equals(accessor.getPropertyValue("double2"))).as("Correct double2 value").isTrue();
		assertThat(Double.valueOf("6.1")).as("Correct double2 value").isEqualTo(target.getDouble2());
		assertThat(new BigDecimal("4.0")).as("Correct bigDecimal value").isEqualTo(accessor.getPropertyValue("bigDecimal"));
		assertThat(new BigDecimal("4.0")).as("Correct bigDecimal value").isEqualTo(target.getBigDecimal());
	}

	@Test
	void setNumberPropertiesWithCoercion() {
		NumberTestBean target = new NumberTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setPropertyValue("short2", 2);
		accessor.setPropertyValue("int2", 8L);
		accessor.setPropertyValue("long2", new BigInteger("6"));
		accessor.setPropertyValue("bigInteger", 3L);
		accessor.setPropertyValue("float2", 8.1D);
		accessor.setPropertyValue("double2", new BigDecimal("6.1"));
		accessor.setPropertyValue("bigDecimal", 4.0F);
		assertThat(Short.valueOf("2")).as("Correct short2 value").isEqualTo(accessor.getPropertyValue("short2"));
		assertThat(Short.valueOf("2")).as("Correct short2 value").isEqualTo(target.getShort2());
		assertThat(Integer.valueOf("8")).as("Correct int2 value").isEqualTo(accessor.getPropertyValue("int2"));
		assertThat(Integer.valueOf("8")).as("Correct int2 value").isEqualTo(target.getInt2());
		assertThat(Long.valueOf("6")).as("Correct long2 value").isEqualTo(accessor.getPropertyValue("long2"));
		assertThat(Long.valueOf("6")).as("Correct long2 value").isEqualTo(target.getLong2());
		assertThat(new BigInteger("3").equals(accessor.getPropertyValue("bigInteger"))).as("Correct bigInteger value").isTrue();
		assertThat(new BigInteger("3")).as("Correct bigInteger value").isEqualTo(target.getBigInteger());
		assertThat(Float.valueOf("8.1")).as("Correct float2 value").isEqualTo(accessor.getPropertyValue("float2"));
		assertThat(Float.valueOf("8.1")).as("Correct float2 value").isEqualTo(target.getFloat2());
		assertThat(Double.valueOf("6.1")).as("Correct double2 value").isEqualTo(accessor.getPropertyValue("double2"));
		assertThat(Double.valueOf("6.1")).as("Correct double2 value").isEqualTo(target.getDouble2());
		assertThat(new BigDecimal("4.0")).as("Correct bigDecimal value").isEqualTo(accessor.getPropertyValue("bigDecimal"));
		assertThat(new BigDecimal("4.0")).as("Correct bigDecimal value").isEqualTo(target.getBigDecimal());
	}

	@Test
	void setPrimitiveProperties() {
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

		assertThat(target.getMyPrimitiveByte()).isEqualTo(Byte.MAX_VALUE);
		assertThat(target.getMyByte()).isEqualTo(Byte.MAX_VALUE);

		assertThat(target.getMyPrimitiveShort()).isEqualTo(Short.MAX_VALUE);
		assertThat(target.getMyShort()).isEqualTo(Short.MAX_VALUE);

		assertThat(target.getMyPrimitiveInt()).isEqualTo(Integer.MAX_VALUE);
		assertThat(target.getMyInteger()).isEqualTo(Integer.MAX_VALUE);

		assertThat(target.getMyPrimitiveLong()).isEqualTo(Long.MAX_VALUE);
		assertThat(target.getMyLong()).isEqualTo(Long.MAX_VALUE);

		assertThat((double) target.getMyPrimitiveFloat()).isCloseTo(Float.MAX_VALUE, within(0.001));
		assertThat((double) target.getMyFloat()).isCloseTo(Float.MAX_VALUE, within(0.001));

		assertThat(target.getMyPrimitiveDouble()).isCloseTo(Double.MAX_VALUE, within(0.001));
		assertThat(target.getMyDouble()).isCloseTo(Double.MAX_VALUE, within(0.001));
	}

	@Test
	void setEnumProperty() {
		EnumTester target = new EnumTester();
		AbstractPropertyAccessor accessor = createAccessor(target);

		accessor.setPropertyValue("autowire", "BY_NAME");
		assertThat(target.getAutowire()).isEqualTo(Autowire.BY_NAME);

		accessor.setPropertyValue("autowire", "  BY_TYPE ");
		assertThat(target.getAutowire()).isEqualTo(Autowire.BY_TYPE);

		assertThatExceptionOfType(TypeMismatchException.class).isThrownBy(() ->
				accessor.setPropertyValue("autowire", "NHERITED"));
	}

	@Test
	void setGenericEnumProperty() {
		EnumConsumer target = new EnumConsumer();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setPropertyValue("enumValue", TestEnum.class.getName() + ".TEST_VALUE");
		assertThat(target.getEnumValue()).isEqualTo(TestEnum.TEST_VALUE);
	}

	@Test
	void setWildcardEnumProperty() {
		WildcardEnumConsumer target = new WildcardEnumConsumer();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setPropertyValue("enumValue", TestEnum.class.getName() + ".TEST_VALUE");
		assertThat(target.getEnumValue()).isEqualTo(TestEnum.TEST_VALUE);
	}

	@Test
	void setPropertiesProperty() throws Exception {
		PropsTester target = new PropsTester();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setPropertyValue("name", "ptest");

		// Note format...
		String ps = "peace=war\nfreedom=slavery";
		accessor.setPropertyValue("properties", ps);

		assertThat(target.name).as("name was set").isEqualTo("ptest");
		assertThat(target.properties).as("properties non null").isNotNull();
		String freedomVal = target.properties.getProperty("freedom");
		String peaceVal = target.properties.getProperty("peace");
		assertThat(peaceVal).as("peace==war").isEqualTo("war");
		assertThat(freedomVal).as("Freedom==slavery").isEqualTo("slavery");
	}

	@Test
	void setStringArrayProperty() throws Exception {
		PropsTester target = new PropsTester();
		AbstractPropertyAccessor accessor = createAccessor(target);

		accessor.setPropertyValue("stringArray", new String[]{"foo", "fi", "fi", "fum"});
		assertThat(target.stringArray).containsExactly("foo", "fi", "fi", "fum");

		accessor.setPropertyValue("stringArray", Arrays.asList("foo", "fi", "fi", "fum"));
		assertThat(target.stringArray).containsExactly("foo", "fi", "fi", "fum");

		Set<String> set = new HashSet<>();
		set.add("foo");
		set.add("fi");
		set.add("fum");
		accessor.setPropertyValue("stringArray", set);
		assertThat(target.stringArray).containsExactlyInAnyOrder("foo", "fi", "fum");

		accessor.setPropertyValue("stringArray", "one");
		assertThat(target.stringArray).containsExactly("one");

		accessor.setPropertyValue("stringArray", null);
		assertThat(target.stringArray).as("stringArray is null").isNull();
	}

	@Test
	void setStringArrayPropertyWithCustomStringEditor() throws Exception {
		PropsTester target = new PropsTester();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.registerCustomEditor(String.class, "stringArray", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) {
				setValue(text.substring(1));
			}
		});

		accessor.setPropertyValue("stringArray", new String[] {"4foo", "7fi", "6fi", "5fum"});
		assertThat(target.stringArray).containsExactly("foo", "fi", "fi", "fum");

		List<String> list = Arrays.asList("4foo", "7fi", "6fi", "5fum");
		accessor.setPropertyValue("stringArray", list);
		assertThat(target.stringArray).containsExactly("foo", "fi", "fi", "fum");

		Set<String> set = new HashSet<>();
		set.add("4foo");
		set.add("7fi");
		set.add("6fum");
		accessor.setPropertyValue("stringArray", set);
		assertThat(target.stringArray).containsExactlyInAnyOrder("foo", "fi", "fum");

		accessor.setPropertyValue("stringArray", "8one");
		assertThat(target.stringArray).containsExactly("one");
	}

	@Test
	void setStringArrayPropertyWithStringSplitting() throws Exception {
		PropsTester target = new PropsTester();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.useConfigValueEditors();
		accessor.setPropertyValue("stringArray", "a1,b2");
		assertThat(target.stringArray).containsExactly("a1", "b2");
	}

	@Test
	void setStringArrayPropertyWithCustomStringDelimiter() throws Exception {
		PropsTester target = new PropsTester();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.registerCustomEditor(String[].class, "stringArray", new StringArrayPropertyEditor("-"));
		accessor.setPropertyValue("stringArray", "a1-b2");
		assertThat(target.stringArray).containsExactly("a1", "b2");
	}

	@Test
	void setStringArrayWithAutoGrow() throws Exception {
		StringArrayBean target = new StringArrayBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setAutoGrowNestedPaths(true);

		accessor.setPropertyValue("array[0]", "Test0");
		assertThat(target.getArray()).containsExactly("Test0");

		accessor.setPropertyValue("array[2]", "Test2");
		assertThat(target.getArray()).containsExactly("Test0", null, "Test2");
	}

	@Test
	void setIntArrayProperty() {
		PropsTester target = new PropsTester();
		AbstractPropertyAccessor accessor = createAccessor(target);

		accessor.setPropertyValue("intArray", new int[] {4, 5, 2, 3});
		assertThat(target.intArray).containsExactly(4, 5, 2, 3);

		accessor.setPropertyValue("intArray", new String[] {"4", "5", "2", "3"});
		assertThat(target.intArray).containsExactly(4, 5, 2, 3);

		accessor.setPropertyValue("intArray", Arrays.asList(4, "5", 2, "3"));
		assertThat(target.intArray).containsExactly(4, 5, 2, 3);

		Set<Object> set = new HashSet<>();
		set.add("4");
		set.add(5);
		set.add("3");
		accessor.setPropertyValue("intArray", set);
		assertThat(target.intArray).containsExactlyInAnyOrder(4, 5, 3);

		accessor.setPropertyValue("intArray", new Integer[] {1});
		assertThat(target.intArray).containsExactly(1);

		accessor.setPropertyValue("intArray", 1);
		assertThat(target.intArray).containsExactly(1);

		accessor.setPropertyValue("intArray", new String[] {"1"});
		assertThat(target.intArray).containsExactly(1);

		accessor.setPropertyValue("intArray", "1");
		assertThat(target.intArray).containsExactly(1);
	}

	@Test
	void setIntArrayPropertyWithCustomEditor() {
		PropsTester target = new PropsTester();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.registerCustomEditor(int.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) {
				setValue(Integer.parseInt(text) + 1);
			}
		});

		accessor.setPropertyValue("intArray", new int[] {4, 5, 2, 3});
		assertThat(target.intArray).containsExactly(4, 5, 2, 3);

		accessor.setPropertyValue("intArray", new String[] {"3", "4", "1", "2"});
		assertThat(target.intArray).containsExactly(4, 5, 2, 3);

		accessor.setPropertyValue("intArray", 1);
		assertThat(target.intArray).containsExactly(1);

		accessor.setPropertyValue("intArray", new String[]{"0"});
		assertThat(target.intArray).containsExactly(1);

		accessor.setPropertyValue("intArray", "0");
		assertThat(target.intArray).containsExactly(1);
	}

	@Test
	void setIntArrayPropertyWithStringSplitting() throws Exception {
		PropsTester target = new PropsTester();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.useConfigValueEditors();
		accessor.setPropertyValue("intArray", "4,5");
		assertThat(target.intArray).containsExactly(4, 5);
	}

	@Test
	void setPrimitiveArrayProperty() {
		PrimitiveArrayBean target = new PrimitiveArrayBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setPropertyValue("array", new String[]{"1", "2"});
		assertThat(target.getArray()).containsExactly(1, 2);
	}

	@Test
	void setPrimitiveArrayPropertyLargeMatchingWithSpecificEditor() {
		PrimitiveArrayBean target = new PrimitiveArrayBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.registerCustomEditor(int.class, "array", new PropertyEditorSupport() {
			@Override
			public void setValue(Object value) {
				if (value instanceof Integer) {
					super.setValue((Integer) value + 1);
				}
			}
		});
		int[] input = new int[10];
		accessor.setPropertyValue("array", input);
		assertThat(target.getArray()).hasSize(10);
		assertThat(Arrays.stream(target.getArray())).allMatch(n -> n == 1);
	}

	@Test
	void setPrimitiveArrayPropertyLargeMatchingWithIndexSpecificEditor() {
		PrimitiveArrayBean target = new PrimitiveArrayBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.registerCustomEditor(int.class, "array[1]", new PropertyEditorSupport() {
			@Override
			public void setValue(Object value) {
				if (value instanceof Integer) {
					super.setValue((Integer) value + 1);
				}
			}
		});
		int[] input = new int[10];
		accessor.setPropertyValue("array", input);
		assertThat(target.getArray()).hasSize(10);
		assertThat(target.getArray()[0]).isZero();
		assertThat(target.getArray()[1]).isEqualTo(1);
		assertThat(Arrays.stream(target.getArray()).skip(2)).allMatch(n -> n == 0);
	}

	@Test
	void setPrimitiveArrayPropertyWithAutoGrow() throws Exception {
		PrimitiveArrayBean target = new PrimitiveArrayBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setAutoGrowNestedPaths(true);

		accessor.setPropertyValue("array[0]", 1);
		assertThat(target.getArray()).containsExactly(1);

		accessor.setPropertyValue("array[2]", 3);
		assertThat(target.getArray()).containsExactly(1, 0, 3);
	}

	@Test
	void setGenericArrayProperty() {
		@SuppressWarnings("rawtypes")
		SkipReaderStub target = new SkipReaderStub();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setPropertyValue("items", Arrays.asList("1", "2", "3", "4"));
		assertThat(target.items).containsExactly("1", "2", "3", "4");
	}

	@Test
	void setArrayPropertyToObject() {
		ArrayToObject target = new ArrayToObject();
		AbstractPropertyAccessor accessor = createAccessor(target);

		Object[] array = new Object[] {"1", "2"};
		accessor.setPropertyValue("object", array);
		assertThat(target.getObject()).isEqualTo(array);

		array = new Object[] {"1"};
		accessor.setPropertyValue("object", array);
		assertThat(target.getObject()).isEqualTo(array);
	}

	@Test
	void setCollectionProperty() {
		IndexedTestBean target = new IndexedTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		Collection<String> coll = new HashSet<>();
		coll.add("coll1");
		accessor.setPropertyValue("collection", coll);
		Set<String> set = new HashSet<>();
		set.add("set1");
		accessor.setPropertyValue("set", set);
		SortedSet<String> sortedSet = new TreeSet<>();
		sortedSet.add("sortedSet1");
		accessor.setPropertyValue("sortedSet", sortedSet);
		List<String> list = new ArrayList<>();
		list.add("list1");
		accessor.setPropertyValue("list", list);
		assertThat(target.getCollection()).isSameAs(coll);
		assertThat(target.getSet()).isSameAs(set);
		assertThat(target.getSortedSet()).isSameAs(sortedSet);
		assertThat((List<?>) target.getList()).isSameAs(list);
	}

	@Test
	@SuppressWarnings("unchecked") // list cannot be properly parameterized as it breaks other tests
	void setCollectionPropertyNonMatchingType() {
		IndexedTestBean target = new IndexedTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		Collection<String> coll = new ArrayList<>();
		coll.add("coll1");
		accessor.setPropertyValue("collection", coll);
		List<String> set = new ArrayList<>();
		set.add("set1");
		accessor.setPropertyValue("set", set);
		List<String> sortedSet = new ArrayList<>();
		sortedSet.add("sortedSet1");
		accessor.setPropertyValue("sortedSet", sortedSet);
		Set<String> list = new HashSet<>();
		list.add("list1");
		accessor.setPropertyValue("list", list);
		assertThat(target.getCollection()).hasSize(1);
		assertThat(target.getCollection().containsAll(coll)).isTrue();
		assertThat(target.getSet()).hasSize(1);
		assertThat(target.getSet().containsAll(set)).isTrue();
		assertThat(target.getSortedSet()).hasSize(1);
		assertThat(target.getSortedSet().containsAll(sortedSet)).isTrue();
		assertThat(target.getList()).hasSize(1);
		assertThat(target.getList().containsAll(list)).isTrue();
	}

	@Test
	@SuppressWarnings("unchecked") // list cannot be properly parameterized as it breaks other tests
	void setCollectionPropertyWithArrayValue() {
		IndexedTestBean target = new IndexedTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		Collection<String> coll = new HashSet<>();
		coll.add("coll1");
		accessor.setPropertyValue("collection", coll.toArray());
		List<String> set = new ArrayList<>();
		set.add("set1");
		accessor.setPropertyValue("set", set.toArray());
		List<String> sortedSet = new ArrayList<>();
		sortedSet.add("sortedSet1");
		accessor.setPropertyValue("sortedSet", sortedSet.toArray());
		Set<String> list = new HashSet<>();
		list.add("list1");
		accessor.setPropertyValue("list", list.toArray());
		assertThat(target.getCollection()).hasSize(1);
		assertThat(target.getCollection().containsAll(coll)).isTrue();
		assertThat(target.getSet()).hasSize(1);
		assertThat(target.getSet().containsAll(set)).isTrue();
		assertThat(target.getSortedSet()).hasSize(1);
		assertThat(target.getSortedSet().containsAll(sortedSet)).isTrue();
		assertThat(target.getList()).hasSize(1);
		assertThat(target.getList().containsAll(list)).isTrue();
	}

	@Test
	@SuppressWarnings("unchecked") // list cannot be properly parameterized as it breaks other tests
	void setCollectionPropertyWithIntArrayValue() {
		IndexedTestBean target = new IndexedTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		Collection<Integer> coll = new HashSet<>();
		coll.add(0);
		accessor.setPropertyValue("collection", new int[]{0});
		List<Integer> set = new ArrayList<>();
		set.add(1);
		accessor.setPropertyValue("set", new int[]{1});
		List<Integer> sortedSet = new ArrayList<>();
		sortedSet.add(2);
		accessor.setPropertyValue("sortedSet", new int[]{2});
		Set<Integer> list = new HashSet<>();
		list.add(3);
		accessor.setPropertyValue("list", new int[]{3});
		assertThat(target.getCollection()).hasSize(1);
		assertThat(target.getCollection().containsAll(coll)).isTrue();
		assertThat(target.getSet()).hasSize(1);
		assertThat(target.getSet().containsAll(set)).isTrue();
		assertThat(target.getSortedSet()).hasSize(1);
		assertThat(target.getSortedSet().containsAll(sortedSet)).isTrue();
		assertThat(target.getList()).hasSize(1);
		assertThat(target.getList().containsAll(list)).isTrue();
	}

	@Test
	@SuppressWarnings("unchecked") // list cannot be properly parameterized as it breaks other tests
	void setCollectionPropertyWithIntegerValue() {
		IndexedTestBean target = new IndexedTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		Collection<Integer> coll = new HashSet<>();
		coll.add(0);
		accessor.setPropertyValue("collection", 0);
		List<Integer> set = new ArrayList<>();
		set.add(1);
		accessor.setPropertyValue("set", 1);
		List<Integer> sortedSet = new ArrayList<>();
		sortedSet.add(2);
		accessor.setPropertyValue("sortedSet", 2);
		Set<Integer> list = new HashSet<>();
		list.add(3);
		accessor.setPropertyValue("list", 3);
		assertThat(target.getCollection()).hasSize(1);
		assertThat(target.getCollection().containsAll(coll)).isTrue();
		assertThat(target.getSet()).hasSize(1);
		assertThat(target.getSet().containsAll(set)).isTrue();
		assertThat(target.getSortedSet()).hasSize(1);
		assertThat(target.getSortedSet().containsAll(sortedSet)).isTrue();
		assertThat(target.getList()).hasSize(1);
		assertThat(target.getList().containsAll(list)).isTrue();
	}

	@Test
	@SuppressWarnings("unchecked") // list cannot be properly parameterized as it breaks other tests
	void setCollectionPropertyWithStringValue() {
		IndexedTestBean target = new IndexedTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		List<String> set = new ArrayList<>();
		set.add("set1");
		accessor.setPropertyValue("set", "set1");
		List<String> sortedSet = new ArrayList<>();
		sortedSet.add("sortedSet1");
		accessor.setPropertyValue("sortedSet", "sortedSet1");
		Set<String> list = new HashSet<>();
		list.add("list1");
		accessor.setPropertyValue("list", "list1");
		assertThat(target.getSet()).hasSize(1);
		assertThat(target.getSet().containsAll(set)).isTrue();
		assertThat(target.getSortedSet()).hasSize(1);
		assertThat(target.getSortedSet().containsAll(sortedSet)).isTrue();
		assertThat(target.getList()).hasSize(1);
		assertThat(target.getList().containsAll(list)).isTrue();
	}

	@Test
	@SuppressWarnings("unchecked")
	void setCollectionPropertyWithStringValueAndCustomEditor() {
		IndexedTestBean target = new IndexedTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.registerCustomEditor(String.class, "set", new StringTrimmerEditor(false));
		accessor.registerCustomEditor(String.class, "list", new StringTrimmerEditor(false));

		accessor.setPropertyValue("set", "set1 ");
		accessor.setPropertyValue("sortedSet", "sortedSet1");
		accessor.setPropertyValue("list", "list1 ");
		assertThat(target.getSet()).hasSize(1);
		assertThat(target.getSet().contains("set1")).isTrue();
		assertThat(target.getSortedSet()).hasSize(1);
		assertThat(target.getSortedSet().contains("sortedSet1")).isTrue();
		assertThat(target.getList()).hasSize(1);
		assertThat(target.getList().contains("list1")).isTrue();

		accessor.setPropertyValue("list", Collections.singletonList("list1 "));
		assertThat(target.getList().contains("list1")).isTrue();
	}

	@Test
	void setMapProperty() {
		IndexedTestBean target = new IndexedTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		Map<String, String> map = new HashMap<>();
		map.put("key", "value");
		accessor.setPropertyValue("map", map);
		SortedMap<?, ?> sortedMap = new TreeMap<>();
		map.put("sortedKey", "sortedValue");
		accessor.setPropertyValue("sortedMap", sortedMap);
		assertThat((Map<?, ?>) target.getMap()).isSameAs(map);
		assertThat((Map<?, ?>) target.getSortedMap()).isSameAs(sortedMap);
	}

	@Test
	@SuppressWarnings("unchecked")
	void setMapPropertyNonMatchingType() {
		IndexedTestBean target = new IndexedTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		Map<String, String> map = new TreeMap<>();
		map.put("key", "value");
		accessor.setPropertyValue("map", map);
		Map<String, String> sortedMap = new TreeMap<>();
		sortedMap.put("sortedKey", "sortedValue");
		accessor.setPropertyValue("sortedMap", sortedMap);
		assertThat(target.getMap()).hasSize(1);
		assertThat(target.getMap().get("key")).isEqualTo("value");
		assertThat(target.getSortedMap()).hasSize(1);
		assertThat(target.getSortedMap().get("sortedKey")).isEqualTo("sortedValue");
	}

	@Test
	void setMapPropertyWithTypeConversion() {
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

		MutablePropertyValues goodValues = new MutablePropertyValues();
		goodValues.add("map[key1]", "rod");
		goodValues.add("map[key2]", "rob");
		accessor.setPropertyValues(goodValues);
		assertThat(((TestBean) target.getMap().get("key1")).getName()).isEqualTo("rod");
		assertThat(((TestBean) target.getMap().get("key2")).getName()).isEqualTo("rob");

		MutablePropertyValues badValues = new MutablePropertyValues();
		badValues.add("map[key1]", "rod");
		badValues.add("map[key2]", "");
		assertThatExceptionOfType(PropertyBatchUpdateException.class).isThrownBy(() ->
				accessor.setPropertyValues(badValues))
			.satisfies(ex -> assertThat(ex.getPropertyAccessException("map[key2]")).isInstanceOf(TypeMismatchException.class));
	}

	@Test
	void setMapPropertyWithUnmodifiableMap() {
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

		Map<Integer, String> inputMap = new HashMap<>();
		inputMap.put(1, "rod");
		inputMap.put(2, "rob");
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("map", Collections.unmodifiableMap(inputMap));
		accessor.setPropertyValues(pvs);
		assertThat(((TestBean) target.getMap().get(1)).getName()).isEqualTo("rod");
		assertThat(((TestBean) target.getMap().get(2)).getName()).isEqualTo("rob");
	}

	@Test
	void setMapPropertyWithCustomUnmodifiableMap() {
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

		Map<Object, Object> inputMap = new HashMap<>();
		inputMap.put(1, "rod");
		inputMap.put(2, "rob");
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("map", new ReadOnlyMap<>(inputMap));
		accessor.setPropertyValues(pvs);
		assertThat(((TestBean) target.getMap().get(1)).getName()).isEqualTo("rod");
		assertThat(((TestBean) target.getMap().get(2)).getName()).isEqualTo("rob");
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" }) // must work with raw map in this test
	void setRawMapPropertyWithNoEditorRegistered() {
		IndexedTestBean target = new IndexedTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		Map inputMap = new HashMap();
		inputMap.put(1, "rod");
		inputMap.put(2, "rob");
		ReadOnlyMap readOnlyMap = new ReadOnlyMap(inputMap);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("map", readOnlyMap);
		accessor.setPropertyValues(pvs);
		assertThat(target.getMap()).isSameAs(readOnlyMap);
		assertThat(readOnlyMap.isAccessed()).isFalse();
	}

	@Test
	void setUnknownProperty() {
		Simple target = new Simple("John", 2);
		AbstractPropertyAccessor accessor = createAccessor(target);
		assertThatExceptionOfType(NotWritablePropertyException.class).isThrownBy(() ->
				accessor.setPropertyValue("name1", "value"))
			.satisfies(ex -> {
				assertThat(ex.getBeanClass()).isEqualTo(Simple.class);
				assertThat(ex.getPropertyName()).isEqualTo("name1");
				assertThat(ex.getPossibleMatches()).containsExactly("name");
			});
	}

	@Test
	void setUnknownPropertyWithPossibleMatches() {
		Simple target = new Simple("John", 2);
		AbstractPropertyAccessor accessor = createAccessor(target);
		assertThatExceptionOfType(NotWritablePropertyException.class).isThrownBy(() ->
				accessor.setPropertyValue("foo", "value"))
			.satisfies(ex -> {
				assertThat(ex.getBeanClass()).isEqualTo(Simple.class);
				assertThat(ex.getPropertyName()).isEqualTo("foo");
			});
	}

	@Test
	void setUnknownOptionalProperty() {
		Simple target = new Simple("John", 2);
		AbstractPropertyAccessor accessor = createAccessor(target);
		PropertyValue value = new PropertyValue("foo", "value");
		value.setOptional(true);
		accessor.setPropertyValue(value);
	}

	@Test
	void setPropertyInProtectedBaseBean() {
		DerivedFromProtectedBaseBean target = new DerivedFromProtectedBaseBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setPropertyValue("someProperty", "someValue");
		assertThat(accessor.getPropertyValue("someProperty")).isEqualTo("someValue");
		assertThat(target.getSomeProperty()).isEqualTo("someValue");
	}

	@Test
	void setPropertyTypeMismatch() {
		TestBean target = new TestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		assertThatExceptionOfType(TypeMismatchException.class).isThrownBy(() ->
				accessor.setPropertyValue("age", "foobar"));
	}

	@Test
	void setEmptyValueForPrimitiveProperty() {
		TestBean target = new TestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		assertThatExceptionOfType(TypeMismatchException.class).isThrownBy(() ->
				accessor.setPropertyValue("age", ""));
	}

	@Test
	void setUnknownNestedProperty() {
		Person target = createPerson("John", "Paris", "FR");
		AbstractPropertyAccessor accessor = createAccessor(target);

		assertThatExceptionOfType(NotWritablePropertyException.class).isThrownBy(() ->
				accessor.setPropertyValue("address.bar", "value"));
	}

	@Test
	void setPropertyValuesIgnoresInvalidNestedOnRequest() {
		ITestBean target = new TestBean();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue(new PropertyValue("name", "rod"));
		pvs.addPropertyValue(new PropertyValue("graceful.rubbish", "tony"));
		pvs.addPropertyValue(new PropertyValue("more.garbage", new Object()));
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setPropertyValues(pvs, true);
		assertThat(target.getName()).as("Set valid and ignored invalid").isEqualTo("rod");
		assertThatExceptionOfType(NotWritablePropertyException.class).isThrownBy(() ->
				accessor.setPropertyValues(pvs, false)); // Don't ignore: should fail
	}

	@Test
	void getAndSetIndexedProperties() {
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
		TestBean tb8 = ((TestBean) target.getMap().get("key5[foo]"));
		assertThat(tb0.getName()).isEqualTo("name0");
		assertThat(tb1.getName()).isEqualTo("name1");
		assertThat(tb2.getName()).isEqualTo("name2");
		assertThat(tb3.getName()).isEqualTo("name3");
		assertThat(tb6.getName()).isEqualTo("name6");
		assertThat(tb7.getName()).isEqualTo("name7");
		assertThat(tb4.getName()).isEqualTo("name4");
		assertThat(tb5.getName()).isEqualTo("name5");
		assertThat(tb8.getName()).isEqualTo("name8");
		assertThat(accessor.getPropertyValue("array[0].name")).isEqualTo("name0");
		assertThat(accessor.getPropertyValue("array[1].name")).isEqualTo("name1");
		assertThat(accessor.getPropertyValue("list[0].name")).isEqualTo("name2");
		assertThat(accessor.getPropertyValue("list[1].name")).isEqualTo("name3");
		assertThat(accessor.getPropertyValue("set[0].name")).isEqualTo("name6");
		assertThat(accessor.getPropertyValue("set[1].name")).isEqualTo("name7");
		assertThat(accessor.getPropertyValue("map[key1].name")).isEqualTo("name4");
		assertThat(accessor.getPropertyValue("map[key.3].name")).isEqualTo("name5");
		assertThat(accessor.getPropertyValue("map['key1'].name")).isEqualTo("name4");
		assertThat(accessor.getPropertyValue("map[\"key.3\"].name")).isEqualTo("name5");
		assertThat(accessor.getPropertyValue("map[key4][0].name")).isEqualTo("nameX");
		assertThat(accessor.getPropertyValue("map[key4][1].name")).isEqualTo("nameY");
		assertThat(accessor.getPropertyValue("map[key5[foo]].name")).isEqualTo("name8");
		assertThat(accessor.getPropertyValue("map['key5[foo]'].name")).isEqualTo("name8");
		assertThat(accessor.getPropertyValue("map[\"key5[foo]\"].name")).isEqualTo("name8");
		assertThat(accessor.getPropertyValue("myTestBeans[0].name")).isEqualTo("nameZ");

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
		pvs.add("map[key5[foo]].name", "name10");
		pvs.add("myTestBeans[0].name", "nameZZ");
		accessor.setPropertyValues(pvs);
		assertThat(tb0.getName()).isEqualTo("name5");
		assertThat(tb1.getName()).isEqualTo("name4");
		assertThat(tb2.getName()).isEqualTo("name3");
		assertThat(tb3.getName()).isEqualTo("name2");
		assertThat(tb4.getName()).isEqualTo("name1");
		assertThat(tb5.getName()).isEqualTo("name0");
		assertThat(accessor.getPropertyValue("array[0].name")).isEqualTo("name5");
		assertThat(accessor.getPropertyValue("array[1].name")).isEqualTo("name4");
		assertThat(accessor.getPropertyValue("list[0].name")).isEqualTo("name3");
		assertThat(accessor.getPropertyValue("list[1].name")).isEqualTo("name2");
		assertThat(accessor.getPropertyValue("set[0].name")).isEqualTo("name8");
		assertThat(accessor.getPropertyValue("set[1].name")).isEqualTo("name9");
		assertThat(accessor.getPropertyValue("map[\"key1\"].name")).isEqualTo("name1");
		assertThat(accessor.getPropertyValue("map['key.3'].name")).isEqualTo("name0");
		assertThat(accessor.getPropertyValue("map[key4][0].name")).isEqualTo("nameA");
		assertThat(accessor.getPropertyValue("map[key4][1].name")).isEqualTo("nameB");
		assertThat(accessor.getPropertyValue("map[key5[foo]].name")).isEqualTo("name10");
		assertThat(accessor.getPropertyValue("myTestBeans[0].name")).isEqualTo("nameZZ");
	}

	@Test
	void getAndSetIndexedPropertiesWithDirectAccess() {
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
		assertThat(accessor.getPropertyValue("array[0]")).isEqualTo(tb0);
		assertThat(accessor.getPropertyValue("array[1]")).isEqualTo(tb1);
		assertThat(accessor.getPropertyValue("list[0]")).isEqualTo(tb2);
		assertThat(accessor.getPropertyValue("list[1]")).isEqualTo(tb3);
		assertThat(accessor.getPropertyValue("set[0]")).isEqualTo(tb6);
		assertThat(accessor.getPropertyValue("set[1]")).isEqualTo(tb7);
		assertThat(accessor.getPropertyValue("map[key1]")).isEqualTo(tb4);
		assertThat(accessor.getPropertyValue("map[key2]")).isEqualTo(tb5);
		assertThat(accessor.getPropertyValue("map['key1']")).isEqualTo(tb4);
		assertThat(accessor.getPropertyValue("map[\"key2\"]")).isEqualTo(tb5);

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
		assertThat(target.getArray()[0]).isEqualTo(tb5);
		assertThat(target.getArray()[1]).isEqualTo(tb4);
		assertThat((target.getList().get(0))).isEqualTo(tb3);
		assertThat((target.getList().get(1))).isEqualTo(tb2);
		assertThat((target.getList().get(2))).isEqualTo(tb0);
		assertThat((target.getList().get(3))).isNull();
		assertThat((target.getList().get(4))).isEqualTo(tb1);
		assertThat((target.getMap().get("key1"))).isEqualTo(tb1);
		assertThat((target.getMap().get("key2"))).isEqualTo(tb0);
		assertThat((target.getMap().get("key5"))).isEqualTo(tb4);
		assertThat((target.getMap().get("key9"))).isEqualTo(tb5);
		assertThat(accessor.getPropertyValue("array[0]")).isEqualTo(tb5);
		assertThat(accessor.getPropertyValue("array[1]")).isEqualTo(tb4);
		assertThat(accessor.getPropertyValue("list[0]")).isEqualTo(tb3);
		assertThat(accessor.getPropertyValue("list[1]")).isEqualTo(tb2);
		assertThat(accessor.getPropertyValue("list[2]")).isEqualTo(tb0);
		assertThat(accessor.getPropertyValue("list[3]")).isNull();
		assertThat(accessor.getPropertyValue("list[4]")).isEqualTo(tb1);
		assertThat(accessor.getPropertyValue("map[\"key1\"]")).isEqualTo(tb1);
		assertThat(accessor.getPropertyValue("map['key2']")).isEqualTo(tb0);
		assertThat(accessor.getPropertyValue("map[\"key5\"]")).isEqualTo(tb4);
		assertThat(accessor.getPropertyValue("map['key9']")).isEqualTo(tb5);
	}

	@Test
	void propertyType() {
		Person target = createPerson("John", "Paris", "FR");
		AbstractPropertyAccessor accessor = createAccessor(target);

		assertThat(accessor.getPropertyType("address.city")).isEqualTo(String.class);
	}

	@Test
	void propertyTypeUnknownProperty() {
		Simple target = new Simple("John", 2);
		AbstractPropertyAccessor accessor = createAccessor(target);

		assertThat(accessor.getPropertyType("foo")).isNull();
	}

	@Test
	void propertyTypeDescriptor() {
		Person target = createPerson("John", "Paris", "FR");
		AbstractPropertyAccessor accessor = createAccessor(target);

		assertThat(accessor.getPropertyTypeDescriptor("address.city")).isNotNull();
	}

	@Test
	void propertyTypeDescriptorUnknownProperty() {
		Simple target = new Simple("John", 2);
		AbstractPropertyAccessor accessor = createAccessor(target);

		assertThat(accessor.getPropertyTypeDescriptor("foo")).isNull();
	}

	@Test
	void propertyTypeIndexedProperty() {
		IndexedTestBean target = new IndexedTestBean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		assertThat(accessor.getPropertyType("map[key0]")).isNull();

		accessor = createAccessor(target);
		accessor.setPropertyValue("map[key0]", "my String");
		assertThat(accessor.getPropertyType("map[key0]")).isEqualTo(String.class);

		accessor = createAccessor(target);
		accessor.registerCustomEditor(String.class, "map[key0]", new StringTrimmerEditor(false));
		assertThat(accessor.getPropertyType("map[key0]")).isEqualTo(String.class);
	}

	@Test
	void cornerSpr10115() {
		Spr10115Bean target = new Spr10115Bean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setPropertyValue("prop1", "val1");
		assertThat(Spr10115Bean.prop1).isEqualTo("val1");
	}

	@Test
	void cornerSpr13837() {
		Spr13837Bean target = new Spr13837Bean();
		AbstractPropertyAccessor accessor = createAccessor(target);
		accessor.setPropertyValue("something", 42);
		assertThat(target.something).isEqualTo(Integer.valueOf(42));
	}


	private Person createPerson(String name, String city, String country) {
		return new Person(name, new Address(city, country));
	}


	@SuppressWarnings("unused")
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

	@SuppressWarnings("unused")
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

	@SuppressWarnings("unused")
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

	@SuppressWarnings("unused")
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

	@SuppressWarnings({ "unused", "rawtypes" })
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
		// class to test naming of beans in an error message
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

		@SuppressWarnings("unchecked")
		public SkipReaderStub(T... items) {
			this.items = items;
		}

		@SuppressWarnings("unchecked")
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

	@SuppressWarnings("unchecked")
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
