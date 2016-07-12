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

import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.context.WebApplicationContext;


/**
 * Allows a sub-class to encapsulate logic for pre-configuring a
 * {@code ConfigurableMockMvcBuilder} for some specific purpose. A 3rd party
 * library may use this to provide shortcuts for setting up MockMvc.
 *
 * <p>Can be plugged in via {@link ConfigurableMockMvcBuilder#apply} with
 * instances of this type likely created via static methods, e.g.:
 *
 * <pre class="code">
 * 	MockMvcBuilders.webAppContextSetup(context).apply(mySetup("foo","bar")).build();
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 * @see org.springframework.test.web.servlet.setup.MockMvcConfigurerAdapter
 */
public interface MockMvcConfigurer {

	/**
	 * Invoked immediately after a {@code MockMvcConfigurer} is added via
	 * {@link ConfigurableMockMvcBuilder#apply}.
	 */
	default void afterConfigurerAdded(ConfigurableMockMvcBuilder<?> builder) {
	}

	/**
	 * Invoked just before the MockMvc instance is created. Implementations may
	 * return a RequestPostProcessor to be applied to every request performed
	 * through the created {@code MockMvc} instance.
	 */
	default RequestPostProcessor beforeMockMvcCreated(ConfigurableMockMvcBuilder<?> builder, WebApplicationContext cxt) {
		return null;
	}

}
