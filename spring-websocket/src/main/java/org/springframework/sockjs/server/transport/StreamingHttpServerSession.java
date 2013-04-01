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
package org.springframework.sockjs.server.transport;

import java.io.IOException;
import java.io.OutputStream;

import org.springframework.sockjs.SockJsHandler;
import org.springframework.sockjs.server.SockJsConfiguration;
import org.springframework.sockjs.server.SockJsFrame;


public class StreamingHttpServerSession extends AbstractHttpServerSession {

	private int byteCount;


	public StreamingHttpServerSession(String sessionId, SockJsHandler delegate, SockJsConfiguration sockJsConfig) {
		super(sessionId, delegate, sockJsConfig);
	}

	protected void flush() {

		cancelHeartbeat();

		do {
			String message = getMessageCache().poll();
			SockJsFrame frame = SockJsFrame.messageFrame(message);
			writeFrame(frame);

			this.byteCount += frame.getContentBytes().length + 1;
			if (logger.isTraceEnabled()) {
				logger.trace(this.byteCount + " bytes written, " + getMessageCache().size() + " more messages");
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
	public void writeFrame(OutputStream outputStream, SockJsFrame frame) throws IOException {
		super.writeFrame(outputStream, frame);
		outputStream.flush();
	}
}

