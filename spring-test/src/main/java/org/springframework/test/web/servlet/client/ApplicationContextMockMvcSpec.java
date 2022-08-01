/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.test.web.servlet.client;

import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Simple wrapper around a {@link DefaultMockMvcBuilder}.
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 */
class ApplicationContextMockMvcSpec extends AbstractMockMvcServerSpec<ApplicationContextMockMvcSpec> {

	private final DefaultMockMvcBuilder mockMvcBuilder;


	public ApplicationContextMockMvcSpec(WebApplicationContext context) {
		this.mockMvcBuilder = MockMvcBuilders.webAppContextSetup(context);
	}

	@Override
	protected ConfigurableMockMvcBuilder<?> getMockMvcBuilder() {
		return this.mockMvcBuilder;
	}

}
