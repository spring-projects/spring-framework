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

package org.springframework.web.bind;

import java.beans.PropertyEditorSupport;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Scott Andrews
 */
class ServletRequestDataBinderTests {

	@Test
	void testBindingWithNestedObjectCreation() {
		TestBean tb = new TestBean();

		ServletRequestDataBinder binder = new ServletRequestDataBinder(tb, "person");
		binder.registerCustomEditor(ITestBean.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(new TestBean());
			}
		});

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("spouse", "someValue");
		request.addParameter("spouse.name", "test");
		binder.bind(request);

		assertThat(tb.getSpouse()).isNotNull();
		assertThat(tb.getSpouse().getName()).isEqualTo("test");
	}

	@Test
	void testFieldPrefixCausesFieldReset() {
		TestBean target = new TestBean();
		ServletRequestDataBinder binder = new ServletRequestDataBinder(target);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("_postProcessed", "visible");
		request.addParameter("postProcessed", "on");
		binder.bind(request);
		assertThat(target.isPostProcessed()).isTrue();

		request.removeParameter("postProcessed");
		binder.bind(request);
		assertThat(target.isPostProcessed()).isFalse();
	}

	@Test
	void testFieldPrefixCausesFieldResetWithIgnoreUnknownFields() {
		TestBean target = new TestBean();
		ServletRequestDataBinder binder = new ServletRequestDataBinder(target);
		binder.setIgnoreUnknownFields(false);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("_postProcessed", "visible");
		request.addParameter("postProcessed", "on");
		binder.bind(request);
		assertThat(target.isPostProcessed()).isTrue();

		request.removeParameter("postProcessed");
		binder.bind(request);
		assertThat(target.isPostProcessed()).isFalse();
	}

	@Test
	void testFieldDefault() {
		TestBean target = new TestBean();
		ServletRequestDataBinder binder = new ServletRequestDataBinder(target);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("!postProcessed", "off");
		request.addParameter("postProcessed", "on");
		binder.bind(request);
		assertThat(target.isPostProcessed()).isTrue();

		request.removeParameter("postProcessed");
		binder.bind(request);
		assertThat(target.isPostProcessed()).isFalse();
	}

	@Test
	void testFieldDefaultPreemptsFieldMarker() {
		TestBean target = new TestBean();
		ServletRequestDataBinder binder = new ServletRequestDataBinder(target);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("!postProcessed", "on");
		request.addParameter("_postProcessed", "visible");
		request.addParameter("postProcessed", "on");
		binder.bind(request);
		assertThat(target.isPostProcessed()).isTrue();

		request.removeParameter("postProcessed");
		binder.bind(request);
		assertThat(target.isPostProcessed()).isTrue();

		request.removeParameter("!postProcessed");
		binder.bind(request);
		assertThat(target.isPostProcessed()).isFalse();
	}

	@Test
	void testFieldDefaultNonBoolean() {
		TestBean target = new TestBean();
		ServletRequestDataBinder binder = new ServletRequestDataBinder(target);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("!name", "anonymous");
		request.addParameter("name", "Scott");
		binder.bind(request);
		assertThat(target.getName()).isEqualTo("Scott");

		request.removeParameter("name");
		binder.bind(request);
		assertThat(target.getName()).isEqualTo("anonymous");
	}

	@Test
	void testWithCommaSeparatedStringArray() {
		TestBean target = new TestBean();
		ServletRequestDataBinder binder = new ServletRequestDataBinder(target);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("stringArray", "bar");
		request.addParameter("stringArray", "abc");
		request.addParameter("stringArray", "123,def");
		binder.bind(request);
		assertThat(target.getStringArray().length).as("Expected all three items to be bound").isEqualTo(3);

		request.removeParameter("stringArray");
		request.addParameter("stringArray", "123,def");
		binder.bind(request);
		assertThat(target.getStringArray().length).as("Expected only 1 item to be bound").isEqualTo(1);
	}

	@Test
	void testBindingWithNestedObjectCreationAndWrongOrder() {
		TestBean tb = new TestBean();

		ServletRequestDataBinder binder = new ServletRequestDataBinder(tb, "person");
		binder.registerCustomEditor(ITestBean.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(new TestBean());
			}
		});

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("spouse.name", "test");
		request.addParameter("spouse", "someValue");
		binder.bind(request);

		assertThat(tb.getSpouse()).isNotNull();
		assertThat(tb.getSpouse().getName()).isEqualTo("test");
	}

	@Test
	void testNoPrefix() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("forname", "Tony");
		request.addParameter("surname", "Blair");
		request.addParameter("age", "" + 50);

		ServletRequestParameterPropertyValues pvs = new ServletRequestParameterPropertyValues(request);
		doTestTony(pvs);
	}

	@Test
	void testPrefix() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("test_forname", "Tony");
		request.addParameter("test_surname", "Blair");
		request.addParameter("test_age", "" + 50);

		ServletRequestParameterPropertyValues pvs = new ServletRequestParameterPropertyValues(request);
		boolean condition = !pvs.contains("forname");
		assertThat(condition).as("Didn't find normal when given prefix").isTrue();
		assertThat(pvs.contains("test_forname")).as("Did treat prefix as normal when not given prefix").isTrue();

		pvs = new ServletRequestParameterPropertyValues(request, "test");
		doTestTony(pvs);
	}

	@Test
	void testNoParameters() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		ServletRequestParameterPropertyValues pvs = new ServletRequestParameterPropertyValues(request);
		assertThat(pvs.getPropertyValues().length).as("Found no parameters").isEqualTo(0);
	}

	@Test
	void testMultipleValuesForParameter() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		String[] original = new String[] {"Tony", "Rod"};
		request.addParameter("forname", original);

		ServletRequestParameterPropertyValues pvs = new ServletRequestParameterPropertyValues(request);
		assertThat(pvs.getPropertyValues().length).as("Found 1 parameter").isEqualTo(1);
		boolean condition = pvs.getPropertyValue("forname").getValue() instanceof String[];
		assertThat(condition).as("Found array value").isTrue();
		String[] values = (String[]) pvs.getPropertyValue("forname").getValue();
		assertThat(Arrays.asList(original)).as("Correct values").isEqualTo(Arrays.asList(values));
	}

	/**
	 * Must contain: forname=Tony surname=Blair age=50
	 */
	protected void doTestTony(PropertyValues pvs) {
		assertThat(pvs.getPropertyValues().length).as("Contains 3").isEqualTo(3);
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
			assertThat(val).as("Can't have unexpected value").isNotNull();
			boolean condition = val instanceof String;
			assertThat(condition).as("Val i string").isTrue();
			assertThat(val.equals(element.getValue())).as("val matches expected").isTrue();
			m.remove(element.getName());
		}
		assertThat(m.size()).as("Map size is 0").isEqualTo(0);
	}

}
