/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.docs.web.webmvc.mvcservlet.mvccontainerconfig;

import jakarta.servlet.Filter;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import org.springframework.web.servlet.support.AbstractDispatcherServletInitializer;

// tag::snippet[]
public class MyFilterDispatcherServletInitializer extends AbstractDispatcherServletInitializer {

	@Override
	protected Filter[] getServletFilters() {
		return new Filter[] {
			new HiddenHttpMethodFilter(), new CharacterEncodingFilter() };
	}

	// @fold:on
	@Override
	protected WebApplicationContext createServletApplicationContext() {
		/**/return null;
	}

	@Override
	protected String[] getServletMappings() {
		/**/return new String[] { "/" };
	}

	@Override
	protected WebApplicationContext createRootApplicationContext() {
		/**/return null;
	}
	// @fold:off
}
// end::snippet[]
