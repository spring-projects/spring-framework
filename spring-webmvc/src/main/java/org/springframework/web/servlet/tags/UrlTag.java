/*
 * Copyright 2002-2023 the original author or authors.
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
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.PageContext;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.RequestDataValueProcessor;
import org.springframework.web.util.JavaScriptUtils;
import org.springframework.web.util.TagUtils;
import org.springframework.web.util.UriUtils;

/**
 * The {@code <url>} tag creates URLs. Modeled after the JSTL {@code c:url} tag with
 * backwards compatibility in mind.
 *
 * <p>Enhancements to the JSTL functionality include:
 * <ul>
 * <li>URL encoded template URI variables</li>
 * <li>HTML/XML escaping of URLs</li>
 * <li>JavaScript escaping of URLs</li>
 * </ul>
 *
 * <p>Template URI variables are indicated in the {@link #setValue(String) 'value'}
 * attribute and marked by braces '{variableName}'. The braces and attribute name are
 * replaced by the URL encoded value of a parameter defined with the spring:param tag
 * in the body of the url tag. If no parameter is available the literal value is
 * passed through. Params matched to template variables will not be added to the query
 * string.
 *
 * <p>Use of the spring:param tag for URI template variables is strongly recommended
 * over direct EL substitution as the values are URL encoded. Failure to properly
 * encode URL can leave an application vulnerable to XSS and other injection attacks.
 *
 * <p>URLs can be HTML/XML escaped by setting the {@link #setHtmlEscape(boolean)
 * 'htmlEscape'} attribute to 'true'. Detects an HTML escaping setting, either on
 * this tag instance, the page level, or the {@code web.xml} level. The default
 * is 'false'. When setting the URL value into a variable, escaping is not recommended.
 *
 * <p>Example usage:
 * <pre class="code">&lt;spring:url value="/url/path/{variableName}"&gt;
 *   &lt;spring:param name="variableName" value="more than JSTL c:url" /&gt;
 * &lt;/spring:url&gt;</pre>
 *
 * <p>The above results in:
 * {@code /currentApplicationContext/url/path/more%20than%20JSTL%20c%3Aurl}
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
 * <td>value</td>
 * <td>true</td>
 * <td>true</td>
 * <td>The URL to build. This value can include template {placeholders} that are
 * replaced with the URL encoded value of the named parameter. Parameters
 * must be defined using the param tag inside the body of this tag.</td>
 * </tr>
 * <tr>
 * <td>context</td>
 * <td>false</td>
 * <td>true</td>
 * <td>Specifies a remote application context path.
 * The default is the current application context path.</td>
 * </tr>
 * <tr>
 * <td>var</td>
 * <td>false</td>
 * <td>true</td>
 * <td>The name of the variable to export the URL value to.
 * If not specified the URL is written as output.</td>
 * </tr>
 * <tr>
 * <td>scope</td>
 * <td>false</td>
 * <td>true</td>
 * <td>The scope for the var. 'application', 'session', 'request' and 'page'
 * scopes are supported. Defaults to page scope. This attribute has no
 * effect unless the var attribute is also defined.</td>
 * </tr>
 * <tr>
 * <td>htmlEscape</td>
 * <td>false</td>
 * <td>true</td>
 * <td>Set HTML escaping for this tag, as a boolean value. Overrides the
 * default HTML escaping setting for the current page.</td>
 * </tr>
 * <tr>
 * <td>javaScriptEscape</td>
 * <td>false</td>
 * <td>true</td>
 * <td>Set JavaScript escaping for this tag, as a boolean value.
 * Default is {@code false}.</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Scott Andrews
 * @since 3.0
 * @see ParamTag
 */
@SuppressWarnings("serial")
public class UrlTag extends HtmlEscapingAwareTag implements ParamAware {

	private static final String URL_TEMPLATE_DELIMITER_PREFIX = "{";

	private static final String URL_TEMPLATE_DELIMITER_SUFFIX = "}";

	private static final String URL_TYPE_ABSOLUTE = "://";


	private List<Param> params = Collections.emptyList();

	private Set<String> templateParams = Collections.emptySet();

	@Nullable
	private UrlType type;

	@Nullable
	private String value;

	@Nullable
	private String context;

	@Nullable
	private String var;

	private int scope = PageContext.PAGE_SCOPE;

	private boolean javaScriptEscape = false;


	/**
	 * Set the value of the URL.
	 */
	public void setValue(String value) {
		if (value.contains(URL_TYPE_ABSOLUTE)) {
			this.type = UrlType.ABSOLUTE;
			this.value = value;
		}
		else if (value.startsWith("/")) {
			this.type = UrlType.CONTEXT_RELATIVE;
			this.value = value;
		}
		else {
			this.type = UrlType.RELATIVE;
			this.value = value;
		}
	}

	/**
	 * Set the context path for the URL.
	 * Defaults to the current context.
	 */
	public void setContext(String context) {
		if (context.startsWith("/")) {
			this.context = context;
		}
		else {
			this.context = "/" + context;
		}
	}

	/**
	 * Set the variable name to expose the URL under. Defaults to rendering the
	 * URL to the current JspWriter
	 */
	public void setVar(String var) {
		this.var = var;
	}

	/**
	 * Set the scope to export the URL variable to. This attribute has no
	 * meaning unless var is also defined.
	 */
	public void setScope(String scope) {
		this.scope = TagUtils.getScope(scope);
	}

	/**
	 * Set JavaScript escaping for this tag, as boolean value.
	 * Default is "false".
	 */
	public void setJavaScriptEscape(boolean javaScriptEscape) throws JspException {
		this.javaScriptEscape = javaScriptEscape;
	}

