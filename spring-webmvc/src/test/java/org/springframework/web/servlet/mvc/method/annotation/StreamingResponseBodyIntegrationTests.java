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

package org.springframework.web.servlet.mvc.method.annotation;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link StreamingResponseBody} on a running Servlet
 * container, checking when written bytes actually reach the client.
 */
class StreamingResponseBodyIntegrationTests {

	private static final String FIRST = "first-chunk";

	private static final String SECOND = "second-chunk";

	private static final CountDownLatch firstChunkReceived = new CountDownLatch(1);

	private static Server server;

	private static int port;


	@BeforeAll
	static void startServer() throws Exception {
		server = new Server(0);

		ServletHolder servlet = new ServletHolder(DispatcherServlet.class);
		servlet.setInitParameter("contextConfigLocation", TestConfig.class.getName());
		servlet.setInitParameter("contextClass", AnnotationConfigWebApplicationContext.class.getName());
		servlet.setAsyncSupported(true);

		ServletContextHandler handler = new ServletContextHandler();
		handler.setContextPath("/");
		handler.addServlet(servlet, "/");

		server.setHandler(handler);
		server.start();
		port = ((NetworkConnector) server.getConnectors()[0]).getLocalPort();
	}

	@AfterAll
	static void stopServer() throws Exception {
		if (server != null) {
			server.stop();
		}
	}


	@Test
	void flushSendsWrittenBytesToClient() throws Exception {
		try (Socket socket = new Socket("localhost", port)) {
			socket.setSoTimeout(10_000);
			OutputStream request = socket.getOutputStream();
			request.write(("GET /stream HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n")
					.getBytes(StandardCharsets.US_ASCII));
			request.flush();

			// Read until the first chunk shows up, then let the server write the second one.
			// If the flush was ignored, the first chunk only arrives together with the second
			// one, once the server gave up waiting and completed the response.
			InputStream input = socket.getInputStream();
			ByteArrayOutputStream received = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			while (!received.toString(StandardCharsets.US_ASCII).contains(FIRST)) {
				int count = input.read(buffer);
				assertThat(count).as("connection closed before the first chunk").isPositive();
				received.write(buffer, 0, count);
			}
			assertThat(received.toString(StandardCharsets.US_ASCII)).doesNotContain(SECOND);
			firstChunkReceived.countDown();

			received.write(input.readAllBytes());
			assertThat(received.toString(StandardCharsets.US_ASCII)).contains(SECOND);
		}
	}


	@Configuration
	@EnableWebMvc
	static class TestConfig {

		@Bean
		StreamingController streamingController() {
			return new StreamingController();
		}
	}


	@RestController
	static class StreamingController {

		@GetMapping("/stream")
		StreamingResponseBody stream() {
			return outputStream -> {
				outputStream.write(FIRST.getBytes(StandardCharsets.US_ASCII));
				outputStream.flush();
				try {
					firstChunkReceived.await(2, TimeUnit.SECONDS);
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
				outputStream.write(SECOND.getBytes(StandardCharsets.US_ASCII));
			};
		}
	}

}
