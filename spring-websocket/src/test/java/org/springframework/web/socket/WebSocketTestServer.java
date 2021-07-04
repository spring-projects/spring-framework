/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.socket;

import javax.servlet.Filter;
import javax.servlet.ServletContext;

import org.springframework.web.context.WebApplicationContext;

/**
 * Contract for a test server to use for WebSocket integration tests.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public interface WebSocketTestServer {

	void setup();

	void deployConfig(WebApplicationContext cxt, Filter... filters);

	void undeployConfig();

	void start() throws Exception;

	void stop() throws Exception;

	int getPort();

	/**
	 * Get the {@link ServletContext} created by the underlying server.
	 * <p>The {@code ServletContext} is only guaranteed to be available
	 * after {@link #deployConfig} has been invoked.
	 * @since 4.2
	 */
	ServletContext getServletContext();

}
