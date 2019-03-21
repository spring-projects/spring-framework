/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.servlet.tags.form;

import java.beans.PropertyEditor;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.Tag;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockPageContext;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.tags.RequestContextAwareTag;

import static org.junit.Assert.*;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Scott Andrews
 * @author Jeremy Grelle
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class OptionsTagTests extends AbstractHtmlElementTagTests {

	private static final String COMMAND_NAME = "testBean";

	private SelectTag selectTag;
	private OptionsTag tag;

	@Override
	@SuppressWarnings("serial")
	protected void onSetUp() {
		this.tag = new OptionsTag() {
			@Override
			protected TagWriter createTagWriter() {
				return new TagWriter(getWriter());
			}
		};
		selectTag = new SelectTag() {
			@Override
			protected TagWriter createTagWriter() {
				return new TagWriter(getWriter());
			}
			@Override
			public String getName() {
				// Should not be used other than to delegate to
				// RequestDataValueDataProcessor
				return "testName";
			}
		};
		selectTag.setPageContext(getPageContext());
		this.tag.setParent(selectTag);
		this.tag.setPageContext(getPageContext());
	}

	@Test
	public void withCollection() throws Exception {
		getPageContext().setAttribute(
				SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, new BindStatus(getRequestContext(), "testBean.country", false));

		this.tag.setItems(Country.getCountries());
		this.tag.setItemValue("isoCode");
		this.tag.setItemLabel("name");
		this.tag.setId("myOption");
		this.tag.setCssClass("myClass");
		this.tag.setOnclick("CLICK");
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);
		String output = getOutput();
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();

		List children = rootElement.elements();
		assertEquals("Incorrect number of children", 4, children.size());

		Element element = (Element) rootElement.selectSingleNode("option[@value = 'UK']");
		assertEquals("UK node not selected", "selected", element.attribute("selected").getValue());
		assertEquals("myOption3", element.attribute("id").getValue());
		assertEquals("myClass", element.attribute("class").getValue());
		assertEquals("CLICK", element.attribute("onclick").getValue());
	}

	@Test
	public void withCollectionAndDynamicAttributes() throws Exception {
		String dynamicAttribute1 = "attr1";
		String dynamicAttribute2 = "attr2";

		getPageContext().setAttribute(
				SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, new BindStatus(getRequestContext(), "testBean.country", false));

		this.tag.setItems(Country.getCountries());
		this.tag.setItemValue("isoCode");
		this.tag.setItemLabel("name");
		this.tag.setId("myOption");
		this.tag.setCssClass("myClass");
		this.tag.setOnclick("CLICK");
		this.tag.setDynamicAttribute(null, dynamicAttribute1, dynamicAttribute1);
		this.tag.setDynamicAttribute(null, dynamicAttribute2, dynamicAttribute2);

		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);
		String output = getOutput();
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();

		List children = rootElement.elements();
		assertEquals("Incorrect number of children", 4, children.size());

		Element element = (Element) rootElement.selectSingleNode("option[@value = 'UK']");
		assertEquals("UK node not selected", "selected", element.attribute("selected").getValue());
		assertEquals("myOption3", element.attribute("id").getValue());
		assertEquals("myClass", element.attribute("class").getValue());
		assertEquals("CLICK", element.attribute("onclick").getValue());
		assertEquals(dynamicAttribute1, element.attribute(dynamicAttribute1).getValue());
		assertEquals(dynamicAttribute2, element.attribute(dynamicAttribute2).getValue());
	}

	@Test
	public void withCollectionAndCustomEditor() throws Exception {
		PropertyEditor propertyEditor = new SimpleFloatEditor();

		TestBean target = new TestBean();
		target.setMyFloat(new Float("12.34"));

		BeanPropertyBindingResult errors = new BeanPropertyBindingResult(target, COMMAND_NAME);
		errors.getPropertyAccessor().registerCustomEditor(Float.class, propertyEditor);
		exposeBindingResult(errors);

		getPageContext().setAttribute(
				SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, new BindStatus(getRequestContext(), "testBean.myFloat", false));

		List<Float> floats = new ArrayList<>();
		floats.add(new Float("12.30"));
		floats.add(new Float("12.31"));
		floats.add(new Float("12.32"));
		floats.add(new Float("12.33"));
		floats.add(new Float("12.34"));
		floats.add(new Float("12.35"));

		this.tag.setItems(floats);
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);
		String output = getOutput();
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();

		List children = rootElement.elements();
		assertEquals("Incorrect number of children", 6, children.size());

		Element element = (Element) rootElement.selectSingleNode("option[text() = '12.34f']");
		assertNotNull("Option node should not be null", element);
		assertEquals("12.34 node not selected", "selected", element.attribute("selected").getValue());
		assertNull("No id rendered", element.attribute("id"));

		element = (Element) rootElement.selectSingleNode("option[text() = '12.35f']");
		assertNotNull("Option node should not be null", element);
		assertNull("12.35 node incorrectly selected", element.attribute("selected"));
		assertNull("No id rendered", element.attribute("id"));
	}

	@Test
	public void withItemsNullReference() throws Exception {
		getPageContext().setAttribute(
				SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, new BindStatus(getRequestContext(), "testBean.country", false));

		this.tag.setItems(Collections.emptyList());
		this.tag.setItemValue("isoCode");
		this.tag.setItemLabel("name");
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);
		String output = getOutput();
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();

		List children = rootElement.elements();
		assertEquals("Incorrect number of children", 0, children.size());
	}

	@Test
	public void withoutItems() throws Exception {
		this.tag.setItemValue("isoCode");
		this.tag.setItemLabel("name");
		this.selectTag.setPath("testBean");

		this.selectTag.doStartTag();
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);
		this.tag.doEndTag();
		this.selectTag.doEndTag();

		String output = getOutput();
		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();

		List children = rootElement.elements();
		assertEquals("Incorrect number of children", 0, children.size());
	}

	@Test
	public void withoutItemsEnumParent() throws Exception {
		BeanWithEnum testBean = new BeanWithEnum();
		testBean.setTestEnum(TestEnum.VALUE_2);
		getPageContext().getRequest().setAttribute("testBean", testBean);

		this.selectTag.setPath("testBean.testEnum");

		this.selectTag.doStartTag();
		int result = this.tag.doStartTag();
		assertEquals(BodyTag.SKIP_BODY, result);
		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);
		this.selectTag.doEndTag();

		String output = getWriter().toString();
		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();

		assertEquals(2, rootElement.elements().size());
		Node value1 = rootElement.selectSingleNode("option[@value = 'VALUE_1']");
		Node value2 = rootElement.selectSingleNode("option[@value = 'VALUE_2']");
		assertEquals("TestEnum: VALUE_1", value1.getText());
		assertEquals("TestEnum: VALUE_2", value2.getText());
		assertEquals(value2, rootElement.selectSingleNode("option[@selected]"));
	}

	@Test
	public void withoutItemsEnumParentWithExplicitLabelsAndValues() throws Exception {
		BeanWithEnum testBean = new BeanWithEnum();
		testBean.setTestEnum(TestEnum.VALUE_2);
		getPageContext().getRequest().setAttribute("testBean", testBean);

		this.selectTag.setPath("testBean.testEnum");
		this.tag.setItemLabel("enumLabel");
		this.tag.setItemValue("enumValue");

		this.selectTag.doStartTag();
		int result = this.tag.doStartTag();
		assertEquals(BodyTag.SKIP_BODY, result);
		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);
		this.selectTag.doEndTag();

		String output = getWriter().toString();
		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();

		assertEquals(2, rootElement.elements().size());
		Node value1 = rootElement.selectSingleNode("option[@value = 'Value: VALUE_1']");
		Node value2 = rootElement.selectSingleNode("option[@value = 'Value: VALUE_2']");
		assertEquals("Label: VALUE_1", value1.getText());
		assertEquals("Label: VALUE_2", value2.getText());
		assertEquals(value2, rootElement.selectSingleNode("option[@selected]"));
	}

	@Override
	protected void extendRequest(MockHttpServletRequest request) {
		TestBean bean = new TestBean();
		bean.setName("foo");
		bean.setCountry("UK");
		bean.setMyFloat(new Float("12.34"));
		request.setAttribute(COMMAND_NAME, bean);

		List floats = new ArrayList();
		floats.add(new Float("12.30"));
		floats.add(new Float("12.31"));
		floats.add(new Float("12.32"));
		floats.add(new Float("12.33"));
		floats.add(new Float("12.34"));
		floats.add(new Float("12.35"));

		request.setAttribute("floats", floats);
	}

	@Override
	protected void exposeBindingResult(Errors errors) {
		// wrap errors in a Model
		Map model = new HashMap();
		model.put(BindingResult.MODEL_KEY_PREFIX + COMMAND_NAME, errors);

		// replace the request context with one containing the errors
		MockPageContext pageContext = getPageContext();
		RequestContext context = new RequestContext((HttpServletRequest) pageContext.getRequest(), model);
		pageContext.setAttribute(RequestContextAwareTag.REQUEST_CONTEXT_PAGE_ATTRIBUTE, context);
	}

}
