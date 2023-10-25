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

package org.springframework.web.bind;

import java.lang.reflect.Array;
import java.util.List;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.multipart.support.StandardServletPartUtils;
import org.springframework.web.util.WebUtils;

/**
 * Special {@link org.springframework.validation.DataBinder} to perform data binding
 * from servlet request parameters to JavaBeans, including support for multipart files.
 *
 * <p><strong>WARNING</strong>: Data binding can lead to security issues by exposing
 * parts of the object graph that are not meant to be accessed or modified by
 * external clients. Therefore the design and use of data binding should be considered
 * carefully with regard to security. For more details, please refer to the dedicated
 * sections on data binding for
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-initbinder-model-design">Spring Web MVC</a> and
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-ann-initbinder-model-design">Spring WebFlux</a>
 * in the reference manual.
 *
 * <p>See the DataBinder/WebDataBinder superclasses for customization options,
 * which include specifying allowed/required fields, and registering custom
 * property editors.
 *
 * <p>Can also be used for manual data binding. Simply instantiate a
 * ServletRequestDataBinder for each binding process, and invoke {@code bind}
 * with the current ServletRequest as argument:
 *
 * <pre class="code">
 * MyBean myBean = new MyBean();
 * // apply binder to custom target object
 * ServletRequestDataBinder binder = new ServletRequestDataBinder(myBean);
 * // register custom editors, if desired
 * binder.registerCustomEditor(...);
 * // trigger actual binding of request parameters
 * binder.bind(request);
 * // optionally evaluate binding errors
 * Errors errors = binder.getErrors();
 * ...</pre>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #bind(jakarta.servlet.ServletRequest)
 * @see #registerCustomEditor
 * @see #setAllowedFields
 * @see #setRequiredFields
 * @see #setFieldMarkerPrefix
 */
public class ServletRequestDataBinder extends WebDataBinder {

	/**
	 * Create a new ServletRequestDataBinder instance, with default object name.
	 * @param target the target object to bind onto (or {@code null}
	 * if the binder is just used to convert a plain parameter value)
	 * @see #DEFAULT_OBJECT_NAME
	 */
	public ServletRequestDataBinder(@Nullable Object target) {
		super(target);
	}

	/**
	 * Create a new ServletRequestDataBinder instance.
	 * @param target the target object to bind onto (or {@code null}
	 * if the binder is just used to convert a plain parameter value)
	 * @param objectName the name of the target object
	 */
	public ServletRequestDataBinder(@Nullable Object target, String objectName) {
		super(target, objectName);
	}


	/**
	 * Use a default or single data constructor to create the target by
	 * binding request parameters, multipart files, or parts to constructor args.
	 * <p>After the call, use {@link #getBindingResult()} to check for bind errors.
	 * If there are none, the target is set, and {@link #bind(ServletRequest)}
	 * can be called for further initialization via setters.
	 * @param request the request to bind
	 * @since 6.1
	 */
	public void construct(ServletRequest request) {
		construct(createValueResolver(request));
	}

	/**
	 * Allow subclasses to create the {@link ValueResolver} instance to use.
	 * @since 6.1
	 */
	protected ServletRequestValueResolver createValueResolver(ServletRequest request) {
		return new ServletRequestValueResolver(request, this);
	}

	/**
	 * Bind the parameters of the given request to this binder's target,
	 * also binding multipart files in case of a multipart request.
	 * <p>This call can create field errors, representing basic binding
	 * errors like a required field (code "required"), or type mismatch
	 * between value and bean property (code "typeMismatch").
	 * <p>Multipart files are bound via their parameter name, just like normal
	 * HTTP parameters: i.e. "uploadedFile" to an "uploadedFile" bean property,
	 * invoking a "setUploadedFile" setter method.
	 * <p>The type of the target property for a multipart file can be MultipartFile,
	 * byte[], or String. Servlet Part binding is also supported when the
	 * request has not been parsed to MultipartRequest via MultipartResolver.
	 * @param request the request with parameters to bind (can be multipart)
	 * @see org.springframework.web.multipart.MultipartHttpServletRequest
	 * @see org.springframework.web.multipart.MultipartRequest
	 * @see org.springframework.web.multipart.MultipartFile
	 * @see jakarta.servlet.http.Part
	 * @see #bind(org.springframework.beans.PropertyValues)
	 */
	public void bind(ServletRequest request) {
		if (shouldNotBindPropertyValues()) {
			return;
		}
		MutablePropertyValues mpvs = new ServletRequestParameterPropertyValues(request);
		MultipartRequest multipartRequest = WebUtils.getNativeRequest(request, MultipartRequest.class);
		if (multipartRequest != null) {
			bindMultipart(multipartRequest.getMultiFileMap(), mpvs);
		}
		else if (isFormDataPost(request)) {
			HttpServletRequest httpServletRequest = WebUtils.getNativeRequest(request, HttpServletRequest.class);
			if (httpServletRequest != null && HttpMethod.POST.matches(httpServletRequest.getMethod())) {
				StandardServletPartUtils.bindParts(httpServletRequest, mpvs, isBindEmptyMultipartFiles());
			}
		}
		addBindValues(mpvs, request);
		doBind(mpvs);
	}

