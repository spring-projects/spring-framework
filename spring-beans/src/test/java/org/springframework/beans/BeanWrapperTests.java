/*
 * Copyright 2002-2021 the original author or authors.
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

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Specific {@link BeanWrapperImpl} tests.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Alef Arendsen
 * @author Arjen Poutsma
 * @author Chris Beams
 * @author Dave Syer
 */
class BeanWrapperTests extends AbstractPropertyAccessorTests {

	@Override
	protected BeanWrapperImpl createAccessor(Object target) {
		return new BeanWrapperImpl(target);
	}


	@Test
	void setterDoesNotCallGetter() {
		GetterBean target = new GetterBean();
		BeanWrapper accessor = createAccessor(target);
		accessor.setPropertyValue("name", "tom");
		assertThat(target.getAliasedName()).isEqualTo("tom");
		assertThat(accessor.getPropertyValue("aliasedName")).isEqualTo("tom");
	}

	@Test
	void getterSilentlyFailWithOldValueExtraction() {
		GetterBean target = new GetterBean();
		BeanWrapper accessor = createAccessor(target);
		accessor.setExtractOldValueForEditor(true); // This will call the getter
		accessor.setPropertyValue("name", "tom");
		assertThat(target.getAliasedName()).isEqualTo("tom");
		assertThat(accessor.getPropertyValue("aliasedName")).isEqualTo("tom");
	}

	@Test
	void aliasedSetterThroughDefaultMethod() {
		GetterBean target = new GetterBean();
		BeanWrapper accessor = createAccessor(target);
		accessor.setPropertyValue("aliasedName", "tom");
		assertThat(target.getAliasedName()).isEqualTo("tom");
		assertThat(accessor.getPropertyValue("aliasedName")).isEqualTo("tom");
	}

	@Test
	void setValidAndInvalidPropertyValuesShouldContainExceptionDetails() {
		TestBean target = new TestBean();
		String newName = "tony";
		String invalidTouchy = ".valid";
		BeanWrapper accessor = createAccessor(target);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue(new PropertyValue("age", "foobar"));
		pvs.addPropertyValue(new PropertyValue("name", newName));
		pvs.addPropertyValue(new PropertyValue("touchy", invalidTouchy));
		assertThatExceptionOfType(PropertyBatchUpdateException.class).isThrownBy(() ->
				accessor.setPropertyValues(pvs))
			.satisfies(ex -> {
				assertThat(ex.getExceptionCount()).isEqualTo(2);
				assertThat(ex.getPropertyAccessException("touchy").getPropertyChangeEvent()
						.getNewValue()).isEqualTo(invalidTouchy);
			});
		// Test validly set property matches
		assertThat(target.getName()).as("Valid set property must stick").isEqualTo(newName);
		assertThat(target.getAge()).as("Invalid set property must retain old value").isEqualTo(0);
	}

	@Test
	void checkNotWritablePropertyHoldPossibleMatches() {
		TestBean target = new TestBean();
		BeanWrapper accessor = createAccessor(target);
		assertThatExceptionOfType(NotWritablePropertyException.class).isThrownBy(() ->
				accessor.setPropertyValue("ag", "foobar"))
			.satisfies(ex -> assertThat(ex.getPossibleMatches()).containsExactly("age"));
	}

	@Test // Can't be shared; there is no such thing as a read-only field
	void setReadOnlyMapProperty() {
		TypedReadOnlyMap map = new TypedReadOnlyMap(Collections.singletonMap("key", new TestBean()));
		TypedReadOnlyMapClient target = new TypedReadOnlyMapClient();
		BeanWrapper accessor = createAccessor(target);
		assertThatNoException().isThrownBy(() -> accessor.setPropertyValue("map", map));
	}

	@Test
	void notWritablePropertyExceptionContainsAlternativeMatch() {
		IntelliBean target = new IntelliBean();
		BeanWrapper bw = createAccessor(target);
		try {
			bw.setPropertyValue("names", "Alef");
		}
		catch (NotWritablePropertyException ex) {
			assertThat(ex.getPossibleMatches()).as("Possible matches not determined").isNotNull();
			assertThat(ex.getPossibleMatches()).as("Invalid amount of alternatives").hasSize(1);
		}
	}

	@Test
	void notWritablePropertyExceptionContainsAlternativeMatches() {
		IntelliBean target = new IntelliBean();
		BeanWrapper bw = createAccessor(target);
		try {
			bw.setPropertyValue("mystring", "Arjen");
		}
		catch (NotWritablePropertyException ex) {
			assertThat(ex.getPossibleMatches()).as("Possible matches not determined").isNotNull();
			assertThat(ex.getPossibleMatches()).as("Invalid amount of alternatives").hasSize(3);
		}
	}

