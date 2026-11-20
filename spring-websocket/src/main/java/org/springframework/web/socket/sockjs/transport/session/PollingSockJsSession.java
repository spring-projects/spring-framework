/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.sockjs.transport.session;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.SockJsTransportFailureException;
import org.springframework.web.socket.sockjs.frame.SockJsFrame;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;
import org.springframework.web.socket.sockjs.transport.SockJsServiceConfig;

/**
 * A SockJS session for use with polling HTTP transports.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class PollingSockJsSession extends AbstractHttpSockJsSession {

	public PollingSockJsSession(String sessionId, SockJsServiceConfig config,
			WebSocketHandler wsHandler, Map<String, Object> attributes) {

		super(sessionId, config, wsHandler, attributes);
	}


	@Override
	protected void handleRequestInternal(ServerHttpRequest request, ServerHttpResponse response,
			boolean initialRequest) throws IOException {

		if (initialRequest) {
			writeFrame(SockJsFrame.openFrame());
		}
		else if (!getMessageCache().isEmpty()) {
			flushCache();
		}
		else {
			scheduleHeartbeat();
		}
	}

	@Override
	protected void flushCache() throws SockJsTransportFailureException {
		String[] messages = new String[getMessageCache().size()];
		for (int i = 0; i < messages.length; i++) {
			messages[i] = getMessageCache().poll();
		}
		SockJsMessageCodec messageCodec = getSockJsServiceConfig().getMessageCodec();
		SockJsFrame frame = SockJsFrame.messageFrame(messageCodec, messages);
		writeFrame(frame);
	}

	@Override
	protected void writeFrame(SockJsFrame frame) throws SockJsTransportFailureException {
		super.writeFrame(frame);
		resetRequest();
	}

}
