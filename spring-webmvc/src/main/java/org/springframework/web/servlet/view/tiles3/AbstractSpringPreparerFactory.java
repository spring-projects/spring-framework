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

package org.springframework.web.servlet.view.tiles3;

import org.apache.tiles.TilesException;
import org.apache.tiles.preparer.ViewPreparer;
import org.apache.tiles.preparer.factory.PreparerFactory;
import org.apache.tiles.request.Request;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Abstract implementation of the Tiles3 {@link org.apache.tiles.preparer.PreparerFactory}
 * interface, obtaining the current Spring WebApplicationContext and delegating to
 * {@link #getPreparer(String, org.springframework.web.context.WebApplicationContext)}.
 *
 * @author Juergen Hoeller
 * @since 3.2
 * @see #getPreparer(String, org.springframework.web.context.WebApplicationContext)
 * @see SimpleSpringPreparerFactory
 * @see SpringBeanPreparerFactory
 */
public abstract class AbstractSpringPreparerFactory implements PreparerFactory {

	@Override
	public ViewPreparer getPreparer(String name, Request context) {
		WebApplicationContext webApplicationContext = (WebApplicationContext) context.getContext("request").get(
				DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		if (webApplicationContext == null) {
			webApplicationContext = (WebApplicationContext) context.getContext("application").get(
					WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
			if (webApplicationContext == null) {
				throw new IllegalStateException("No WebApplicationContext found: no ContextLoaderListener registered?");
			}
		}
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
