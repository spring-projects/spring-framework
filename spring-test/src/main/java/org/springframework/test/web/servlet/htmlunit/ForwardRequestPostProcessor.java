/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
