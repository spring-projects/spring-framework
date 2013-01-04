/*
 * Copyright 2002-2013 the original author or authors.
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

import org.springframework.tests.sample.beans.TestBean;

/**
 * @author Rossen Stoyanchev
 */
public class ButtonTagTests extends AbstractFormTagTests {

	private ButtonTag tag;

	@Override
	protected void onSetUp() {
		this.tag = createTag(getWriter());
		this.tag.setParent(getFormTag());
		this.tag.setPageContext(getPageContext());
		this.tag.setId("My Id");
		this.tag.setName("My Name");
		this.tag.setValue("My Button");
	}

	public void testButtonTag() throws Exception {
		assertEquals(Tag.EVAL_BODY_INCLUDE, this.tag.doStartTag());
		assertEquals(Tag.EVAL_PAGE, this.tag.doEndTag());

		String output = getOutput();
		assertTagOpened(output);
		assertTagClosed(output);

		assertContainsAttribute(output, "id", "My Id");
		assertContainsAttribute(output, "name", "My Name");
		assertContainsAttribute(output, "type", "submit");
		assertContainsAttribute(output, "value", "My Button");
		assertAttributeNotPresent(output, "disabled");
	}

	public void testDisabled() throws Exception {
		this.tag.setDisabled("true");

		this.tag.doStartTag();
		this.tag.doEndTag();

		String output = getOutput();
		assertTagOpened(output);
		assertTagClosed(output);

		assertContainsAttribute(output, "disabled", "disabled");
	}

	@Override
	protected TestBean createTestBean() {
		return new TestBean();
	}

	protected final void assertTagClosed(String output) {
		assertTrue("Tag not closed properly", output.endsWith("</button>"));
	}

	protected final void assertTagOpened(String output) {
		assertTrue("Tag not opened properly", output.startsWith("<button "));
	}

	@SuppressWarnings("serial")
	protected ButtonTag createTag(final Writer writer) {
		return new ButtonTag() {
			@Override
			protected TagWriter createTagWriter() {
				return new TagWriter(writer);
			}
		};
	}

}
