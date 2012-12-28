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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.Tag;

/**
 * @author Rob Harrop
 * @author Rick Evans
 * @author Jeremy Grelle
 */
public class PasswordInputTagTests extends InputTagTests {

	/*
	 * http://opensource.atlassian.com/projects/spring/browse/SPR-2866
	 */
	public void testPasswordValueIsNotRenderedByDefault() throws Exception {
		this.getTag().setPath("name");

		assertEquals(Tag.SKIP_BODY, this.getTag().doStartTag());

		String output = getOutput();
		assertTagOpened(output);
		assertTagClosed(output);

		assertContainsAttribute(output, "type", getType());
		assertValueAttribute(output, "");
	}

	/*
	 * http://opensource.atlassian.com/projects/spring/browse/SPR-2866
	 */
	public void testPasswordValueIsRenderedIfShowPasswordAttributeIsSetToTrue() throws Exception {
		this.getTag().setPath("name");
		this.getPasswordTag().setShowPassword(true);

		assertEquals(Tag.SKIP_BODY, this.getTag().doStartTag());

		String output = getOutput();
		assertTagOpened(output);
		assertTagClosed(output);

		assertContainsAttribute(output, "type", getType());
		assertValueAttribute(output, "Rob");
	}

	/*
	 * http://opensource.atlassian.com/projects/spring/browse/SPR-2866
	 */
	public void testPasswordValueIsNotRenderedIfShowPasswordAttributeIsSetToFalse() throws Exception {
		this.getTag().setPath("name");
		this.getPasswordTag().setShowPassword(false);

		assertEquals(Tag.SKIP_BODY, this.getTag().doStartTag());

		String output = getOutput();
		assertTagOpened(output);
		assertTagClosed(output);

		assertContainsAttribute(output, "type", getType());
		assertValueAttribute(output, "");
	}

	@Override
	public void testDynamicTypeAttribute() throws JspException {
		try {
			this.getTag().setDynamicAttribute(null, "type", "email");
			fail("Expected exception");
		}
		catch (IllegalArgumentException e) {
			assertEquals("Attribute type=\"email\" is not allowed", e.getMessage());
		}
	}

	@Override
	protected void assertValueAttribute(String output, String expectedValue) {
		if (this.getPasswordTag().isShowPassword()) {
			super.assertValueAttribute(output, expectedValue);
		} else {
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
