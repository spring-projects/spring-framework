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

package org.springframework.test.web.servlet;

import jakarta.servlet.ServletContext;

import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Builds a {@link MockHttpServletRequest}.
 *
 * <p>See static factory methods in
 * {@link org.springframework.test.web.servlet.request.MockMvcRequestBuilders MockMvcRequestBuilders}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public interface RequestBuilder {

	/**
	 * Build the request.
	 * @param servletContext the {@link ServletContext} to use to create the request
	 * @return the request
	 */
	MockHttpServletRequest buildRequest(ServletContext servletContext);

}
