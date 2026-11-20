/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.server;

import java.net.InetSocketAddress;
import java.security.Principal;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpRequest;

/**
 * Represents a server-side HTTP request.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.0
 */
public interface ServerHttpRequest extends HttpRequest, HttpInputMessage {

	/**
	 * Return a {@link java.security.Principal} instance containing the name of the
	 * authenticated user.
	 * <p>If the user has not been authenticated, the method returns <code>null</code>.
	 */
	@Nullable Principal getPrincipal();

	/**
	 * Return the address on which the request was received.
	 */
	InetSocketAddress getLocalAddress();

	/**
	 * Return the address of the remote client.
	 */
	InetSocketAddress getRemoteAddress();

	/**
	 * Return a control that allows putting the request in asynchronous mode so the
	 * response remains open until closed explicitly from the current or another thread.
	 */
	ServerHttpAsyncRequestControl getAsyncRequestControl(ServerHttpResponse response);

}
