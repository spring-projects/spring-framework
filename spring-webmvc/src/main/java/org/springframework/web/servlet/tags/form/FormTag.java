/*
 * Copyright 2002-2015 the original author or authors.
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

import java.io.UnsupportedEncodingException;
import java.util.Map;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.springframework.beans.PropertyAccessor;
import org.springframework.core.Conventions;
import org.springframework.http.HttpMethod;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.RequestDataValueProcessor;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriUtils;

/**
 * Databinding-aware JSP tag for rendering an HTML '{@code form}' whose
 * inner elements are bound to properties on a <em>form object</em>.
 *
 * <p>Users should place the form object into the
 * {@link org.springframework.web.servlet.ModelAndView ModelAndView} when
 * populating the data for their view. The name of this form object can be
 * configured using the {@link #setModelAttribute "modelAttribute"} property.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Scott Andrews
 * @author Rossen Stoyanchev
 * @since 2.0
 */
@SuppressWarnings("serial")
public class FormTag extends AbstractHtmlElementTag {

	/** The default HTTP method using which form values are sent to the server: "post" */
	private static final String DEFAULT_METHOD = "post";

	/** The default attribute name: &quot;command&quot; */
	public static final String DEFAULT_COMMAND_NAME = "command";

	/** The name of the '{@code modelAttribute}' setting */
	private static final String MODEL_ATTRIBUTE = "modelAttribute";

	/**
	 * The name of the {@link javax.servlet.jsp.PageContext} attribute under which the
	 * form object name is exposed.
	 */
	public static final String MODEL_ATTRIBUTE_VARIABLE_NAME =
			Conventions.getQualifiedAttributeName(AbstractFormTag.class, MODEL_ATTRIBUTE);

	/** Default method parameter, i.e. {@code _method}. */
	private static final String DEFAULT_METHOD_PARAM = "_method";

	private static final String FORM_TAG = "form";

	private static final String INPUT_TAG = "input";

	private static final String ACTION_ATTRIBUTE = "action";

	private static final String METHOD_ATTRIBUTE = "method";

	private static final String TARGET_ATTRIBUTE = "target";

	private static final String ENCTYPE_ATTRIBUTE = "enctype";

	private static final String ACCEPT_CHARSET_ATTRIBUTE = "accept-charset";

	private static final String ONSUBMIT_ATTRIBUTE = "onsubmit";

	private static final String ONRESET_ATTRIBUTE = "onreset";

	private static final String AUTOCOMPLETE_ATTRIBUTE = "autocomplete";

	private static final String NAME_ATTRIBUTE = "name";

	private static final String VALUE_ATTRIBUTE = "value";

	private static final String TYPE_ATTRIBUTE = "type";


	private TagWriter tagWriter;

	private String modelAttribute = DEFAULT_COMMAND_NAME;

	private String name;

	private String action;

	private String servletRelativeAction;

	private String method = DEFAULT_METHOD;

	private String target;

	private String enctype;

	private String acceptCharset;

	private String onsubmit;

	private String onreset;

	private String autocomplete;

	private String methodParam = DEFAULT_METHOD_PARAM;

	/** Caching a previous nested path, so that it may be reset */
	private String previousNestedPath;


	/**
	 * Set the name of the form attribute in the model.
	 * <p>May be a runtime expression.
	 */
	public void setModelAttribute(String modelAttribute) {
		this.modelAttribute = modelAttribute;
	}

	/**
	 * Get the name of the form attribute in the model.
	 */
	protected String getModelAttribute() {
		return this.modelAttribute;
	}

	/**
	 * Set the name of the form attribute in the model.
	 * <p>May be a runtime expression.
	 * @see #setModelAttribute
	 */
	public void setCommandName(String commandName) {
		this.modelAttribute = commandName;
	}

	/**
	 * Get the name of the form attribute in the model.
	 * @see #getModelAttribute
	 */
	protected String getCommandName() {
		return this.modelAttribute;
	}

	/**
	 * Set the value of the '{@code name}' attribute.
	 * <p>May be a runtime expression.
	 * <p>Name is not a valid attribute for form on XHTML 1.0. However,
	 * it is sometimes needed for backward compatibility.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get the value of the '{@code name}' attribute.
	 */
	@Override
	protected String getName() throws JspException {
		return this.name;
	}

	/**
	 * Set the value of the '{@code action}' attribute.
	 * <p>May be a runtime expression.
	 */
	public void setAction(String action) {
		this.action = (action != null ? action : "");
	}

