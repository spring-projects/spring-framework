/*
 * Copyright 2002-2019 the original author or authors.
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

import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.junit.jupiter.api.BeforeEach;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockPageContext;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.support.JspAwareRequestContext;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.support.RequestDataValueProcessor;
import org.springframework.web.servlet.support.RequestDataValueProcessorWrapper;
import org.springframework.web.servlet.tags.AbstractTagTests;
import org.springframework.web.servlet.tags.RequestContextAwareTag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public abstract class AbstractHtmlElementTagTests extends AbstractTagTests {

	public static final String COMMAND_NAME = "testBean";

	private StringWriter writer;

	private MockPageContext pageContext;


	@BeforeEach
	public final void setUp() throws Exception {
		// set up a writer for the tag content to be written to
		this.writer = new StringWriter();

		// configure the page context
		this.pageContext = createAndPopulatePageContext();

		onSetUp();
	}

	protected MockPageContext createAndPopulatePageContext() throws JspException {
		MockPageContext pageContext = createPageContext();
		MockHttpServletRequest request = (MockHttpServletRequest) pageContext.getRequest();
		((StaticWebApplicationContext) RequestContextUtils.findWebApplicationContext(request))
				.registerSingleton("requestDataValueProcessor", RequestDataValueProcessorWrapper.class);
		extendRequest(request);
		extendPageContext(pageContext);
		RequestContext requestContext = new JspAwareRequestContext(pageContext);
		pageContext.setAttribute(RequestContextAwareTag.REQUEST_CONTEXT_PAGE_ATTRIBUTE, requestContext);
		return pageContext;
	}

	protected void extendPageContext(MockPageContext pageContext) throws JspException {
	}

	protected void extendRequest(MockHttpServletRequest request) {
	}

	protected void onSetUp() {
	}

	protected MockPageContext getPageContext() {
		return this.pageContext;
	}

	protected Writer getWriter() {
		return this.writer;
	}

	protected String getOutput() {
		return this.writer.toString();
	}

	protected final RequestContext getRequestContext() {
		return (RequestContext) getPageContext().getAttribute(RequestContextAwareTag.REQUEST_CONTEXT_PAGE_ATTRIBUTE);
	}

	protected RequestDataValueProcessor getMockRequestDataValueProcessor() {
		RequestDataValueProcessor mockProcessor = mock(RequestDataValueProcessor.class);
		HttpServletRequest request = (HttpServletRequest) getPageContext().getRequest();
		WebApplicationContext wac = RequestContextUtils.findWebApplicationContext(request);
		wac.getBean(RequestDataValueProcessorWrapper.class).setRequestDataValueProcessor(mockProcessor);
		return mockProcessor;
	}

	protected void exposeBindingResult(Errors errors) {
		// wrap errors in a Model
		Map<String, Object> model = Collections.singletonMap(
				BindingResult.MODEL_KEY_PREFIX + COMMAND_NAME, errors);

		// replace the request context with one containing the errors
		MockPageContext pageContext = getPageContext();
		RequestContext context = new RequestContext((HttpServletRequest) pageContext.getRequest(), model);
		pageContext.setAttribute(RequestContextAwareTag.REQUEST_CONTEXT_PAGE_ATTRIBUTE, context);
	}

	protected final void assertContainsAttribute(String output, String attributeName, String attributeValue) {
		String attributeString = attributeName + "=\"" + attributeValue + "\"";
		assertThat(output.contains(attributeString)).as("Expected to find attribute '" + attributeName +
				"' with value '" + attributeValue +
				"' in output + '" + output + "'").isTrue();
	}

	protected final void assertAttributeNotPresent(String output, String attributeName) {
		boolean condition = !output.contains(attributeName + "=\"");
		assertThat(condition).as("Unexpected attribute '" + attributeName + "' in output '" + output + "'.").isTrue();
	}

	protected final void assertBlockTagContains(String output, String desiredContents) {
		String contents = output.substring(output.indexOf(">") + 1, output.lastIndexOf("<"));
		assertThat(contents.contains(desiredContents)).as("Expected to find '" + desiredContents + "' in the contents of block tag '" + output + "'").isTrue();
	}

}
