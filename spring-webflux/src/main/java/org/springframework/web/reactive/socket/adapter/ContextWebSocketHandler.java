/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.socket.adapter;

import java.util.List;

import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;

/**
 * {@link WebSocketHandler} decorator that enriches the context of the target handler.
 *
 * @author Rossen Stoyanchev
 * @since 5.3.3
 */
public final class ContextWebSocketHandler implements WebSocketHandler {

	private final WebSocketHandler delegate;

	private final ContextView contextView;


	private ContextWebSocketHandler(WebSocketHandler delegate, ContextView contextView) {
		this.delegate = delegate;
		this.contextView = contextView;
	}


	@Override
	public List<String> getSubProtocols() {
		return this.delegate.getSubProtocols();
	}

	@Override
	public Mono<Void> handle(WebSocketSession session) {
		return this.delegate.handle(session).contextWrite(this.contextView);
	}


	/**
	 * Return the given handler, decorated to insert the given context, or the
	 * same handler instance when the context is empty.
	 */
	public static WebSocketHandler decorate(WebSocketHandler handler, ContextView contextView) {
		return (!contextView.isEmpty() ? new ContextWebSocketHandler(handler, contextView) : handler);
	}

}
