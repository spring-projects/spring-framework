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

package org.springframework.web.servlet.support;

import javax.servlet.http.HttpServletRequest;

import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriBuilder;

/** @author Arjen Poutsma */
public class ServletUriBuilder extends UriBuilder {


	public static ServletUriBuilder fromCurrentServletRequest() {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		Assert.state(requestAttributes != null, "Could not find current RequestAttributes in RequestContextHolder");
		Assert.isInstanceOf(ServletRequestAttributes.class, requestAttributes);

		HttpServletRequest servletRequest = ((ServletRequestAttributes) requestAttributes).getRequest();
		Assert.state(servletRequest != null, "Could not find current HttpServletRequest in ServletRequestAttributes");
		return fromServletRequest(servletRequest);
	}

	public static ServletUriBuilder fromServletRequest(HttpServletRequest request) {
		Assert.notNull(request, "'request' must not be null");

		ServletUriBuilder builder = new ServletUriBuilder();
		builder.scheme(request.getScheme());
		builder.host(request.getServerName());
		builder.port(request.getServerPort());
		builder.path(request.getRequestURI());
//		builder.query(request.getQueryString());

		return builder;
	}

}
