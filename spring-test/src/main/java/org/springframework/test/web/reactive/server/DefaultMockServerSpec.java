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

import org.springframework.util.Assert;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

/**
 * Simple extension of {@link AbstractMockServerSpec} that is given a target
 * {@link WebHandler}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultMockServerSpec extends AbstractMockServerSpec<DefaultMockServerSpec> {

	private final WebHandler webHandler;


	DefaultMockServerSpec(WebHandler webHandler) {
		Assert.notNull(webHandler, "'WebHandler' is required");
		this.webHandler = webHandler;
	}


	@Override
	protected WebHttpHandlerBuilder initHttpHandlerBuilder() {
		return WebHttpHandlerBuilder.webHandler(this.webHandler);
	}

}
