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
		assertThat(pvs.getPropertyValues()).as("Contains 3").hasSize(3);
		assertThat(pvs.contains("forname")).as("Contains forname").isTrue();
		assertThat(pvs.contains("surname")).as("Contains surname").isTrue();
		assertThat(pvs.contains("age")).as("Contains age").isTrue();
		assertThat(!pvs.contains("tory")).as("Doesn't contain tory").isTrue();

		PropertyValue[] ps = pvs.getPropertyValues();
		Map<String, String> m = new HashMap<>();
		m.put("forname", "Tony");
		m.put("surname", "Blair");
		m.put("age", "50");
		for (PropertyValue element : ps) {
			Object val = m.get(element.getName());
			assertThat(val).as("Can't have unexpected value").isNotNull();
			assertThat(val instanceof String).as("Val i string").isTrue();
			assertThat(val.equals(element.getValue())).as("val matches expected").isTrue();
			m.remove(element.getName());
		}
		assertThat(m).as("Map size is 0").isEmpty();
	}

}
