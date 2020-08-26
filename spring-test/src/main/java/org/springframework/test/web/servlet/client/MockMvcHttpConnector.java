/*
 * Copyright 2002-2020 the original author or authors.
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
package org.springframework.test.web.servlet.client;

import java.io.StringWriter;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import javax.servlet.http.Cookie;

import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.ResponseCookie;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.codec.multipart.DefaultPartHttpMessageReader;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.lang.Nullable;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.mock.http.client.reactive.MockClientHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockPart;
import org.springframework.test.web.reactive.server.MockServerClientHttpResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;

/**
 * Connector that handles requests by invoking a {@link MockMvc} rather than
 * making actual requests over HTTP.
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 */
public class MockMvcHttpConnector implements ClientHttpConnector {

	private static final DefaultPartHttpMessageReader MULTIPART_READER = new DefaultPartHttpMessageReader();

	private static final Duration TIMEOUT = Duration.ofSeconds(5);


	private final MockMvc mockMvc;


	public MockMvcHttpConnector(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}


	@Override
	public Mono<ClientHttpResponse> connect(
			HttpMethod method, URI uri, Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

		RequestBuilder requestBuilder = adaptRequest(method, uri, requestCallback);
		try {
			MvcResult mvcResult = this.mockMvc.perform(requestBuilder).andReturn();
			if (mvcResult.getRequest().isAsyncStarted()) {
				mvcResult.getAsyncResult();
				mvcResult = this.mockMvc.perform(asyncDispatch(mvcResult)).andReturn();
			}
			return Mono.just(adaptResponse(mvcResult));
		}
		catch (Exception ex) {
			return Mono.error(ex);
		}
	}

	private RequestBuilder adaptRequest(
			HttpMethod httpMethod, URI uri, Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

		MockClientHttpRequest httpRequest = new MockClientHttpRequest(httpMethod, uri);

		AtomicReference<byte[]> contentRef = new AtomicReference<>();
		httpRequest.setWriteHandler(dataBuffers ->
				DataBufferUtils.join(dataBuffers)
						.doOnNext(buffer -> {
							byte[] bytes = new byte[buffer.readableByteCount()];
							buffer.read(bytes);
							DataBufferUtils.release(buffer);
							contentRef.set(bytes);
						})
						.then());

		// Initialize the client request
		requestCallback.apply(httpRequest).block(TIMEOUT);

		MockHttpServletRequestBuilder requestBuilder =
				initRequestBuilder(httpMethod, uri, httpRequest, contentRef.get());

		requestBuilder.headers(httpRequest.getHeaders());
		for (List<HttpCookie> cookies : httpRequest.getCookies().values()) {
			for (HttpCookie cookie : cookies) {
				requestBuilder.cookie(new Cookie(cookie.getName(), cookie.getValue()));
			}
		}

		return requestBuilder;
	}

	private MockHttpServletRequestBuilder initRequestBuilder(
			HttpMethod httpMethod, URI uri, MockClientHttpRequest httpRequest, @Nullable byte[] bytes) {

		String contentType = httpRequest.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
		if (!StringUtils.startsWithIgnoreCase(contentType, "multipart/")) {
			MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.request(httpMethod, uri);
			if (!ObjectUtils.isEmpty(bytes)) {
				requestBuilder.content(bytes);
			}
			return requestBuilder;
		}

		// Parse the multipart request in order to adapt to Servlet Part's
		MockMultipartHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.multipart(uri);

		Assert.notNull(bytes, "No multipart content");
		ReactiveHttpInputMessage inputMessage = MockServerHttpRequest.post(uri.toString())
				.headers(httpRequest.getHeaders())
				.body(Mono.just(DefaultDataBufferFactory.sharedInstance.wrap(bytes)));

		MULTIPART_READER.read(ResolvableType.forClass(Part.class), inputMessage, Collections.emptyMap())
				.flatMap(part ->
						DataBufferUtils.join(part.content())
								.doOnNext(buffer -> {
									byte[] partBytes = new byte[buffer.readableByteCount()];
									buffer.read(partBytes);
									DataBufferUtils.release(buffer);

									// Adapt to javax.servlet.http.Part...
									MockPart mockPart = (part instanceof FilePart ?
											new MockPart(part.name(), ((FilePart) part).filename(), partBytes) :
											new MockPart(part.name(), partBytes));
									mockPart.getHeaders().putAll(part.headers());
									requestBuilder.part(mockPart);
								}))
				.blockLast(TIMEOUT);

		return requestBuilder;
	}

