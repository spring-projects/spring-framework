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

package org.springframework.http.client.reactive;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.function.Function;

import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseCookie;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.MultiValueMap;

/**
 * {@link ClientHttpResponse} for the Java {@link HttpClient}.
 *
 * @author Julien Eyraud
 * @author Rossen Stoyanchev
 * @since 6.0
 */
class JdkClientHttpResponse extends AbstractClientHttpResponse {

	public JdkClientHttpResponse(
			HttpResponse<Flow.Publisher<List<ByteBuffer>>> response,
			DataBufferFactory bufferFactory, MultiValueMap<String, ResponseCookie> cookies) {

		super(HttpStatusCode.valueOf(response.statusCode()),
				adaptHeaders(response), cookies, adaptBody(response, bufferFactory)
		);
	}

	private static HttpHeaders adaptHeaders(HttpResponse<Flow.Publisher<List<ByteBuffer>>> response) {
		Map<String, List<String>> rawHeaders = response.headers().map();
		Map<String, List<String>> map = new LinkedCaseInsensitiveMap<>(rawHeaders.size(), Locale.ROOT);
		MultiValueMap<String, String> multiValueMap = CollectionUtils.toMultiValueMap(map);
		multiValueMap.putAll(rawHeaders);
		return HttpHeaders.readOnlyHttpHeaders(multiValueMap);
	}

	private static Flux<DataBuffer> adaptBody(
			HttpResponse<Flow.Publisher<List<ByteBuffer>>> response, DataBufferFactory bufferFactory) {

		return JdkFlowAdapter.flowPublisherToFlux(response.body())
				.flatMapIterable(Function.identity())
				.map(bufferFactory::wrap)
				.doOnDiscard(DataBuffer.class, DataBufferUtils::release)
				.cache(0);
	}

}
