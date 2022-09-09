/*
 * Copyright 2002-2018 the original author or authors.
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

import java.io.IOException;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.BodyContent;
import jakarta.servlet.jsp.tagext.BodyTag;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.util.JavaScriptUtils;

/**
 * The {@code <escapeBody>} tag is used to escape its enclosed body content,
 * applying HTML escaping and/or JavaScript escaping.
 *
 * <p>Provides a "htmlEscape" property for explicitly specifying whether to
 * apply HTML escaping. If not set, a page-level default (e.g. from the
 * HtmlEscapeTag) or an application-wide default (the "defaultHtmlEscape"
 * context-param in web.xml) is used.
 *
 * <p>Provides a "javaScriptEscape" property for specifying whether to apply
 * JavaScript escaping. Can be combined with HTML escaping or used standalone.
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
 * <td>htmlEscape</td>
 * <td>false</td>
 * <td>true</td>
 * <td>Set HTML escaping for this tag, as boolean value.
 * Overrides the default HTML escaping setting for the current page.</td>
 * </tr>
 * <tr>
 * <td>javaScriptEscape</td>
 * <td>false</td>
 * <td>true</td>
 * <td>Set JavaScript escaping for this tag, as boolean value.
 * Default is {@code false}.</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Juergen Hoeller
 * @since 1.1.1
 * @see org.springframework.web.util.HtmlUtils
 * @see org.springframework.web.util.JavaScriptUtils
 */
@SuppressWarnings("serial")
public class EscapeBodyTag extends HtmlEscapingAwareTag implements BodyTag {

	private boolean javaScriptEscape = false;

	@Nullable
	private BodyContent bodyContent;


	/**
	 * Set JavaScript escaping for this tag, as boolean value.
	 * Default is "false".
	 */
	public void setJavaScriptEscape(boolean javaScriptEscape) throws JspException {
		this.javaScriptEscape = javaScriptEscape;
	}


	@Override
	protected int doStartTagInternal() {
		// do nothing
		return EVAL_BODY_BUFFERED;
	}

	@Override
	public void doInitBody() {
		// do nothing
	}

	@Override
	public void setBodyContent(BodyContent bodyContent) {
		this.bodyContent = bodyContent;
	}

	@Override
	public int doAfterBody() throws JspException {
		try {
			String content = readBodyContent();
			// HTML and/or JavaScript escape, if demanded
			content = htmlEscape(content);
			content = (this.javaScriptEscape ? JavaScriptUtils.javaScriptEscape(content) : content);
			writeBodyContent(content);
		}
		catch (IOException ex) {
			throw new JspException("Could not write escaped body", ex);
		}
		return (SKIP_BODY);
	}

	/**
	 * Read the unescaped body content from the page.
	 * @return the original content
	 * @throws IOException if reading failed
	 */
	protected String readBodyContent() throws IOException {
		Assert.state(this.bodyContent != null, "No BodyContent set");
		return this.bodyContent.getString();
	}

	/**
	 * Write the escaped body content to the page.
	 * <p>Can be overridden in subclasses, e.g. for testing purposes.
	 * @param content the content to write
	 * @throws IOException if writing failed
	 */
	protected void writeBodyContent(String content) throws IOException {
		Assert.state(this.bodyContent != null, "No BodyContent set");
		this.bodyContent.getEnclosingWriter().print(content);
	}

}
