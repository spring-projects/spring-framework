/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.web.portlet.bind;

import javax.portlet.PortletRequest;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.validation.BindException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.multipart.MultipartRequest;

/**
 * Special {@link org.springframework.validation.DataBinder} to perform data binding
 * from portlet request parameters to JavaBeans, including support for multipart files.
 *
 * <p>See the DataBinder/WebDataBinder superclasses for customization options,
 * which include specifying allowed/required fields, and registering custom
 * property editors.
 *
 * <p>Used by Spring Portlet MVC's BaseCommandController.
 * Note that BaseCommandController and its subclasses allow for easy customization
 * of the binder instances that they use through overriding <code>initBinder</code>.
 *
 * <p>Can also be used for manual data binding in custom web controllers:
 * for example, in a plain Portlet Controller implementation. Simply instantiate
 * a PortletRequestDataBinder for each binding process, and invoke <code>bind</code>
 * with the current PortletRequest as argument:
 *
 * <pre class="code">
 * MyBean myBean = new MyBean();
 * // apply binder to custom target object
 * PortletRequestDataBinder binder = new PortletRequestDataBinder(myBean);
 * // register custom editors, if desired
 * binder.registerCustomEditor(...);
 * // trigger actual binding of request parameters
 * binder.bind(request);
 * // optionally evaluate binding errors
 * Errors errors = binder.getErrors();
 * ...</pre>
 *
 * @author Juergen Hoeller
 * @author John A. Lewis
 * @since 2.0
 * @see #bind(javax.portlet.PortletRequest)
 * @see #registerCustomEditor
 * @see #setAllowedFields
 * @see #setRequiredFields
 * @see #setFieldMarkerPrefix
 * @see org.springframework.web.portlet.mvc.BaseCommandController#initBinder
 */
public class PortletRequestDataBinder extends WebDataBinder {

	/**
	 * Create a new PortletRequestDataBinder instance, with default object name.
	 * @param target the target object to bind onto (or <code>null</code>
	 * if the binder is just used to convert a plain parameter value)
	 * @see #DEFAULT_OBJECT_NAME
	 */
	public PortletRequestDataBinder(Object target) {
		super(target);
	}

	/**
	 * Create a new PortletRequestDataBinder instance.
	 * @param target the target object to bind onto (or <code>null</code>
	 * if the binder is just used to convert a plain parameter value)
	 * @param objectName the name of the target object
	 */
	public PortletRequestDataBinder(Object target, String objectName) {
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
	 * <p>The type of the target property for a multipart file can be MultipartFile,
	 * byte[], or String. The latter two receive the contents of the uploaded file;
	 * all metadata like original file name, content type, etc are lost in those cases.
	 * @param request request with parameters to bind (can be multipart)
	 * @see org.springframework.web.portlet.multipart.MultipartActionRequest
	 * @see org.springframework.web.multipart.MultipartFile
	 * @see #bindMultipartFiles
	 * @see #bind(org.springframework.beans.PropertyValues)
	 */
	public void bind(PortletRequest request) {
		MutablePropertyValues mpvs = new PortletRequestParameterPropertyValues(request);
		if (request instanceof MultipartRequest) {
			MultipartRequest multipartRequest = (MultipartRequest) request;
			bindMultipart(multipartRequest.getMultiFileMap(), mpvs);
		}
		doBind(mpvs);
	}

	/**
	 * Treats errors as fatal.
	 * <p>Use this method only if it's an error if the input isn't valid.
	 * This might be appropriate if all input is from dropdowns, for example.
	 * @throws PortletRequestBindingException subclass of PortletException on any binding problem
	 */
	public void closeNoCatch() throws PortletRequestBindingException {
		if (getBindingResult().hasErrors()) {
			throw new PortletRequestBindingException(
					"Errors binding onto object '" + getBindingResult().getObjectName() + "'",
					new BindException(getBindingResult()));
		}
	}

}
