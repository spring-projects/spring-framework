/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.bind.support;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.beans.PropertyEditorSupport;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockMultipartFile;
import org.springframework.mock.web.test.MockMultipartHttpServletRequest;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.web.bind.ServletRequestParameterPropertyValues;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.support.StringMultipartFileEditor;

/**
 * @author Juergen Hoeller
 */
public class WebRequestDataBinderTests {

	@Test
	public void testBindingWithNestedObjectCreation() throws Exception {
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

		assertNotNull(tb.getSpouse());
		assertEquals("test", tb.getSpouse().getName());
	}

	@Test
	public void testBindingWithNestedObjectCreationThroughAutoGrow() throws Exception {
		TestBean tb = new TestBeanWithConcreteSpouse();

		WebRequestDataBinder binder = new WebRequestDataBinder(tb, "person");
		binder.setIgnoreUnknownFields(false);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("concreteSpouse.name", "test");
		binder.bind(new ServletWebRequest(request));

		assertNotNull(tb.getSpouse());
		assertEquals("test", tb.getSpouse().getName());
	}

	@Test
	public void testFieldPrefixCausesFieldReset() throws Exception {
		TestBean target = new TestBean();
		WebRequestDataBinder binder = new WebRequestDataBinder(target);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("_postProcessed", "visible");
		request.addParameter("postProcessed", "on");
		binder.bind(new ServletWebRequest(request));
		assertTrue(target.isPostProcessed());

		request.removeParameter("postProcessed");
		binder.bind(new ServletWebRequest(request));
		assertFalse(target.isPostProcessed());
	}

	@Test
	public void testFieldPrefixCausesFieldResetWithIgnoreUnknownFields() throws Exception {
		TestBean target = new TestBean();
		WebRequestDataBinder binder = new WebRequestDataBinder(target);
		binder.setIgnoreUnknownFields(false);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("_postProcessed", "visible");
		request.addParameter("postProcessed", "on");
		binder.bind(new ServletWebRequest(request));
		assertTrue(target.isPostProcessed());

		request.removeParameter("postProcessed");
		binder.bind(new ServletWebRequest(request));
		assertFalse(target.isPostProcessed());
	}

	@Test
	public void testFieldDefault() throws Exception {
		TestBean target = new TestBean();
		WebRequestDataBinder binder = new WebRequestDataBinder(target);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("!postProcessed", "off");
		request.addParameter("postProcessed", "on");
		binder.bind(new ServletWebRequest(request));
		assertTrue(target.isPostProcessed());

		request.removeParameter("postProcessed");
		binder.bind(new ServletWebRequest(request));
		assertFalse(target.isPostProcessed());
	}

	// SPR-13502
	@Test
	public void testCollectionFieldsDefault() throws Exception {
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
		assertThat(target.getSomeSet(), notNullValue());
		assertThat(target.getSomeSet(), isA(Set.class));

		assertThat(target.getSomeList(), notNullValue());
		assertThat(target.getSomeList(), isA(List.class));

		assertThat(target.getSomeMap(), notNullValue());
		assertThat(target.getSomeMap(), isA(Map.class));
	}

	@Test
	public void testFieldDefaultPreemptsFieldMarker() throws Exception {
		TestBean target = new TestBean();
		WebRequestDataBinder binder = new WebRequestDataBinder(target);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("!postProcessed", "on");
		request.addParameter("_postProcessed", "visible");
		request.addParameter("postProcessed", "on");
		binder.bind(new ServletWebRequest(request));
		assertTrue(target.isPostProcessed());

		request.removeParameter("postProcessed");
		binder.bind(new ServletWebRequest(request));
		assertTrue(target.isPostProcessed());

		request.removeParameter("!postProcessed");
		binder.bind(new ServletWebRequest(request));
		assertFalse(target.isPostProcessed());
	}

	@Test
	public void testFieldDefaultWithNestedProperty() throws Exception {
		TestBean target = new TestBean();
		target.setSpouse(new TestBean());
		WebRequestDataBinder binder = new WebRequestDataBinder(target);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("!spouse.postProcessed", "on");
		request.addParameter("_spouse.postProcessed", "visible");
		request.addParameter("spouse.postProcessed", "on");
		binder.bind(new ServletWebRequest(request));
		assertTrue(((TestBean) target.getSpouse()).isPostProcessed());

		request.removeParameter("spouse.postProcessed");
		binder.bind(new ServletWebRequest(request));
		assertTrue(((TestBean) target.getSpouse()).isPostProcessed());

		request.removeParameter("!spouse.postProcessed");
		binder.bind(new ServletWebRequest(request));
		assertFalse(((TestBean) target.getSpouse()).isPostProcessed());
	}

	@Test
	public void testFieldDefaultNonBoolean() throws Exception {
		TestBean target = new TestBean();
		WebRequestDataBinder binder = new WebRequestDataBinder(target);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("!name", "anonymous");
		request.addParameter("name", "Scott");
		binder.bind(new ServletWebRequest(request));
		assertEquals("Scott", target.getName());

		request.removeParameter("name");
		binder.bind(new ServletWebRequest(request));
		assertEquals("anonymous", target.getName());
	}

