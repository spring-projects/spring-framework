/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.sockjs.transport.handler;

import org.junit.jupiter.api.Test;

import org.springframework.web.socket.AbstractHttpRequestTests;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.SockJsMessageDeliveryException;
import org.springframework.web.socket.sockjs.transport.session.AbstractSockJsSession;
import org.springframework.web.socket.sockjs.transport.session.StubSockJsServiceConfig;
import org.springframework.web.socket.sockjs.transport.session.TestHttpSockJsSession;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Test fixture for {@link AbstractHttpReceivingTransportHandler} and
 * {@link XhrReceivingTransportHandler}.
 *
 * @author Rossen Stoyanchev
 */
class HttpReceivingTransportHandlerTests extends AbstractHttpRequestTests {

	@Test
	void readMessagesXhr() throws Exception {
		this.servletRequest.setContent("[\"x\"]".getBytes(UTF_8));
		handleRequest(new XhrReceivingTransportHandler());

		assertThat(this.servletResponse.getStatus()).isEqualTo(204);
	}

	@Test
	void readMessagesBadContent() {
		this.servletRequest.setContent("".getBytes(UTF_8));
		handleRequestAndExpectFailure();

		this.servletRequest.setContent("[\"x]".getBytes(UTF_8));
		handleRequestAndExpectFailure();
	}

	@Test
	void readMessagesNoSession() {
		WebSocketHandler webSocketHandler = mock();
		assertThatIllegalArgumentException().isThrownBy(() ->
				new XhrReceivingTransportHandler().handleRequest(this.request, this.response, webSocketHandler, null));
	}

	@Test
	void delegateMessageException() throws Exception {
		StubSockJsServiceConfig sockJsConfig = new StubSockJsServiceConfig();
		this.servletRequest.setContent("[\"x\"]".getBytes(UTF_8));

		WebSocketHandler wsHandler = mock();
		TestHttpSockJsSession session = new TestHttpSockJsSession("1", sockJsConfig, wsHandler, null);
		session.delegateConnectionEstablished();

		willThrow(new Exception()).given(wsHandler).handleMessage(session, new TextMessage("x"));

		XhrReceivingTransportHandler transportHandler = new XhrReceivingTransportHandler();
		transportHandler.initialize(sockJsConfig);
		assertThatExceptionOfType(SockJsMessageDeliveryException.class).isThrownBy(() ->
				transportHandler.handleRequest(this.request, this.response, wsHandler, session));
		assertThat(session.getCloseStatus()).isNull();
	}


	private void handleRequest(AbstractHttpReceivingTransportHandler transportHandler) throws Exception {
		WebSocketHandler wsHandler = mock();
		AbstractSockJsSession session = new TestHttpSockJsSession("1", new StubSockJsServiceConfig(), wsHandler, null);

		transportHandler.initialize(new StubSockJsServiceConfig());
		transportHandler.handleRequest(this.request, this.response, wsHandler, session);

		assertThat(this.response.getHeaders().getContentType()).hasToString("text/plain;charset=UTF-8");
		verify(wsHandler).handleMessage(session, new TextMessage("x"));
	}

	private void handleRequestAndExpectFailure() {
		resetResponse();

		WebSocketHandler wsHandler = mock();
		AbstractSockJsSession session = new TestHttpSockJsSession("1", new StubSockJsServiceConfig(), wsHandler, null);

		new XhrReceivingTransportHandler().handleRequest(this.request, this.response, wsHandler, session);

		assertThat(this.servletResponse.getStatus()).isEqualTo(500);
		verifyNoMoreInteractions(wsHandler);
	}

}
