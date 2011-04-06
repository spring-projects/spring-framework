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

import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.annotation.support.WebArgumentResolverAdapter;

/**
 * A Servlet-specific {@link WebArgumentResolverAdapter} that provides access to a {@link NativeWebRequest}.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ServletWebArgumentResolverAdapter extends WebArgumentResolverAdapter {

	public ServletWebArgumentResolverAdapter(WebArgumentResolver adaptee) {
		super(adaptee);
	}

	@Override
	protected NativeWebRequest getWebRequest() {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		if (requestAttributes instanceof ServletRequestAttributes) {
			ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
			return new ServletWebRequest(servletRequestAttributes.getRequest());
		}
		return null;
	}

}
