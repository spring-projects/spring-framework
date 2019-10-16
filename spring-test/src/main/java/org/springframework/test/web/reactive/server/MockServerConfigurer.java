/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.test.web.reactive.server;

import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

/**
 * Contract that frameworks or applications can use to pre-package a set of
 * customizations to a {@link WebTestClient.MockServerSpec} and expose that
 * as a shortcut.
 *
 * <p>An implementation of this interface can be plugged in via
 * {@link WebTestClient.MockServerSpec#apply} where instances are likely obtained
 * via static methods, e.g.:
 *
 * <pre class="code">
 * import static org.example.ExampleSetup.securitySetup;
 *
 * // ...
 *
 * WebTestClient.bindToController(new TestController())
 *     .apply(securitySetup("foo","bar"))
 *     .build();
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see WebTestClientConfigurer
 */
public interface MockServerConfigurer {

	/**
	 * Invoked immediately, i.e. before this method returns.
	 * @param serverSpec the serverSpec to which the configurer is added
	 */
	default void afterConfigureAdded(WebTestClient.MockServerSpec<?> serverSpec) {
	}

	/**
	 * Invoked just before the mock server is built. Use this hook to inspect
	 * and/or modify application-declared filters and exception handlers.
	 * @param builder the builder for the {@code HttpHandler} that will handle
	 * requests (i.e. the mock server)
	 */
	default void beforeServerCreated(WebHttpHandlerBuilder builder) {
	}

}
