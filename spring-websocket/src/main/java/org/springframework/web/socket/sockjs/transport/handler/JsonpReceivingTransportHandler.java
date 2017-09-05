/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.socket.sockjs.transport.handler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.SockJsException;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;
import org.springframework.web.socket.sockjs.transport.TransportHandler;
import org.springframework.web.socket.sockjs.transport.TransportType;
import org.springframework.web.socket.sockjs.transport.session.AbstractHttpSockJsSession;

/**
 * A {@link TransportHandler} that receives messages over HTTP.
 *
 * @author Rossen Stoyanchev
 */
public class JsonpReceivingTransportHandler extends AbstractHttpReceivingTransportHandler {

	private final FormHttpMessageConverter formConverter = new FormHttpMessageConverter();


	@Override
	public TransportType getTransportType() {
		return TransportType.JSONP_SEND;
	}

	@Override
	public void handleRequestInternal(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, AbstractHttpSockJsSession sockJsSession) throws SockJsException {

		super.handleRequestInternal(request, response, wsHandler, sockJsSession);
		try {
			response.getBody().write("ok".getBytes(StandardCharsets.UTF_8));
		}
		catch (IOException ex) {
			throw new SockJsException("Failed to write to the response body", sockJsSession.getId(), ex);
		}
	}

	@Override
	@Nullable
	protected String[] readMessages(ServerHttpRequest request) throws IOException {
		SockJsMessageCodec messageCodec = getServiceConfig().getMessageCodec();
		MediaType contentType = request.getHeaders().getContentType();
		if (contentType != null && MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(contentType)) {
			MultiValueMap<String, String> map = this.formConverter.read(null, request);
			String d = map.getFirst("d");
			return (StringUtils.hasText(d) ? messageCodec.decode(d) : null);
		}
		else {
			return messageCodec.decodeInputStream(request.getBody());
		}
	}

	@Override
	protected HttpStatus getResponseStatus() {
		return HttpStatus.OK;
	}

}
