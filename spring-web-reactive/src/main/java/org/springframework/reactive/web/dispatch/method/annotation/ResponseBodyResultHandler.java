/*
 * Copyright 2002-2015 the original author or authors.
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
package org.springframework.reactive.web.dispatch.method.annotation;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.reactivestreams.Publisher;
import reactor.rx.Promise;
import reactor.rx.Streams;
import rx.Observable;
import rx.RxReactiveStreams;
import rx.Single;

import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.reactive.codec.encoder.MessageToByteEncoder;
import org.springframework.reactive.util.CompletableFutureUtils;
import org.springframework.reactive.web.dispatch.HandlerResult;
import org.springframework.reactive.web.dispatch.HandlerResultHandler;
import org.springframework.reactive.web.http.ServerHttpRequest;
import org.springframework.reactive.web.http.ServerHttpResponse;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;


/**
 * First version using {@link MessageToByteEncoder}s
 *
 * @author Rossen Stoyanchev
 */
public class ResponseBodyResultHandler implements HandlerResultHandler, Ordered {

	private static final Charset UTF_8 = Charset.forName("UTF-8");


	private final List<MessageToByteEncoder<?>> serializers;
	private final List<MessageToByteEncoder<ByteBuffer>> postProcessors;

	private int order = 0;


	public ResponseBodyResultHandler(List<MessageToByteEncoder<?>> serializers) {
		this(serializers, Collections.EMPTY_LIST);
	}

	public ResponseBodyResultHandler(List<MessageToByteEncoder<?>> serializers, List<MessageToByteEncoder<ByteBuffer>> postProcessors) {
		this.serializers = serializers;
		this.postProcessors = postProcessors;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	@Override
	public boolean supports(HandlerResult result) {
		Object handler = result.getHandler();
		if (handler instanceof HandlerMethod) {
			HandlerMethod handlerMethod = (HandlerMethod) handler;
			Type publisherVoidType = new ParameterizedTypeReference<Publisher<Void>>(){}.getType();
			return AnnotatedElementUtils.isAnnotated(handlerMethod.getMethod(), ResponseBody.class.getName()) &&
					!handlerMethod.getReturnType().getGenericParameterType().equals(publisherVoidType);
		}
		return false;
	}

	@Override
	public Publisher<Void> handleResult(ServerHttpRequest request, ServerHttpResponse response,
			HandlerResult result) {

		Object value = result.getValue();
		HandlerMethod handlerMethod = (HandlerMethod) result.getHandler();
		MethodParameter returnType = handlerMethod.getReturnValueType(value);

		if (value == null) {
			return Streams.empty();
		}

		MediaType mediaType = resolveMediaType(request);
		ResolvableType type = ResolvableType.forMethodParameter(returnType);
		List<Object> hints = new ArrayList<>();
		hints.add(UTF_8);
		MessageToByteEncoder<Object> serializer = (MessageToByteEncoder<Object>)resolveSerializer(request, type, mediaType, hints.toArray());
		if (serializer != null) {
			Publisher<Object> elementStream;

			// TODO: Refactor type conversion
			if (Promise.class.isAssignableFrom(type.getRawClass())) {
				elementStream = ((Promise)value).stream();
			}
			else if (Observable.class.isAssignableFrom(type.getRawClass())) {
				elementStream = RxReactiveStreams.toPublisher((Observable) value);
			}
			else if (Single.class.isAssignableFrom(type.getRawClass())) {
				elementStream = RxReactiveStreams.toPublisher(((Single)value).toObservable());
			}
			else if (CompletableFuture.class.isAssignableFrom(type.getRawClass())) {
				elementStream = CompletableFutureUtils.toPublisher((CompletableFuture) value);
			}
			else if (Publisher.class.isAssignableFrom(type.getRawClass())) {
				elementStream = (Publisher)value;
			}
			else {
				elementStream = Streams.just(value);
			}

			Publisher<ByteBuffer> outputStream = serializer.encode(elementStream, type, mediaType, hints.toArray());
			List<MessageToByteEncoder<ByteBuffer>> postProcessors = resolvePostProcessors(request, type, mediaType, hints.toArray());
			for (MessageToByteEncoder<ByteBuffer> postProcessor : postProcessors) {
				outputStream = postProcessor.encode(outputStream, type, mediaType, hints.toArray());
			}
			response.getHeaders().setContentType(mediaType);
			return response.writeWith(Streams.wrap(outputStream));
		}
		return Streams.fail(new IllegalStateException(
				"Return value type not supported: " + returnType));
	}

	private MediaType resolveMediaType(ServerHttpRequest request) {
		String acceptHeader = request.getHeaders().getFirst(HttpHeaders.ACCEPT);
		List<MediaType> mediaTypes = MediaType.parseMediaTypes(acceptHeader);
		MediaType.sortBySpecificityAndQuality(mediaTypes);
		return ( mediaTypes.size() > 0 ? mediaTypes.get(0) : MediaType.TEXT_PLAIN);
	}

	private MessageToByteEncoder<?> resolveSerializer(ServerHttpRequest request, ResolvableType type, MediaType mediaType, Object[] hints) {
		for (MessageToByteEncoder<?> codec : this.serializers) {
			if (codec.canEncode(type, mediaType, hints)) {
				return codec;
			}
		}
		return null;
	}

	private List<MessageToByteEncoder<ByteBuffer>> resolvePostProcessors(ServerHttpRequest request, ResolvableType type, MediaType mediaType, Object[] hints) {
		List<MessageToByteEncoder<ByteBuffer>> postProcessors = new ArrayList<>();
		for (MessageToByteEncoder<ByteBuffer> postProcessor : this.postProcessors) {
			if (postProcessor.canEncode(type, mediaType, hints)) {
				postProcessors.add(postProcessor);
			}
		}
		return postProcessors;
	}

}
