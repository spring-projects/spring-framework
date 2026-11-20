/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletContext;

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
