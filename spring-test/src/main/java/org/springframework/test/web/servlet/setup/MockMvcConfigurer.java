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

import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.web.context.WebApplicationContext;

/**
 * A contract that allows the encapsulation of a "recipe" for configuring a
 * MockMvcBuilder for some specific purpose. For example a 3rd party library
 * may use this to provide convenient, easy ways to set up MockMvc.
 *
 * <p>Supported via {@link AbstractMockMvcBuilder#add(MockMvcConfigurer)}
 * with instances of class likely created via static methods, e.g.:
 *
 * <pre class="code">
 * 	MockMvcBuilders.webAppContextSetup(context)
 * 		.add(myLibrary("foo","bar").myProperty("foo"))
 * 		.build();
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public interface MockMvcConfigurer {


	/**
	 * Invoked immediately after a {@code MockMvcConfigurer} is configured via
	 * {@link AbstractMockMvcBuilder#add(MockMvcConfigurer)}.
	 */
	void afterConfigurerAdded(MockMvcBuilder mockMvcBuilder);

	/**
	 * Invoked just before the MockMvc instance is built providing access to the
	 * configured "default" RequestBuilder. If a "default" RequestBuilder is
	 * needed but was not configured and is {@code null}), it can still be added
	 * via {@link AbstractMockMvcBuilder#defaultRequest}.
	 */
	void beforeMockMvcCreated(MockMvcBuilder mockMvcBuilder, RequestBuilder defaultRequestBuilder,
			WebApplicationContext applicationContext);

}
