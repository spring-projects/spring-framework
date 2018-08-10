/*
 * Copyright 2002-2018 the original author or authors.
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
package org.springframework.web.reactive.function.client;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Consumer;

import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.ImmediateEventExecutor;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import reactor.core.publisher.Mono;
import reactor.netty.FutureMono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpClient;
import reactor.test.StepVerifier;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.mock.web.test.server.MockWebSession;

import static org.junit.Assert.*;

/**
 *
 * @author Rossen Stoyanchev
 */
public class MyTest {

	public static void main(String[] args) throws IOException {

LoopResources resources = LoopResources.create("test-loop");
ConnectionProvider provider = ConnectionProvider.elastic("test-pool");
TcpClient tcpClient = TcpClient.create(provider).runOn(resources, false);
HttpClient httpClient = HttpClient.from(tcpClient);

WebClient webClient = WebClient.builder()
	.clientConnector(new ReactorClientHttpConnector(httpClient))
	.build();

makeCalls(webClient);

provider.dispose();
resources.dispose();

//Mono<Void> result1 = FutureMono.from(channelGroup.close());
//Mono<Void> result2 = connProvider.disposeLater();
//Mono<Void> result3 = loopResources.disposeLater();
//Mono.whenDelayError(result1, result2, result3).block(Duration.ofSeconds(5));

		System.in.read();
		System.exit(0);

	}

	private static void makeCalls(WebClient webClient) {
		webClient.get().uri("http://httpbin.org/ip")
				.retrieve()
				.bodyToMono(String.class)
				.block(Duration.ofSeconds(5));
	}

}
