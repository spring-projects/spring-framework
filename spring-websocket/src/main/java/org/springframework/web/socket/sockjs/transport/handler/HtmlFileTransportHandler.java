/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.socket.sockjs.transport.handler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.SockJsException;
import org.springframework.web.socket.sockjs.SockJsTransportFailureException;
import org.springframework.web.socket.sockjs.frame.DefaultSockJsFrameFormat;
import org.springframework.web.socket.sockjs.frame.SockJsFrameFormat;
import org.springframework.web.socket.sockjs.transport.SockJsServiceConfig;
import org.springframework.web.socket.sockjs.transport.SockJsSession;
import org.springframework.web.socket.sockjs.transport.TransportHandler;
import org.springframework.web.socket.sockjs.transport.TransportType;
import org.springframework.web.socket.sockjs.transport.session.AbstractHttpSockJsSession;
import org.springframework.web.socket.sockjs.transport.session.StreamingSockJsSession;
import org.springframework.web.util.JavaScriptUtils;

/**
 * An HTTP {@link TransportHandler} that uses a famous browser
 * {@code document.domain} technique. See <a href=
 * "https://stackoverflow.com/questions/1481251/what-does-document-domain-document-domain-do">
 * stackoverflow.com/questions/1481251/what-does-document-domain-document-domain-do</a>
 * for details.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class HtmlFileTransportHandler extends AbstractHttpSendingTransportHandler {

	private static final String PARTIAL_HTML_CONTENT;

	// Safari needs at least 1024 bytes to parse the website.
	// https://code.google.com/p/browsersec/wiki/Part2#Survey_of_content_sniffing_behaviors
	private static final int MINIMUM_PARTIAL_HTML_CONTENT_LENGTH = 1024;


	static {
		StringBuilder sb = new StringBuilder("""
				<!DOCTYPE html>
				<html>
				<head>
					<meta http-equiv="X-UA-Compatible" content="IE=edge" />
					<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
				</head>
				<body>
					<h2>Don't panic!</h2>
					<script>
						document.domain = document.domain;
						var c = parent.%s;
						c.start();
						function p(d) {c.message(d);};
						window.onload = function() {c.stop();};
					</script>""");

		sb.append(" ".repeat(MINIMUM_PARTIAL_HTML_CONTENT_LENGTH - sb.length()));
		PARTIAL_HTML_CONTENT = sb.append('\n').toString();
	}


	@Override
	public TransportType getTransportType() {
		return TransportType.HTML_FILE;
	}

	@Override
	protected MediaType getContentType() {
		return new MediaType("text", "html", StandardCharsets.UTF_8);
	}

	@Override
	public boolean checkSessionType(SockJsSession session) {
		return (session instanceof HtmlFileStreamingSockJsSession);
	}

	@Override
	public StreamingSockJsSession createSession(
			String sessionId, WebSocketHandler handler, Map<String, Object> attributes) {

		return new HtmlFileStreamingSockJsSession(sessionId, getServiceConfig(), handler, attributes);
	}

	@Override
	public void handleRequestInternal(ServerHttpRequest request, ServerHttpResponse response,
			AbstractHttpSockJsSession sockJsSession) throws SockJsException {

		String callback = getCallbackParam(request);
		if (!StringUtils.hasText(callback)) {
			response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
			try {
				response.getBody().write("\"callback\" parameter required".getBytes(StandardCharsets.UTF_8));
			}
			catch (IOException ex) {
				sockJsSession.tryCloseWithSockJsTransportError(ex, CloseStatus.SERVER_ERROR);
				throw new SockJsTransportFailureException("Failed to write to response", sockJsSession.getId(), ex);
			}
			return;
		}

		super.handleRequestInternal(request, response, sockJsSession);
	}

	@Override
	protected SockJsFrameFormat getFrameFormat(ServerHttpRequest request) {
		return new DefaultSockJsFrameFormat("<script>\np(\"%s\");\n</script>\r\n") {
			@Override
			protected String preProcessContent(String content) {
				return JavaScriptUtils.javaScriptEscape(content);
			}
		};
	}


	private class HtmlFileStreamingSockJsSession extends StreamingSockJsSession {

		public HtmlFileStreamingSockJsSession(String sessionId, SockJsServiceConfig config,
				WebSocketHandler wsHandler, Map<String, Object> attributes) {

			super(sessionId, config, wsHandler, attributes);
		}

		@Override
		protected byte[] getPrelude(ServerHttpRequest request) {
			// We already validated the parameter above...
			String callback = getCallbackParam(request);
			String html = String.format(PARTIAL_HTML_CONTENT, callback);
			return html.getBytes(StandardCharsets.UTF_8);
		}
	}

}
