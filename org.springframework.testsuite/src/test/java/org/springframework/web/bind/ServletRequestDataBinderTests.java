/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.web.bind;

import java.beans.PropertyEditorSupport;
import java.util.Arrays;

import org.springframework.beans.AbstractPropertyValuesTests;
import org.springframework.beans.ITestBean;
import org.springframework.beans.TestBean;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class ServletRequestDataBinderTests extends AbstractPropertyValuesTests {

	public void testBindingWithNestedObjectCreation() throws Exception {
		TestBean tb = new TestBean();

		ServletRequestDataBinder binder = new ServletRequestDataBinder(tb, "person");
		binder.registerCustomEditor(ITestBean.class, new PropertyEditorSupport() {
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(new TestBean());
			}
		});

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("spouse", "someValue");
		request.addParameter("spouse.name", "test");
		binder.bind(request);

		assertNotNull(tb.getSpouse());
		assertEquals("test", tb.getSpouse().getName());
	}

	public void testFieldPrefixCausesFieldReset() throws Exception {
		TestBean target = new TestBean();
		ServletRequestDataBinder binder = new ServletRequestDataBinder(target);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("_postProcessed", "visible");
		request.addParameter("postProcessed", "on");
		binder.bind(request);
		assertTrue(target.isPostProcessed());

		request.removeParameter("postProcessed");
		binder.bind(request);
		assertFalse(target.isPostProcessed());
	}

	public void testFieldPrefixCausesFieldResetWithIgnoreUnknownFields() throws Exception {
		TestBean target = new TestBean();
		ServletRequestDataBinder binder = new ServletRequestDataBinder(target);
		binder.setIgnoreUnknownFields(false);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("_postProcessed", "visible");
		request.addParameter("postProcessed", "on");
		binder.bind(request);
		assertTrue(target.isPostProcessed());

		request.removeParameter("postProcessed");
		binder.bind(request);
		assertFalse(target.isPostProcessed());
	}

	public void testWithCommaSeparatedStringArray() throws Exception {
		TestBean target = new TestBean();
		ServletRequestDataBinder binder = new ServletRequestDataBinder(target);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("stringArray", "bar");
		request.addParameter("stringArray", "abc");
		request.addParameter("stringArray", "123,def");
		binder.bind(request);
		assertEquals("Expected all three items to be bound", 3, target.getStringArray().length);

		request.removeParameter("stringArray");
		request.addParameter("stringArray", "123,def");
		binder.bind(request);
		assertEquals("Expected only 1 item to be bound", 1, target.getStringArray().length);
	}

	public void testBindingWithNestedObjectCreationAndWrongOrder() throws Exception {
		TestBean tb = new TestBean();

		ServletRequestDataBinder binder = new ServletRequestDataBinder(tb, "person");
		binder.registerCustomEditor(ITestBean.class, new PropertyEditorSupport() {
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(new TestBean());
			}
		});

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("spouse.name", "test");
		request.addParameter("spouse", "someValue");
		binder.bind(request);

		assertNotNull(tb.getSpouse());
		assertEquals("test", tb.getSpouse().getName());
	}

	public void testNoPrefix() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("forname", "Tony");
		request.addParameter("surname", "Blair");
		request.addParameter("age", "" + 50);

		ServletRequestParameterPropertyValues pvs = new ServletRequestParameterPropertyValues(request);
		doTestTony(pvs);
	}

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

	public void testNoParameters() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		ServletRequestParameterPropertyValues pvs = new ServletRequestParameterPropertyValues(request);
		assertTrue("Found no parameters", pvs.getPropertyValues().length == 0);
	}

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

}
