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

package org.springframework.test.web.servlet.setup;

import javax.servlet.ServletContext;

import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * The main class to import to access all available {@link MockMvcBuilder}s.
 *
 * <p><strong>Eclipse users:</strong> consider adding this class as a Java editor
 * favorite. To navigate, open the Preferences and type "favorites".
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class MockMvcBuilders {

	/**
	 * Build a {@link MockMvc} using the given, fully initialized, i.e.
	 * refreshed, {@link WebApplicationContext}. The {@link DispatcherServlet}
	 * will use the context to discover Spring MVC infrastructure and
	 * application controllers in it. The context must have been configured with
	 * a {@link ServletContext}.
	 */
	public static DefaultMockMvcBuilder<DefaultMockMvcBuilder<?>> webAppContextSetup(WebApplicationContext context) {
		return new DefaultMockMvcBuilder<DefaultMockMvcBuilder<?>>(context);
	}

	/**
	 * Build a {@link MockMvc} by registering one or more {@code @Controller}'s
	 * instances and configuring Spring MVC infrastructure programmatically.
	 * This allows full control over the instantiation and initialization of
	 * controllers, and their dependencies, similar to plain unit tests while
	 * also making it possible to test one controller at a time.
	 *
	 * <p>When this option is used, the minimum infrastructure required by the
	 * {@link DispatcherServlet} to serve requests with annotated controllers is
	 * automatically created, and can be customized, resulting in configuration
	 * that is equivalent to what the MVC Java configuration provides except
	 * using builder style methods.
	 *
	 * <p>If the Spring MVC configuration of an application is relatively
	 * straight-forward, for example when using the MVC namespace or the MVC
	 * Java config, then using this builder might be a good option for testing
	 * a majority of controllers. A much smaller number of tests can be used
	 * to focus on testing and verifying the actual Spring MVC configuration.
	 *
	 * @param controllers one or more {@link Controller @Controller}'s to test
	 */
	public static StandaloneMockMvcBuilder standaloneSetup(Object... controllers) {
		return new StandaloneMockMvcBuilder(controllers);
	}

}
