/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.htmlunit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.htmlunit.WebResponseData;
import org.htmlunit.util.NameValuePair;

import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Builder used internally to create {@link WebResponse WebResponses}.
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @author Rossen Stoyanchev
 * @since 4.2
 */
final class MockWebResponseBuilder {

	private static final String DEFAULT_STATUS_MESSAGE = "N/A";


	private final long startTime;

	private final WebRequest webRequest;

	private final MockHttpServletResponse response;


	public MockWebResponseBuilder(long startTime, WebRequest webRequest, MockHttpServletResponse response) {
		Assert.notNull(webRequest, "WebRequest must not be null");
		Assert.notNull(response, "HttpServletResponse must not be null");
		this.startTime = startTime;
		this.webRequest = webRequest;
		this.response = response;
	}


	public WebResponse build() throws IOException {
		WebResponseData webResponseData = webResponseData();
		long endTime = System.currentTimeMillis();
		return new WebResponse(webResponseData, this.webRequest, endTime - this.startTime);
	}

	private WebResponseData webResponseData() throws IOException {
		List<NameValuePair> responseHeaders = responseHeaders();
		int statusCode = (this.response.getRedirectedUrl() != null ?
				HttpStatus.MOVED_PERMANENTLY.value() : this.response.getStatus());
		String statusMessage = statusMessage(statusCode);
		return new WebResponseData(this.response.getContentAsByteArray(), statusCode, statusMessage, responseHeaders);
	}

	private String statusMessage(int statusCode) {
		String errorMessage = this.response.getErrorMessage();
		if (StringUtils.hasText(errorMessage)) {
			return errorMessage;
		}

		try {
			return HttpStatus.valueOf(statusCode).getReasonPhrase();
		}
		catch (IllegalArgumentException ex) {
			// ignore
		}

		return DEFAULT_STATUS_MESSAGE;
	}

	private List<NameValuePair> responseHeaders() {
		Collection<String> headerNames = this.response.getHeaderNames();
		List<NameValuePair> responseHeaders = new ArrayList<>(headerNames.size());
		for (String headerName : headerNames) {
			List<Object> headerValues = this.response.getHeaderValues(headerName);
			for (Object value : headerValues) {
				responseHeaders.add(new NameValuePair(headerName, String.valueOf(value)));
			}
		}
		String location = this.response.getRedirectedUrl();
		if (location != null) {
			responseHeaders.add(new NameValuePair("Location", location));
		}
		return responseHeaders;
	}

}
