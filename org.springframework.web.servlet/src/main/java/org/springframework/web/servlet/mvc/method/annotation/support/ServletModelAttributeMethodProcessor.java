/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation.support;

import javax.servlet.ServletRequest;

import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.support.ModelAttributeMethodProcessor;

/**
 * A {@link ModelAttributeMethodProcessor} for Servlet environments.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ServletModelAttributeMethodProcessor extends ModelAttributeMethodProcessor {

	/**
	 * Creates a {@link ServletModelAttributeMethodProcessor} instance.
	 * @param resolveWithoutAnnotations enable default resolution mode in which parameters without
	 * 		annotations that aren't simple types (see {@link BeanUtils#isSimpleProperty(Class)})  
	 * 		are also treated as model attributes with a default name based on the model attribute type.
	 */
	public ServletModelAttributeMethodProcessor(boolean resolveWithoutAnnotations) {
		super(resolveWithoutAnnotations);
	}

	/**
	 * Expects the data binder to be an instance of {@link ServletRequestDataBinder}.
	 */
	@Override
	protected void doBind(WebDataBinder binder, NativeWebRequest request) {
		ServletRequest servletRequest = request.getNativeRequest(ServletRequest.class);
		((ServletRequestDataBinder) binder).bind(servletRequest);
	}

}