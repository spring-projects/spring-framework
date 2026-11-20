/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.handler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Wraps another {@link org.springframework.web.socket.WebSocketSession} instance
 * and delegates to it.
 *
 * <p>Also provides a {@link #getDelegate()} method to return the decorated session
 * as well as a {@link #getLastSession()} method to go through all nested delegates
 * and return the "last" session.
 *
 * @author Rossen Stoyanchev
 * @since 4.0.3
 */
public class WebSocketSessionDecorator implements WebSocketSession {

	private final WebSocketSession delegate;


	public WebSocketSessionDecorator(WebSocketSession session) {
		Assert.notNull(session, "Delegate WebSocketSessionSession is required");
		this.delegate = session;
	}


	public WebSocketSession getDelegate() {
		return this.delegate;
	}

	public WebSocketSession getLastSession() {
		WebSocketSession result = this.delegate;
		while (result instanceof WebSocketSessionDecorator webSocketSessionDecorator) {
			result = webSocketSessionDecorator.getDelegate();
		}
		return result;
	}

	public static WebSocketSession unwrap(WebSocketSession session) {
		if (session instanceof WebSocketSessionDecorator webSocketSessionDecorator) {
			return webSocketSessionDecorator.getLastSession();
		}
		else {
			return session;
		}
	}

	@Override
	public String getId() {
		return this.delegate.getId();
	}

	@Override
	public @Nullable URI getUri() {
		return this.delegate.getUri();
	}

	@Override
	public HttpHeaders getHandshakeHeaders() {
		return this.delegate.getHandshakeHeaders();
	}

	@Override
	public Map<String, Object> getAttributes() {
		return this.delegate.getAttributes();
	}

	@Override
	public @Nullable Principal getPrincipal() {
		return this.delegate.getPrincipal();
	}

	@Override
	public @Nullable InetSocketAddress getLocalAddress() {
		return this.delegate.getLocalAddress();
	}

	@Override
	public @Nullable InetSocketAddress getRemoteAddress() {
		return this.delegate.getRemoteAddress();
	}

	@Override
	public @Nullable String getAcceptedProtocol() {
		return this.delegate.getAcceptedProtocol();
	}

	@Override
	public List<WebSocketExtension> getExtensions() {
		return this.delegate.getExtensions();
	}

	@Override
	public void setTextMessageSizeLimit(int messageSizeLimit) {
		this.delegate.setTextMessageSizeLimit(messageSizeLimit);
	}

	@Override
	public int getTextMessageSizeLimit() {
		return this.delegate.getTextMessageSizeLimit();
	}

	@Override
	public void setBinaryMessageSizeLimit(int messageSizeLimit) {
		this.delegate.setBinaryMessageSizeLimit(messageSizeLimit);
	}

	@Override
	public int getBinaryMessageSizeLimit() {
		return this.delegate.getBinaryMessageSizeLimit();
	}

	@Override
	public boolean isOpen() {
		return this.delegate.isOpen();
	}

	@Override
	public void sendMessage(WebSocketMessage<?> message) throws IOException {
		this.delegate.sendMessage(message);
	}

	@Override
	public void close() throws IOException {
		this.delegate.close();
	}

	@Override
	public void close(CloseStatus status) throws IOException {
		this.delegate.close(status);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [delegate=" + this.delegate + "]";
	}

}
