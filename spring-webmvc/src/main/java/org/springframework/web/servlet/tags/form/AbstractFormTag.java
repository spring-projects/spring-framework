/*
 * Copyright 2002-2010 the original author or authors.
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

import java.beans.PropertyEditor;
import javax.servlet.jsp.JspException;

import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.tags.HtmlEscapingAwareTag;
import org.springframework.web.util.ExpressionEvaluationUtils;

/**
 * Base class for all JSP form tags. Provides utility methods for
 * null-safe EL evaluation and for accessing and working with a {@link TagWriter}.
 *
 * <p>Subclasses should implement the {@link #writeTagContent(TagWriter)} to perform
 * actual tag rendering.
 *
 * <p>Subclasses (or test classes) can override the {@link #createTagWriter()} method to
 * redirect output to a {@link java.io.Writer} other than the {@link javax.servlet.jsp.JspWriter}
 * associated with the current {@link javax.servlet.jsp.PageContext}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public abstract class AbstractFormTag extends HtmlEscapingAwareTag {

	/**
	 * Evaluate the supplied value for the supplied attribute name. If the supplied value
	 * is {@code null} then {@code null} is returned, otherwise evaluation is
	 * handled using {@link ExpressionEvaluationUtils#evaluate(String, String, javax.servlet.jsp.PageContext)}.
	 */
	protected Object evaluate(String attributeName, Object value) throws JspException {
		if (value instanceof String) {
			return ExpressionEvaluationUtils.evaluate(attributeName, (String) value, this.pageContext);
		}
		else {
			return value;
		}
	}

	/**
	 * Evaluate the supplied value for the supplied attribute name. If the supplied value
	 * is {@code null} then {@code false} is returned, otherwise evaluation is
	 * handled using {@link ExpressionEvaluationUtils#evaluate(String, String, javax.servlet.jsp.PageContext)},
	 * with subsequent matching against {@code Boolean.TRUE} and {@code Boolean.valueOf}.
	 */
	protected boolean evaluateBoolean(String attributeName, String value) throws JspException {
		Object evaluated = ExpressionEvaluationUtils.evaluate(attributeName, value, this.pageContext);
		return (Boolean.TRUE.equals(evaluated) ||
				(evaluated instanceof String && Boolean.valueOf((String) evaluated)));
	}

	/**
	 * Optionally writes the supplied value under the supplied attribute name into the supplied
	 * {@link TagWriter}. In this case, the supplied value is {@link #evaluate evaluated} first
	 * and then the {@link ObjectUtils#getDisplayString String representation} is written as the
	 * attribute value. If the resultant {@code String} representation is {@code null}
	 * or empty, no attribute is written.
	 * @see TagWriter#writeOptionalAttributeValue(String, String)
	 */
	protected final void writeOptionalAttribute(TagWriter tagWriter, String attributeName, String value)
			throws JspException {

		if (value != null) {
			tagWriter.writeOptionalAttributeValue(attributeName, getDisplayString(evaluate(attributeName, value)));
		}
	}

	/**
	 * Create the {@link TagWriter} which all output will be written to. By default,
	 * the {@link TagWriter} writes its output to the {@link javax.servlet.jsp.JspWriter}
	 * for the current {@link javax.servlet.jsp.PageContext}. Subclasses may choose to
	 * change the {@link java.io.Writer} to which output is actually written.
	 */
	protected TagWriter createTagWriter() {
		return new TagWriter(this.pageContext);
	}

	/**
	 * Provide a simple template method that calls {@link #createTagWriter()} and passes
	 * the created {@link TagWriter} to the {@link #writeTagContent(TagWriter)} method.
	 * @return the value returned by {@link #writeTagContent(TagWriter)}
	 */
	@Override
	protected final int doStartTagInternal() throws Exception {
		return writeTagContent(createTagWriter());
	}

	/**
	 * Get the display value of the supplied {@code Object}, HTML escaped
	 * as required. This version is <strong>not</strong> {@link PropertyEditor}-aware.
	 */
	protected String getDisplayString(Object value) {
		return ValueFormatter.getDisplayString(value, isHtmlEscape());
	}

	/**
	 * Get the display value of the supplied {@code Object}, HTML escaped
	 * as required. If the supplied value is not a {@link String} and the supplied
	 * {@link PropertyEditor} is not null then the {@link PropertyEditor} is used
	 * to obtain the display value.
	 */
	protected String getDisplayString(Object value, PropertyEditor propertyEditor) {
		return ValueFormatter.getDisplayString(value, propertyEditor, isHtmlEscape());
	}

	/**
	 * Overridden to default to {@code true} in case of no explicit default given.
	 */
	@Override
	protected boolean isDefaultHtmlEscape() {
		Boolean defaultHtmlEscape = getRequestContext().getDefaultHtmlEscape();
		return (defaultHtmlEscape == null || defaultHtmlEscape.booleanValue());
	}


	/**
	 * Subclasses should implement this method to perform tag content rendering.
	 * @return valid tag render instruction as per {@link javax.servlet.jsp.tagext.Tag#doStartTag()}.
	 */
	protected abstract int writeTagContent(TagWriter tagWriter) throws JspException;

}
