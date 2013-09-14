/*
 * Copyright 2002-2013 the original author or authors.
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

import org.springframework.web.util.WebUtils;

/**
 * Sets the HTML close policy for the current page. This determines whether
 * HTML tags that don't require closing tags are ended in XML style
 * (<code>/&gt;</code>) or traditional HTML style (<code>&gt;</code>),
 * allowing the user to control the syntax and better pass W3C validators.
 * This overrides the {@code defaultHtmlClosePolicy} property set in
 * {@code web.xml}, if any.
 *
 * @author Nick Williams
 * @since 4.0
 */
public class HtmlClosePolicyTag extends RequestContextAwareTag {

	private boolean xml;

	/**
	 * Set the HTML close policy for the current page.
	 *
	 * @param xml The HTML close policy
	 */
	public void setXml(boolean xml) {
		this.xml = xml;
	}

	@Override
	protected int doStartTagInternal() {
		this.pageContext.setAttribute(WebUtils.HTML_CLOSE_POLICY_ATTRIBUTE, this.xml);
		return EVAL_BODY_INCLUDE;
	}
}
