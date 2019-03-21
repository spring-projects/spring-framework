/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.portlet.bind;

import java.beans.PropertyEditorSupport;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;

import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.mock.web.portlet.MockPortletRequest;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.validation.BindingResult;

import static org.junit.Assert.*;

/**
 * @author Mark Fisher
 */
public class PortletRequestDataBinderTests {

	private final MockPortletRequest request = new MockPortletRequest();

	private final TestBean bean = new TestBean();

	@Test
	public void simpleBind() {

		request.addParameter("age", "35");
		request.addParameter("name", "test");

		PortletRequestDataBinder binder = new PortletRequestDataBinder(bean);
		binder.bind(request);

		assertEquals(35, bean.getAge());
		assertEquals("test", bean.getName());
	}

	@Test
	public void nestedBind() {
		bean.setSpouse(new TestBean());

		request.addParameter("spouse.name", "test");

		PortletRequestDataBinder binder = new PortletRequestDataBinder(bean);
		binder.bind(request);

		assertNotNull(bean.getSpouse());
		assertEquals("test", bean.getSpouse().getName());
	}

	@Test
	public void nestedBindWithPropertyEditor() {
		PortletRequestDataBinder binder = new PortletRequestDataBinder(bean);
		binder.registerCustomEditor(ITestBean.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(new TestBean(text));
			}
		});

		request.addParameter("spouse", "test");
		request.addParameter("spouse.age", "32");
		binder.bind(request);

		assertNotNull(bean.getSpouse());
		assertEquals("test", bean.getSpouse().getName());
		assertEquals(32, bean.getSpouse().getAge());
	}

	@Test
	public void bindingMismatch() {
		bean.setAge(30);

		request.addParameter("age", "zzz");

		PortletRequestDataBinder binder = new PortletRequestDataBinder(bean);
		binder.bind(request);

		BindingResult error = binder.getBindingResult();
		assertNotNull(error.getFieldError("age"));
		assertEquals("typeMismatch", error.getFieldError("age").getCode());
		assertEquals(30, bean.getAge());
	}

	@Test
	public void bindingStringWithCommaSeparatedValue() throws Exception {
		request.addParameter("stringArray", "test1,test2");

		PortletRequestDataBinder binder = new PortletRequestDataBinder(bean);
		binder.bind(request);

		assertNotNull(bean.getStringArray());
		assertEquals(1, bean.getStringArray().length);
		assertEquals("test1,test2", bean.getStringArray()[0]);
	}

	@Test
	public void bindingStringArrayWithSplitting() {
		request.addParameter("stringArray", "test1,test2");

		PortletRequestDataBinder binder = new PortletRequestDataBinder(bean);
		binder.registerCustomEditor(String[].class, new StringArrayPropertyEditor());
		binder.bind(request);

		assertNotNull(bean.getStringArray());
		assertEquals(2, bean.getStringArray().length);
		assertEquals("test1", bean.getStringArray()[0]);
		assertEquals("test2", bean.getStringArray()[1]);
	}

	@Test
	public void bindingList() {
		request.addParameter("someList[0]", "test1");
		request.addParameter("someList[1]", "test2");

		PortletRequestDataBinder binder = new PortletRequestDataBinder(bean);
		binder.bind(request);

		assertNotNull(bean.getSomeList());
		assertEquals(2, bean.getSomeList().size());
		assertEquals("test1", bean.getSomeList().get(0));
		assertEquals("test2", bean.getSomeList().get(1));
	}

	@Test
	public void bindingMap() {
		request.addParameter("someMap['key1']", "val1");
		request.addParameter("someMap['key2']", "val2");

		PortletRequestDataBinder binder = new PortletRequestDataBinder(bean);
		binder.bind(request);

		assertNotNull(bean.getSomeMap());
		assertEquals(2, bean.getSomeMap().size());
		assertEquals("val1", bean.getSomeMap().get("key1"));
		assertEquals("val2", bean.getSomeMap().get("key2"));
	}

	@Test
	public void bindingSet() {
		Set<TestBean> set = new LinkedHashSet<TestBean>(2);
		set.add(new TestBean("test1"));
		set.add(new TestBean("test2"));
		bean.setSomeSet(set);

		request.addParameter("someSet[0].age", "35");
		request.addParameter("someSet[1].age", "36");

		PortletRequestDataBinder binder = new PortletRequestDataBinder(bean);
		binder.bind(request);

		assertNotNull(bean.getSomeSet());
		assertEquals(2, bean.getSomeSet().size());

		Iterator<?> iter = bean.getSomeSet().iterator();

		TestBean bean1 = (TestBean) iter.next();
		assertEquals("test1", bean1.getName());
		assertEquals(35, bean1.getAge());

		TestBean bean2 = (TestBean) iter.next();
		assertEquals("test2", bean2.getName());
		assertEquals(36, bean2.getAge());
	}

	@Test
	public void bindingDate() throws Exception {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
		Date expected = dateFormat.parse("06-03-2006");

		request.addParameter("date", "06-03-2006");

		PortletRequestDataBinder binder = new PortletRequestDataBinder(bean);
		binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
		binder.bind(request);

		assertEquals(expected, bean.getDate());
	}

	@Test
	public void bindingFailsWhenMissingRequiredParam() {
		PortletRequestDataBinder binder = new PortletRequestDataBinder(bean);
		binder.setRequiredFields(new String[] {"age", "name"});

		request.addParameter("age", "35");
		binder.bind(request);

		BindingResult error = binder.getBindingResult();
		assertNotNull(error.getFieldError("name"));
		assertEquals("required", error.getFieldError("name").getCode());
	}

	@Test
	public void bindingExcludesDisallowedParam() {
		PortletRequestDataBinder binder = new PortletRequestDataBinder(bean);
		binder.setAllowedFields(new String[] {"age"});

		request.addParameter("age", "35");
		request.addParameter("name", "test");
		binder.bind(request);

		assertEquals(35, bean.getAge());
		assertNull(bean.getName());
	}

}
