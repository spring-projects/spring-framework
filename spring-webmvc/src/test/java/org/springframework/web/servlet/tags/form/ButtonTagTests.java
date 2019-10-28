/*
 * Copyright 2002-2019 the original author or authors.
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

import java.io.Writer;

import javax.servlet.jsp.tagext.Tag;

import org.junit.jupiter.api.Test;

import org.springframework.tests.sample.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

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

	@Test
	public void buttonTag() throws Exception {
		assertThat(this.tag.doStartTag()).isEqualTo(Tag.EVAL_BODY_INCLUDE);
		assertThat(this.tag.doEndTag()).isEqualTo(Tag.EVAL_PAGE);

		String output = getOutput();
		assertTagOpened(output);
		assertTagClosed(output);

		assertContainsAttribute(output, "id", "My Id");
		assertContainsAttribute(output, "name", "My Name");
		assertContainsAttribute(output, "type", "submit");
		assertContainsAttribute(output, "value", "My Button");
		assertAttributeNotPresent(output, "disabled");
	}

	@Test
	public void disabled() throws Exception {
		this.tag.setDisabled(true);

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
		assertThat(output.endsWith("</button>")).as("Tag not closed properly").isTrue();
	}

	protected final void assertTagOpened(String output) {
		assertThat(output.startsWith("<button ")).as("Tag not opened properly").isTrue();
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
