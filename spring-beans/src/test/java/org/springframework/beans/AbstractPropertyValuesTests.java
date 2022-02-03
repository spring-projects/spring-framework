/*
 * Copyright 2002-2019 the original author or authors.
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public abstract class AbstractPropertyValuesTests {

	/**
	 * Must contain: forname=Tony surname=Blair age=50
	 */
	protected void doTestTony(PropertyValues pvs) {
		assertThat(pvs.getPropertyValues().length == 3).as("Contains 3").isTrue();
		assertThat(pvs.contains("forname")).as("Contains forname").isTrue();
		assertThat(pvs.contains("surname")).as("Contains surname").isTrue();
		assertThat(pvs.contains("age")).as("Contains age").isTrue();
		boolean condition1 = !pvs.contains("tory");
		assertThat(condition1).as("Doesn't contain tory").isTrue();

		PropertyValue[] ps = pvs.getPropertyValues();
		Map<String, String> m = new HashMap<>();
		m.put("forname", "Tony");
		m.put("surname", "Blair");
		m.put("age", "50");
		for (PropertyValue element : ps) {
			Object val = m.get(element.getName());
			assertThat(val != null).as("Can't have unexpected value").isTrue();
			boolean condition = val instanceof String;
			assertThat(condition).as("Val i string").isTrue();
			assertThat(val.equals(element.getValue())).as("val matches expected").isTrue();
			m.remove(element.getName());
		}
		assertThat(m.size() == 0).as("Map size is 0").isTrue();
	}

}
