/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.mock.web.portlet;

import java.io.IOException;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.PortletResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;

/**
 * Mock implementation of the {@link javax.portlet.PortletRequestDispatcher} interface.
 *
 * @author John A. Lewis
 * @author Juergen Hoeller
 * @since 2.0
 */
public class MockPortletRequestDispatcher implements PortletRequestDispatcher {

	private final Log logger = LogFactory.getLog(getClass());

	private final String url;


	/**
	 * Create a new MockPortletRequestDispatcher for the given URL.
	 * @param url the URL to dispatch to.
	 */
	public MockPortletRequestDispatcher(String url) {
		Assert.notNull(url, "URL must not be null");
		this.url = url;
	}


	@Override
	public void include(RenderRequest request, RenderResponse response) throws PortletException, IOException {
		include((PortletRequest) request, (PortletResponse) response);
	}

	@Override
	public void include(PortletRequest request, PortletResponse response) throws PortletException, IOException {
		Assert.notNull(request, "Request must not be null");
		Assert.notNull(response, "Response must not be null");
		if (!(response instanceof MockMimeResponse)) {
			throw new IllegalArgumentException("MockPortletRequestDispatcher requires MockMimeResponse");
		}
		((MockMimeResponse) response).setIncludedUrl(this.url);
		if (logger.isDebugEnabled()) {
			logger.debug("MockPortletRequestDispatcher: including URL [" + this.url + "]");
		}
	}

	@Override
	public void forward(PortletRequest request, PortletResponse response) throws PortletException, IOException {
		Assert.notNull(request, "Request must not be null");
		Assert.notNull(response, "Response must not be null");
		if (!(response instanceof MockMimeResponse)) {
			throw new IllegalArgumentException("MockPortletRequestDispatcher requires MockMimeResponse");
		}
		((MockMimeResponse) response).setForwardedUrl(this.url);
		if (logger.isDebugEnabled()) {
			logger.debug("MockPortletRequestDispatcher: forwarding to URL [" + this.url + "]");
		}
	}

}
