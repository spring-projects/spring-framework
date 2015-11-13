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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.reactivestreams.Publisher;
import reactor.Publishers;

import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.MediaType;
import org.springframework.http.server.ReactiveServerHttpRequest;
import org.springframework.http.server.ReactiveServerHttpResponse;
import org.springframework.reactive.codec.encoder.Encoder;
import org.springframework.reactive.web.dispatch.HandlerResult;
import org.springframework.reactive.web.dispatch.HandlerResultHandler;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;


/**
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 * @author Sebastien Deleuze
 */
public class ResponseBodyResultHandler implements HandlerResultHandler, Ordered {

	private static final MediaType MEDIA_TYPE_APPLICATION = new MediaType("application");

	private final List<MediaType> allSupportedMediaTypes;

	private final List<Encoder<?>> encoders;

	private final ConversionService conversionService;

	private int order = 0;


	public ResponseBodyResultHandler(List<Encoder<?>> encoders, ConversionService service) {
		Assert.notEmpty(encoders, "At least one encoders is required.");
		Assert.notNull(service, "'conversionService' is required.");
		this.encoders = encoders;
		this.allSupportedMediaTypes = getAllSupportedMediaTypes(encoders);
		this.conversionService = service;
	}

	private static List<MediaType> getAllSupportedMediaTypes(List<Encoder<?>> encoders) {
		Set<MediaType> allSupportedMediaTypes = new LinkedHashSet<>();
		for (Encoder<?> encoder : encoders) {
			for (MimeType mimeType : encoder.getSupportedMimeTypes()) {
				allSupportedMediaTypes.add(
						new MediaType(mimeType.getType(), mimeType.getSubtype(), mimeType.getParameters()));
			}
		}
		List<MediaType> result = new ArrayList<>(allSupportedMediaTypes);
		MediaType.sortBySpecificity(result);
		return Collections.unmodifiableList(result);
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
			Method method = ((HandlerMethod) handler).getMethod();
			return AnnotatedElementUtils.isAnnotated(method, ResponseBody.class.getName());
		}
		return false;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Publisher<Void> handleResult(ReactiveServerHttpRequest request,
			ReactiveServerHttpResponse response, HandlerResult result) {

		Object value = result.getValue();
		if (value == null) {
			return Publishers.empty();
		}

		HandlerMethod hm = (HandlerMethod) result.getHandler();
		ResolvableType returnType = ResolvableType.forMethodParameter(hm.getReturnValueType(value));

		List<MediaType> requestedMediaTypes = getAcceptableMediaTypes(request);
		List<MediaType> producibleMediaTypes = getProducibleMediaTypes(returnType);

		if (producibleMediaTypes.isEmpty()) {
			Publishers.error(new IllegalArgumentException(
					"No encoder found for return value of type: " + returnType));
		}

		Set<MediaType> compatibleMediaTypes = new LinkedHashSet<>();
		for (MediaType requestedType : requestedMediaTypes) {
			for (MediaType producibleType : producibleMediaTypes) {
				if (requestedType.isCompatibleWith(producibleType)) {
					compatibleMediaTypes.add(getMostSpecificMediaType(requestedType, producibleType));
				}
			}
		}
		if (compatibleMediaTypes.isEmpty()) {
			return Publishers.error(new HttpMediaTypeNotAcceptableException(producibleMediaTypes));
		}

		List<MediaType> mediaTypes = new ArrayList<>(compatibleMediaTypes);
		MediaType.sortBySpecificityAndQuality(mediaTypes);

		MediaType selectedMediaType = null;
		for (MediaType mediaType : mediaTypes) {
			if (mediaType.isConcrete()) {
				selectedMediaType = mediaType;
				break;
			}
			else if (mediaType.equals(MediaType.ALL) || mediaType.equals(MEDIA_TYPE_APPLICATION)) {
				selectedMediaType = MediaType.APPLICATION_OCTET_STREAM;
				break;
			}
		}

		if (selectedMediaType != null) {
			Publisher<?> publisher;
			ResolvableType elementType;
			if (this.conversionService.canConvert(returnType.getRawClass(), Publisher.class)) {
				publisher = this.conversionService.convert(value, Publisher.class);
				elementType = returnType.getGeneric(0);
			}
			else {
				publisher = Publishers.just(value);
				elementType = returnType;
			}
			Encoder<?> encoder = resolveEncoder(elementType, selectedMediaType);
			if (encoder != null) {
				response.getHeaders().setContentType(selectedMediaType);
				return response.setBody(encoder.encode((Publisher) publisher, elementType, selectedMediaType));
			}
		}

		return Publishers.error(new HttpMediaTypeNotAcceptableException(this.allSupportedMediaTypes));
	}

	private List<MediaType> getAcceptableMediaTypes(ReactiveServerHttpRequest request) {
		List<MediaType> mediaTypes = request.getHeaders().getAccept();
		return (mediaTypes.isEmpty() ? Collections.singletonList(MediaType.ALL) : mediaTypes);
	}

	private List<MediaType> getProducibleMediaTypes(ResolvableType type) {
		List<MediaType> result = new ArrayList<>();
		for (Encoder<?> encoder : this.encoders) {
			if (encoder.canEncode(type, null)) {
				for (MimeType mimeType : encoder.getSupportedMimeTypes()) {
					result.add(new MediaType(mimeType.getType(), mimeType.getSubtype(),
							mimeType.getParameters()));
				}
			}
		}
		if (result.isEmpty()) {
			result.add(MediaType.ALL);
		}
		return result;
	}

	/**
	 * Return the more specific of the acceptable and the producible media types
	 * with the q-value of the former.
	 */
	private MediaType getMostSpecificMediaType(MediaType acceptType, MediaType produceType) {
		produceType = produceType.copyQualityValue(acceptType);
		Comparator<MediaType> comparator = MediaType.SPECIFICITY_COMPARATOR;
		return (comparator.compare(acceptType, produceType) <= 0 ? acceptType : produceType);
	}

	private Encoder<?> resolveEncoder(ResolvableType type, MediaType mediaType, Object... hints) {
		for (Encoder<?> encoder : this.encoders) {
			if (encoder.canEncode(type, mediaType, hints)) {
				return encoder;
			}
		}
		return null;
	}

}
