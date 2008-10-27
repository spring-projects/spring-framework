/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.web.servlet.view.tiles2;

import javax.servlet.ServletRequest;

import org.apache.tiles.TilesException;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.PreparerFactory;
import org.apache.tiles.preparer.ViewPreparer;
import org.apache.tiles.servlet.context.ServletTilesApplicationContext;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Abstract implementation of the Tiles2 {@link org.apache.tiles.preparer.PreparerFactory}
 * interface, obtaining the current Spring WebApplicationContext and delegating to
 * {@link #getPreparer(String, org.springframework.web.context.WebApplicationContext)}.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see #getPreparer(String, org.springframework.web.context.WebApplicationContext)
 * @see SimpleSpringPreparerFactory
 * @see SpringBeanPreparerFactory
 */
public abstract class AbstractSpringPreparerFactory implements PreparerFactory {

	public ViewPreparer getPreparer(String name, TilesRequestContext context) throws TilesException {
		ServletRequest servletRequest = null;
		if (context.getRequest() instanceof ServletRequest) {
			servletRequest = (ServletRequest) context.getRequest();
		}
		ServletTilesApplicationContext tilesApplicationContext = null;
		if (context instanceof ServletTilesApplicationContext) {
			tilesApplicationContext = (ServletTilesApplicationContext) context;
		}
		if (servletRequest == null && tilesApplicationContext == null) {
			throw new IllegalStateException("SpringBeanPreparerFactory requires either a " +
					"ServletRequest or a ServletTilesApplicationContext to operate on");
		}
		WebApplicationContext webApplicationContext = RequestContextUtils.getWebApplicationContext(
				servletRequest, tilesApplicationContext.getServletContext());
		return getPreparer(name, webApplicationContext);
	}

	/**
	 * Obtain a preparer instance for the given preparer name,
	 * based on the given Spring WebApplicationContext.
	 * @param name the name of the preparer
	 * @param context the current Spring WebApplicationContext
	 * @return the preparer instance
	 * @throws TilesException in case of failure
	 */
	protected abstract ViewPreparer getPreparer(String name, WebApplicationContext context) throws TilesException;

}
