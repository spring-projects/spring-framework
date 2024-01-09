/*
 * Copyright 2002-2024 the original author or authors.
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

import java.beans.PropertyEditorSupport;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.web.bind.ServletRequestParameterPropertyValues;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.support.StringMultipartFileEditor;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockMultipartFile;
import org.springframework.web.testfixture.servlet.MockMultipartHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 */
class WebRequestDataBinderTests {

	@Test
	void testBindingWithNestedObjectCreation() {
		TestBean tb = new TestBean();

		WebRequestDataBinder binder = new WebRequestDataBinder(tb, "person");
		binder.registerCustomEditor(ITestBean.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(new TestBean());
			}
		});

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("spouse", "someValue");
		request.addParameter("spouse.name", "test");
		binder.bind(new ServletWebRequest(request));

		assertThat(tb.getSpouse()).isNotNull();
		assertThat(tb.getSpouse().getName()).isEqualTo("test");
	}

	@Test
	void testBindingWithNestedObjectCreationThroughAutoGrow() {
		TestBean tb = new TestBeanWithConcreteSpouse();

		WebRequestDataBinder binder = new WebRequestDataBinder(tb, "person");
		binder.setIgnoreUnknownFields(false);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("concreteSpouse.name", "test");
		binder.bind(new ServletWebRequest(request));

		assertThat(tb.getSpouse()).isNotNull();
		assertThat(tb.getSpouse().getName()).isEqualTo("test");
	}

	@Test
	void testFieldPrefixCausesFieldReset() {
		TestBean target = new TestBean();
		WebRequestDataBinder binder = new WebRequestDataBinder(target);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("_postProcessed", "visible");
		request.addParameter("postProcessed", "on");
		binder.bind(new ServletWebRequest(request));
		assertThat(target.isPostProcessed()).isTrue();

		request.removeParameter("postProcessed");
		binder.bind(new ServletWebRequest(request));
		assertThat(target.isPostProcessed()).isFalse();
	}

	@Test
	void testFieldPrefixCausesFieldResetWithIgnoreUnknownFields() {
		TestBean target = new TestBean();
		WebRequestDataBinder binder = new WebRequestDataBinder(target);
		binder.setIgnoreUnknownFields(false);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("_postProcessed", "visible");
		request.addParameter("postProcessed", "on");
		binder.bind(new ServletWebRequest(request));
		assertThat(target.isPostProcessed()).isTrue();

		request.removeParameter("postProcessed");
		binder.bind(new ServletWebRequest(request));
		assertThat(target.isPostProcessed()).isFalse();
	}

	@Test // gh-25836
	public void testFieldWithEmptyArrayIndex() {
		TestBean target = new TestBean();
		WebRequestDataBinder binder = new WebRequestDataBinder(target);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("stringArray[]", "ONE");
		request.addParameter("stringArray[]", "TWO");
		binder.bind(new ServletWebRequest(request));
		assertThat(target.getStringArray()).containsExactly("ONE", "TWO");
	}

	@Test
	void testFieldDefault() {
		TestBean target = new TestBean();
		WebRequestDataBinder binder = new WebRequestDataBinder(target);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("!postProcessed", "off");
		request.addParameter("postProcessed", "on");
		binder.bind(new ServletWebRequest(request));
		assertThat(target.isPostProcessed()).isTrue();

		request.removeParameter("postProcessed");
		binder.bind(new ServletWebRequest(request));
		assertThat(target.isPostProcessed()).isFalse();
	}

	// SPR-13502
	@Test
	void testCollectionFieldsDefault() {
		TestBean target = new TestBean();
		target.setSomeSet(null);
		target.setSomeList(null);
		target.setSomeMap(null);
		WebRequestDataBinder binder = new WebRequestDataBinder(target);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("_someSet", "visible");
		request.addParameter("_someList", "visible");
		request.addParameter("_someMap", "visible");

		binder.bind(new ServletWebRequest(request));
		assertThat(target.getSomeSet()).isInstanceOf(Set.class);
		assertThat(target.getSomeList()).isInstanceOf(List.class);
		assertThat(target.getSomeMap()).isInstanceOf(Map.class);
	}

	@Test
	void testFieldDefaultPreemptsFieldMarker() {
		TestBean target = new TestBean();
		WebRequestDataBinder binder = new WebRequestDataBinder(target);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("!postProcessed", "on");
		request.addParameter("_postProcessed", "visible");
		request.addParameter("postProcessed", "on");
		binder.bind(new ServletWebRequest(request));
		assertThat(target.isPostProcessed()).isTrue();

		request.removeParameter("postProcessed");
		binder.bind(new ServletWebRequest(request));
		assertThat(target.isPostProcessed()).isTrue();

		request.removeParameter("!postProcessed");
		binder.bind(new ServletWebRequest(request));
		assertThat(target.isPostProcessed()).isFalse();
	}

	@Test
	void testFieldDefaultWithNestedProperty() {
		TestBean target = new TestBean();
		target.setSpouse(new TestBean());
		WebRequestDataBinder binder = new WebRequestDataBinder(target);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("!spouse.postProcessed", "on");
		request.addParameter("_spouse.postProcessed", "visible");
		request.addParameter("spouse.postProcessed", "on");
		binder.bind(new ServletWebRequest(request));
		assertThat(((TestBean) target.getSpouse()).isPostProcessed()).isTrue();

		request.removeParameter("spouse.postProcessed");
		binder.bind(new ServletWebRequest(request));
		assertThat(((TestBean) target.getSpouse()).isPostProcessed()).isTrue();

		request.removeParameter("!spouse.postProcessed");
		binder.bind(new ServletWebRequest(request));
		assertThat(((TestBean) target.getSpouse()).isPostProcessed()).isFalse();
	}

	@Test
	void testFieldDefaultNonBoolean() {
		TestBean target = new TestBean();
		WebRequestDataBinder binder = new WebRequestDataBinder(target);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("!name", "anonymous");
		request.addParameter("name", "Scott");
		binder.bind(new ServletWebRequest(request));
		assertThat(target.getName()).isEqualTo("Scott");

		request.removeParameter("name");
		binder.bind(new ServletWebRequest(request));
		assertThat(target.getName()).isEqualTo("anonymous");
	}

	@Test
	void testWithCommaSeparatedStringArray() {
		TestBean target = new TestBean();
		WebRequestDataBinder binder = new WebRequestDataBinder(target);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("stringArray", "bar");
		request.addParameter("stringArray", "abc");
		request.addParameter("stringArray", "123,def");
		binder.bind(new ServletWebRequest(request));
		assertThat(target.getStringArray().length).as("Expected all three items to be bound").isEqualTo(3);

		request.removeParameter("stringArray");
		request.addParameter("stringArray", "123,def");
		binder.bind(new ServletWebRequest(request));
		assertThat(target.getStringArray().length).as("Expected only 1 item to be bound").isEqualTo(1);
	}

	@Test
	void testEnumBinding() {
		EnumHolder target = new EnumHolder();
		WebRequestDataBinder binder = new WebRequestDataBinder(target);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("myEnum", "FOO");
		binder.bind(new ServletWebRequest(request));
		assertThat(target.getMyEnum()).isEqualTo(MyEnum.FOO);
	}

	@Test
	void testMultipartFileAsString() {
		TestBean target = new TestBean();
		WebRequestDataBinder binder = new WebRequestDataBinder(target);
		binder.registerCustomEditor(String.class, new StringMultipartFileEditor());

		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.addFile(new MockMultipartFile("name", "Juergen".getBytes()));
		binder.bind(new ServletWebRequest(request));
		assertThat(target.getName()).isEqualTo("Juergen");
	}

	@Test
	void testMultipartFileAsStringArray() {
		TestBean target = new TestBean();
		WebRequestDataBinder binder = new WebRequestDataBinder(target);
		binder.registerCustomEditor(String.class, new StringMultipartFileEditor());

		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.addFile(new MockMultipartFile("stringArray", "Juergen".getBytes()));
		binder.bind(new ServletWebRequest(request));
		assertThat(target.getStringArray()).hasSize(1);
		assertThat(target.getStringArray()[0]).isEqualTo("Juergen");
	}

	@Test
	void testMultipartFilesAsStringArray() {
		TestBean target = new TestBean();
		WebRequestDataBinder binder = new WebRequestDataBinder(target);
		binder.registerCustomEditor(String.class, new StringMultipartFileEditor());

		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.addFile(new MockMultipartFile("stringArray", "Juergen".getBytes()));
		request.addFile(new MockMultipartFile("stringArray", "Eva".getBytes()));
		binder.bind(new ServletWebRequest(request));
		assertThat(target.getStringArray()).hasSize(2);
		assertThat(target.getStringArray()[0]).isEqualTo("Juergen");
		assertThat(target.getStringArray()[1]).isEqualTo("Eva");
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

		PropertyValue[] pvArray = pvs.getPropertyValues();
		Map<String, String> m = new HashMap<>();
		m.put("forname", "Tony");
		m.put("surname", "Blair");
		m.put("age", "50");
		for (PropertyValue pv : pvArray) {
			Object val = m.get(pv.getName());
			assertThat(val).as("Can't have unexpected value").isNotNull();
			boolean condition = val instanceof String;
			assertThat(condition).as("Val i string").isTrue();
			assertThat(val.equals(pv.getValue())).as("val matches expected").isTrue();
			m.remove(pv.getName());
		}
		assertThat(m.size()).as("Map size is 0").isEqualTo(0);
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


	public static class EnumHolder {

		private MyEnum myEnum;

		public MyEnum getMyEnum() {
			return myEnum;
		}

		public void setMyEnum(MyEnum myEnum) {
			this.myEnum = myEnum;
		}
	}

	public enum MyEnum {
		FOO, BAR
	}

	static class TestBeanWithConcreteSpouse extends TestBean {
		public void setConcreteSpouse(TestBean spouse) {
			setSpouse(spouse);
		}

		public TestBean getConcreteSpouse() {
			return (TestBean) getSpouse();
		}
	}


}
