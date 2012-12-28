/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.portlet.bind;

import java.beans.PropertyEditorSupport;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import junit.framework.TestCase;

import org.springframework.beans.ITestBean;
import org.springframework.beans.TestBean;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.core.CollectionFactory;
import org.springframework.mock.web.portlet.MockPortletRequest;
import org.springframework.validation.BindingResult;

/**
 * @author Mark Fisher
 */
public class PortletRequestDataBinderTests extends TestCase {

	public void testSimpleBind() {
		TestBean bean = new TestBean();

		MockPortletRequest request = new MockPortletRequest();
		request.addParameter("age", "35");
		request.addParameter("name", "test");

		PortletRequestDataBinder binder = new PortletRequestDataBinder(bean);
		binder.bind(request);

		assertEquals(35, bean.getAge());
		assertEquals("test", bean.getName());
	}

	public void testNestedBind() {
		TestBean bean = new TestBean();
		bean.setSpouse(new TestBean());

		MockPortletRequest request = new MockPortletRequest();
		request.addParameter("spouse.name", "test");

		PortletRequestDataBinder binder = new PortletRequestDataBinder(bean);
		binder.bind(request);

		assertNotNull(bean.getSpouse());
		assertEquals("test", bean.getSpouse().getName());
	}

	public void testNestedBindWithPropertyEditor() {
		TestBean bean = new TestBean();

		PortletRequestDataBinder binder = new PortletRequestDataBinder(bean);
		binder.registerCustomEditor(ITestBean.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(new TestBean(text));
			}
		});

		MockPortletRequest request = new MockPortletRequest();
		request.addParameter("spouse", "test");
		request.addParameter("spouse.age", "32");
		binder.bind(request);

		assertNotNull(bean.getSpouse());
		assertEquals("test", bean.getSpouse().getName());
		assertEquals(32, bean.getSpouse().getAge());
	}

	public void testBindingMismatch() {
		TestBean bean = new TestBean();
		bean.setAge(30);

		MockPortletRequest request = new MockPortletRequest();
		request.addParameter("age", "zzz");

		PortletRequestDataBinder binder = new PortletRequestDataBinder(bean);
		binder.bind(request);

		BindingResult error = binder.getBindingResult();
		assertNotNull(error.getFieldError("age"));
		assertEquals("typeMismatch", error.getFieldError("age").getCode());
		assertEquals(30, bean.getAge());
	}

	public void testBindingStringWithCommaSeparatedValue() throws Exception {
	  TestBean bean = new TestBean();

		MockPortletRequest request = new MockPortletRequest();
		request.addParameter("stringArray", "test1,test2");

		PortletRequestDataBinder binder = new PortletRequestDataBinder(bean);
		binder.bind(request);

		assertNotNull(bean.getStringArray());
		assertEquals(1, bean.getStringArray().length);
		assertEquals("test1,test2", bean.getStringArray()[0]);
	}

	public void testBindingStringArrayWithSplitting() {
		TestBean bean = new TestBean();

		MockPortletRequest request = new MockPortletRequest();
		request.addParameter("stringArray", "test1,test2");

		PortletRequestDataBinder binder = new PortletRequestDataBinder(bean);
		binder.registerCustomEditor(String[].class, new StringArrayPropertyEditor());
		binder.bind(request);

		assertNotNull(bean.getStringArray());
		assertEquals(2, bean.getStringArray().length);
		assertEquals("test1", bean.getStringArray()[0]);
		assertEquals("test2", bean.getStringArray()[1]);
	}

	public void testBindingList() {
		TestBean bean = new TestBean();

		MockPortletRequest request = new MockPortletRequest();
		request.addParameter("someList[0]", "test1");
		request.addParameter("someList[1]", "test2");

		PortletRequestDataBinder binder = new PortletRequestDataBinder(bean);
		binder.bind(request);

		assertNotNull(bean.getSomeList());
		assertEquals(2, bean.getSomeList().size());
		assertEquals("test1", bean.getSomeList().get(0));
		assertEquals("test2", bean.getSomeList().get(1));
	}

	public void testBindingMap() {
		TestBean bean = new TestBean();

		MockPortletRequest request = new MockPortletRequest();
		request.addParameter("someMap['key1']", "val1");
		request.addParameter("someMap['key2']", "val2");

		PortletRequestDataBinder binder = new PortletRequestDataBinder(bean);
		binder.bind(request);

		assertNotNull(bean.getSomeMap());
		assertEquals(2, bean.getSomeMap().size());
		assertEquals("val1", bean.getSomeMap().get("key1"));
		assertEquals("val2", bean.getSomeMap().get("key2"));
	}

	public void testBindingSet() {
		TestBean bean = new TestBean();
		Set set = CollectionFactory.createLinkedSetIfPossible(2);
		set.add(new TestBean("test1"));
		set.add(new TestBean("test2"));
		bean.setSomeSet(set);

		MockPortletRequest request = new MockPortletRequest();
		request.addParameter("someSet[0].age", "35");
		request.addParameter("someSet[1].age", "36");

		PortletRequestDataBinder binder = new PortletRequestDataBinder(bean);
		binder.bind(request);

		assertNotNull(bean.getSomeSet());
		assertEquals(2, bean.getSomeSet().size());

		Iterator iter = bean.getSomeSet().iterator();

		TestBean bean1 = (TestBean) iter.next();
		assertEquals("test1", bean1.getName());
		assertEquals(35, bean1.getAge());

		TestBean bean2 = (TestBean) iter.next();
		assertEquals("test2", bean2.getName());
		assertEquals(36, bean2.getAge());
	}

	public void testBindingDate() throws Exception {
		TestBean bean = new TestBean();
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
		Date expected = dateFormat.parse("06-03-2006");

		MockPortletRequest request = new MockPortletRequest();
		request.addParameter("date", "06-03-2006");

		PortletRequestDataBinder binder = new PortletRequestDataBinder(bean);
		binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
		binder.bind(request);

		assertEquals(expected, bean.getDate());
	}

	public void testBindingFailsWhenMissingRequiredParam() {
		TestBean bean = new TestBean();
		PortletRequestDataBinder binder = new PortletRequestDataBinder(bean);
		binder.setRequiredFields(new String[] {"age", "name"});

		MockPortletRequest request = new MockPortletRequest();
		request.addParameter("age", "35");
		binder.bind(request);

		BindingResult error = binder.getBindingResult();
		assertNotNull(error.getFieldError("name"));
		assertEquals("required", error.getFieldError("name").getCode());
	}

	public void testBindingExcludesDisallowedParam() {
		TestBean bean = new TestBean();
		PortletRequestDataBinder binder = new PortletRequestDataBinder(bean);
		binder.setAllowedFields(new String[] {"age"});

		MockPortletRequest request = new MockPortletRequest();
		request.addParameter("age", "35");
		request.addParameter("name", "test");
		binder.bind(request);

		assertEquals(35, bean.getAge());
		assertNull(bean.getName());
	}

}
