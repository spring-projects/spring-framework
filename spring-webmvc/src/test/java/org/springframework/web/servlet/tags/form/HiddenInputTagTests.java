/*
 * Copyright 2002-present the original author or authors.
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

import jakarta.servlet.jsp.tagext.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.validation.BeanPropertyBindingResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rob Harrop
 */
class HiddenInputTagTests extends AbstractFormTagTests {

	private HiddenInputTag tag;

	private TestBean bean;

	@Override
	protected void onSetUp() {
		this.tag = new HiddenInputTag() {
			@Override
			protected TagWriter createTagWriter() {
				return new TagWriter(getWriter());
			}
		};
		this.tag.setPageContext(getPageContext());
	}

	@Test
	void render() throws Exception {
		this.tag.setPath("name");
		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();

		assertTagOpened(output);
		assertTagClosed(output);

		assertContainsAttribute(output, "type", "hidden");
		assertContainsAttribute(output, "value", "Sally Greenwood");
		assertAttributeNotPresent(output, "disabled");
	}

	@Test
	void withCustomBinder() throws Exception {
		this.tag.setPath("myFloat");

		BeanPropertyBindingResult errors = new BeanPropertyBindingResult(this.bean, COMMAND_NAME);
		errors.getPropertyAccessor().registerCustomEditor(Float.class, new SimpleFloatEditor());
		exposeBindingResult(errors);

		assertThat(this.tag.doStartTag()).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();

		assertTagOpened(output);
		assertTagClosed(output);

		assertContainsAttribute(output, "type", "hidden");
		assertContainsAttribute(output, "value", "12.34f");
	}

	@Test
	void dynamicTypeAttribute() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				this.tag.setDynamicAttribute(null, "type", "email"))
			.withMessage("Attribute type=\"email\" is not allowed");
	}

	@Test
	void disabledTrue() throws Exception {
		this.tag.setDisabled(true);

		this.tag.doStartTag();
		this.tag.doEndTag();

		String output = getOutput();
		assertTagOpened(output);
		assertTagClosed(output);

		assertContainsAttribute(output, "disabled", "disabled");
	}

	// SPR-8661

	@Test
	void disabledFalse() throws Exception {
		this.tag.setDisabled(false);

		this.tag.doStartTag();
		this.tag.doEndTag();

		String output = getOutput();
		assertTagOpened(output);
		assertTagClosed(output);

		assertAttributeNotPresent(output, "disabled");
	}

	private void assertTagClosed(String output) {
		assertThat(output).endsWith("/>");
	}

	private void assertTagOpened(String output) {
		assertThat(output).startsWith("<input ");
	}

	@Override
	protected TestBean createTestBean() {
		this.bean = new TestBean();
		bean.setName("Sally Greenwood");
		bean.setMyFloat(Float.valueOf("12.34"));
		return bean;
	}

}
