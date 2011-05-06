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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.support.ModelAttributeMethodProcessor;

/**
 * A Servlet-specific {@link ModelAttributeMethodProcessor} variant that casts the {@link WebDataBinder}
 * instance to {@link ServletRequestDataBinder} prior to invoking data binding.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ServletModelAttributeMethodProcessor extends ModelAttributeMethodProcessor {

	/**
	 * @param useDefaultResolution in default resolution mode a method argument that isn't a simple type, as
	 * defined in {@link BeanUtils#isSimpleProperty(Class)}, is treated as a model attribute even if it doesn't
	 * have an @{@link ModelAttribute} annotation with its name derived from the model attribute type.
	 */
	public ServletModelAttributeMethodProcessor(boolean useDefaultResolution) {
		super(useDefaultResolution);
	}

	/**
	 * {@inheritDoc}
	 * <p>This method downcasts the binder instance to {@link ServletRequestDataBinder} and invokes
	 * its bind method passing a {@link ServletRequest} to it.
	 */
	@Override
	protected void doBind(WebDataBinder binder, NativeWebRequest request) {
		ServletRequest servletRequest = request.getNativeRequest(ServletRequest.class);
		ServletRequestDataBinder servletBinder = (ServletRequestDataBinder) binder;
		servletBinder.bind(servletRequest);
	}

}