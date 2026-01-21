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

package org.springframework.docs.web.webmvc.mvcservlet.mvcloggingsensitivedata;

import jakarta.servlet.ServletRegistration;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

// tag::snippet[]
public class MyInitializer
		extends AbstractAnnotationConfigDispatcherServletInitializer {

	// @fold:on
	@Override
	protected Class<?>[] getRootConfigClasses() {
		/**/throw new UnsupportedOperationException();
	}

	@Override
	protected Class<?>[] getServletConfigClasses() {
		/**/throw new UnsupportedOperationException();
	}

	@Override
	protected String[] getServletMappings() {
		/**/throw new UnsupportedOperationException();
	}

	// @fold:off
	@Override
	protected void customizeRegistration(ServletRegistration.Dynamic registration) {
		registration.setInitParameter("enableLoggingRequestDetails", "true");
	}

}
// end::snippet[]
