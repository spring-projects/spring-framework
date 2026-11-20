/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.tags.form;

import jakarta.servlet.jsp.JspException;

import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockPageContext;

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
