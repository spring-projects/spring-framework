/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.test.web.servlet.htmlunit;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link RequestPostProcessor} to update the request for a forwarded dispatch.
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @author Rossen Stoyanchev
 * @since 4.2
 */
final class ForwardRequestPostProcessor implements RequestPostProcessor {

	private final String forwardedUrl;


	public ForwardRequestPostProcessor(String forwardedUrl) {
		Assert.hasText(forwardedUrl, "Forwarded URL must not be null or empty");
		this.forwardedUrl = forwardedUrl;
	}

	@Override
	public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
		request.setRequestURI(this.forwardedUrl);
		request.setServletPath(initServletPath(request.getContextPath()));
		return request;
	}

	private String initServletPath(String contextPath) {
		if (StringUtils.hasText(contextPath)) {
			Assert.state(this.forwardedUrl.startsWith(contextPath), "Forward supported to same contextPath only");
			return (this.forwardedUrl.length() > contextPath.length() ?
					this.forwardedUrl.substring(contextPath.length()) : "");
		}
		else {
			return this.forwardedUrl;
		}
	}

}
