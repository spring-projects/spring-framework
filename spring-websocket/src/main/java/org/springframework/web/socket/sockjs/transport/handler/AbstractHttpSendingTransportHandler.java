/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.sockjs.transport.handler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.SockJsException;
import org.springframework.web.socket.sockjs.frame.SockJsFrame;
import org.springframework.web.socket.sockjs.frame.SockJsFrameFormat;
import org.springframework.web.socket.sockjs.transport.SockJsSession;
import org.springframework.web.socket.sockjs.transport.SockJsSessionFactory;
import org.springframework.web.socket.sockjs.transport.session.AbstractHttpSockJsSession;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

/**
 * Base class for HTTP transport handlers that push messages to connected clients.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractHttpSendingTransportHandler extends AbstractTransportHandler
		implements SockJsSessionFactory {

	/**
	 * Pattern for validating callback parameter values.
	 */
	private static final Pattern CALLBACK_PARAM_PATTERN = Pattern.compile("[0-9A-Za-z_.]*");


	@Override
	public final void handleRequest(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, SockJsSession wsSession) throws SockJsException {

		AbstractHttpSockJsSession sockJsSession = (AbstractHttpSockJsSession) wsSession;

		// https://github.com/sockjs/sockjs-client/issues/130
		// sockJsSession.setAcceptedProtocol(protocol);

		// Set content type before writing
		response.getHeaders().setContentType(getContentType());

		handleRequestInternal(request, response, sockJsSession);
	}

	protected void handleRequestInternal(ServerHttpRequest request, ServerHttpResponse response,
			AbstractHttpSockJsSession sockJsSession) throws SockJsException {

		if (sockJsSession.isNew()) {
			if (logger.isDebugEnabled()) {
				logger.debug(request.getMethod() + " " + request.getURI());
			}
			sockJsSession.handleInitialRequest(request, response, getFrameFormat(request));
		}
		else if (sockJsSession.isClosed()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Connection already closed (but not removed yet) for " + sockJsSession);
			}
			writeFrame(SockJsFrame.closeFrameGoAway(), request, response, sockJsSession);
		}
		else if (!sockJsSession.isActive()) {
			if (logger.isTraceEnabled()) {
				logger.trace("Starting " + getTransportType() + " async request.");
			}
			sockJsSession.handleSuccessiveRequest(request, response, getFrameFormat(request));
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("Another " + getTransportType() + " connection still open for " + sockJsSession);
			}
			writeFrame(SockJsFrame.closeFrameAnotherConnectionOpen(), request, response, sockJsSession);
		}
	}

	private void writeFrame(SockJsFrame frame, ServerHttpRequest request, ServerHttpResponse response,
			AbstractHttpSockJsSession sockJsSession) {

		String formattedFrame = getFrameFormat(request).format(frame);
		try {
			response.getBody().write(formattedFrame.getBytes(SockJsFrame.CHARSET));
		}
		catch (IOException ex) {
			throw new SockJsException("Failed to send " + formattedFrame, sockJsSession.getId(), ex);
		}
	}


	protected abstract MediaType getContentType();

	protected abstract SockJsFrameFormat getFrameFormat(ServerHttpRequest request);


	protected final @Nullable String getCallbackParam(ServerHttpRequest request) {
		String query = request.getURI().getQuery();
		MultiValueMap<String, String> params = UriComponentsBuilder.newInstance().query(query).build().getQueryParams();
		String value = params.getFirst("c");
		if (!StringUtils.hasLength(value)) {
			return null;
		}
		String result = UriUtils.decode(value, StandardCharsets.UTF_8);
		return (CALLBACK_PARAM_PATTERN.matcher(result).matches() ? result : null);
	}

}
