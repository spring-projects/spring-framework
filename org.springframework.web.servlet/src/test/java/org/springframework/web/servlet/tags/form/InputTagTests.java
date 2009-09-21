/*
 * Copyright 2002-2008 the original author or authors.
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

import java.io.Writer;

import javax.servlet.jsp.tagext.Tag;

import org.springframework.beans.TestBean;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.servlet.tags.BindTag;
import org.springframework.web.servlet.tags.NestedPathTag;

/**
 * @author Rob Harrop
 * @author Rick Evans
 * @author Jeremy Grelle
 */
public class InputTagTests extends AbstractFormTagTests {

	private InputTag tag;

	private TestBean rob;


	protected void onSetUp() {
		this.tag = createTag(getWriter());
		this.tag.setParent(getFormTag());
		this.tag.setPageContext(getPageContext());
	}

	protected TestBean createTestBean() {
		// set up test data
		this.rob = new TestBean();
		this.rob.setName("Rob");
		this.rob.setMyFloat(new Float(12.34));

		TestBean sally = new TestBean();
		sally.setName("Sally");
		this.rob.setSpouse(sally);

		return this.rob;
	}

	protected final InputTag getTag() {
		return this.tag;
	}


	public void testSimpleBind() throws Exception {
		this.tag.setPath("name");

		assertEquals(Tag.SKIP_BODY, this.tag.doStartTag());

		String output = getOutput();
		assertTagOpened(output);
		assertTagClosed(output);

		assertContainsAttribute(output, "type", getType());
		assertValueAttribute(output, "Rob");
	}

	public void testSimpleBindTagWithinForm() throws Exception {
		BindTag bindTag = new BindTag();
		bindTag.setPath("name");
		bindTag.setPageContext(getPageContext());
		bindTag.doStartTag();

		BindStatus bindStatus = (BindStatus) getPageContext().findAttribute(BindTag.STATUS_VARIABLE_NAME);
		assertEquals("Rob", bindStatus.getValue());
	}

	public void testSimpleBindWithHtmlEscaping() throws Exception {
		final String NAME = "Rob \"I Love Mangos\" Harrop";
		final String HTML_ESCAPED_NAME = "Rob &quot;I Love Mangos&quot; Harrop";

		this.tag.setPath("name");
		this.rob.setName(NAME);

		assertEquals(Tag.SKIP_BODY, this.tag.doStartTag());

		String output = getOutput();
		assertTagOpened(output);
		assertTagClosed(output);

		assertContainsAttribute(output, "type", getType());
		assertValueAttribute(output, HTML_ESCAPED_NAME);
	}

	protected void assertValueAttribute(String output, String expectedValue) {
		assertContainsAttribute(output, "value", expectedValue);
	}

	public void testComplexBind() throws Exception {
		this.tag.setPath("spouse.name");

		assertEquals(Tag.SKIP_BODY, this.tag.doStartTag());

		String output = getOutput();
		assertTagOpened(output);
		assertTagClosed(output);

		assertContainsAttribute(output, "id", "spouse.name");
		assertContainsAttribute(output, "name", "spouse.name");
		assertContainsAttribute(output, "type", getType());
		assertValueAttribute(output, "Sally");
	}

