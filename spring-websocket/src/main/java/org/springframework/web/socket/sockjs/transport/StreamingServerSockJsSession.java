/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.sockjs.transport;

import java.io.IOException;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.SockJsConfiguration;
import org.springframework.web.socket.sockjs.SockJsFrame;
import org.springframework.web.socket.sockjs.TransportErrorException;
import org.springframework.web.socket.sockjs.SockJsFrame.FrameFormat;

public class StreamingServerSockJsSession extends AbstractHttpServerSockJsSession {

	private int byteCount;


	public StreamingServerSockJsSession(String sessionId, SockJsConfiguration config, WebSocketHandler handler) {
		super(sessionId, config, handler);
	}


	@Override
	public synchronized void setInitialRequest(ServerHttpRequest request, ServerHttpResponse response,
			FrameFormat frameFormat) throws TransportErrorException {

		super.setInitialRequest(request, response, frameFormat);
		super.setLongPollingRequest(request, response, frameFormat);
	}

	protected void flushCache() throws IOException {

		cancelHeartbeat();

		do {
			String message = getMessageCache().poll();
			SockJsFrame frame = SockJsFrame.messageFrame(message);
			writeFrame(frame);

			this.byteCount += frame.getContentBytes().length + 1;
			if (logger.isTraceEnabled()) {
				logger.trace(this.byteCount + " bytes written so far, "
						+ getMessageCache().size() + " more messages not flushed");
			}
			if (this.byteCount >= getSockJsConfig().getStreamBytesLimit()) {
				if (logger.isTraceEnabled()) {
					logger.trace("Streamed bytes limit reached. Recycling current request");
				}
				resetRequest();
				break;
			}
		} while (!getMessageCache().isEmpty());

		scheduleHeartbeat();
	}

	@Override
	protected synchronized void resetRequest() {
		super.resetRequest();
		this.byteCount = 0;
	}

	@Override
	protected synchronized void writeFrameInternal(SockJsFrame frame) throws IOException {
		if (isActive()) {
			super.writeFrameInternal(frame);
			getResponse().flush();
		}
	}

}

