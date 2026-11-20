/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
		Assert.notNull(webHandler, "WebHandler is required");
		this.webHandler = webHandler;
	}


	@Override
	protected WebHttpHandlerBuilder initHttpHandlerBuilder() {
		return WebHttpHandlerBuilder.webHandler(this.webHandler);
	}

}
