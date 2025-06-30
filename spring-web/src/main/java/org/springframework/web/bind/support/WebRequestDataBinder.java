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

package org.springframework.web.bind.support;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.multipart.support.StandardServletPartUtils;

/**
 * Special {@link org.springframework.validation.DataBinder} to perform data binding
 * from web request parameters to JavaBeans, including support for multipart files.
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
 * <p>Can also used for manual data binding in custom web controllers or interceptors
 * that build on Spring's {@link org.springframework.web.context.request.WebRequest}
 * abstraction: for example, in a {@link org.springframework.web.context.request.WebRequestInterceptor}
 * implementation. Simply instantiate a WebRequestDataBinder for each binding
 * process, and invoke {@code bind} with the current WebRequest as argument:
 *
 * <pre class="code">
 * MyBean myBean = new MyBean();
 * // apply binder to custom target object
 * WebRequestDataBinder binder = new WebRequestDataBinder(myBean);
 * // register custom editors, if desired
 * binder.registerCustomEditor(...);
 * // trigger actual binding of request parameters
 * binder.bind(request);
 * // optionally evaluate binding errors
 * Errors errors = binder.getErrors();
 * ...</pre>
 *
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @since 2.5.2
 * @see #bind(org.springframework.web.context.request.WebRequest)
 * @see #registerCustomEditor
 * @see #setAllowedFields
 * @see #setRequiredFields
 * @see #setFieldMarkerPrefix
 */
public class WebRequestDataBinder extends WebDataBinder {

	/**
	 * Create a new WebRequestDataBinder instance, with default object name.
	 * @param target the target object to bind onto (or {@code null}
	 * if the binder is just used to convert a plain parameter value)
	 * @see #DEFAULT_OBJECT_NAME
	 */
	public WebRequestDataBinder(@Nullable Object target) {
		super(target);
	}

	/**
	 * Create a new WebRequestDataBinder instance.
	 * @param target the target object to bind onto (or {@code null}
	 * if the binder is just used to convert a plain parameter value)
	 * @param objectName the name of the target object
	 */
	public WebRequestDataBinder(@Nullable Object target, String objectName) {
		super(target, objectName);
	}


	/**
	 * Use a default or single data constructor to create the target by
	 * binding request parameters, multipart files, or parts to constructor args.
	 * <p>After the call, use {@link #getBindingResult()} to check for bind errors.
	 * If there are none, the target is set, and {@link #bind(WebRequest)}
	 * can be called for further initialization via setters.
	 * @param request the request to bind
	 * @since 6.1
	 */
	public void construct(WebRequest request) {
		if (request instanceof NativeWebRequest nativeRequest) {
			ServletRequest servletRequest = nativeRequest.getNativeRequest(ServletRequest.class);
			if (servletRequest != null) {
				construct(ServletRequestDataBinder.valueResolver(servletRequest, this));
			}
		}
	}

	@Override
	protected boolean shouldConstructArgument(MethodParameter param) {
		Class<?> type = param.nestedIfOptional().getNestedParameterType();
		return (super.shouldConstructArgument(param) &&
				!MultipartFile.class.isAssignableFrom(type) && !Part.class.isAssignableFrom(type));
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
	 * @see org.springframework.web.multipart.MultipartRequest
	 * @see org.springframework.web.multipart.MultipartFile
	 * @see jakarta.servlet.http.Part
	 * @see #bind(org.springframework.beans.PropertyValues)
	 */
	public void bind(WebRequest request) {
		if (shouldNotBindPropertyValues()) {
			return;
		}
		MutablePropertyValues mpvs = new MutablePropertyValues(request.getParameterMap());
		if (request instanceof NativeWebRequest nativeRequest) {
			MultipartRequest multipartRequest = nativeRequest.getNativeRequest(MultipartRequest.class);
			if (multipartRequest != null) {
				bindMultipart(multipartRequest.getMultiFileMap(), mpvs);
			}
			else if (StringUtils.startsWithIgnoreCase(
					request.getHeader(HttpHeaders.CONTENT_TYPE), MediaType.MULTIPART_FORM_DATA_VALUE)) {
				HttpServletRequest servletRequest = nativeRequest.getNativeRequest(HttpServletRequest.class);
				if (servletRequest != null && HttpMethod.POST.matches(servletRequest.getMethod())) {
					StandardServletPartUtils.bindParts(servletRequest, mpvs, isBindEmptyMultipartFiles());
				}
			}
		}
		doBind(mpvs);
	}

	/**
	 * Treats errors as fatal.
	 * <p>Use this method only if it's an error if the input isn't valid.
	 * This might be appropriate if all input is from dropdowns, for example.
	 * @throws BindException if binding errors have been encountered
	 */
	public void closeNoCatch() throws BindException {
		if (getBindingResult().hasErrors()) {
			throw new BindException(getBindingResult());
		}
	}

}
