/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.web.portlet.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import javax.portlet.ActionRequest;

/**
 * Simple wrapper for a Portlet {@link javax.portlet.ActionRequest},
 * delegating all calls to the underlying request.
 * 
 * <p>(In the style of the Servlet API's {@link javax.servlet.http.HttpServletRequestWrapper}.)
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see ActionRequestWrapper
 * @see javax.servlet.http.HttpServletRequestWrapper
 */
public class ActionRequestWrapper extends PortletRequestWrapper implements ActionRequest {

	/** Original request that we're delegating to */
	private final ActionRequest actionRequest;


	/**
	 * Create a ActionRequestWrapper for the given request.
	 * @param request the original request to wrap
	 * @throws IllegalArgumentException if the supplied <code>request</code> is <code>null</code>
	 */
	public ActionRequestWrapper(ActionRequest request) {
		super(request);
		this.actionRequest = request;
	}


	public InputStream getPortletInputStream() throws IOException {
		return this.actionRequest.getPortletInputStream();
	}

	public void setCharacterEncoding(String enc) throws UnsupportedEncodingException {
		this.actionRequest.setCharacterEncoding(enc);
	}

	public BufferedReader getReader() throws IOException {
		return this.actionRequest.getReader();
	}

	public String getCharacterEncoding() {
		return this.actionRequest.getCharacterEncoding();
	}

	public String getContentType() {
		return this.actionRequest.getContentType();
	}

	public int getContentLength() {
		return this.actionRequest.getContentLength();
	}

}
