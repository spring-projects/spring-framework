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

package org.springframework.web.servlet.tags.form;

import java.nio.charset.UnsupportedCharsetException;
import java.util.Map;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.PageContext;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.PropertyAccessor;
import org.springframework.core.Conventions;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.RequestDataValueProcessor;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriUtils;

/**
 * The {@code <form>} tag renders an HTML 'form' tag and exposes a binding path to
 * inner tags for binding.
 *
 * <p>Users should place the form object into the
 * {@link org.springframework.web.servlet.ModelAndView ModelAndView} when
 * populating the data for their view. The name of this form object can be
 * configured using the {@link #setModelAttribute "modelAttribute"} property.
 *
 * <h3>Attribute Summary</h3>
 * <table>
 * <thead>
 * <tr>
 * <th class="table-header col-first">Attribute</th>
 * <th class="table-header col-second">Required?</th>
 * <th class="table-header col-second">Runtime Expression?</th>
 * <th class="table-header col-last">Description</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr class="even-row-color">
 * <td><p>acceptCharset</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>Specifies the list of character encodings for input data that is accepted
 * by the server processing this form. The value is a space- and/or comma-delimited
 * list of charset values. The client must interpret this list as an exclusive-or
 * list, i.e., the server is able to accept any single character encoding per
 * entity received.</p></td>
 * </tr>
 * <tr class="odd-row-color">
 * <td><p>action</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Required Attribute</p></td>
 * </tr>
 * <tr class="even-row-color">
 * <td><p>cssClass</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Optional Attribute</p></td>
 * </tr>
 * <tr class="odd-row-color">
 * <td><p>cssStyle</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Optional Attribute</p></td>
 * </tr>
 * <tr class="even-row-color">
 * <td><p>dir</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Standard Attribute</p></td>
 * </tr>
 * <tr class="odd-row-color">
 * <td><p>enctype</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Optional Attribute</p></td>
 * </tr>
 * <tr class="even-row-color">
 * <td><p>htmlEscape</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>Enable/disable HTML escaping of rendered values.</p></td>
 * </tr>
 * <tr class="odd-row-color">
 * <td><p>id</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Standard Attribute</p></td>
 * </tr>
 * <tr class="even-row-color">
 * <td><p>lang</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Standard Attribute</p></td>
 * </tr>
 * <tr class="odd-row-color">
 * <td><p>method</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Optional Attribute</p></td>
 * </tr>
 * <tr class="even-row-color">
 * <td><p>methodParam</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>The parameter name used for HTTP methods other then GET and POST.
 * Default is '_method'.</p></td>
 * </tr>
 * <tr class="odd-row-color">
 * <td><p>modelAttribute</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>Name of the model attribute under which the form object is exposed.
 * Defaults to 'command'.</p></td>
 * </tr>
 * <tr class="even-row-color">
 * <td><p>name</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Standard Attribute - added for backwards compatibility cases</p></td>
 * </tr>
 * <tr class="odd-row-color">
 * <td><p>onclick</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Event Attribute</p></td>
 * </tr>
 * <tr class="even-row-color">
 * <td><p>ondblclick</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Event Attribute</p></td>
 * </tr>
 * <tr class="odd-row-color">
 * <td><p>onkeydown</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Event Attribute</p></td>
 * </tr>
 * <tr class="even-row-color">
 * <td><p>onkeypress</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Event Attribute</p></td>
 * </tr>
 * <tr class="odd-row-color">
 * <td><p>onkeyup</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Event Attribute</p></td>
 * </tr>
 * <tr class="even-row-color">
 * <td><p>onmousedown</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Event Attribute</p></td>
 * </tr>
 * <tr class="odd-row-color">
 * <td><p>onmousemove</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Event Attribute</p></td>
 * </tr>
 * <tr class="even-row-color">
 * <td><p>onmouseout</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Event Attribute</p></td>
 * </tr>
 * <tr class="odd-row-color">
 * <td><p>onmouseover</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Event Attribute</p></td>
 * </tr>
 * <tr class="even-row-color">
 * <td><p>onmouseup</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Event Attribute</p></td>
 * </tr>
 * <tr class="odd-row-color">
 * <td><p>onreset</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Event Attribute</p></td>
 * </tr>
 * <tr class="even-row-color">
 * <td><p>onsubmit</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Event Attribute</p></td>
 * </tr>
 * <tr class="odd-row-color">
 * <td><p>servletRelativeAction</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>Action reference to be appended to the current servlet path</p></td>
 * </tr>
 * <tr class="even-row-color">
 * <td><p>target</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Optional Attribute</p></td>
 * </tr>
 * <tr class="odd-row-color">
 * <td><p>title</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Standard Attribute</p></td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Scott Andrews
 * @author Rossen Stoyanchev
 * @since 2.0
 */
