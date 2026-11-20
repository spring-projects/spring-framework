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
 * A SockJS session for use with streaming HTTP transports.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class StreamingSockJsSession extends AbstractHttpSockJsSession {

	private int byteCount;


	public StreamingSockJsSession(String sessionId, SockJsServiceConfig config,
			WebSocketHandler wsHandler, Map<String, Object> attributes) {

		super(sessionId, config, wsHandler, attributes);
	}


	/**
	 * Get the prelude to write to the response before any other data.
	 * @since 4.2
	 */
	protected abstract byte[] getPrelude(ServerHttpRequest request);


	@Override
	protected void handleRequestInternal(ServerHttpRequest request, ServerHttpResponse response,
			boolean initialRequest) throws IOException {

		byte[] prelude = getPrelude(request);
		response.getBody().write(prelude);
		response.flush();

		if (initialRequest) {
			writeFrame(SockJsFrame.openFrame());
		}
		flushCache();
	}

	@Override
	protected void flushCache() throws SockJsTransportFailureException {
		while (!getMessageCache().isEmpty()) {
			String message = getMessageCache().poll();
			SockJsMessageCodec messageCodec = getSockJsServiceConfig().getMessageCodec();
			SockJsFrame frame = SockJsFrame.messageFrame(messageCodec, message);
			writeFrame(frame);

			this.byteCount += (frame.getContentBytes().length + 1);
			if (logger.isTraceEnabled()) {
				logger.trace(this.byteCount + " bytes written so far, " +
						getMessageCache().size() + " more messages not flushed");
			}
			if (this.byteCount >= getSockJsServiceConfig().getStreamBytesLimit()) {
				logger.trace("Streamed bytes limit reached, recycling current request");
				resetRequest();
				this.byteCount = 0;
				break;
			}
		}
		scheduleHeartbeat();
	}

}
