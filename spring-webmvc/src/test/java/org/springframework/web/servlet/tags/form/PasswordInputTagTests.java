/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.tags.form;

import java.io.Writer;

import jakarta.servlet.jsp.tagext.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rob Harrop
 * @author Rick Evans
 * @author Jeremy Grelle
 */
class PasswordInputTagTests extends InputTagTests {

	@Test // SPR-2866
	void passwordValueIsNotRenderedByDefault() throws Exception {
		getTag().setPath("name");

		assertThat(getTag().doStartTag()).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();
		assertTagOpened(output);
		assertTagClosed(output);

		assertContainsAttribute(output, "type", getType());
		assertValueAttribute(output, "");
	}

	@Test // SPR-2866
	void passwordValueIsRenderedIfShowPasswordAttributeIsSetToTrue() throws Exception {
		getTag().setPath("name");
		getPasswordTag().setShowPassword(true);

		assertThat(getTag().doStartTag()).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();
		assertTagOpened(output);
		assertTagClosed(output);

		assertContainsAttribute(output, "type", getType());
		assertValueAttribute(output, "Rob");
	}

	@Test // >SPR-2866
	void passwordValueIsNotRenderedIfShowPasswordAttributeIsSetToFalse() throws Exception {
		getTag().setPath("name");
		getPasswordTag().setShowPassword(false);

		assertThat(getTag().doStartTag()).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();
		assertTagOpened(output);
		assertTagClosed(output);

		assertContainsAttribute(output, "type", getType());
		assertValueAttribute(output, "");
	}

	@Test
	@Override
	void dynamicTypeAttribute() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> getTag().setDynamicAttribute(null, "type", "email"))
				.withMessage("Attribute type=\"email\" is not allowed");
	}

	@Override
	protected void assertValueAttribute(String output, String expectedValue) {
		if (getPasswordTag().isShowPassword()) {
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
		return (PasswordInputTag) getTag();
	}

}
