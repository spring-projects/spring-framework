/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.web.servlet.setup;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.function.RouterFunction;

/**
 * The main class to import in order to access all available {@link MockMvcBuilder MockMvcBuilders}.
 *
 * <h3>Eclipse Users</h3>
 * <p>Consider adding this class as a Java editor favorite. To navigate to
 * this setting, open the Preferences and type "favorites".
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.2
 * @see #webAppContextSetup(WebApplicationContext)
 * @see #standaloneSetup(Object...)
 */
public final class MockMvcBuilders {

	private MockMvcBuilders() {
	}


	/**
	 * Build a {@link MockMvc} instance using the given, fully initialized
	 * (i.e., <em>refreshed</em>) {@link WebApplicationContext}.
	 * <p>The {@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet}
	 * will use the context to discover Spring MVC infrastructure and application
	 * controllers in it. The context must have been configured with a
	 * {@link jakarta.servlet.ServletContext ServletContext}.
	 */
	public static DefaultMockMvcBuilder webAppContextSetup(WebApplicationContext context) {
		return new DefaultMockMvcBuilder(context);
	}

	/**
	 * Build a {@link MockMvc} instance by registering one or more
	 * {@code @Controller} instances and configuring Spring MVC infrastructure
	 * programmatically.
	 * <p>This allows full control over the instantiation and initialization of
	 * controllers and their dependencies, similar to plain unit tests while
	 * also making it possible to test one controller at a time.
	 * <p>When this builder is used, the minimum infrastructure required by the
	 * {@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet}
	 * to serve requests with annotated controllers is created automatically
	 * and can be customized, resulting in configuration that is equivalent to
	 * what MVC Java configuration provides except using builder-style methods.
	 * <p>If the Spring MVC configuration of an application is relatively
	 * straight-forward &mdash; for example, when using the MVC namespace in
	 * XML or MVC Java config &mdash; then using this builder might be a good
	 * option for testing a majority of controllers. In such cases, a much
	 * smaller number of tests can be used to focus on testing and verifying
	 * the actual Spring MVC configuration.
	 * @param controllers one or more {@code @Controller} instances to test
	 * (specified {@code Class} will be turned into instance)
	 */
	public static StandaloneMockMvcBuilder standaloneSetup(Object... controllers) {
		return new StandaloneMockMvcBuilder(controllers);
	}

	/**
	 * Build a {@link MockMvc} instance by registering one or more
	 * {@link RouterFunction RouterFunction} instances and configuring Spring
	 * MVC infrastructure programmatically.
	 * <p>This allows full control over the instantiation and initialization of
	 * router functions and their dependencies, similar to plain unit tests while
	 * also making it possible to test one router function at a time.
	 * <p>When this builder is used, the minimum infrastructure required by the
	 * {@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet}
	 * to serve requests with router functions is created automatically
	 * and can be customized, resulting in configuration that is equivalent to
	 * what MVC Java configuration provides except using builder-style methods.
	 * @param routerFunctions one or more {@code RouterFunction} instances to test
	 * @since 6.2
	 */
	public static RouterFunctionMockMvcBuilder routerFunctions(RouterFunction<?>... routerFunctions) {
		return new RouterFunctionMockMvcBuilder(routerFunctions);
	}

}