	/**
	 * Get the value of the '{@code action}' attribute.
	 */
	protected String getAction() {
		return this.action;
	}

	/**
	 * Set the value of the '{@code action}' attribute through a value
	 * that is to be appended to the current servlet path.
	 * <p>May be a runtime expression.
	 * @since 3.2.3
	 */
	public void setServletRelativeAction(String servletRelativeAction) {
		this.servletRelativeAction = (servletRelativeAction != null ? servletRelativeAction : "");
	}

	/**
	 * Get the servlet-relative value of the '{@code action}' attribute.
	 * @since 3.2.3
	 */
	protected String getServletRelativeAction() {
		return this.servletRelativeAction;
	}

	/**
	 * Set the value of the '{@code method}' attribute.
	 * <p>May be a runtime expression.
	 */
	public void setMethod(String method) {
		this.method = method;
	}

	/**
	 * Get the value of the '{@code method}' attribute.
	 */
	protected String getMethod() {
		return this.method;
	}

	/**
	 * Set the value of the '{@code target}' attribute.
	 * <p>May be a runtime expression.
	 */
	public void setTarget(String target) {
		this.target = target;
	}

	/**
	 * Get the value of the '{@code target}' attribute.
	 */
	public String getTarget() {
		return this.target;
	}

	/**
	 * Set the value of the '{@code enctype}' attribute.
	 * <p>May be a runtime expression.
	 */
	public void setEnctype(String enctype) {
		this.enctype = enctype;
	}

	/**
	 * Get the value of the '{@code enctype}' attribute.
	 */
	protected String getEnctype() {
		return this.enctype;
	}

	/**
	 * Set the value of the '{@code acceptCharset}' attribute.
	 * <p>May be a runtime expression.
	 */
	public void setAcceptCharset(String acceptCharset) {
		this.acceptCharset = acceptCharset;
	}

	/**
	 * Get the value of the '{@code acceptCharset}' attribute.
	 */
	protected String getAcceptCharset() {
		return this.acceptCharset;
	}

	/**
	 * Set the value of the '{@code onsubmit}' attribute.
	 * <p>May be a runtime expression.
	 */
	public void setOnsubmit(String onsubmit) {
		this.onsubmit = onsubmit;
	}

	/**
	 * Get the value of the '{@code onsubmit}' attribute.
	 */
	protected String getOnsubmit() {
		return this.onsubmit;
	}

	/**
	 * Set the value of the '{@code onreset}' attribute.
	 * <p>May be a runtime expression.
	 */
	public void setOnreset(String onreset) {
		this.onreset = onreset;
	}

	/**
	 * Get the value of the '{@code onreset}' attribute.
	 */
	protected String getOnreset() {
		return this.onreset;
	}

	/**
	 * Set the value of the '{@code autocomplete}' attribute.
	 * May be a runtime expression.
	 */
	public void setAutocomplete(String autocomplete) {
		this.autocomplete = autocomplete;
	}

	/**
	 * Get the value of the '{@code autocomplete}' attribute.
	 */
	protected String getAutocomplete() {
		return this.autocomplete;
	}

	/**
	 * Set the name of the request param for non-browser supported HTTP methods.
	 */
	public void setMethodParam(String methodParam) {
		this.methodParam = methodParam;
	}

	/**
	 * Get the name of the request param for non-browser supported HTTP methods.
	 * @since 4.2.3
	 */
	@SuppressWarnings("deprecation")
	protected String getMethodParam() {
		return getMethodParameter();
	}

	/**
	 * Get the name of the request param for non-browser supported HTTP methods.
	 * @deprecated as of 4.2.3, in favor of {@link #getMethodParam()} which is
	 * a proper pairing for {@link #setMethodParam(String)}
	 */
	@Deprecated
	protected String getMethodParameter() {
		return this.methodParam;
	}

	/**
	 * Determine if the HTTP method is supported by browsers (i.e. GET or POST).
	 */
	protected boolean isMethodBrowserSupported(String method) {
		return ("get".equalsIgnoreCase(method) || "post".equalsIgnoreCase(method));
	}


	/**
	 * Writes the opening part of the block	'{@code form}' tag and exposes
	 * the form object name in the {@link javax.servlet.jsp.PageContext}.
	 * @param tagWriter the {@link TagWriter} to which the form content is to be written
	 * @return {@link javax.servlet.jsp.tagext.Tag#EVAL_BODY_INCLUDE}
	 */
	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		this.tagWriter = tagWriter;

