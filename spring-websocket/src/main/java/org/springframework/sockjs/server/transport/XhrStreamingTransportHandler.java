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
import java.nio.charset.Charset;

import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.sockjs.server.SockJsConfiguration;
import org.springframework.sockjs.server.TransportType;
import org.springframework.sockjs.server.SockJsFrame.DefaultFrameFormat;
import org.springframework.sockjs.server.SockJsFrame.FrameFormat;


/**
 * TODO
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class XhrStreamingTransportHandler extends AbstractStreamingTransportHandler {


	public XhrStreamingTransportHandler(SockJsConfiguration sockJsConfig) {
		super(sockJsConfig);
	}

	@Override
	public TransportType getTransportType() {
		return TransportType.XHR_STREAMING;
	}

	@Override
	protected MediaType getContentType() {
		return new MediaType("application", "javascript", Charset.forName("UTF-8"));
	}

	@Override
	protected void writePrelude(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
		for (int i=0; i < 2048; i++) {
			response.getBody().write('h');
		}
		response.getBody().write('\n');
		response.flush();
	}

	@Override
	protected FrameFormat getFrameFormat(ServerHttpRequest request) {
		return new DefaultFrameFormat("%s\n");
	}

}
