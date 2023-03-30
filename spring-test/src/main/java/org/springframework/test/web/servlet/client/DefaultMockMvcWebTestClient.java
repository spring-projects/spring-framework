package org.springframework.test.web.servlet.client;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.test.web.reactive.server.DefaultWebTestClient;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.util.UriBuilderFactory;

public class DefaultMockMvcWebTestClient extends DefaultWebTestClient implements MockMvcWebTestClient {
	private final DefaultMockMvcWebTestClientBuilder builder;

	DefaultMockMvcWebTestClient(ClientHttpConnector connector,
			Function<ClientHttpConnector, ExchangeFunction> exchangeFactory, UriBuilderFactory uriBuilderFactory,
			HttpHeaders headers, MultiValueMap<String, String> cookies,
			Consumer<EntityExchangeResult<?>> entityResultConsumer, Duration responseTimeout,
			DefaultMockMvcWebTestClientBuilder clientBuilder) {
		super(connector, exchangeFactory, uriBuilderFactory, headers, cookies, entityResultConsumer, responseTimeout,
				clientBuilder);
		this.builder = clientBuilder;
	}

	@Override
	public MockMvcWebTestClient.Builder mutate() {
		return new DefaultMockMvcWebTestClientBuilder(builder);
	}

	@Override
	public MockMvcWebTestClient mutateWith(RequestPostProcessor requestPostProcessor) {
		return mutate().requestPostProcessor(requestPostProcessor).build();
	}

	@Override
	public MockMvcWebTestClient mutateWith(MockMvcWebTestClientConfigurer configurer) {
		return mutate().apply(configurer).build();
	}
}