		tagWriter.startTag(FORM_TAG);
		writeDefaultAttributes(tagWriter);
		tagWriter.writeAttribute(ACTION_ATTRIBUTE, resolveAction());
		writeOptionalAttribute(tagWriter, METHOD_ATTRIBUTE, getHttpMethod());
		writeOptionalAttribute(tagWriter, TARGET_ATTRIBUTE, getTarget());
		writeOptionalAttribute(tagWriter, ENCTYPE_ATTRIBUTE, getEnctype());
		writeOptionalAttribute(tagWriter, ACCEPT_CHARSET_ATTRIBUTE, getAcceptCharset());
		writeOptionalAttribute(tagWriter, ONSUBMIT_ATTRIBUTE, getOnsubmit());
		writeOptionalAttribute(tagWriter, ONRESET_ATTRIBUTE, getOnreset());
		writeOptionalAttribute(tagWriter, AUTOCOMPLETE_ATTRIBUTE, getAutocomplete());

		tagWriter.forceBlock();

		if (!isMethodBrowserSupported(getMethod())) {
			assertHttpMethod(getMethod());
			String inputName = getMethodParam();
			String inputType = "hidden";
			tagWriter.startTag(INPUT_TAG);
			writeOptionalAttribute(tagWriter, TYPE_ATTRIBUTE, inputType);
			writeOptionalAttribute(tagWriter, NAME_ATTRIBUTE, inputName);
			writeOptionalAttribute(tagWriter, VALUE_ATTRIBUTE, processFieldValue(inputName, getMethod(), inputType));
			tagWriter.endTag();
		}

		// Expose the form object name for nested tags...
		String modelAttribute = resolveModelAttribute();
		this.pageContext.setAttribute(MODEL_ATTRIBUTE_VARIABLE_NAME, modelAttribute, PageContext.REQUEST_SCOPE);

		// Save previous nestedPath value, build and expose current nestedPath value.
		// Use request scope to expose nestedPath to included pages too.
		this.previousNestedPath =
				(String) this.pageContext.getAttribute(NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		this.pageContext.setAttribute(NESTED_PATH_VARIABLE_NAME,
				modelAttribute + PropertyAccessor.NESTED_PROPERTY_SEPARATOR, PageContext.REQUEST_SCOPE);

		return EVAL_BODY_INCLUDE;
	}

	private String getHttpMethod() {
		return (isMethodBrowserSupported(getMethod()) ? getMethod() : DEFAULT_METHOD);
	}

	private void assertHttpMethod(String method) {
		for (HttpMethod httpMethod : HttpMethod.values()) {
			if (httpMethod.name().equalsIgnoreCase(method)) {
				return;
			}
		}
		throw new IllegalArgumentException("Invalid HTTP method: " + method);
	}

	/**
	 * Autogenerated IDs correspond to the form object name.
	 */
	@Override
	protected String autogenerateId() throws JspException {
		return resolveModelAttribute();
	}

	/**
	 * {@link #evaluate Resolves} and returns the name of the form object.
	 * @throws IllegalArgumentException if the form object resolves to {@code null}
	 */
	protected String resolveModelAttribute() throws JspException {
		Object resolvedModelAttribute = evaluate(MODEL_ATTRIBUTE, getModelAttribute());
		if (resolvedModelAttribute == null) {
			throw new IllegalArgumentException(MODEL_ATTRIBUTE + " must not be null");
		}
		return (String) resolvedModelAttribute;
	}

