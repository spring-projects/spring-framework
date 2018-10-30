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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.Tag;

import org.junit.Test;

import org.springframework.mock.web.test.MockPageContext;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.web.servlet.tags.NestedPathTag;

import static org.junit.Assert.*;

/**
 * @author Rob Harrop
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Jeremy Grelle
 */
public class LabelTagTests extends AbstractFormTagTests {

	private LabelTag tag;


	@Override
	@SuppressWarnings("serial")
	protected void onSetUp() {
		this.tag = new LabelTag() {
			@Override
			protected TagWriter createTagWriter() {
				return new TagWriter(getWriter());
			}
		};
		this.tag.setPageContext(getPageContext());
	}

	@Override
	protected void extendPageContext(MockPageContext pageContext) throws JspException {
		super.extendPageContext(pageContext);

		NestedPathTag nestedPathTag = new NestedPathTag();
		nestedPathTag.setPath("spouse.");
		nestedPathTag.setPageContext(pageContext);
		nestedPathTag.doStartTag();
	}


	@Test
	public void simpleRender() throws Exception {
		this.tag.setPath("name");
		int startResult = this.tag.doStartTag();
		int endResult = this.tag.doEndTag();

		assertEquals(Tag.EVAL_BODY_INCLUDE, startResult);
		assertEquals(Tag.EVAL_PAGE, endResult);

		String output = getOutput();
		// we are using a nested path (see extendPageContext(..)), so...
		assertContainsAttribute(output, "for", "spouse.name");
		// name attribute is not supported by <label/>
		assertAttributeNotPresent(output, "name");
		// id attribute is supported, but we don't want it
		assertAttributeNotPresent(output, "id");
		assertTrue(output.startsWith("<label "));
		assertTrue(output.endsWith("</label>"));
	}

	@Test
	public void simpleRenderWithDynamicAttributes() throws Exception {
		String dynamicAttribute1 = "attr1";
		String dynamicAttribute2 = "attr2";

		this.tag.setPath("name");
		this.tag.setDynamicAttribute(null, dynamicAttribute1, dynamicAttribute1);
		this.tag.setDynamicAttribute(null, dynamicAttribute2, dynamicAttribute2);

		int startResult = this.tag.doStartTag();
		int endResult = this.tag.doEndTag();

		assertEquals(Tag.EVAL_BODY_INCLUDE, startResult);
		assertEquals(Tag.EVAL_PAGE, endResult);

		String output = getOutput();
		// we are using a nested path (see extendPageContext(..)), so...
		assertContainsAttribute(output, "for", "spouse.name");
		assertContainsAttribute(output, dynamicAttribute1, dynamicAttribute1);
		assertContainsAttribute(output, dynamicAttribute2, dynamicAttribute2);
		// name attribute is not supported by <label/>
		assertAttributeNotPresent(output, "name");
		// id attribute is supported, but we don't want it
		assertAttributeNotPresent(output, "id");
		assertTrue(output.startsWith("<label "));
		assertTrue(output.endsWith("</label>"));
	}

	@Test
	public void simpleRenderWithMapElement() throws Exception {
		this.tag.setPath("someMap[1]");
		int startResult = this.tag.doStartTag();
		int endResult = this.tag.doEndTag();

		assertEquals(Tag.EVAL_BODY_INCLUDE, startResult);
		assertEquals(Tag.EVAL_PAGE, endResult);

		String output = getOutput();
		// we are using a nested path (see extendPageContext(..)), so...
		assertContainsAttribute(output, "for", "spouse.someMap1");
		// name attribute is not supported by <label/>
		assertAttributeNotPresent(output, "name");
		// id attribute is supported, but we don't want it
		assertAttributeNotPresent(output, "id");
		assertTrue(output.startsWith("<label "));
		assertTrue(output.endsWith("</label>"));
	}

	@Test
	public void overrideFor() throws Exception {
		this.tag.setPath("name");
		this.tag.setFor("myElement");
		int startResult = this.tag.doStartTag();
		int endResult = this.tag.doEndTag();

		assertEquals(Tag.EVAL_BODY_INCLUDE, startResult);
		assertEquals(Tag.EVAL_PAGE, endResult);

		String output = getOutput();
		assertContainsAttribute(output, "for", "myElement");
		// name attribute is not supported by <label/>
		assertAttributeNotPresent(output, "name");
		// id attribute is supported, but we don't want it
		assertAttributeNotPresent(output, "id");
		assertTrue(output.startsWith("<label "));
		assertTrue(output.endsWith("</label>"));
	}


	@Override
	protected TestBean createTestBean() {
		TestBean bean = new TestBean();
		bean.setSpouse(new TestBean("Hoopy"));
		return bean;
	}

}
