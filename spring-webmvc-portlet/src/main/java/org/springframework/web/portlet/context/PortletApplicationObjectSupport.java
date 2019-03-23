/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.portlet.context;

import java.io.File;
import javax.portlet.PortletContext;

import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.web.portlet.util.PortletUtils;

/**
 * Convenient superclass for application objects running in a Portlet ApplicationContext.
 * Provides getApplicationContext, getServletContext, and getTempDir methods.
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
public abstract class PortletApplicationObjectSupport extends ApplicationObjectSupport
		implements PortletContextAware {

	private PortletContext portletContext;


	@Override
	public void setPortletContext(PortletContext portletContext) {
		this.portletContext = portletContext;
	}


	/**
	 * Overrides the base class behavior to enforce running in an ApplicationContext.
	 * All accessors will throw IllegalStateException if not running in a context.
	 * @see #getApplicationContext()
	 * @see #getMessageSourceAccessor()
	 * @see #getPortletContext()
	 * @see #getTempDir()
	 */
	@Override
	protected boolean isContextRequired() {
		return true;
	}

	/**
	 * Return the current PortletContext.
	 * @throws IllegalStateException if not running within a PortletContext
	 */
	protected final PortletContext getPortletContext() throws IllegalStateException {
		if (this.portletContext == null) {
			throw new IllegalStateException(
					"PortletApplicationObjectSupport instance [" + this + "] does not run within a PortletContext");
		}
		return this.portletContext;
	}

	/**
	 * Return the temporary directory for the current web application,
	 * as provided by the servlet container.
	 * @return the File representing the temporary directory
	 * @throws IllegalStateException if not running within a PortletContext
	 * @see org.springframework.web.portlet.util.PortletUtils#getTempDir(javax.portlet.PortletContext)
	 */
	protected final File getTempDir() throws IllegalStateException {
		return PortletUtils.getTempDir(getPortletContext());
	}

}
