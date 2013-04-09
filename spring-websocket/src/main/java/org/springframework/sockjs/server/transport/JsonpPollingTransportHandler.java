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

import java.nio.charset.Charset;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.sockjs.server.SockJsConfiguration;
import org.springframework.sockjs.server.SockJsFrame;
import org.springframework.sockjs.server.TransportType;
import org.springframework.sockjs.server.SockJsFrame.FrameFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.util.JavaScriptUtils;


/**
 * TODO
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class JsonpPollingTransportHandler extends AbstractHttpSendingTransportHandler {


	public JsonpPollingTransportHandler(SockJsConfiguration sockJsConfig) {
		super(sockJsConfig);
	}

	@Override
	public TransportType getTransportType() {
		return TransportType.JSONP;
	}

	@Override
	protected MediaType getContentType() {
		return new MediaType("application", "javascript", Charset.forName("UTF-8"));
	}

	@Override
	public PollingHttpServerSession createSession(String sessionId) {
		return new PollingHttpServerSession(sessionId, getSockJsConfig());
	}

	@Override
	public void handleRequestInternal(ServerHttpRequest request, ServerHttpResponse response,
			AbstractHttpServerSession session) throws Exception {

		String callback = request.getQueryParams().getFirst("c");
		if (! StringUtils.hasText(callback)) {
			response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
			response.getBody().write("\"callback\" parameter required".getBytes("UTF-8"));
			return;
		}
		super.handleRequest(request, response, session);
	}

	@Override
	protected FrameFormat getFrameFormat(ServerHttpRequest request) {

		// we already validated the parameter..
		String callback = request.getQueryParams().getFirst("c");

		return new SockJsFrame.DefaultFrameFormat(callback + "(\"%s\");\r\n") {
			@Override
			protected String preProcessContent(String content) {
				return JavaScriptUtils.javaScriptEscape(content);
			}
		};
	}

}
