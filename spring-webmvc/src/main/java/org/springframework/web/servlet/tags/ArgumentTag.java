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

package org.springframework.web.servlet.tags;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.BodyTagSupport;
import org.jspecify.annotations.Nullable;

/**
 * The {@code <argument>} tag is based on the JSTL {@code fmt:param} tag.
 * The purpose is to support arguments inside the message tags.
 *
 * <p>This tag must be nested under an argument aware tag.
 *
 * <table>
 * <caption>Attribute Summary</caption>
 * <thead>
 * <tr>
 * <th>Attribute</th>
 * <th>Required?</th>
 * <th>Runtime Expression?</th>
 * <th>Description</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>value</td>
 * <td>false</td>
 * <td>true</td>
 * <td>The value of the argument.</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Nicholas Williams
 * @since 4.0
 * @see MessageTag
 */
@SuppressWarnings("serial")
public class ArgumentTag extends BodyTagSupport {

	private @Nullable Object value;

	private boolean valueSet;


	/**
	 * Set the value of the argument (optional).
	 * If not set, the tag's body content will get evaluated.
	 * @param value the parameter value
	 */
	public void setValue(Object value) {
		this.value = value;
		this.valueSet = true;
	}


	@Override
	public int doEndTag() throws JspException {
		Object argument = null;
		if (this.valueSet) {
			argument = this.value;
		}
		else if (getBodyContent() != null) {
			// Get the value from the tag body
			argument = getBodyContent().getString().trim();
		}

		// Find a param-aware ancestor
		ArgumentAware argumentAwareTag = (ArgumentAware) findAncestorWithClass(this, ArgumentAware.class);
		if (argumentAwareTag == null) {
			throw new JspException("The argument tag must be a descendant of a tag that supports arguments");
		}
		argumentAwareTag.addArgument(argument);

		return EVAL_PAGE;
	}

	@Override
	public void release() {
		super.release();
		this.value = null;
		this.valueSet = false;
	}

}
