package org.springframework.web.socket.sockjs.client;

import java.io.ByteArrayOutputStream;
import java.net.URI;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for
 * {@link org.springframework.web.socket.sockjs.client.AbstractEventSourceTransport}.
 *
 * @author Sebastian Lövdahl
 */
public class EventSourceTransportTest {


	private AbstractEventSourceTransport eventSourceTransport;

	private EventSourceClientSockJsSession sockJsSession;


	@Before
	public void setup() {
		this.eventSourceTransport = new TestEventSourceTransport();
		this.sockJsSession = mock(EventSourceClientSockJsSession.class);
	}

	@Test
	public void eventAndData() throws Exception {
		this.eventSourceTransport.handleEventSourceFrame(fromString("data:sockjs data"), this.sockJsSession);
		verify(this.sockJsSession, times(1)).handleFrame("sockjs data");
	}

	@Test
	public void eventAndDataWithTrailingNewline() throws Exception {
		this.eventSourceTransport.handleEventSourceFrame(fromString("data:sockjs data\n"), this.sockJsSession);
		verify(this.sockJsSession, times(1)).handleFrame("sockjs data");
	}

	@Test
	public void eventAndMultilineData() throws Exception {
		this.eventSourceTransport.handleEventSourceFrame(fromString("data:sockjs data\ndata:more sockjs data"), this.sockJsSession);
		verify(this.sockJsSession, times(1)).handleFrame("sockjs data\nmore sockjs data");
	}

	@Test
	public void eventAndDataAndId() throws Exception {
		this.eventSourceTransport.handleEventSourceFrame(fromString("data:sockjs data\nid: 123"), this.sockJsSession);
		verify(this.sockJsSession, times(1)).handleFrame("sockjs data");
	}

	@Test
	public void eventAndMultilineDataAndId() throws Exception {
		this.eventSourceTransport.handleEventSourceFrame(fromString("data:sockjs data\ndata:more sockjs data\nid: 123"), this.sockJsSession);
		verify(this.sockJsSession, times(1)).handleFrame("sockjs data\nmore sockjs data");
	}

	@Test
	public void emptyValue() throws Exception {
		this.eventSourceTransport.handleEventSourceFrame(fromString("data:"), this.sockJsSession);
		verify(this.sockJsSession, never()).handleFrame(anyString());
	}

	@Test
	public void emptyValueWithSpace() throws Exception {
		this.eventSourceTransport.handleEventSourceFrame(fromString("data: "), this.sockJsSession);
		verify(this.sockJsSession, never()).handleFrame(anyString());
	}

	@Test
	public void onlyString() throws Exception {
		this.eventSourceTransport.handleEventSourceFrame(fromString("random data"), this.sockJsSession);
		verify(this.sockJsSession, never()).handleFrame(anyString());
	}

	@Test
	public void emptyFrame() throws Exception {
		this.eventSourceTransport.handleEventSourceFrame(fromString(""), this.sockJsSession);
		verify(this.sockJsSession, never()).handleFrame(anyString());
	}

	@Test
	public void oneSpace() throws Exception {
		this.eventSourceTransport.handleEventSourceFrame(fromString(" "), this.sockJsSession);
		verify(this.sockJsSession, never()).handleFrame(anyString());
	}

	@Test
	public void twoSpaces() throws Exception {
		this.eventSourceTransport.handleEventSourceFrame(fromString("  "), this.sockJsSession);
		verify(this.sockJsSession, never()).handleFrame(anyString());
	}

	@Test
	public void eventNameOtherThanData() throws Exception {
		this.eventSourceTransport.handleEventSourceFrame(fromString("unknown:"), this.sockJsSession);
		verify(this.sockJsSession, never()).handleFrame(anyString());
	}

	@Test
	public void eventNameOtherThanDataWithData() throws Exception {
		this.eventSourceTransport.handleEventSourceFrame(fromString("unknown: unknown data"), this.sockJsSession);
		verify(this.sockJsSession, never()).handleFrame(anyString());
	}

	@Test
	public void commentFrame() throws Exception {
		this.eventSourceTransport.handleEventSourceFrame(fromString(":this is a comment"), this.sockJsSession);
		verify(this.sockJsSession, never()).handleFrame(anyString());
	}


	private static ByteArrayOutputStream fromString(String string) {
		byte[] bytes = string.getBytes();
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(bytes.length);
		byteArrayOutputStream.write(bytes, 0, bytes.length);
		return byteArrayOutputStream;
	}


	private static class TestEventSourceTransport extends AbstractEventSourceTransport {

		@Override
		protected ResponseEntity<String> executeInfoRequestInternal(
				URI infoUrl, HttpHeaders headers) {

			return null;
		}

		@Override
		protected ResponseEntity<String> executeSendRequestInternal(
				URI url, HttpHeaders headers, TextMessage message) {

			return null;
		}

		@Override
		protected void connectInternal(TransportRequest request, WebSocketHandler handler,
				URI receiveUrl, HttpHeaders handshakeHeaders, EventSourceClientSockJsSession session,
				SettableListenableFuture<WebSocketSession> connectFuture) {
		}
	}

}