	/**
	 * Resolve the value of the '{@code action}' attribute.
	 * <p>If the user configured an '{@code action}' value then the result of
	 * evaluating this value is used. If the user configured an
	 * '{@code servletRelativeAction}' value then the value is prepended
	 * with the context and servlet paths, and the result is used. Otherwise, the
	 * {@link org.springframework.web.servlet.support.RequestContext#getRequestUri()
	 * originating URI} is used.
	 * @return the value that is to be used for the '{@code action}' attribute
	 */
	protected String resolveAction() throws JspException {
		String action = getAction();
		String servletRelativeAction = getServletRelativeAction();
		if (StringUtils.hasText(action)) {
			action = getDisplayString(evaluate(ACTION_ATTRIBUTE, action));
			return processAction(action);
		}
		else if (StringUtils.hasText(servletRelativeAction)) {
			String pathToServlet = getRequestContext().getPathToServlet();
			if (servletRelativeAction.startsWith("/") &&
					!servletRelativeAction.startsWith(getRequestContext().getContextPath())) {
				servletRelativeAction = pathToServlet + servletRelativeAction;
			}
			servletRelativeAction = getDisplayString(evaluate(ACTION_ATTRIBUTE, servletRelativeAction));
			return processAction(servletRelativeAction);
		}
		else {
			String requestUri = getRequestContext().getRequestUri();
			String encoding = this.pageContext.getResponse().getCharacterEncoding();
			try {
				requestUri = UriUtils.encodePath(requestUri, encoding);
			}
			catch (UnsupportedEncodingException ex) {
				// shouldn't happen - if it does, proceed with requestUri as-is
			}
			ServletResponse response = this.pageContext.getResponse();
			if (response instanceof HttpServletResponse) {
				requestUri = ((HttpServletResponse) response).encodeURL(requestUri);
				String queryString = getRequestContext().getQueryString();
				if (StringUtils.hasText(queryString)) {
					requestUri += "?" + HtmlUtils.htmlEscape(queryString);
				}
			}
			if (StringUtils.hasText(requestUri)) {
				return processAction(requestUri);
			}
			else {
				throw new IllegalArgumentException("Attribute 'action' is required. " +
						"Attempted to resolve against current request URI but request URI was null.");
			}
		}
	}

	/**
	 * Process the action through a {@link RequestDataValueProcessor} instance
	 * if one is configured or otherwise returns the action unmodified.
	 */
	private String processAction(String action) {
		RequestDataValueProcessor processor = getRequestContext().getRequestDataValueProcessor();
		ServletRequest request = this.pageContext.getRequest();
		if (processor != null && request instanceof HttpServletRequest) {
			action = processor.processAction((HttpServletRequest) request, action, getHttpMethod());
		}
		return action;
	}

	/**
	 * Closes the '{@code form}' block tag and removes the form object name
	 * from the {@link javax.servlet.jsp.PageContext}.
	 */
	@Override
	public int doEndTag() throws JspException {
		RequestDataValueProcessor processor = getRequestContext().getRequestDataValueProcessor();
		ServletRequest request = this.pageContext.getRequest();
		if ((processor != null) && (request instanceof HttpServletRequest)) {
			writeHiddenFields(processor.getExtraHiddenFields((HttpServletRequest) request));
		}
		this.tagWriter.endTag();
		return EVAL_PAGE;
	}

	/**
	 * Writes the given values as hidden fields.
	 */
	private void writeHiddenFields(Map<String, String> hiddenFields) throws JspException {
		if (hiddenFields != null) {
			this.tagWriter.appendValue("<div>\n");
			for (String name : hiddenFields.keySet()) {
				this.tagWriter.appendValue("<input type=\"hidden\" ");
				this.tagWriter.appendValue("name=\"" + name + "\" value=\"" + hiddenFields.get(name) + "\" ");
				this.tagWriter.appendValue("/>\n");
			}
			this.tagWriter.appendValue("</div>");
		}
	}

	/**
	 * Clears the stored {@link TagWriter}.
	 */
	@Override
	public void doFinally() {
		super.doFinally();
		this.pageContext.removeAttribute(MODEL_ATTRIBUTE_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		if (this.previousNestedPath != null) {
			// Expose previous nestedPath value.
			this.pageContext.setAttribute(NESTED_PATH_VARIABLE_NAME, this.previousNestedPath, PageContext.REQUEST_SCOPE);
		}
		else {
			// Remove exposed nestedPath value.
			this.pageContext.removeAttribute(NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		}
		this.tagWriter = null;
		this.previousNestedPath = null;
	}


	/**
	 * Override resolve CSS class since error class is not supported.
	 */
	@Override
	protected String resolveCssClass() throws JspException {
		return ObjectUtils.getDisplayString(evaluate("cssClass", getCssClass()));
	}

	/**
	 * Unsupported for forms.
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void setPath(String path) {
		throw new UnsupportedOperationException("The 'path' attribute is not supported for forms");
	}

	/**
	 * Unsupported for forms.
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void setCssErrorClass(String cssErrorClass) {
		throw new UnsupportedOperationException("The 'cssErrorClass' attribute is not supported for forms");
	}

}
