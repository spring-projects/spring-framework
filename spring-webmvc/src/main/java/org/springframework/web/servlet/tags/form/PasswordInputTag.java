/*
 * Copyright 2002-2007 the original author or authors.
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

/**
 * Databinding-aware JSP tag for rendering an HTML '{@code input}'
 * element with a '{@code type}' of '{@code password}'.
 *
 * @author Rob Harrop
 * @author Rick Evans
 * @author Rossen Stoyanchev
 * @since 2.0
 */
public class PasswordInputTag extends InputTag {

	private boolean showPassword = false;


	/**
	 * Is the password value to be rendered?
	 * @return {@code true} if the password value to be rendered.
	 */
	public boolean isShowPassword() {
		return this.showPassword;
	}

	/**
	 * Is the password value to be rendered?
	 * @param showPassword {@code true} if the password value is to be rendered.
	 */
	public void setShowPassword(boolean showPassword) {
		this.showPassword = showPassword;
	}

	/**
	 * Flags "type" as an illegal dynamic attribute.
	 */
	@Override
	protected boolean isValidDynamicAttribute(String localName, Object value) {
		return !"type".equals(localName);
	}

	/**
	 * Return '{@code password}' causing the rendered HTML '{@code input}'
	 * element to have a '{@code type}' of '{@code password}'.
	 */
	@Override
	protected String getType() {
		return "password";
	}

	/**
	 * The {@link PasswordInputTag} only writes it's value if the
	 * {@link #setShowPassword(boolean) 'showPassword'} property value is
	 * {@link Boolean#TRUE true}.
	 */
	@Override
	protected void writeValue(TagWriter tagWriter) throws JspException {
		if (this.showPassword) {
			super.writeValue(tagWriter);
		} else {
			tagWriter.writeAttribute("value", processFieldValue(getName(), "", getType()));
		}
	}
}
