/*
 * Copyright 2002-2016 the original author or authors.
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

import javax.servlet.jsp.JspException;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockPageContext;
import org.springframework.tests.sample.beans.TestBean;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public abstract class AbstractFormTagTests extends AbstractHtmlElementTagTests {

	private FormTag formTag = new FormTag();


	@Override
	protected void extendRequest(MockHttpServletRequest request) {
		request.setAttribute(COMMAND_NAME, createTestBean());
	}

	protected abstract TestBean createTestBean();

	@Override
	protected void extendPageContext(MockPageContext pageContext) throws JspException {
		this.formTag.setModelAttribute(COMMAND_NAME);
		this.formTag.setAction("myAction");
		this.formTag.setPageContext(pageContext);
		this.formTag.doStartTag();
	}

	protected final FormTag getFormTag() {
		return this.formTag;
	}

}
