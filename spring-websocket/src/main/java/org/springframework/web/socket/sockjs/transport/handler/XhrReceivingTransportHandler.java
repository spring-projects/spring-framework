/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