@SuppressWarnings("serial")
public class FormTag extends AbstractHtmlElementTag {

	/** The default HTTP method using which form values are sent to the server: "post". */
	private static final String DEFAULT_METHOD = "post";

	/** The default attribute name: &quot;command&quot;. */
	public static final String DEFAULT_COMMAND_NAME = "command";

	/** The name of the '{@code modelAttribute}' setting. */
	private static final String MODEL_ATTRIBUTE = "modelAttribute";

	/**
	 * The name of the {@link jakarta.servlet.jsp.PageContext} attribute under which the
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


	private @Nullable TagWriter tagWriter;

	private String modelAttribute = DEFAULT_COMMAND_NAME;

	private @Nullable String name;

	private @Nullable String action;

	private @Nullable String servletRelativeAction;

	private String method = DEFAULT_METHOD;

	private @Nullable String target;

	private @Nullable String enctype;

	private @Nullable String acceptCharset;

	private @Nullable String onsubmit;

	private @Nullable String onreset;

	private @Nullable String autocomplete;

	private String methodParam = DEFAULT_METHOD_PARAM;

	/** Caching a previous nested path, so that it may be reset. */
	private @Nullable String previousNestedPath;


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
	protected @Nullable String getName() throws JspException {
		return this.name;
	}

	/**
	 * Set the value of the '{@code action}' attribute.
	 * <p>May be a runtime expression.
	 */
	public void setAction(@Nullable String action) {
		this.action = (action != null ? action : "");
	}

	/**
	 * Get the value of the '{@code action}' attribute.
	 */
	protected @Nullable String getAction() {
		return this.action;
	}

	/**
	 * Set the value of the '{@code action}' attribute through a value
	 * that is to be appended to the current servlet path.
	 * <p>May be a runtime expression.
	 * @since 3.2.3
	 */
	public void setServletRelativeAction(@Nullable String servletRelativeAction) {
		this.servletRelativeAction = servletRelativeAction;
	}

	/**
	 * Get the servlet-relative value of the '{@code action}' attribute.
	 * @since 3.2.3
	 */
	protected @Nullable String getServletRelativeAction() {
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
	public @Nullable String getTarget() {
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
	protected @Nullable String getEnctype() {
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
	protected @Nullable String getAcceptCharset() {
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
	protected @Nullable String getOnsubmit() {
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
	protected @Nullable String getOnreset() {
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
	protected @Nullable String getAutocomplete() {
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
	protected String getMethodParam() {
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
	 * the form object name in the {@link jakarta.servlet.jsp.PageContext}.
	 * @param tagWriter the {@link TagWriter} to which the form content is to be written
	 * @return {@link jakarta.servlet.jsp.tagext.Tag#EVAL_BODY_INCLUDE}
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
			catch (UnsupportedCharsetException ex) {
				// shouldn't happen - if it does, proceed with requestUri as-is
			}
			ServletResponse response = this.pageContext.getResponse();
			if (response instanceof HttpServletResponse httpServletResponse) {
				requestUri = httpServletResponse.encodeURL(requestUri);
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
		if (processor != null && request instanceof HttpServletRequest httpServletRequest) {
			action = processor.processAction(httpServletRequest, action, getHttpMethod());
		}
		return action;
	}

	/**
	 * Closes the '{@code form}' block tag and removes the form object name
	 * from the {@link jakarta.servlet.jsp.PageContext}.
	 */
	@Override
	public int doEndTag() throws JspException {
		RequestDataValueProcessor processor = getRequestContext().getRequestDataValueProcessor();
		ServletRequest request = this.pageContext.getRequest();
		if (processor != null && request instanceof HttpServletRequest httpServletRequest) {
			writeHiddenFields(processor.getExtraHiddenFields(httpServletRequest));
		}
		Assert.state(this.tagWriter != null, "No TagWriter set");
		this.tagWriter.endTag();
		return EVAL_PAGE;
	}

	/**
	 * Writes the given values as hidden fields.
	 */
	private void writeHiddenFields(@Nullable Map<String, String> hiddenFields) throws JspException {
		if (!CollectionUtils.isEmpty(hiddenFields)) {
			Assert.state(this.tagWriter != null, "No TagWriter set");
			this.tagWriter.appendValue("<div>\n");
			for (Map.Entry<String, String> entry : hiddenFields.entrySet()) {
				this.tagWriter.appendValue("<input type=\"hidden\" ");
				this.tagWriter.appendValue("name=\"" + entry.getKey() + "\" value=\"" + entry.getValue() + "\" ");
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