	private MockClientHttpResponse adaptResponse(MvcResult mvcResult) {
		MockClientHttpResponse clientResponse = new MockMvcServerClientHttpResponse(mvcResult);
		MockHttpServletResponse servletResponse = mvcResult.getResponse();
		for (String header : servletResponse.getHeaderNames()) {
			for (String value : servletResponse.getHeaders(header)) {
				clientResponse.getHeaders().add(header, value);
			}
		}
		if (servletResponse.getForwardedUrl() != null) {
			clientResponse.getHeaders().add("Forwarded-Url", servletResponse.getForwardedUrl());
		}
		for (Cookie cookie : servletResponse.getCookies()) {
			ResponseCookie httpCookie =
					ResponseCookie.fromClientResponse(cookie.getName(), cookie.getValue())
							.maxAge(Duration.ofSeconds(cookie.getMaxAge()))
							.domain(cookie.getDomain())
							.path(cookie.getPath())
							.secure(cookie.getSecure())
							.httpOnly(cookie.isHttpOnly())
							.build();
			clientResponse.getCookies().add(httpCookie.getName(), httpCookie);
		}
		byte[] bytes = servletResponse.getContentAsByteArray();
		DefaultDataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);
		clientResponse.setBody(Mono.just(dataBuffer));
		return clientResponse;
	}


	private static class MockMvcServerClientHttpResponse
			extends MockClientHttpResponse implements MockServerClientHttpResponse {

		private final MvcResult mvcResult;


		public MockMvcServerClientHttpResponse(MvcResult result) {
			super(result.getResponse().getStatus());
			this.mvcResult = new PrintingMvcResult(result);
		}

		@Override
		public Object getServerResult() {
			return this.mvcResult;
		}
	}


	private static class PrintingMvcResult implements MvcResult {

		private final MvcResult mvcResult;

		public PrintingMvcResult(MvcResult mvcResult) {
			this.mvcResult = mvcResult;
		}

		@Override
		public MockHttpServletRequest getRequest() {
			return this.mvcResult.getRequest();
		}

		@Override
		public MockHttpServletResponse getResponse() {
			return this.mvcResult.getResponse();
		}

		@Nullable
		@Override
		public Object getHandler() {
			return this.mvcResult.getHandler();
		}

		@Nullable
		@Override
		public HandlerInterceptor[] getInterceptors() {
			return this.mvcResult.getInterceptors();
		}

		@Nullable
		@Override
		public ModelAndView getModelAndView() {
			return this.mvcResult.getModelAndView();
		}

		@Nullable
		@Override
		public Exception getResolvedException() {
			return this.mvcResult.getResolvedException();
		}

		@Override
		public FlashMap getFlashMap() {
			return this.mvcResult.getFlashMap();
		}

		@Override
		public Object getAsyncResult() {
			return this.mvcResult.getAsyncResult();
		}

		@Override
		public Object getAsyncResult(long timeToWait) {
			return this.mvcResult.getAsyncResult(timeToWait);
		}

		@Override
		public String toString() {
			StringWriter writer = new StringWriter();
			try {
				MockMvcResultHandlers.print(writer).handle(this);
			}
			catch (Exception ex) {
				writer.append("Unable to format ")
						.append(String.valueOf(this))
						.append(": ")
						.append(ex.getMessage());
			}
			return writer.toString();
		}
	}

}
