/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.servlet.mvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

/**
 * Controller that returns a specified status code and optionally redirect the request.
 *
 * @author Sebastien Deleuze
 * @since 4.1
 */
public class StatusController extends ParameterizableViewController {

	private final HttpStatus statusCode;

	private String reason;

	private String redirectPath;

	private boolean useQueryString = false;

	/**
	 * Create a new StatusController with the provided status code.
	 * @param statusCode the http response status code
	 */
	public StatusController(HttpStatus statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * Set the error message to display. It is not possible to set both viewName and reason,
	 * you must use either one or the other.
	 */
	public void setReason(String reason) {
		this.reason = reason;
	}

	/**
	 * Set the path where the request should be redirected for 3xx requests.
	 */
	public void setRedirectPath(String redirectPath) {
		this.redirectPath = redirectPath;
	}

	/**
	 * Set whether the initial request query string should be appended to the redirect path or not.
	 */
	public void setUseQueryString(boolean useQueryString) {
		this.useQueryString = useQueryString;
	}

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		Assert.state(!(this.reason !=null && this.getViewName() != null),
				"Can't set both viewName and reason, you must use either one or the other.");
		Assert.state(!(this.redirectPath != null && !this.statusCode.is3xxRedirection()),
				"Status code should be 3xx when redirect path is set.");

		if(statusCode.is3xxRedirection()) {
			if(this.useQueryString && request.getQueryString() != null) {
				response.addHeader(HttpHeaders.LOCATION, this.redirectPath + "?" + request.getQueryString());
			}
			else {
				response.addHeader(HttpHeaders.LOCATION, this.redirectPath);
			}
		}

		if(this.getViewName() == null) {
			if (!StringUtils.hasText(reason)) {
				response.setStatus(statusCode.value());
			}
			else {
				response.sendError(statusCode.value(), reason);
			}
			return null;
		}
		response.setStatus(statusCode.value());
		// to be picked up by the RedirectView
		request.setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, statusCode.value());
		return super.handleRequestInternal(request, response);
	}

}
