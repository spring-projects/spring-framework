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

package org.springframework.test.web.servlet.setup;

import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

/**
 * An concrete implementation of {@link AbstractMockMvcBuilder} that simply
 * provides the WebApplicationContext given to it as a constructor argument.
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @since 3.2
 */
public class DefaultMockMvcBuilder extends AbstractMockMvcBuilder<DefaultMockMvcBuilder> {

	private final WebApplicationContext webAppContext;


	/**
	 * Protected constructor. Not intended for direct instantiation.
	 * @see MockMvcBuilders#webAppContextSetup(WebApplicationContext)
	 */
	protected DefaultMockMvcBuilder(WebApplicationContext webAppContext) {
		Assert.notNull(webAppContext, "WebApplicationContext is required");
		Assert.notNull(webAppContext.getServletContext(), "WebApplicationContext must have a ServletContext");
		this.webAppContext = webAppContext;
	}


	@Override
	protected WebApplicationContext initWebAppContext() {
		return this.webAppContext;
	}

}
