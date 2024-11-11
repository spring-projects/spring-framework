/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.http.client.reactive;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import io.netty5.util.AttributeKey;
import reactor.core.publisher.Mono;
import reactor.netty5.NettyOutbound;
import reactor.netty5.http.client.HttpClient;
import reactor.netty5.http.client.HttpClientRequest;
import reactor.netty5.resources.ConnectionProvider;
import reactor.netty5.resources.LoopResources;

import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * Reactor Netty 2 (Netty 5) implementation of {@link ClientHttpConnector}.
 *
 * <p>This class is based on {@link ReactorClientHttpConnector}.
 *
 * @author Violeta Georgieva
 * @since 6.0
 * @see HttpClient
 */
public class ReactorNetty2ClientHttpConnector implements ClientHttpConnector {

	/**
	 * Channel attribute key under which {@code WebClient} request attributes are stored as a Map.
	 * @since 6.2
	 */
	public static final AttributeKey<Map<String, Object>> ATTRIBUTES_KEY =
			AttributeKey.valueOf(ReactorNetty2ClientHttpRequest.class.getName() + ".ATTRIBUTES");

	private static final Function<HttpClient, HttpClient> defaultInitializer = client -> client.compress(true);


	private final HttpClient httpClient;


	/**
	 * Default constructor. Initializes {@link HttpClient} via:
	 * <pre class="code">
	 * HttpClient.create().compress()
	 * </pre>
	 */
	public ReactorNetty2ClientHttpConnector() {
		this.httpClient = defaultInitializer.apply(HttpClient.create().wiretap(true));
	}

	/**
	 * Constructor with externally managed Reactor Netty resources, including
	 * {@link LoopResources} for event loop threads, and {@link ConnectionProvider}
	 * for the connection pool.
	 * <p>This constructor should be used only when you don't want the client
	 * to participate in the Reactor Netty global resources. By default, the
	 * client participates in the Reactor Netty global resources held in
	 * {@link reactor.netty5.http.HttpResources}, which is recommended since
	 * fixed, shared resources are favored for event loop concurrency. However,
	 * consider declaring a {@link ReactorNetty2ResourceFactory} bean with
	 * {@code globalResources=true} in order to ensure the Reactor Netty global
	 * resources are shut down when the Spring ApplicationContext is closed.
	 * @param factory the resource factory to obtain the resources from
	 * @param mapper a mapper for further initialization of the created client
	 * @since 5.1
	 */
	public ReactorNetty2ClientHttpConnector(ReactorNetty2ResourceFactory factory, Function<HttpClient, HttpClient> mapper) {
		ConnectionProvider provider = factory.getConnectionProvider();
		Assert.notNull(provider, "No ConnectionProvider: is ReactorNetty2ResourceFactory not initialized yet?");
		this.httpClient = defaultInitializer.andThen(mapper).andThen(applyLoopResources(factory))
				.apply(HttpClient.create(provider));
	}

	private static Function<HttpClient, HttpClient> applyLoopResources(ReactorNetty2ResourceFactory factory) {
		return httpClient -> {
			LoopResources resources = factory.getLoopResources();
			Assert.notNull(resources, "No LoopResources: is ReactorNetty2ResourceFactory not initialized yet?");
			return httpClient.runOn(resources);
		};
	}


	/**
	 * Constructor with a pre-configured {@code HttpClient} instance.
	 * @param httpClient the client to use
	 * @since 5.1
	 */
	public ReactorNetty2ClientHttpConnector(HttpClient httpClient) {
		Assert.notNull(httpClient, "HttpClient is required");
		this.httpClient = httpClient;
	}


	@Override
	public Mono<ClientHttpResponse> connect(HttpMethod method, URI uri,
			Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

		AtomicReference<ReactorNetty2ClientHttpResponse> responseRef = new AtomicReference<>();

		HttpClient.RequestSender requestSender = this.httpClient
				.request(io.netty5.handler.codec.http.HttpMethod.valueOf(method.name()));

		requestSender = (uri.isAbsolute() ? requestSender.uri(uri) : requestSender.uri(uri.toString()));

		return requestSender
				.send((request, outbound) -> requestCallback.apply(adaptRequest(method, uri, request, outbound)))
				.responseConnection((response, connection) -> {
					responseRef.set(new ReactorNetty2ClientHttpResponse(response, connection));
					return Mono.just((ClientHttpResponse) responseRef.get());
				})
				.next()
				.doOnCancel(() -> {
					ReactorNetty2ClientHttpResponse response = responseRef.get();
					if (response != null) {
						response.releaseAfterCancel(method);
					}
				});
	}

	private ReactorNetty2ClientHttpRequest adaptRequest(HttpMethod method, URI uri, HttpClientRequest request,
			NettyOutbound nettyOutbound) {

		return new ReactorNetty2ClientHttpRequest(method, uri, request, nettyOutbound);
	}

}
