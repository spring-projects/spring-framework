/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.servlet.tags.form;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.Tag;

import org.junit.Test;

import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.mock.web.test.MockBodyContent;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.tests.sample.beans.Colour;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.util.StringUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.support.BindStatus;

import static org.junit.Assert.*;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Jeremy Grelle
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class OptionTagTests extends AbstractHtmlElementTagTests {

	private static final String ARRAY_SOURCE = "abc,123,def";

	private static final String[] ARRAY = StringUtils.commaDelimitedListToStringArray(ARRAY_SOURCE);

	private OptionTag tag;

	private SelectTag parentTag;

	@Override
	@SuppressWarnings("serial")
	protected void onSetUp() {
		this.tag = new OptionTag() {
			@Override
			protected TagWriter createTagWriter() {
				return new TagWriter(getWriter());
			}
		};
		this.parentTag = new SelectTag() {
			@Override
			public String getName() {
				// Should not be used other than to delegate to
				// RequestDataValueDataProcessor
				return "testName";
			}
		};
		this.tag.setParent(this.parentTag);
		this.tag.setPageContext(getPageContext());
	}


	@Test
	public void canBeDisabledEvenWhenSelected() throws Exception {
		String selectName = "testBean.name";
		BindStatus bindStatus = new BindStatus(getRequestContext(), selectName, false);
		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, bindStatus);
		this.tag.setValue("bar");
		this.tag.setLabel("Bar");
		this.tag.setDisabled(true);
		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);
		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();

		assertOptionTagOpened(output);
		assertOptionTagClosed(output);
		assertContainsAttribute(output, "value", "bar");
		assertContainsAttribute(output, "disabled", "disabled");
		assertBlockTagContains(output, "Bar");
	}

	@Test
	public void renderNotSelected() throws Exception {
		String selectName = "testBean.name";
		BindStatus bindStatus = new BindStatus(getRequestContext(), selectName, false);
		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, bindStatus);
		this.tag.setValue("bar");
		this.tag.setLabel("Bar");
		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);
		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();

		assertOptionTagOpened(output);
		assertOptionTagClosed(output);
		assertContainsAttribute(output, "value", "bar");
		assertBlockTagContains(output, "Bar");
	}

	@Test
	public void renderWithDynamicAttributes() throws Exception {
		String dynamicAttribute1 = "attr1";
		String dynamicAttribute2 = "attr2";

		String selectName = "testBean.name";
		BindStatus bindStatus = new BindStatus(getRequestContext(), selectName, false);
		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, bindStatus);
		this.tag.setValue("bar");
		this.tag.setLabel("Bar");
		this.tag.setDynamicAttribute(null, dynamicAttribute1, dynamicAttribute1);
		this.tag.setDynamicAttribute(null, dynamicAttribute2, dynamicAttribute2);

		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);
		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();

		assertOptionTagOpened(output);
		assertOptionTagClosed(output);
		assertContainsAttribute(output, "value", "bar");
		assertContainsAttribute(output, dynamicAttribute1, dynamicAttribute1);
		assertContainsAttribute(output, dynamicAttribute2, dynamicAttribute2);
		assertBlockTagContains(output, "Bar");
	}

	@Test
	public void renderSelected() throws Exception {
		String selectName = "testBean.name";
		BindStatus bindStatus = new BindStatus(getRequestContext(), selectName, false);
		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, bindStatus);
		this.tag.setId("myOption");
		this.tag.setValue("foo");
		this.tag.setLabel("Foo");
		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);
		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();

		assertOptionTagOpened(output);
		assertOptionTagClosed(output);
		assertContainsAttribute(output, "id", "myOption");
		assertContainsAttribute(output, "value", "foo");
		assertContainsAttribute(output, "selected", "selected");
		assertBlockTagContains(output, "Foo");
	}

	@Test
	public void withNoLabel() throws Exception {
		String selectName = "testBean.name";
		BindStatus bindStatus = new BindStatus(getRequestContext(), selectName, false);
		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, bindStatus);
		this.tag.setValue("bar");
		this.tag.setCssClass("myClass");
		this.tag.setOnclick("CLICK");
		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);
		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();

		assertOptionTagOpened(output);
		assertOptionTagClosed(output);
		assertContainsAttribute(output, "value", "bar");
		assertContainsAttribute(output, "class", "myClass");
		assertContainsAttribute(output, "onclick", "CLICK");
		assertBlockTagContains(output, "bar");
	}

	@Test
	public void withoutContext() throws Exception {
		this.tag.setParent(null);
		this.tag.setValue("foo");
		this.tag.setLabel("Foo");
		try {
			tag.doStartTag();
			fail("Must not be able to use <option> tag without exposed context.");
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	public void withPropertyEditor() throws Exception {
		String selectName = "testBean.stringArray";
		BindStatus bindStatus = new BindStatus(getRequestContext(), selectName, false) {
			@Override
			public PropertyEditor getEditor() {
				return new StringArrayPropertyEditor();
			}
		};
		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, bindStatus);

		this.tag.setValue(ARRAY_SOURCE);
		this.tag.setLabel("someArray");

		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);
		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();

		assertOptionTagOpened(output);
		assertOptionTagClosed(output);
		assertContainsAttribute(output, "value", ARRAY_SOURCE);
		assertContainsAttribute(output, "selected", "selected");
		assertBlockTagContains(output, "someArray");

	}

	@Test
	public void withPropertyEditorStringComparison() throws Exception {
		final PropertyEditor testBeanEditor = new TestBeanPropertyEditor();
		testBeanEditor.setValue(new TestBean("Sally"));
		String selectName = "testBean.spouse";
		BindStatus bindStatus = new BindStatus(getRequestContext(), selectName, false) {
			@Override
			public PropertyEditor getEditor() {
				return testBeanEditor;
			}
		};
		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, bindStatus);

		this.tag.setValue("Sally");

		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);
		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();
		assertOptionTagOpened(output);
		assertOptionTagClosed(output);
		assertContainsAttribute(output, "value", "Sally");
		assertContainsAttribute(output, "selected", "selected");
		assertBlockTagContains(output, "Sally");
	}

	@Test
	public void withCustomObjectSelected() throws Exception {
		String selectName = "testBean.someNumber";
		BindStatus bindStatus = new BindStatus(getRequestContext(), selectName, false);
		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, bindStatus);
		this.tag.setValue(new Float(12.34));
		this.tag.setLabel("GBP 12.34");
		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);
		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();

		assertOptionTagOpened(output);
		assertOptionTagClosed(output);
		assertContainsAttribute(output, "value", "12.34");
		assertContainsAttribute(output, "selected", "selected");
		assertBlockTagContains(output, "GBP 12.34");
	}

	@Test
	public void withCustomObjectNotSelected() throws Exception {
		String selectName = "testBean.someNumber";
		BindStatus bindStatus = new BindStatus(getRequestContext(), selectName, false);
		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, bindStatus);
		this.tag.setValue(new Float(12.35));
		this.tag.setLabel("GBP 12.35");
		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);
		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();

		assertOptionTagOpened(output);
		assertOptionTagClosed(output);
		assertContainsAttribute(output, "value", "12.35");
		assertAttributeNotPresent(output, "selected");
		assertBlockTagContains(output, "GBP 12.35");
	}

	@Test
	public void withCustomObjectAndEditorSelected() throws Exception {
		final PropertyEditor floatEditor = new SimpleFloatEditor();
		floatEditor.setValue(new Float("12.34"));
		String selectName = "testBean.someNumber";
		BindStatus bindStatus = new BindStatus(getRequestContext(), selectName, false) {
			@Override
			public PropertyEditor getEditor() {
				return floatEditor;
			}
		};
		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, bindStatus);

		this.tag.setValue(new Float(12.34));
		this.tag.setLabel("12.34f");

		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);
		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();
		assertOptionTagOpened(output);
		assertOptionTagClosed(output);
		assertContainsAttribute(output, "selected", "selected");
		assertBlockTagContains(output, "12.34f");
	}

	@Test
	public void withCustomObjectAndEditorNotSelected() throws Exception {
		final PropertyEditor floatEditor = new SimpleFloatEditor();
		String selectName = "testBean.someNumber";
		BindStatus bindStatus = new BindStatus(getRequestContext(), selectName, false) {
			@Override
			public PropertyEditor getEditor() {
				return floatEditor;
			}
		};
		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, bindStatus);

		this.tag.setValue(new Float(12.35));
		this.tag.setLabel("12.35f");

		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);
		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();
		assertOptionTagOpened(output);
		assertOptionTagClosed(output);
		assertAttributeNotPresent(output, "selected");
		assertBlockTagContains(output, "12.35f");
	}

	@Test
	public void asBodyTag() throws Exception {
		String selectName = "testBean.name";
		BindStatus bindStatus = new BindStatus(getRequestContext(), selectName, false);
		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, bindStatus);

		String bodyContent = "some content";

		this.tag.setValue("foo");
		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);
		this.tag.setBodyContent(new MockBodyContent(bodyContent, getWriter()));
		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();
		assertOptionTagOpened(output);
		assertOptionTagClosed(output);
		assertContainsAttribute(output, "selected", "selected");
		assertBlockTagContains(output, bodyContent);
	}

	@Test
	public void asBodyTagSelected() throws Exception {
		String selectName = "testBean.name";
		BindStatus bindStatus = new BindStatus(getRequestContext(), selectName, false);
		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, bindStatus);

		String bodyContent = "some content";

		this.tag.setValue("Rob Harrop");
		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);
		this.tag.setBodyContent(new MockBodyContent(bodyContent, getWriter()));
		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();
		assertOptionTagOpened(output);
		assertOptionTagClosed(output);
		assertBlockTagContains(output, bodyContent);
	}

	@Test
	public void asBodyTagCollapsed() throws Exception {
		String selectName = "testBean.name";
		BindStatus bindStatus = new BindStatus(getRequestContext(), selectName, false);
		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, bindStatus);

		String bodyContent = "some content";

		this.tag.setValue(bodyContent);
		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);
		this.tag.setBodyContent(new MockBodyContent(bodyContent, getWriter()));
		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();
		assertOptionTagOpened(output);
		assertOptionTagClosed(output);
		assertContainsAttribute(output, "value", bodyContent);
		assertBlockTagContains(output, bodyContent);
	}

	@Test
	public void asBodyTagWithEditor() throws Exception {
		String selectName = "testBean.stringArray";
		BindStatus bindStatus = new BindStatus(getRequestContext(), selectName, false) {
			@Override
			public PropertyEditor getEditor() {
				return new RulesVariantEditor();
			}
		};
		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, bindStatus);

		RulesVariant rulesVariant = new RulesVariant("someRules", "someVariant");
		this.tag.setValue(rulesVariant);

		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);

		assertEquals(rulesVariant, getPageContext().getAttribute("value"));
		assertEquals(rulesVariant.toId(), getPageContext().getAttribute("displayValue"));

		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);
	}

	@Test
	public void multiBind() throws Exception {
		BeanPropertyBindingResult result = new BeanPropertyBindingResult(new TestBean(), "testBean");
		result.getPropertyAccessor().registerCustomEditor(TestBean.class, "friends", new FriendEditor());
		exposeBindingResult(result);

		BindStatus bindStatus = new BindStatus(getRequestContext(), "testBean.friends", false);

		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, bindStatus);
		this.tag.setValue(new TestBean("foo"));
		this.tag.doStartTag();
		this.tag.doEndTag();

		assertEquals("<option value=\"foo\">foo</option>", getOutput());
	}

	@Test
	public void optionTagNotNestedWithinSelectTag() throws Exception {
		try {
			tag.setParent(null);
			tag.setValue("foo");
			tag.doStartTag();
			fail("Must throw an IllegalStateException when not nested within a <select/> tag.");
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}


	private void assertOptionTagOpened(String output) {
		assertTrue(output.startsWith("<option"));
	}

	private void assertOptionTagClosed(String output) {
		assertTrue(output.endsWith("</option>"));
	}

	@Override
	protected void extendRequest(MockHttpServletRequest request) {
		TestBean bean = new TestBean();
		bean.setName("foo");
		bean.setFavouriteColour(Colour.GREEN);
		bean.setStringArray(ARRAY);
		bean.setSpouse(new TestBean("Sally"));
		bean.setSomeNumber(new Float("12.34"));

		List friends = new ArrayList();
		friends.add(new TestBean("bar"));
		friends.add(new TestBean("penc"));
		bean.setFriends(friends);

		request.setAttribute("testBean", bean);
	}


	private static class TestBeanPropertyEditor extends PropertyEditorSupport {

		@Override
		public void setAsText(String text) throws IllegalArgumentException {
			setValue(new TestBean(text + "k", 123));
		}

		@Override
		public String getAsText() {
			return ((TestBean) getValue()).getName();
		}
	}


	@SuppressWarnings("serial")
	public static class RulesVariant implements Serializable {

		private String rules;

		private String variant;

		public RulesVariant(String rules, String variant) {
			this.setRules(rules);
			this.setVariant(variant);
		}

		private void setRules(String rules) {
			this.rules = rules;
		}

		public String getRules() {
			return rules;
		}

		private void setVariant(String variant) {
			this.variant = variant;
		}

		public String getVariant() {
			return variant;
		}

		public String toId() {
			if (this.variant != null) {
				return this.rules + "-" + this.variant;
			}
			else {
				return rules;
			}
		}

		public static RulesVariant fromId(String id) {
			String[] s = id.split("-", 2);
			String rules = s[0];
			String variant = s.length > 1 ? s[1] : null;
			return new RulesVariant(rules, variant);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof RulesVariant) {
				RulesVariant other = (RulesVariant) obj;
				return this.toId().equals(other.toId());
			}
			return false;
		}

		@Override
		public int hashCode() {
			return this.toId().hashCode();
		}
	}


	public class RulesVariantEditor extends PropertyEditorSupport {

		@Override
		public void setAsText(String text) throws IllegalArgumentException {
			setValue(RulesVariant.fromId(text));
		}

		@Override
		public String getAsText() {
			RulesVariant rulesVariant = (RulesVariant) getValue();
			return rulesVariant.toId();
		}
	}


	private static class FriendEditor extends PropertyEditorSupport {

		@Override
		public void setAsText(String text) throws IllegalArgumentException {
			setValue(new TestBean(text));
		}


		@Override
		public String getAsText() {
			return ((TestBean) getValue()).getName();
		}
	}

}