	public void testWithAllAttributes() throws Exception {
		String title = "aTitle";
		String id = "123";
		String size = "12";
		String cssClass = "textfield";
		String cssStyle = "width:10px";
		String lang = "en";
		String dir = "ltr";
		String tabindex = "2";
		String disabled = "true";
		String onclick = "doClick()";
		String ondblclick = "doDblclick()";
		String onkeydown = "doKeydown()";
		String onkeypress = "doKeypress()";
		String onkeyup = "doKeyup()";
		String onmousedown = "doMouseDown()";
		String onmousemove = "doMouseMove()";
		String onmouseout = "doMouseOut()";
		String onmouseover = "doMouseOver()";
		String onmouseup = "doMouseUp()";
		String onfocus = "doFocus()";
		String onblur = "doBlur()";
		String onchange = "doChange()";
		String accesskey = "a";
		String maxlength = "12";
		String alt = "Some text";
		String onselect = "doSelect()";
		String readonly = "true";
		String autocomplete = "off";
		String dynamicAttribute1 = "attr1";
		String dynamicAttribute2 = "attr2";

		this.tag.setId(id);
		this.tag.setPath("name");
		this.tag.setSize(size);
		this.tag.setCssClass(cssClass);
		this.tag.setCssStyle(cssStyle);
		this.tag.setTitle(title);
		this.tag.setLang(lang);
		this.tag.setDir(dir);
		this.tag.setTabindex(tabindex);
		this.tag.setDisabled(disabled);
		this.tag.setOnclick(onclick);
		this.tag.setOndblclick(ondblclick);
		this.tag.setOnkeydown(onkeydown);
		this.tag.setOnkeypress(onkeypress);
		this.tag.setOnkeyup(onkeyup);
		this.tag.setOnmousedown(onmousedown);
		this.tag.setOnmousemove(onmousemove);
		this.tag.setOnmouseout(onmouseout);
		this.tag.setOnmouseover(onmouseover);
		this.tag.setOnmouseup(onmouseup);
		this.tag.setOnfocus(onfocus);
		this.tag.setOnblur(onblur);
		this.tag.setOnchange(onchange);
		this.tag.setAccesskey(accesskey);
		this.tag.setMaxlength(maxlength);
		this.tag.setAlt(alt);
		this.tag.setOnselect(onselect);
		this.tag.setReadonly(readonly);
		this.tag.setAutocomplete(autocomplete);
		this.tag.setDynamicAttribute(null, dynamicAttribute1, dynamicAttribute1);
		this.tag.setDynamicAttribute(null, dynamicAttribute2, dynamicAttribute2);

		assertEquals(Tag.SKIP_BODY, this.tag.doStartTag());

		String output = getOutput();
		assertTagOpened(output);
		assertTagClosed(output);

		assertContainsAttribute(output, "type", getType());

		assertContainsAttribute(output, "id", id);
		assertValueAttribute(output, "Rob");
		assertContainsAttribute(output, "size", size);
		assertContainsAttribute(output, "class", cssClass);
		assertContainsAttribute(output, "style", cssStyle);
		assertContainsAttribute(output, "title", title);
		assertContainsAttribute(output, "lang", lang);
		assertContainsAttribute(output, "dir", dir);
		assertContainsAttribute(output, "tabindex", tabindex);
		assertContainsAttribute(output, "disabled", "disabled");
		assertContainsAttribute(output, "onclick", onclick);
		assertContainsAttribute(output, "ondblclick", ondblclick);
		assertContainsAttribute(output, "onkeydown", onkeydown);
		assertContainsAttribute(output, "onkeypress", onkeypress);
		assertContainsAttribute(output, "onkeyup", onkeyup);
		assertContainsAttribute(output, "onmousedown", onmousedown);
		assertContainsAttribute(output, "onmousemove", onmousemove);
		assertContainsAttribute(output, "onmouseout", onmouseout);
		assertContainsAttribute(output, "onmouseover", onmouseover);
		assertContainsAttribute(output, "onmouseup", onmouseup);
		assertContainsAttribute(output, "onfocus", onfocus);
		assertContainsAttribute(output, "onblur", onblur);
		assertContainsAttribute(output, "onchange", onchange);
		assertContainsAttribute(output, "accesskey", accesskey);
		assertContainsAttribute(output, "maxlength", maxlength);
		assertContainsAttribute(output, "alt", alt);
		assertContainsAttribute(output, "onselect", onselect);
		assertContainsAttribute(output, "readonly", "readonly");
		assertContainsAttribute(output, "autocomplete", autocomplete);
		assertContainsAttribute(output, dynamicAttribute1, dynamicAttribute1);
		assertContainsAttribute(output, dynamicAttribute2, dynamicAttribute2);
	}

	public void testWithNestedBind() throws Exception {
		NestedPathTag nestedPathTag = new NestedPathTag();
		nestedPathTag.setPath("spouse.");
		nestedPathTag.setPageContext(getPageContext());
		nestedPathTag.doStartTag();

		this.tag.setPath("name");

		assertEquals(Tag.SKIP_BODY, this.tag.doStartTag());

		String output = getOutput();
		assertTagOpened(output);
		assertTagClosed(output);

		assertContainsAttribute(output, "type", getType());
		assertValueAttribute(output, "Sally");
	}

