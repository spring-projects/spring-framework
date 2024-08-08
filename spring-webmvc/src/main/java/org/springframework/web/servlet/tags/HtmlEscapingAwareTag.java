/*
 * Copyright 2002-2024 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.web.util.HtmlUtils;

/**
 * Superclass for tags that output content that might get HTML-escaped.
 *
 * <p>Provides a "htmlEscape" property for explicitly specifying whether to
 * apply HTML escaping. If not set, a page-level default (e.g. from the
 * HtmlEscapeTag) or an application-wide default (the "defaultHtmlEscape"
 * context-param in {@code web.xml}) is used.
 *
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @since 1.1
 * @see #setHtmlEscape
 * @see HtmlEscapeTag
 * @see org.springframework.web.servlet.support.RequestContext#isDefaultHtmlEscape
 * @see org.springframework.web.util.WebUtils#getDefaultHtmlEscape
 * @see org.springframework.web.util.WebUtils#getResponseEncodedHtmlEscape
 */
@SuppressWarnings("serial")
public abstract class HtmlEscapingAwareTag extends RequestContextAwareTag {

	@Nullable
	private Boolean htmlEscape;


	/**
	 * Set HTML escaping for this tag, as boolean value.
	 * Overrides the default HTML escaping setting for the current page.
	 * @see HtmlEscapeTag#setDefaultHtmlEscape
	 */
	public void setHtmlEscape(boolean htmlEscape) throws JspException {
		this.htmlEscape = htmlEscape;
	}

	/**
	 * Return the HTML escaping setting for this tag,
	 * or the default setting if not overridden.
	 * @see #isDefaultHtmlEscape()
	 */
	protected boolean isHtmlEscape() {
		if (this.htmlEscape != null) {
			return this.htmlEscape;
		}
		else {
			return isDefaultHtmlEscape();
		}
	}

	/**
	 * Return the applicable default HTML escape setting for this tag.
	 * <p>The default implementation checks the RequestContext's setting,
	 * falling back to {@code false} in case of no explicit default given.
	 * @see #getRequestContext()
	 */
	protected boolean isDefaultHtmlEscape() {
		return getRequestContext().isDefaultHtmlEscape();
	}

	/**
	 * Return the applicable default for the use of response encoding with
	 * HTML escaping for this tag.
	 * <p>The default implementation checks the RequestContext's setting,
	 * falling back to {@code false} in case of no explicit default given.
	 * @since 4.1.2
	 * @see #getRequestContext()
	 */
	protected boolean isResponseEncodedHtmlEscape() {
		return getRequestContext().isResponseEncodedHtmlEscape();
	}

	/**
	 * HTML-encodes the given String, only if the "htmlEscape" setting is enabled.
	 * <p>The response encoding will be taken into account if the
	 * "responseEncodedHtmlEscape" setting is enabled as well.
	 * @param content the String to escape
	 * @return the escaped String
	 * @since 4.1.2
	 * @see #isHtmlEscape()
	 * @see #isResponseEncodedHtmlEscape()
	 */
	protected String htmlEscape(String content) {
		String out = content;
		if (isHtmlEscape()) {
			if (isResponseEncodedHtmlEscape()) {
				out = HtmlUtils.htmlEscape(content, this.pageContext.getResponse().getCharacterEncoding());
			}
			else {
				out = HtmlUtils.htmlEscape(content);
			}
		}
		return out;
	}

}
