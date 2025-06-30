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

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.sockjs.transport.TransportHandler;
import org.springframework.web.socket.sockjs.transport.TransportType;

/**
 * A {@link TransportHandler} that receives messages over HTTP.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class XhrReceivingTransportHandler extends AbstractHttpReceivingTransportHandler {

	@Override
	public TransportType getTransportType() {
		return TransportType.XHR_SEND;
	}

	@Override
	protected String @Nullable [] readMessages(ServerHttpRequest request) throws IOException {
		return getServiceConfig().getMessageCodec().decodeInputStream(request.getBody());
	}

	@Override
	protected HttpStatusCode getResponseStatus() {
		return HttpStatus.NO_CONTENT;
	}

}
