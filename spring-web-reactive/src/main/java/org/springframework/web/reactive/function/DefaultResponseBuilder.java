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

package org.springframework.web.reactive.function;

import java.net.URI;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;

import org.springframework.core.Conventions;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.Assert;

/**
 * Default {@link Response.BodyBuilder} implementation.
 *
 * @author Arjen Poutsma
 */
class DefaultResponseBuilder implements Response.BodyBuilder {

	private final int statusCode;

	private final HttpHeaders headers = new HttpHeaders();

	public DefaultResponseBuilder(int statusCode) {
		this.statusCode = statusCode;
	}

	@Override
	public Response.BodyBuilder header(String headerName, String... headerValues) {
		for (String headerValue : headerValues) {
			this.headers.add(headerName, headerValue);
		}
		return this;
	}

	@Override
	public Response.BodyBuilder headers(HttpHeaders headers) {
		if (headers != null) {
			this.headers.putAll(headers);
		}
		return this;
	}

	@Override
	public Response.BodyBuilder allow(HttpMethod... allowedMethods) {
		this.headers.setAllow(new LinkedHashSet<>(Arrays.asList(allowedMethods)));
		return this;
	}

	@Override
	public Response.BodyBuilder contentLength(long contentLength) {
		this.headers.setContentLength(contentLength);
		return this;
	}

	@Override
	public Response.BodyBuilder contentType(MediaType contentType) {
		this.headers.setContentType(contentType);
		return this;
	}

	@Override
	public Response.BodyBuilder eTag(String eTag) {
		if (eTag != null) {
			if (!eTag.startsWith("\"") && !eTag.startsWith("W/\"")) {
				eTag = "\"" + eTag;
			}
			if (!eTag.endsWith("\"")) {
				eTag = eTag + "\"";
			}
		}
		this.headers.setETag(eTag);
		return this;
	}

	@Override
	public Response.BodyBuilder lastModified(ZonedDateTime lastModified) {
		ZonedDateTime gmt = lastModified.withZoneSameInstant(ZoneId.of("GMT"));
		String headerValue = DateTimeFormatter.RFC_1123_DATE_TIME.format(gmt);
		this.headers.set(HttpHeaders.LAST_MODIFIED, headerValue);
		return this;
	}

	@Override
	public Response.BodyBuilder location(URI location) {
		this.headers.setLocation(location);
		return this;
	}

	@Override
	public Response.BodyBuilder cacheControl(CacheControl cacheControl) {
		String ccValue = cacheControl.getHeaderValue();
		if (ccValue != null) {
			this.headers.setCacheControl(cacheControl.getHeaderValue());
		}
		return this;
	}

	@Override
	public Response.BodyBuilder varyBy(String... requestHeaders) {
		this.headers.setVary(Arrays.asList(requestHeaders));
		return this;
	}

	@Override
	public Response<Void> build() {
		return new EmptyResponse(this.statusCode, this.headers);
	}

	@Override
	public <T extends Publisher<Void>> Response<T> build(T voidPublisher) {
		Assert.notNull(voidPublisher, "'voidPublisher' must not be null");
		return new VoidPublisherResponse<>(this.statusCode, this.headers, voidPublisher);
	}

	@Override
	public <T> Response<T> body(T body) {
		Assert.notNull(body, "'body' must not be null");
		return new BodyResponse<>(this.statusCode, this.headers, body);
	}

	@Override
	public <T, S extends Publisher<T>> Response<S> stream(S publisher, Class<T> elementClass) {
		Assert.notNull(publisher, "'publisher' must not be null");
		Assert.notNull(elementClass, "'elementClass' must not be null");
		return new PublisherResponse<>(this.statusCode, this.headers, publisher, elementClass);
	}

	@Override
	public Response<Resource> resource(Resource resource) {
		Assert.notNull(resource, "'resource' must not be null");
		return new ResourceResponse(this.statusCode, this.headers, resource);
	}

	@Override
	public <T, S extends Publisher<ServerSentEvent<T>>> Response<S> sse(S eventsPublisher) {
		Assert.notNull(eventsPublisher, "'eventsPublisher' must not be null");
		return ServerSentEventResponse
				.fromSseEvents(this.statusCode, this.headers, eventsPublisher);
	}

	@Override
	public <T, S extends Publisher<T>> Response<S> sse(S eventsPublisher, Class<T> eventClass) {
		Assert.notNull(eventsPublisher, "'eventsPublisher' must not be null");
		Assert.notNull(eventClass, "'eventClass' must not be null");
		return ServerSentEventResponse
				.fromPublisher(this.statusCode, this.headers, eventsPublisher, eventClass);
	}

	@Override
	public Response<Rendering> render(String name, Object... modelAttributes) {
		Map<String, Object> modelMap = Arrays.stream(modelAttributes)
				.filter(o -> !isEmptyCollection(o))
				.collect(Collectors.toMap(Conventions::getVariableName, o -> o));
		return new RenderingResponse(this.statusCode, this.headers, name, modelMap);
	}

	@Override
	public Response<Rendering> render(String name, Map<String, ?> model) {
		Assert.hasLength(name, "'name' must not be empty");
		Map<String, Object> modelMap = new LinkedHashMap<>();
		if (model != null) {
			modelMap.putAll(model);
		}
		return new RenderingResponse(this.statusCode, this.headers, name, modelMap);
	}

	private static boolean isEmptyCollection(Object o) {
		return o instanceof Collection && ((Collection<?>) o).isEmpty();
	}

}
