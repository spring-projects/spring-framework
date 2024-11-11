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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link MutablePropertyValues}.
 *
 * @author Rod Johnson
 * @author Chris Beams
 * @author Juergen Hoeller
 */
class MutablePropertyValuesTests {

	@Test
	void valid() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue(new PropertyValue("forname", "Tony"));
		pvs.addPropertyValue(new PropertyValue("surname", "Blair"));
		pvs.addPropertyValue(new PropertyValue("age", "50"));
		doTestTony(pvs);

		MutablePropertyValues deepCopy = new MutablePropertyValues(pvs);
		doTestTony(deepCopy);
		deepCopy.setPropertyValueAt(new PropertyValue("name", "Gordon"), 0);
		doTestTony(pvs);
		assertThat(deepCopy.getPropertyValue("name").getValue()).isEqualTo("Gordon");
	}

	@Test
	void addOrOverride() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue(new PropertyValue("forname", "Tony"));
		pvs.addPropertyValue(new PropertyValue("surname", "Blair"));
		pvs.addPropertyValue(new PropertyValue("age", "50"));
		doTestTony(pvs);
		PropertyValue addedPv = new PropertyValue("rod", "Rod");
		pvs.addPropertyValue(addedPv);
		assertThat(pvs.getPropertyValue("rod")).isEqualTo(addedPv);
		PropertyValue changedPv = new PropertyValue("forname", "Greg");
		pvs.addPropertyValue(changedPv);
		assertThat(pvs.getPropertyValue("forname")).isEqualTo(changedPv);
	}

	@Test
	void changesOnEquals() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue(new PropertyValue("forname", "Tony"));
		pvs.addPropertyValue(new PropertyValue("surname", "Blair"));
		pvs.addPropertyValue(new PropertyValue("age", "50"));
		MutablePropertyValues pvs2 = pvs;
		PropertyValues changes = pvs2.changesSince(pvs);
		assertThat(changes.getPropertyValues()).as("changes are empty").isEmpty();
	}

	@Test
	void changeOfOneField() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue(new PropertyValue("forname", "Tony"));
		pvs.addPropertyValue(new PropertyValue("surname", "Blair"));
		pvs.addPropertyValue(new PropertyValue("age", "50"));

		MutablePropertyValues pvs2 = new MutablePropertyValues(pvs);
		PropertyValues changes = pvs2.changesSince(pvs);
		assertThat(changes.getPropertyValues()).as("changes").isEmpty();

		pvs2.addPropertyValue(new PropertyValue("forname", "Gordon"));
		changes = pvs2.changesSince(pvs);
		assertThat(changes.getPropertyValues()).as("1 change").hasSize(1);
		PropertyValue fn = changes.getPropertyValue("forname");
		assertThat(fn).as("change is forname").isNotNull();
		assertThat(fn.getValue()).isEqualTo("Gordon");

		MutablePropertyValues pvs3 = new MutablePropertyValues(pvs);
		changes = pvs3.changesSince(pvs);
		assertThat(changes.getPropertyValues()).as("changes").isEmpty();

		// add new
		pvs3.addPropertyValue(new PropertyValue("foo", "bar"));
		pvs3.addPropertyValue(new PropertyValue("fi", "fum"));
		changes = pvs3.changesSince(pvs);
		assertThat(changes.getPropertyValues()).as("2 changes").hasSize(2);
		fn = changes.getPropertyValue("foo");
		assertThat(fn).as("change in foo").isNotNull();
		assertThat(fn.getValue()).isEqualTo("bar");
	}

	@Test
	void iteratorContainsPropertyValue() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("foo", "bar");

		Iterator<PropertyValue> it = pvs.iterator();
		assertThat(it.hasNext()).isTrue();
		PropertyValue pv = it.next();
		assertThat(pv.getName()).isEqualTo("foo");
		assertThat(pv.getValue()).isEqualTo("bar");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(it::remove);
		assertThat(it.hasNext()).isFalse();
	}

	@Test
	void iteratorIsEmptyForEmptyValues() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		Iterator<PropertyValue> it = pvs.iterator();
		assertThat(it.hasNext()).isFalse();
	}

	@Test
	void streamContainsPropertyValue() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("foo", "bar");

		assertThat(pvs.stream()).isNotNull();
		assertThat(pvs.stream()).hasSize(1);
		assertThat(pvs.stream()).anyMatch(pv -> "foo".equals(pv.getName()) && "bar".equals(pv.getValue()));
		assertThat(pvs.stream()).noneMatch(pv -> "bar".equals(pv.getName()) && "foo".equals(pv.getValue()));
	}

	@Test
	void streamIsEmptyForEmptyValues() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		assertThat(pvs.stream()).isNotNull();
		assertThat(pvs.stream()).isEmpty();
	}

	/**
	 * Must contain: forname=Tony surname=Blair age=50
	 */
	protected void doTestTony(PropertyValues pvs) {
		PropertyValue[] propertyValues = pvs.getPropertyValues();

		assertThat(propertyValues).hasSize(3);
		assertThat(pvs.contains("forname")).as("Contains forname").isTrue();
		assertThat(pvs.contains("surname")).as("Contains surname").isTrue();
		assertThat(pvs.contains("age")).as("Contains age").isTrue();
		assertThat(pvs.contains("tory")).as("Doesn't contain tory").isFalse();

		Map<String, String> map = new HashMap<>();
		map.put("forname", "Tony");
		map.put("surname", "Blair");
		map.put("age", "50");

		for (PropertyValue element : propertyValues) {
			Object val = map.get(element.getName());
			assertThat(val).as("Can't have unexpected value").isNotNull();
			assertThat(val).isInstanceOf(String.class);
			assertThat(val).isEqualTo(element.getValue());
			map.remove(element.getName());
		}
		assertThat(map).isEmpty();
	}

}
