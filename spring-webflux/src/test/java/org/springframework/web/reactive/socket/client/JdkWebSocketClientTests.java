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

package org.springframework.web.reactive.socket.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Timeout(10)
class JdkWebSocketClientTests {

	private static final URI URL = URI.create("ws://example.org");

	private final HttpClient httpClient = mock();

	private final WebSocket.Builder builder = mock();

	private final WebSocket webSocket = mock();

	private final JdkWebSocketClient client = new JdkWebSocketClient(this.httpClient);

	private final AtomicReference<WebSocket.Listener> listener = new AtomicReference<>();


	@BeforeEach
	void setUp() {
		when(this.httpClient.newWebSocketBuilder()).thenReturn(this.builder);
		when(this.webSocket.getSubprotocol()).thenReturn("");
		when(this.webSocket.sendClose(1000, "")).thenReturn(CompletableFuture.completedFuture(this.webSocket));
		when(this.builder.buildAsync(eq(URL), any())).thenAnswer(invocation -> {
			WebSocket.Listener listener = invocation.getArgument(1);
			this.listener.set(listener);
			listener.onOpen(this.webSocket);
			return CompletableFuture.completedFuture(this.webSocket);
		});
	}


	@Test
	void connectIsDeferredAndRepeatedPerSubscription() {
		Mono<Void> execute = this.client.execute(URL, session -> Mono.empty());
		verify(this.httpClient, never()).newWebSocketBuilder();
		execute.block();
		execute.block();
		verify(this.httpClient, times(2)).newWebSocketBuilder();
		verify(this.webSocket, times(2)).sendClose(1000, "");
		verify(this.webSocket, times(2)).abort();
	}

	@Test
	void requestHeadersAndSubprotocols() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Test", "one");
		headers.add("X-Test", "two");
		when(this.webSocket.getSubprotocol()).thenReturn("echo");
		this.client.execute(URL, headers, new WebSocketHandler() {
			@Override
			public List<String> getSubProtocols() {
				return List.of("echo", "other");
			}
			@Override
			public Mono<Void> handle(WebSocketSession session) {
				assertThat(session.getHandshakeInfo().getHeaders().isEmpty()).isTrue();
				assertThat(session.getHandshakeInfo().getSubProtocol()).isEqualTo("echo");
				return Mono.empty();
			}
		}).block();
		verify(this.builder).header("X-Test", "one");
		verify(this.builder).header("X-Test", "two");
		verify(this.builder).subprotocols("echo", "other");
	}

	@Test
	void subscriberContextReachesHandler() {
		this.client.execute(URL, session -> Mono.deferContextual(context -> {
			assertThat(context.get("key").toString()).isEqualTo("value");
			return Mono.empty();
		})).contextWrite(context -> context.put("key", "value")).block();
	}

	@Test
	void cancellationCancelsHandshake() {
		CompletableFuture<WebSocket> handshake = new CompletableFuture<>();
		when(this.builder.buildAsync(eq(URL), any())).thenReturn(handshake);
		Disposable subscription = this.client.execute(URL, session -> Mono.never()).subscribe();
		subscription.dispose();
		assertThat(handshake).isCancelled();
	}

	@Test
	void handshakeFutureCanCompleteBeforeOnOpen() {
		when(this.builder.buildAsync(eq(URL), any())).thenAnswer(invocation -> {
			this.listener.set(invocation.getArgument(1));
			return CompletableFuture.completedFuture(this.webSocket);
		});
		StepVerifier.create(this.client.execute(URL, session -> Mono.empty()))
				.then(() -> this.listener.get().onOpen(this.webSocket))
				.verifyComplete();
	}

	@Test
	void cancellationWhileWaitingForOnOpenAbortsConnection() {
		when(this.builder.buildAsync(eq(URL), any())).thenAnswer(invocation -> {
			this.listener.set(invocation.getArgument(1));
			return CompletableFuture.completedFuture(this.webSocket);
		});
		WebSocketHandler handler = mock();
		Disposable subscription = this.client.execute(URL, handler).subscribe();
		subscription.dispose();
		verify(this.webSocket).abort();
		this.listener.get().onOpen(this.webSocket);
		verify(handler, never()).handle(any());
	}

	@Test
	void connectionOpenedAfterCancellationIsAborted() {
		CompletableFuture<WebSocket> handshake = new CompletableFuture<>();
		when(this.builder.buildAsync(eq(URL), any())).thenAnswer(invocation -> {
			this.listener.set(invocation.getArgument(1));
			return handshake;
		});
		Disposable subscription = this.client.execute(URL, session -> Mono.never()).subscribe();
		subscription.dispose();
		this.listener.get().onOpen(this.webSocket);
		verify(this.webSocket).abort();
	}

	@Test
	void cancellationAbortsConnectedSession() {
		Disposable subscription = this.client.execute(URL, session -> Mono.never()).subscribe();
		subscription.dispose();
		verify(this.webSocket).abort();
	}

	@Test
	void nativeErrorTerminatesHandlerWithoutReceiveSubscription() {
		IllegalStateException error = new IllegalStateException("connection failed");
		StepVerifier.create(this.client.execute(URL, session -> Mono.never()))
				.then(() -> this.listener.get().onError(this.webSocket, error))
				.expectErrorSatisfies(actual -> assertThat(actual).isSameAs(error)).verify();
		verify(this.webSocket).abort();
	}

	@Test
	void unsupportedCloseStatusTerminatesHandlerWithoutReceiveSubscription() {
		StepVerifier.create(this.client.execute(URL, session -> Mono.never()))
				.then(() -> this.listener.get().onClose(this.webSocket, 65535, ""))
				.verifyError(IllegalArgumentException.class);
		verify(this.webSocket).abort();
	}

	@Test
	void synchronousHandlerFailureAbortsSession() {
		IllegalStateException error = new IllegalStateException("handler failed");
		StepVerifier.create(this.client.execute(URL, session -> { throw error; }))
				.expectErrorSatisfies(actual -> assertThat(actual).isSameAs(error)).verify();
		verify(this.webSocket).abort();
	}

	@Test
	void closeFailureIsPropagatedWithoutSuccessfulCloseStatus() {
		IllegalArgumentException error = new IllegalArgumentException("close rejected");
		when(this.webSocket.sendClose(1000, "")).thenReturn(CompletableFuture.failedFuture(error));
		AtomicReference<WebSocketSession> sessionRef = new AtomicReference<>();
		StepVerifier.create(this.client.execute(URL, session -> {
			sessionRef.set(session);
			return Mono.empty();
		})).expectErrorSatisfies(actual -> assertThat(actual).isSameAs(error)).verify();
		verify(this.webSocket).abort();
		StepVerifier.create(sessionRef.get().closeStatus()).verifyComplete();
	}

	@Test
	void handshakeFailureIsPropagated() {
		IllegalStateException error = new IllegalStateException("handshake failed");
		when(this.builder.buildAsync(eq(URL), any())).thenReturn(CompletableFuture.failedFuture(error));
		StepVerifier.create(this.client.execute(URL, session -> Mono.empty()))
				.expectErrorSatisfies(actual -> assertThat(actual).isSameAs(error)).verify();
	}

}