	public void testWithNestedBindTagWithinForm() throws Exception {
		NestedPathTag nestedPathTag = new NestedPathTag();
		nestedPathTag.setPath("spouse.");
		nestedPathTag.setPageContext(getPageContext());
		nestedPathTag.doStartTag();

		BindTag bindTag = new BindTag();
		bindTag.setPath("name");
		bindTag.setPageContext(getPageContext());
		bindTag.doStartTag();

		BindStatus bindStatus = (BindStatus) getPageContext().findAttribute(BindTag.STATUS_VARIABLE_NAME);
		assertEquals("Sally", bindStatus.getValue());
	}

	public void testWithErrors() throws Exception {
		this.tag.setPath("name");
		this.tag.setCssClass("good");
		this.tag.setCssErrorClass("bad");

		BeanPropertyBindingResult errors = new BeanPropertyBindingResult(this.rob, COMMAND_NAME);
		errors.rejectValue("name", "some.code", "Default Message");
		errors.rejectValue("name", "too.short", "Too Short");
		exposeBindingResult(errors);

		assertEquals(Tag.SKIP_BODY, this.tag.doStartTag());

		String output = getOutput();
		assertTagOpened(output);
		assertTagClosed(output);

		assertContainsAttribute(output, "type", getType());
		assertValueAttribute(output, "Rob");
		assertContainsAttribute(output, "class", "bad");
	}

	public void testDisabledFalse() throws Exception {
		this.tag.setPath("name");
		this.tag.setDisabled("false");
		this.tag.doStartTag();

		String output = getOutput();
		assertAttributeNotPresent(output, "disabled");
	}

	public void testWithCustomBinder() throws Exception {
		this.tag.setPath("myFloat");

		BeanPropertyBindingResult errors = new BeanPropertyBindingResult(this.rob, COMMAND_NAME);
		errors.getPropertyAccessor().registerCustomEditor(Float.class, new SimpleFloatEditor());
		exposeBindingResult(errors);

		assertEquals(Tag.SKIP_BODY, this.tag.doStartTag());

		String output = getOutput();
		assertTagOpened(output);
		assertTagClosed(output);

		assertContainsAttribute(output, "type", getType());
		assertValueAttribute(output, "12.34f");
	}

	/**
	 * See SPR-3127 (http://opensource.atlassian.com/projects/spring/browse/SPR-3127)
	 */
	public void testReadOnlyAttributeRenderingWhenReadonlyIsTrue() throws Exception {
		this.tag.setPath("name");
		this.tag.setReadonly("true");

		assertEquals(Tag.SKIP_BODY, this.tag.doStartTag());

		String output = getOutput();
		assertTagOpened(output);
		assertTagClosed(output);

		assertContainsAttribute(output, "type", getType());
		assertContainsAttribute(output, "readonly", "readonly");
		assertValueAttribute(output, "Rob");
	}

	/**
	 * See SPR-3127 (http://opensource.atlassian.com/projects/spring/browse/SPR-3127)
	 */
	public void testReadOnlyAttributeRenderingWhenReadonlyIsFalse() throws Exception {
		this.tag.setPath("name");
		this.tag.setReadonly("nope, this is not readonly");

		assertEquals(Tag.SKIP_BODY, this.tag.doStartTag());

		String output = getOutput();
		assertTagOpened(output);
		assertTagClosed(output);

		assertContainsAttribute(output, "type", getType());
		assertAttributeNotPresent(output, "readonly");
		assertValueAttribute(output, "Rob");
	}


	protected final void assertTagClosed(String output) {
		assertTrue("Tag not closed properly", output.endsWith("/>"));
	}

	protected final void assertTagOpened(String output) {
		assertTrue("Tag not opened properly", output.startsWith("<input "));
	}

	protected InputTag createTag(final Writer writer) {
		return new InputTag() {
			protected TagWriter createTagWriter() {
				return new TagWriter(writer);
			}
		};
	}

	protected String getType() {
		return "text";
	}

}
