/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.web.servlet.tags;

import javax.servlet.jsp.JspException;

import org.springframework.web.util.ExpressionEvaluationUtils;

/**
 * Sets default HTML escape value for the current page. The actual value
 * can be overridden by escaping-aware tags. The default is "false".
 *
 * <p>Note: You can also set a "defaultHtmlEscape" web.xml context-param.
 * A page-level setting overrides a context-param.
 *
 * @author Juergen Hoeller
 * @since 04.03.2003
 * @see HtmlEscapingAwareTag#setHtmlEscape
 */
public class HtmlEscapeTag extends RequestContextAwareTag {

	private String defaultHtmlEscape;


	/**
	 * Set the default value for HTML escaping,
	 * to be put into the current PageContext.
	 */
	public void setDefaultHtmlEscape(String defaultHtmlEscape) {
		this.defaultHtmlEscape = defaultHtmlEscape;
	}


	@Override
	protected int doStartTagInternal() throws JspException {
		boolean resolvedDefaultHtmlEscape =
				ExpressionEvaluationUtils.evaluateBoolean("defaultHtmlEscape", this.defaultHtmlEscape, pageContext);
		getRequestContext().setDefaultHtmlEscape(resolvedDefaultHtmlEscape);
		return EVAL_BODY_INCLUDE;
	}

}
