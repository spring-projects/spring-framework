package org.springframework.http.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.concurrent.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

class JdkClientHttpRequestTest {

	private HttpClient mockHttpClient;
	private URI uri = URI.create("http://example.com");
	private HttpMethod method = HttpMethod.GET;

	private ExecutorService executor;

	@BeforeEach
	void setup() {
		mockHttpClient = mock(HttpClient.class);
		executor = Executors.newSingleThreadExecutor();
	}

	@AfterEach
	void tearDown() {
		executor.shutdownNow();
	}

	@Test
	void executeInternal_withTimeout_shouldThrowHttpTimeoutException() throws Exception {
		Duration timeout = Duration.ofMillis(10);

		JdkClientHttpRequest request = new JdkClientHttpRequest(mockHttpClient, uri, method, executor, timeout);

		CompletableFuture<HttpResponse<InputStream>> future = new CompletableFuture<>();

		when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
				.thenReturn(future);

		HttpHeaders headers = new HttpHeaders();

		CountDownLatch startLatch = new CountDownLatch(1);

		// Cancellation thread waits for startLatch, then cancels the future after a delay
		Thread canceller = new Thread(() -> {
			try {
				startLatch.await();
				Thread.sleep(500);
				future.cancel(true);
			} catch (InterruptedException ignored) {
			}
		});
		canceller.start();

		IOException ex = assertThrows(IOException.class, () -> {
			startLatch.countDown();
			request.executeInternal(headers, null);
		});

		assertThat(ex)
				.isInstanceOf(HttpTimeoutException.class)
				.hasMessage("Request timed out");

		canceller.join();
	}

	@Test
	void executeInternal_withTimeout_shouldThrowIOException() throws Exception {
		Duration timeout = Duration.ofMillis(500);

		JdkClientHttpRequest request = new JdkClientHttpRequest(mockHttpClient, uri, method, executor, timeout);

		CompletableFuture<HttpResponse<InputStream>> future = new CompletableFuture<>();

		when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
				.thenReturn(future);

		HttpHeaders headers = new HttpHeaders();

		CountDownLatch startLatch = new CountDownLatch(1);

		Thread canceller = new Thread(() -> {
			try {
				startLatch.await();
				Thread.sleep(10);
				future.cancel(true);
			} catch (InterruptedException ignored) {
			}
		});
		canceller.start();

		IOException ex = assertThrows(IOException.class, () -> {
			startLatch.countDown();
			request.executeInternal(headers, null);
		});

		assertThat(ex)
				.isInstanceOf(IOException.class)
				.hasMessage("Request was cancelled");

		canceller.join();
	}

}
