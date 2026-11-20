/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.filter;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.util.Assert;
import org.springframework.web.util.WebUtils;

/**
 * A response wrapper used for the implementation of
 * {@link RelativeRedirectFilter} also shared with {@link ForwardedHeaderFilter}.
 *
 * @author Rossen Stoyanchev
 * @since 4.3.10
 */
final class RelativeRedirectResponseWrapper extends HttpServletResponseWrapper {

	private final HttpStatusCode redirectStatus;


	private RelativeRedirectResponseWrapper(HttpServletResponse response, HttpStatusCode redirectStatus) {
		super(response);
		Assert.notNull(redirectStatus, "'redirectStatus' is required");
		this.redirectStatus = redirectStatus;
	}


	@Override
	public void sendRedirect(String location) throws IOException {
		resetBuffer();
		setStatus(this.redirectStatus.value());
		setHeader(HttpHeaders.LOCATION, location);
		flushBuffer();
	}


	public static HttpServletResponse wrapIfNecessary(HttpServletResponse response,
			HttpStatusCode redirectStatus) {

		RelativeRedirectResponseWrapper wrapper =
				WebUtils.getNativeResponse(response, RelativeRedirectResponseWrapper.class);

		return (wrapper != null ? response :
				new RelativeRedirectResponseWrapper(response, redirectStatus));
	}

}