	@Test
	public void testWithCommaSeparatedStringArray() throws Exception {
		TestBean target = new TestBean();
		WebRequestDataBinder binder = new WebRequestDataBinder(target);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("stringArray", "bar");
		request.addParameter("stringArray", "abc");
		request.addParameter("stringArray", "123,def");
		binder.bind(new ServletWebRequest(request));
		assertEquals("Expected all three items to be bound", 3, target.getStringArray().length);

		request.removeParameter("stringArray");
		request.addParameter("stringArray", "123,def");
		binder.bind(new ServletWebRequest(request));
		assertEquals("Expected only 1 item to be bound", 1, target.getStringArray().length);
	}

	@Test
	public void testEnumBinding() {
		EnumHolder target = new EnumHolder();
		WebRequestDataBinder binder = new WebRequestDataBinder(target);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("myEnum", "FOO");
		binder.bind(new ServletWebRequest(request));
		assertEquals(MyEnum.FOO, target.getMyEnum());
	}

	@Test
	public void testMultipartFileAsString() {
		TestBean target = new TestBean();
		WebRequestDataBinder binder = new WebRequestDataBinder(target);
		binder.registerCustomEditor(String.class, new StringMultipartFileEditor());

		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.addFile(new MockMultipartFile("name", "Juergen".getBytes()));
		binder.bind(new ServletWebRequest(request));
		assertEquals("Juergen", target.getName());
	}

	@Test
	public void testMultipartFileAsStringArray() {
		TestBean target = new TestBean();
		WebRequestDataBinder binder = new WebRequestDataBinder(target);
		binder.registerCustomEditor(String.class, new StringMultipartFileEditor());

		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.addFile(new MockMultipartFile("stringArray", "Juergen".getBytes()));
		binder.bind(new ServletWebRequest(request));
		assertEquals(1, target.getStringArray().length);
		assertEquals("Juergen", target.getStringArray()[0]);
	}

	@Test
	public void testMultipartFilesAsStringArray() {
		TestBean target = new TestBean();
		WebRequestDataBinder binder = new WebRequestDataBinder(target);
		binder.registerCustomEditor(String.class, new StringMultipartFileEditor());

		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.addFile(new MockMultipartFile("stringArray", "Juergen".getBytes()));
		request.addFile(new MockMultipartFile("stringArray", "Eva".getBytes()));
		binder.bind(new ServletWebRequest(request));
		assertEquals(2, target.getStringArray().length);
		assertEquals("Juergen", target.getStringArray()[0]);
		assertEquals("Eva", target.getStringArray()[1]);
	}

	@Test
	public void testNoPrefix() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("forname", "Tony");
		request.addParameter("surname", "Blair");
		request.addParameter("age", "" + 50);

		ServletRequestParameterPropertyValues pvs = new ServletRequestParameterPropertyValues(request);
		doTestTony(pvs);
	}

	@Test
	public void testPrefix() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("test_forname", "Tony");
		request.addParameter("test_surname", "Blair");
		request.addParameter("test_age", "" + 50);

		ServletRequestParameterPropertyValues pvs = new ServletRequestParameterPropertyValues(request);
		assertTrue("Didn't fidn normal when given prefix", !pvs.contains("forname"));
		assertTrue("Did treat prefix as normal when not given prefix", pvs.contains("test_forname"));

		pvs = new ServletRequestParameterPropertyValues(request, "test");
		doTestTony(pvs);
	}

	/**
	 * Must contain: forname=Tony surname=Blair age=50
	 */
	protected void doTestTony(PropertyValues pvs) throws Exception {
		assertTrue("Contains 3", pvs.getPropertyValues().length == 3);
		assertTrue("Contains forname", pvs.contains("forname"));
		assertTrue("Contains surname", pvs.contains("surname"));
		assertTrue("Contains age", pvs.contains("age"));
		assertTrue("Doesn't contain tory", !pvs.contains("tory"));

		PropertyValue[] pvArray = pvs.getPropertyValues();
		Map<String, String> m = new HashMap<String, String>();
		m.put("forname", "Tony");
		m.put("surname", "Blair");
		m.put("age", "50");
		for (PropertyValue pv : pvArray) {
			Object val = m.get(pv.getName());
			assertTrue("Can't have unexpected value", val != null);
			assertTrue("Val i string", val instanceof String);
			assertTrue("val matches expected", val.equals(pv.getValue()));
			m.remove(pv.getName());
		}
		assertTrue("Map size is 0", m.size() == 0);
	}

	@Test
	public void testNoParameters() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		ServletRequestParameterPropertyValues pvs = new ServletRequestParameterPropertyValues(request);
		assertTrue("Found no parameters", pvs.getPropertyValues().length == 0);
	}

	@Test
	public void testMultipleValuesForParameter() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		String[] original = new String[] {"Tony", "Rod"};
		request.addParameter("forname", original);

		ServletRequestParameterPropertyValues pvs = new ServletRequestParameterPropertyValues(request);
		assertTrue("Found 1 parameter", pvs.getPropertyValues().length == 1);
		assertTrue("Found array value", pvs.getPropertyValue("forname").getValue() instanceof String[]);
		String[] values = (String[]) pvs.getPropertyValue("forname").getValue();
		assertEquals("Correct values", Arrays.asList(values), Arrays.asList(original));
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
