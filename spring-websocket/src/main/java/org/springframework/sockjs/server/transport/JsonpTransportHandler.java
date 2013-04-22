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

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.sockjs.AbstractSockJsSession;
import org.springframework.sockjs.server.TransportType;

public class JsonpTransportHandler extends AbstractHttpReceivingTransportHandler {


	@Override
	public TransportType getTransportType() {
		return TransportType.JSONP_SEND;
	}

	@Override
	public void handleRequestInternal(ServerHttpRequest request, ServerHttpResponse response,
			AbstractSockJsSession sockJsSession) throws Exception {

		if (MediaType.APPLICATION_FORM_URLENCODED.equals(request.getHeaders().getContentType())) {
			if (request.getQueryParams().getFirst("d") == null) {
				response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
				response.getBody().write("Payload expected.".getBytes("UTF-8"));
				return;
			}
		}

		super.handleRequestInternal(request, response, sockJsSession);

		response.getBody().write("ok".getBytes("UTF-8"));
	}

	@Override
	protected String[] readMessages(ServerHttpRequest request) throws IOException {
		if (MediaType.APPLICATION_FORM_URLENCODED.equals(request.getHeaders().getContentType())) {
			String d = request.getQueryParams().getFirst("d");
			return getObjectMapper().readValue(d, String[].class);
		}
		else {
			return getObjectMapper().readValue(request.getBody(), String[].class);
		}
	}

	@Override
	protected HttpStatus getResponseStatus() {
		return HttpStatus.OK;
	}

}
