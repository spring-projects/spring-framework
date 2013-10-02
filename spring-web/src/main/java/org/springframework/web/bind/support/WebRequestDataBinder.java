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

package org.springframework.web.bind.support;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartRequest;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.IOException;

/**
 * Special {@link org.springframework.validation.DataBinder} to perform data binding
 * from web request parameters to JavaBeans, including support for multipart files.
 *
 * <p>See the DataBinder/WebDataBinder superclasses for customization options,
 * which include specifying allowed/required fields, and registering custom
 * property editors.
 *
 * <p>Can also used for manual data binding in custom web controllers or interceptors
 * that build on Spring's {@link org.springframework.web.context.request.WebRequest}
 * abstraction: e.g. in a {@link org.springframework.web.context.request.WebRequestInterceptor}
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
 * @since 2.5.2
 * @see #bind(org.springframework.web.context.request.WebRequest)
 * @see #registerCustomEditor
 * @see #setAllowedFields
 * @see #setRequiredFields
 * @see #setFieldMarkerPrefix
 */
public class WebRequestDataBinder extends WebDataBinder {

	private static final String MULTIPART_CONTENT_TYPE = "multipart";

	private static final String CONTENT_TYPE = "Content-Type";

	/**
	 * Create a new WebRequestDataBinder instance, with default object name.
	 * @param target the target object to bind onto (or {@code null}
	 * if the binder is just used to convert a plain parameter value)
	 * @see #DEFAULT_OBJECT_NAME
	 */
	public WebRequestDataBinder(Object target) {
		super(target);
	}

	/**
	 * Create a new WebRequestDataBinder instance.
	 * @param target the target object to bind onto (or {@code null}
	 * if the binder is just used to convert a plain parameter value)
	 * @param objectName the name of the target object
	 */
	public WebRequestDataBinder(Object target, String objectName) {
		super(target, objectName);
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
	 * <p>The type of the target property for a multipart file can be Part, MultipartFile,
	 * byte[], or String. The latter two receive the contents of the uploaded file;
	 * all metadata like original file name, content type, etc are lost in those cases.
	 * @param request request with parameters to bind (can be multipart)
	 * @see org.springframework.web.multipart.MultipartRequest
	 * @see org.springframework.web.multipart.MultipartFile
	 * @see javax.servlet.http.Part
	 * @see #bind(org.springframework.beans.PropertyValues)
	 */
	public void bind(WebRequest request) {
		MutablePropertyValues mpvs = new MutablePropertyValues(request.getParameterMap());

		if(isMultiPartNativeRequest(request)) {
			MultipartRequest multipartRequest = ((NativeWebRequest) request).getNativeRequest(MultipartRequest.class);
			if (multipartRequest != null) {
				bindMultipart(multipartRequest.getMultiFileMap(), mpvs);
			} else if (ClassUtils.hasMethod(HttpServletRequest.class, "getParts")) {
				HttpServletRequest multipartSerlvetRequest = ((NativeWebRequest) request)
						.getNativeRequest(HttpServletRequest.class);
				Servlet3MultiPartBinder binder = new Servlet3MultiPartBinder();
				binder.bindParts(multipartSerlvetRequest, mpvs);
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

	/**
	 * Check if the request given as parameter is a NativeWebRequest
	 * and a multipart request (by checking its Content-Type header).
	 * <p>If so, this request will be parsed to bind its multiple parts.
	 * @param request request with parameters to bind
	 * @see org.springframework.web.context.request.NativeWebRequest
	 */
	protected boolean isMultiPartNativeRequest(WebRequest request) {

		return (StringUtils.startsWithIgnoreCase(
				request.getHeader(CONTENT_TYPE),MULTIPART_CONTENT_TYPE)
				&& request instanceof NativeWebRequest);
	}

	/**
	 * Encapsulate Part binding code for Servlet 3.0+ only containers.
	 * @see javax.servlet.http.Part
	 */
	private class Servlet3MultiPartBinder {

		public void bindParts(HttpServletRequest request, MutablePropertyValues mpvs) {
			try {
				for(Part part : request.getParts()) {
					mpvs.add(part.getName(),part);
				}
			} catch (IOException ex) {
				throw new MultipartException("Could not parse multipart servlet request", ex);
			} catch(ServletException ex) {
				throw new MultipartException("Could not parse multipart servlet request", ex);
			}
		}

	}

}
