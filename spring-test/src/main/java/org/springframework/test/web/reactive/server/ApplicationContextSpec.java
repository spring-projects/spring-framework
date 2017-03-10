/*
 * Copyright 2002-2017 the original author or authors.
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
package org.springframework.test.web.reactive.server;

import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

/**
 * Spec for setting up server-less testing by detecting components in an
 * {@link ApplicationContext}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class ApplicationContextSpec extends AbstractMockServerSpec<ApplicationContextSpec> {

	private final ApplicationContext applicationContext;


	ApplicationContextSpec(ApplicationContext applicationContext) {
		Assert.notNull(applicationContext, "ApplicationContext is required");
		this.applicationContext = applicationContext;
	}


	@Override
	protected WebHttpHandlerBuilder createHttpHandlerBuilder() {
		return WebHttpHandlerBuilder.applicationContext(this.applicationContext);
	}

}
