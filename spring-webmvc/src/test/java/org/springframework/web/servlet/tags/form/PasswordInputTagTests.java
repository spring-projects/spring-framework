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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.Tag;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rob Harrop
 * @author Rick Evans
 * @author Jeremy Grelle
 */
public class PasswordInputTagTests extends InputTagTests {

	/**
	 * https://jira.spring.io/browse/SPR-2866
	 */
	@Test
	public void passwordValueIsNotRenderedByDefault() throws Exception {
		this.getTag().setPath("name");

		assertThat(this.getTag().doStartTag()).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();
		assertTagOpened(output);
		assertTagClosed(output);

		assertContainsAttribute(output, "type", getType());
		assertValueAttribute(output, "");
	}

	/**
	 * https://jira.spring.io/browse/SPR-2866
	 */
	@Test
	public void passwordValueIsRenderedIfShowPasswordAttributeIsSetToTrue() throws Exception {
		this.getTag().setPath("name");
		this.getPasswordTag().setShowPassword(true);

		assertThat(this.getTag().doStartTag()).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();
		assertTagOpened(output);
		assertTagClosed(output);

		assertContainsAttribute(output, "type", getType());
		assertValueAttribute(output, "Rob");
	}

	/**
	 * https://jira.spring.io/browse/SPR-2866
	 */
	@Test
	public void passwordValueIsNotRenderedIfShowPasswordAttributeIsSetToFalse() throws Exception {
		this.getTag().setPath("name");
		this.getPasswordTag().setShowPassword(false);

		assertThat(this.getTag().doStartTag()).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();
		assertTagOpened(output);
		assertTagClosed(output);

		assertContainsAttribute(output, "type", getType());
		assertValueAttribute(output, "");
	}

	@Test
	@Override
	public void dynamicTypeAttribute() throws JspException {
		assertThatIllegalArgumentException().isThrownBy(() ->
				this.getTag().setDynamicAttribute(null, "type", "email"))
			.withMessage("Attribute type=\"email\" is not allowed");
	}

	@Override
	protected void assertValueAttribute(String output, String expectedValue) {
		if (this.getPasswordTag().isShowPassword()) {
			super.assertValueAttribute(output, expectedValue);
		}
		else {
			super.assertValueAttribute(output, "");
		}
	}

	@Override
	protected String getType() {
		return "password";
	}

	@Override
	@SuppressWarnings("serial")
	protected InputTag createTag(final Writer writer) {
		return new PasswordInputTag() {
			@Override
			protected TagWriter createTagWriter() {
				return new TagWriter(writer);
			}
		};
	}

	private PasswordInputTag getPasswordTag() {
		return (PasswordInputTag) this.getTag();
	}

}