	@Override
	public void addParam(Param param) {
		this.params.add(param);
	}


	@Override
	public int doStartTagInternal() throws JspException {
		this.params = new ArrayList<>();
		this.templateParams = new HashSet<>();
		return EVAL_BODY_INCLUDE;
	}

	@Override
	public int doEndTag() throws JspException {
		String url = createUrl();

		RequestDataValueProcessor processor = getRequestContext().getRequestDataValueProcessor();
		ServletRequest request = this.pageContext.getRequest();
		if ((processor != null) && (request instanceof HttpServletRequest httpServletRequest)) {
			url = processor.processUrl(httpServletRequest, url);
		}

		if (this.var == null) {
			// print the url to the writer
			try {
				this.pageContext.getOut().print(url);
			}
			catch (IOException ex) {
				throw new JspException(ex);
			}
		}
		else {
			// store the url as a variable
			this.pageContext.setAttribute(this.var, url, this.scope);
		}
		return EVAL_PAGE;
	}


	/**
	 * Build the URL for the tag from the tag attributes and parameters.
	 * @return the URL value as a String
	 */
	String createUrl() throws JspException {
		Assert.state(this.value != null, "No value set");
		HttpServletRequest request = (HttpServletRequest) this.pageContext.getRequest();
		HttpServletResponse response = (HttpServletResponse) this.pageContext.getResponse();

		StringBuilder url = new StringBuilder();
		if (this.type == UrlType.CONTEXT_RELATIVE) {
			// add application context to url
			if (this.context == null) {
				url.append(request.getContextPath());
			}
			else {
				if (this.context.endsWith("/")) {
					url.append(this.context, 0, this.context.length() - 1);
				}
				else {
					url.append(this.context);
				}
			}
		}
		if (this.type != UrlType.RELATIVE && this.type != UrlType.ABSOLUTE && !this.value.startsWith("/")) {
			url.append('/');
		}
		url.append(replaceUriTemplateParams(this.value, this.params, this.templateParams));
		url.append(createQueryString(this.params, this.templateParams, (url.indexOf("?") == -1)));

		String urlStr = url.toString();
		if (this.type != UrlType.ABSOLUTE) {
			// Add the session identifier if needed
			// (Do not embed the session identifier in a remote link!)
			urlStr = response.encodeURL(urlStr);
		}

		// HTML and/or JavaScript escape, if demanded.
		urlStr = htmlEscape(urlStr);
		urlStr = (this.javaScriptEscape ? JavaScriptUtils.javaScriptEscape(urlStr) : urlStr);

		return urlStr;
	}

	/**
	 * Build the query string from available parameters that have not already
	 * been applied as template params.
	 * <p>The names and values of parameters are URL encoded.
	 * @param params the parameters to build the query string from
	 * @param usedParams set of parameter names that have been applied as
	 * template params
	 * @param includeQueryStringDelimiter true if the query string should start
	 * with a '?' instead of '&amp;'
	 * @return the query string
	 */
	protected String createQueryString(List<Param> params, Set<String> usedParams, boolean includeQueryStringDelimiter)
			throws JspException {

		String encoding = this.pageContext.getResponse().getCharacterEncoding();
		StringBuilder qs = new StringBuilder();
		for (Param param : params) {
			if (!usedParams.contains(param.getName()) && StringUtils.hasLength(param.getName())) {
				if (includeQueryStringDelimiter && qs.length() == 0) {
					qs.append('?');
				}
				else {
					qs.append('&');
				}
				try {
					qs.append(UriUtils.encodeQueryParam(param.getName(), encoding));
					if (param.getValue() != null) {
						qs.append('=');
						qs.append(UriUtils.encodeQueryParam(param.getValue(), encoding));
					}
				}
				catch (UnsupportedCharsetException ex) {
					throw new JspException(ex);
				}
			}
		}
		return qs.toString();
	}

	/**
	 * Replace template markers in the URL matching available parameters. The
	 * name of matched parameters are added to the used parameters set.
	 * <p>Parameter values are URL encoded.
	 * @param uri the URL with template parameters to replace
	 * @param params parameters used to replace template markers
	 * @param usedParams set of template parameter names that have been replaced
	 * @return the URL with template parameters replaced
	 */
	protected String replaceUriTemplateParams(String uri, List<Param> params, Set<String> usedParams)
			throws JspException {

		String encoding = this.pageContext.getResponse().getCharacterEncoding();
		for (Param param : params) {
			String template = URL_TEMPLATE_DELIMITER_PREFIX + param.getName() + URL_TEMPLATE_DELIMITER_SUFFIX;
			if (uri.contains(template)) {
				usedParams.add(param.getName());
				String value = param.getValue();
				try {
					uri = StringUtils.replace(uri, template,
							(value != null ? UriUtils.encodePath(value, encoding) : ""));
				}
				catch (UnsupportedCharsetException ex) {
					throw new JspException(ex);
				}
			}
			else {
				template = URL_TEMPLATE_DELIMITER_PREFIX + '/' + param.getName() + URL_TEMPLATE_DELIMITER_SUFFIX;
				if (uri.contains(template)) {
					usedParams.add(param.getName());
					String value = param.getValue();
					try {
						uri = StringUtils.replace(uri, template,
								(value != null ? UriUtils.encodePathSegment(value, encoding) : ""));
					}
					catch (UnsupportedCharsetException ex) {
						throw new JspException(ex);
					}
				}
			}
		}
		return uri;
	}


	/**
	 * Internal enum that classifies URLs by type.
	 */
	private enum UrlType {

		CONTEXT_RELATIVE, RELATIVE, ABSOLUTE
	}

}
