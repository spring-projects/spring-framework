/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.server.support;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

import org.springframework.context.Lifecycle;
import org.springframework.context.SmartLifecycle;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

/**
 * Extension of {@link SimpleUrlHandlerMapping} with support for more
 * precise mapping of WebSocket handshake requests to handlers of type
 * {@link WebSocketHttpRequestHandler}. Also delegates {@link Lifecycle}
 * methods to handlers in the {@link #getUrlMap()} that implement it.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public class WebSocketHandlerMapping extends SimpleUrlHandlerMapping implements SmartLifecycle {

	private boolean webSocketUpgradeMatch;

	private @Nullable Integer phase;

	private volatile boolean running;


	/**
	 * When this is set, if the matched handler is
	 * {@link WebSocketHttpRequestHandler}, ensure the request is a WebSocket
	 * handshake, i.e. HTTP GET with the header {@code "Upgrade:websocket"},
	 * or otherwise suppress the match and return {@code null} allowing another
	 * {@link org.springframework.web.servlet.HandlerMapping} to match for the
	 * same URL path.
	 * @param match whether to enable matching on {@code "Upgrade: websocket"}
	 * @since 5.3.5
	 */
	public void setWebSocketUpgradeMatch(boolean match) {
		this.webSocketUpgradeMatch = match;
	}

	/**
	 * Set the phase that this handler should run in.
	 * <p>By default, this is {@link SmartLifecycle#DEFAULT_PHASE}, but with
	 * {@code @EnableWebSocketMessageBroker} configuration it is set to 0.
	 * @since 6.1.4
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

	@Override
	public int getPhase() {
		return (this.phase != null ? this.phase : SmartLifecycle.super.getPhase());
	}


	@Override
	protected void initServletContext(ServletContext servletContext) {
		for (Object handler : getUrlMap().values()) {
			if (handler instanceof ServletContextAware servletContextAware) {
				servletContextAware.setServletContext(servletContext);
			}
		}
	}


	@Override
	public void start() {
		if (!isRunning()) {
			this.running = true;
			for (Object handler : getUrlMap().values()) {
				if (handler instanceof Lifecycle lifecycle) {
					lifecycle.start();
				}
			}
		}
	}

	@Override
	public void stop() {
		if (isRunning()) {
			this.running = false;
			for (Object handler : getUrlMap().values()) {
				if (handler instanceof Lifecycle lifecycle) {
					lifecycle.stop();
				}
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}


	@Override
	protected @Nullable Object getHandlerInternal(HttpServletRequest request) throws Exception {
		Object handler = super.getHandlerInternal(request);
		return (matchWebSocketUpgrade(handler, request) ? handler : null);
	}

	private boolean matchWebSocketUpgrade(@Nullable Object handler, HttpServletRequest request) {
		handler = (handler instanceof HandlerExecutionChain chain ? chain.getHandler() : handler);
		if (this.webSocketUpgradeMatch && handler instanceof WebSocketHttpRequestHandler) {
			String header = request.getHeader(HttpHeaders.UPGRADE);
			return (HttpMethod.GET.matches(request.getMethod()) &&
					header != null && header.equalsIgnoreCase("websocket"));
		}
		return true;
	}

}