	@Override
	@Test  // Can't be shared: no type mismatch with a field
	void setPropertyTypeMismatch() {
		PropertyTypeMismatch target = new PropertyTypeMismatch();
		BeanWrapper accessor = createAccessor(target);
		accessor.setPropertyValue("object", "a String");
		assertThat(target.value).isEqualTo("a String");
		assertThat(target.getObject()).isEqualTo(8);
		assertThat(accessor.getPropertyValue("object")).isEqualTo(8);
	}

	@Test
	void propertyDescriptors() {
		TestBean target = new TestBean();
		target.setSpouse(new TestBean());
		BeanWrapper accessor = createAccessor(target);
		accessor.setPropertyValue("name", "a");
		accessor.setPropertyValue("spouse.name", "b");
		assertThat(target.getName()).isEqualTo("a");
		assertThat(target.getSpouse().getName()).isEqualTo("b");
		assertThat(accessor.getPropertyValue("name")).isEqualTo("a");
		assertThat(accessor.getPropertyValue("spouse.name")).isEqualTo("b");
		assertThat(accessor.getPropertyDescriptor("name").getPropertyType()).isEqualTo(String.class);
		assertThat(accessor.getPropertyDescriptor("spouse.name").getPropertyType()).isEqualTo(String.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	void getPropertyWithOptional() {
		GetterWithOptional target = new GetterWithOptional();
		TestBean tb = new TestBean("x");
		BeanWrapper accessor = createAccessor(target);

		accessor.setPropertyValue("object", tb);
		assertThat(target.value).isSameAs(tb);
		assertThat(target.getObject().get()).isSameAs(tb);
		assertThat(((Optional<TestBean>) accessor.getPropertyValue("object")).get()).isSameAs(tb);
		assertThat(target.value.getName()).isEqualTo("x");
		assertThat(target.getObject().get().getName()).isEqualTo("x");
		assertThat(accessor.getPropertyValue("object.name")).isEqualTo("x");

		accessor.setPropertyValue("object.name", "y");
		assertThat(target.value).isSameAs(tb);
		assertThat(target.getObject().get()).isSameAs(tb);
		assertThat(((Optional<TestBean>) accessor.getPropertyValue("object")).get()).isSameAs(tb);
		assertThat(target.value.getName()).isEqualTo("y");
		assertThat(target.getObject().get().getName()).isEqualTo("y");
		assertThat(accessor.getPropertyValue("object.name")).isEqualTo("y");
	}

	@Test
	void getPropertyWithOptionalAndAutoGrow() {
		GetterWithOptional target = new GetterWithOptional();
		BeanWrapper accessor = createAccessor(target);
		accessor.setAutoGrowNestedPaths(true);

		accessor.setPropertyValue("object.name", "x");
		assertThat(target.value.getName()).isEqualTo("x");
		assertThat(target.getObject().get().getName()).isEqualTo("x");
		assertThat(accessor.getPropertyValue("object.name")).isEqualTo("x");
	}

	@Test
	void incompletelyQuotedKeyLeadsToPropertyException() {
		TestBean target = new TestBean();
		BeanWrapper accessor = createAccessor(target);
		assertThatExceptionOfType(NotWritablePropertyException.class).isThrownBy(() ->
				accessor.setPropertyValue("[']", "foobar"))
			.satisfies(ex -> assertThat(ex.getPossibleMatches()).isNull());
	}


	private interface BaseProperty {

		default String getAliasedName() {
			return getName();
		}

		String getName();
	}


	@SuppressWarnings("unused")
	private interface AliasedProperty extends BaseProperty {

		default void setAliasedName(String name) {
			setName(name);
		}

		void setName(String name);
	}


	@SuppressWarnings("unused")
	private static class GetterBean implements AliasedProperty {

		private String name;

		@Override
		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			if (this.name == null) {
				throw new RuntimeException("name property must be set");
			}
			return name;
		}
	}


	@SuppressWarnings("unused")
	private static class IntelliBean {

		public void setName(String name) {
		}

		public void setMyString(String string) {
		}

		public void setMyStrings(String string) {
		}

		public void setMyStriNg(String string) {
		}

		public void setMyStringss(String string) {
		}
	}


	@SuppressWarnings("serial")
	public static class TypedReadOnlyMap extends ReadOnlyMap<String, TestBean> {

		public TypedReadOnlyMap() {
		}

		public TypedReadOnlyMap(Map<? extends String, ? extends TestBean> map) {
			super(map);
		}
	}


	public static class TypedReadOnlyMapClient {

		public void setMap(TypedReadOnlyMap map) {
		}
	}


	public static class PropertyTypeMismatch {

		public String value;

		public void setObject(String object) {
			this.value = object;
		}

		public Integer getObject() {
			return (this.value != null ? this.value.length() : null);
		}
	}


	public static class GetterWithOptional {

		public TestBean value;

		public void setObject(TestBean object) {
			this.value = object;
		}

		public Optional<TestBean> getObject() {
			return Optional.ofNullable(this.value);
		}
	}

}
