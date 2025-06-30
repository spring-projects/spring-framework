/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.servlet.view;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Dummy request context used for FreeMarker macro tests.
 *
 * @author Darren Davison
 * @author Juergen Hoeller
 * @since 25.01.2005
 * @see org.springframework.web.servlet.support.RequestContext
 */
public class DummyMacroRequestContext {

	private final HttpServletRequest request;

	private Map<String, String> messageMap;

	private String contextPath;


	public DummyMacroRequestContext(HttpServletRequest request) {
		this.request = request;
	}


	public void setMessageMap(Map<String, String> messageMap) {
		this.messageMap = messageMap;
	}


	/**
	 * @see org.springframework.web.servlet.support.RequestContext#getMessage(String)
	 */
	public String getMessage(String code) {
		return this.messageMap.get(code);
	}

	/**
	 * @see org.springframework.web.servlet.support.RequestContext#getMessage(String, String)
	 */
	public String getMessage(String code, String defaultMsg) {
		String msg = this.messageMap.get(code);
		return (msg != null ? msg : defaultMsg);
	}

	/**
	 * @see org.springframework.web.servlet.support.RequestContext#getMessage(String, List)
	 */
	public String getMessage(String code, List<?> args) {
		return this.messageMap.get(code) + args;
	}

	/**
	 * @see org.springframework.web.servlet.support.RequestContext#getMessage(String, List, String)
	 */
	public String getMessage(String code, List<?> args, String defaultMsg) {
		String msg = this.messageMap.get(code);
		return (msg != null ? msg + args : defaultMsg);
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	/**
	 * @see org.springframework.web.servlet.support.RequestContext#getContextPath()
	 */
	public String getContextPath() {
		return this.contextPath;
	}

	/**
	 * @see org.springframework.web.servlet.support.RequestContext#getContextUrl(String)
	 */
	public String getContextUrl(String relativeUrl) {
		return getContextPath() + relativeUrl;
	}

	/**
	 * @see org.springframework.web.servlet.support.RequestContext#getContextUrl(String, Map)
	 */
	public String getContextUrl(String relativeUrl, Map<String,String> params) {
		UriComponents uric = UriComponentsBuilder.fromUriString(relativeUrl).buildAndExpand(params);
		return getContextPath() + uric.toUri().toASCIIString();
	}

	/**
	 * @see org.springframework.web.servlet.support.RequestContext#getBindStatus(String)
	 */
	public BindStatus getBindStatus(String path) throws IllegalStateException {
		return getBindStatus(path, false);
	}

	/**
	 * @see org.springframework.web.servlet.support.RequestContext#getBindStatus(String, boolean)
	 */
	public BindStatus getBindStatus(String path, boolean htmlEscape) throws IllegalStateException {
		return new BindStatus(new RequestContext(this.request), path, htmlEscape);
	}

}
