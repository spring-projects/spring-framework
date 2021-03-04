/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.context.WebApplicationContext;

/**
 * Contract for customizing a {@code ConfigurableMockMvcBuilder} in some
 * specific way, e.g. a 3rd party library that wants to provide shortcuts for
 * setting up a MockMvc.
 *
 * <p>An implementation of this interface can be plugged in via
 * {@link ConfigurableMockMvcBuilder#apply} with instances of this type likely
 * created via static methods, e.g.:
 *
 * <pre class="code">
 * import static org.example.ExampleSetup.mySetup;
 *
 * // ...
 *
 * MockMvcBuilders.webAppContextSetup(context).apply(mySetup("foo","bar")).build();
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 * @see org.springframework.test.web.servlet.setup.MockMvcConfigurerAdapter
 */
public interface MockMvcConfigurer {

	/**
	 * Invoked immediately when this {@code MockMvcConfigurer} is added via
	 * {@link ConfigurableMockMvcBuilder#apply}.
	 * @param builder the builder for the MockMvc
	 */
	default void afterConfigurerAdded(ConfigurableMockMvcBuilder<?> builder) {
	}

	/**
	 * Invoked when the MockMvc instance is about to be created with the MockMvc
	 * builder and the Spring WebApplicationContext that will be passed to the
	 * {@code DispatcherServlet}.
	 * @param builder the builder for the MockMvc
	 * @param context the Spring configuration
	 * @return a post processor to be applied to every request performed
	 * through the {@code MockMvc} instance.
	 */
	@Nullable
	default RequestPostProcessor beforeMockMvcCreated(
			ConfigurableMockMvcBuilder<?> builder, WebApplicationContext context) {

		return null;
	}

}