	private static boolean isFormDataPost(ServletRequest request) {
		return StringUtils.startsWithIgnoreCase(request.getContentType(), MediaType.MULTIPART_FORM_DATA_VALUE);
	}

	/**
	 * Extension point that subclasses can use to add extra bind values for a
	 * request. Invoked before {@link #doBind(MutablePropertyValues)}.
	 * The default implementation is empty.
	 * @param mpvs the property values that will be used for data binding
	 * @param request the current request
	 */
	protected void addBindValues(MutablePropertyValues mpvs, ServletRequest request) {
	}

	/**
	 * Treats errors as fatal.
	 * <p>Use this method only if it's an error if the input isn't valid.
	 * This might be appropriate if all input is from dropdowns, for example.
	 * @throws ServletRequestBindingException subclass of ServletException on any binding problem
	 */
	public void closeNoCatch() throws ServletRequestBindingException {
		if (getBindingResult().hasErrors()) {
			throw new ServletRequestBindingException(
					"Errors binding onto object '" + getBindingResult().getObjectName() + "'",
					new BindException(getBindingResult()));
		}
	}

	/**
	 * Return a {@code ServletRequest} {@link ValueResolver}. Mainly for use from
	 * {@link org.springframework.web.bind.support.WebRequestDataBinder}.
	 * @since 6.1
	 */
	public static ValueResolver valueResolver(ServletRequest request, WebDataBinder binder) {
		return new ServletRequestValueResolver(request, binder);
	}


	/**
	 * Resolver that looks up values to bind in a {@link ServletRequest}.
	 */
	protected static class ServletRequestValueResolver implements ValueResolver {

		private final ServletRequest request;

		private final WebDataBinder dataBinder;

		protected ServletRequestValueResolver(ServletRequest request, WebDataBinder dataBinder) {
			this.request = request;
			this.dataBinder = dataBinder;
		}

		protected ServletRequest getRequest() {
			return this.request;
		}

		@Nullable
		@Override
		public final Object resolveValue(String name, Class<?> paramType) {
			Object value = getRequestParameter(name, paramType);
			if (value == null) {
				value = this.dataBinder.resolvePrefixValue(name, paramType, this::getRequestParameter);
			}
			if (value == null) {
				value = getMultipartValue(name);
			}
			return value;
		}

		@Nullable
		protected Object getRequestParameter(String name, Class<?> type) {
			Object value = this.request.getParameterValues(name);
			return (ObjectUtils.isArray(value) && Array.getLength(value) == 1 ? Array.get(value, 0) : value);
		}

		@Nullable
		private Object getMultipartValue(String name) {
			MultipartRequest multipartRequest = WebUtils.getNativeRequest(this.request, MultipartRequest.class);
			if (multipartRequest != null) {
				List<MultipartFile> files = multipartRequest.getFiles(name);
				if (!files.isEmpty()) {
					return (files.size() == 1 ? files.get(0) : files);
				}
			}
			else if (isFormDataPost(this.request)) {
				HttpServletRequest httpRequest = WebUtils.getNativeRequest(this.request, HttpServletRequest.class);
				if (httpRequest != null && HttpMethod.POST.matches(httpRequest.getMethod())) {
					List<Part> parts = StandardServletPartUtils.getParts(httpRequest, name);
					if (!parts.isEmpty()) {
						return (parts.size() == 1 ? parts.get(0) : parts);
					}
				}
			}
			return null;
		}
	}

}
