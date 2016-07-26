/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.client.reactive;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.TestSubscriber;

import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.json.Jackson2JsonDecoder;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.eq;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ResponseExtractors}.
 *
 * @author Brian Clozel
 */
public class ResponseExtractorsTests {

	private HttpHeaders headers;

	private ClientHttpResponse response;

	private List<HttpMessageReader<?>> messageReaders;

	private WebClientConfig webClientConfig;

	private ResponseErrorHandler errorHandler;


	@Before
	public void setup() throws Exception {
		this.headers = new HttpHeaders();
		this.response = mock(ClientHttpResponse.class);
		given(this.response.getHeaders()).willReturn(headers);
		this.messageReaders = Arrays.asList(
				new DecoderHttpMessageReader<>(new StringDecoder()),
				new DecoderHttpMessageReader<>(new Jackson2JsonDecoder()));
		this.webClientConfig = mock(WebClientConfig.class);
		this.errorHandler = mock(ResponseErrorHandler.class);
		given(this.webClientConfig.getMessageReaders()).willReturn(this.messageReaders);
		given(this.webClientConfig.getResponseErrorHandler()).willReturn(this.errorHandler);
	}


	@Test
	public void shouldExtractResponseEntityMono() throws Exception {
		this.headers.setContentType(MediaType.TEXT_PLAIN);
		given(this.response.getStatusCode()).willReturn(HttpStatus.OK);
		given(this.response.getBody()).willReturn(createFluxBody("test content"));

		Mono<ResponseEntity<String>> result = ResponseExtractors.response(String.class)
				.extract(Mono.just(this.response), this.webClientConfig);

		TestSubscriber.subscribe(result)
				.awaitAndAssertNextValuesWith(entity -> {
					assertThat(entity.getStatusCode(), is(HttpStatus.OK));
					assertThat(entity.getHeaders().getContentType(), is(MediaType.TEXT_PLAIN));
					assertThat(entity.getBody(), is("test content"));
				})
				.assertComplete();
	}

	@Test
	public void shouldExtractResponseEntityFlux() throws Exception {
		this.headers.setContentType(MediaType.TEXT_PLAIN);
		given(this.response.getStatusCode()).willReturn(HttpStatus.OK);
		given(this.response.getBody()).willReturn(createFluxBody("test", " content"));

		Mono<ResponseEntity<String>> result = ResponseExtractors.response(String.class)
				.extract(Mono.just(this.response), this.webClientConfig);

		TestSubscriber.subscribe(result)
				.awaitAndAssertNextValuesWith(entity -> {
					assertThat(entity.getStatusCode(), is(HttpStatus.OK));
					assertThat(entity.getHeaders().getContentType(), is(MediaType.TEXT_PLAIN));
					assertThat(entity.getBody(), is("test content"));
				})
				.assertComplete();
	}

	@Test
	public void shouldExtractResponseEntityWithEmptyBody() throws Exception {
		given(this.response.getStatusCode()).willReturn(HttpStatus.NO_CONTENT);
		given(this.response.getBody()).willReturn(Flux.empty());

		Mono<ResponseEntity<String>> result = ResponseExtractors.response(String.class)
				.extract(Mono.just(this.response), this.webClientConfig);

		TestSubscriber.subscribe(result)
				.awaitAndAssertNextValuesWith(entity -> {
					assertThat(entity.getStatusCode(), is(HttpStatus.NO_CONTENT));
					assertNull(entity.getBody());
				})
				.assertComplete();
	}

	@Test
	public void shouldExtractResponseEntityAsStream() throws Exception {
		this.headers.setContentType(MediaType.TEXT_PLAIN);
		given(this.response.getStatusCode()).willReturn(HttpStatus.OK);
		given(this.response.getBody()).willReturn(createFluxBody("test", " content"));

		Mono<ResponseEntity<Flux<String>>> result = ResponseExtractors.responseStream(String.class)
				.extract(Mono.just(this.response), this.webClientConfig);

		TestSubscriber.subscribe(result)
				.awaitAndAssertNextValuesWith(entity -> {
					assertThat(entity.getStatusCode(), is(HttpStatus.OK));
					assertThat(entity.getHeaders().getContentType(), is(MediaType.TEXT_PLAIN));
					TestSubscriber.subscribe(entity.getBody())
							.awaitAndAssertNextValues("test", " content")
							.assertComplete();
				})
				.assertComplete();
	}

	@Test
	public void shouldGetErrorWhenExtractingWithMissingConverter() throws Exception {
		this.headers.setContentType(MediaType.APPLICATION_XML);
		given(this.response.getStatusCode()).willReturn(HttpStatus.OK);
		given(this.response.getBody()).willReturn(createFluxBody("test content"));

		Mono<ResponseEntity<SomePojo>> result = ResponseExtractors.response(SomePojo.class)
				.extract(Mono.just(this.response), this.webClientConfig);

		TestSubscriber.subscribe(result)
				.assertErrorWith(t -> {
					assertThat(t, instanceOf(WebClientException.class));
					WebClientException exc = (WebClientException) t;
					assertThat(exc.getMessage(), containsString("Could not decode response body of type 'application/xml'"));
					assertThat(exc.getMessage(), containsString("$SomePojo"));
				});
	}

	@Test
	public void shouldExtractResponseHeaders() throws Exception {
		this.headers.setContentType(MediaType.TEXT_PLAIN);
		this.headers.setETag("\"Spring\"");
		given(this.response.getStatusCode()).willReturn(HttpStatus.OK);

		Mono<HttpHeaders> result = ResponseExtractors.headers()
				.extract(Mono.just(this.response), this.webClientConfig);

		TestSubscriber.subscribe(result)
				.awaitAndAssertNextValuesWith(headers -> {
					assertThat(headers.getContentType(), is(MediaType.TEXT_PLAIN));
					assertThat(headers.getETag(), is("\"Spring\""));
				})
				.assertComplete();
	}

	@Test
	public void shouldExecuteResponseHandler() throws Exception {
		this.headers.setContentType(MediaType.TEXT_PLAIN);
		given(this.response.getStatusCode()).willReturn(HttpStatus.NOT_FOUND);
		given(this.response.getBody()).willReturn(createFluxBody("test", " content"));

		Mono<String> result = ResponseExtractors.body(String.class)
				.extract(Mono.just(this.response), this.webClientConfig);

		TestSubscriber.subscribe(result)
				.assertValueCount(1)
				.assertComplete();

		then(this.errorHandler).should().handleError(eq(this.response), eq(this.messageReaders));
	}


	private Flux<DataBuffer> createFluxBody(String... items) throws Exception {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		return Flux.just(items)
				.map(item -> {
					DataBuffer buffer = factory.allocateBuffer();
					try {
						buffer.write(new String(item).getBytes("UTF-8"));
					}
					catch (UnsupportedEncodingException exc) {
						Exceptions.propagate(exc);
					}
					return buffer;
				});
	}


	protected class SomePojo {

		public String foo;
	}

}
