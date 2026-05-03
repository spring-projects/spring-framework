/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.bind.support;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.web.bind.ServletRequestParameterPropertyValues;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ServletRequestParameterPropertyValues}.
 */
class ServletRequestParameterPropertyValuesTests {

	private final MockHttpServletRequest request = new MockHttpServletRequest();


	@Test
	void noParameters() {
		var pvs = new ServletRequestParameterPropertyValues(request);
		assertThat(pvs.getPropertyValues()).as("Found no parameters").isEmpty();
	}

	@Test
	void multipleValuesForParameter() {
		String[] original = new String[] {"Tony", "Rod"};
		request.addParameter("forname", original);

		var pvs = new ServletRequestParameterPropertyValues(request);
		assertThat(pvs.getPropertyValues().length).as("Found 1 parameter").isEqualTo(1);
		assertThat(pvs.getPropertyValue("forname").getValue()).as("Found array value").isInstanceOf(String[].class);
		String[] values = (String[]) pvs.getPropertyValue("forname").getValue();
		assertThat(original).containsExactly(values);
	}

	@Test
	void noPrefix() {
		request.addParameter("forname", "Tony");
		request.addParameter("surname", "Blair");
		request.addParameter("age", "" + 50);

		testTony(new ServletRequestParameterPropertyValues(request));
	}

	@Test
	void prefix() {
		request.addParameter("test_forname", "Tony");
		request.addParameter("test_surname", "Blair");
		request.addParameter("test_age", "" + 50);

		var pvs = new ServletRequestParameterPropertyValues(request);
		assertThat(pvs.contains("forname")).as("Didn't find normal when given prefix").isFalse();
		assertThat(pvs.contains("test_forname")).as("Did treat prefix as normal when not given prefix").isTrue();

		testTony(new ServletRequestParameterPropertyValues(request, "test"));
	}

	/**
	 * Must contain: forname=Tony surname=Blair age=50
	 */
	private static void testTony(PropertyValues pvs) {
		assertThat(pvs.getPropertyValues().length).as("Contains 3").isEqualTo(3);
		assertThat(pvs.contains("forname")).as("Contains forname").isTrue();
		assertThat(pvs.contains("surname")).as("Contains surname").isTrue();
		assertThat(pvs.contains("age")).as("Contains age").isTrue();
		assertThat(pvs.contains("tory")).as("Doesn't contain tory").isFalse();

		Map<String, String> map = new HashMap<>();
		map.put("forname", "Tony");
		map.put("surname", "Blair");
		map.put("age", "50");
		for (PropertyValue pv : pvs.getPropertyValues()) {
			Object val = map.get(pv.getName());
			assertThat(val).as("Can't have unexpected value").isNotNull();
			assertThat(val).as("Val is string").isInstanceOf(String.class);
			assertThat(val).as("val matches expected").isEqualTo(pv.getValue());
			map.remove(pv.getName());
		}
		assertThat(map).isEmpty();
	}

}
