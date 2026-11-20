/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.tags.form;

import java.io.Writer;

import jakarta.servlet.jsp.tagext.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
class ButtonTagTests extends AbstractFormTagTests {

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
	void buttonTag() throws Exception {
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
	void value() throws Exception {
		this.tag.setValue("\"My Button\"");

		assertThat(this.tag.doStartTag()).isEqualTo(Tag.EVAL_BODY_INCLUDE);
		assertThat(this.tag.doEndTag()).isEqualTo(Tag.EVAL_PAGE);

		String output = getOutput();
		assertTagOpened(output);
		assertTagClosed(output);

		assertContainsAttribute(output, "value", "&quot;My Button&quot;");
	}

	@Test
	void disabled() throws Exception {
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
		assertThat(output).as("Tag not closed properly").endsWith("</button>");
	}

	protected final void assertTagOpened(String output) {
		assertThat(output).as("Tag not opened properly").startsWith("<button ");
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
