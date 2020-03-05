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

package org.springframework.web.servlet.function;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.servlet.ModelAndView;

/**
 * Default {@link EntityResponse.Builder} implementation.
 *
 * @author Arjen Poutsma
 * @since 5.2
 * @param <T> the entity type
 */
final class DefaultEntityResponseBuilder<T> implements EntityResponse.Builder<T> {

	private static final boolean reactiveStreamsPresent = ClassUtils.isPresent(
			"org.reactivestreams.Publisher", DefaultEntityResponseBuilder.class.getClassLoader());

	private static final Type RESOURCE_REGION_LIST_TYPE =
				new ParameterizedTypeReference<List<ResourceRegion>>() { }.getType();


	private final T entity;

	private final Type entityType;

	private int status = HttpStatus.OK.value();

	private final HttpHeaders headers = new HttpHeaders();

	private final MultiValueMap<String, Cookie> cookies = new LinkedMultiValueMap<>();


	private DefaultEntityResponseBuilder(T entity, @Nullable Type entityType) {
		this.entity = entity;
		this.entityType = (entityType != null) ? entityType : entity.getClass();
	}

	@Override
	public EntityResponse.Builder<T> status(HttpStatus status) {
		Assert.notNull(status, "HttpStatus must not be null");
		this.status = status.value();
		return this;
	}

	@Override
	public EntityResponse.Builder<T> status(int status) {
		this.status = status;
		return this;
	}

	@Override
	public EntityResponse.Builder<T> cookie(Cookie cookie) {
		Assert.notNull(cookie, "Cookie must not be null");
		this.cookies.add(cookie.getName(), cookie);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> cookies(
			Consumer<MultiValueMap<String, Cookie>> cookiesConsumer) {
		cookiesConsumer.accept(this.cookies);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> header(String headerName, String... headerValues) {
		for (String headerValue : headerValues) {
			this.headers.add(headerName, headerValue);
		}
		return this;
	}

	@Override
	public EntityResponse.Builder<T> headers(Consumer<HttpHeaders> headersConsumer) {
		headersConsumer.accept(this.headers);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> allow(HttpMethod... allowedMethods) {
		this.headers.setAllow(new LinkedHashSet<>(Arrays.asList(allowedMethods)));
		return this;
	}

	@Override
	public EntityResponse.Builder<T> allow(Set<HttpMethod> allowedMethods) {
		this.headers.setAllow(allowedMethods);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> contentLength(long contentLength) {
		this.headers.setContentLength(contentLength);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> contentType(MediaType contentType) {
		this.headers.setContentType(contentType);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> eTag(String etag) {
		if (!etag.startsWith("\"") && !etag.startsWith("W/\"")) {
			etag = "\"" + etag;
		}
		if (!etag.endsWith("\"")) {
			etag = etag + "\"";
		}
		this.headers.setETag(etag);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> lastModified(ZonedDateTime lastModified) {
		this.headers.setLastModified(lastModified);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> lastModified(Instant lastModified) {
		this.headers.setLastModified(lastModified);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> location(URI location) {
		this.headers.setLocation(location);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> cacheControl(CacheControl cacheControl) {
		this.headers.setCacheControl(cacheControl);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> varyBy(String... requestHeaders) {
		this.headers.setVary(Arrays.asList(requestHeaders));
		return this;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	public EntityResponse<T> build() {
		if (this.entity instanceof CompletionStage) {
			CompletionStage completionStage = (CompletionStage) this.entity;
			return new CompletionStageEntityResponse(this.status, this.headers, this.cookies,
					completionStage, this.entityType);
		}
		else if (reactiveStreamsPresent && PublisherEntityResponse.isPublisher(this.entity)) {
			Publisher publisher = (Publisher) this.entity;
			return new PublisherEntityResponse(this.status, this.headers, this.cookies, publisher, this.entityType);
		}
		else {
			return new DefaultEntityResponse<>(this.status, this.headers, this.cookies, this.entity, this.entityType);
		}
	}


	/**
	 * Return a new {@link EntityResponse.Builder} from the given object.
	 */
	public static <T> EntityResponse.Builder<T> fromObject(T t) {
		return new DefaultEntityResponseBuilder<>(t, null);
	}

	/**
	 * Return a new {@link EntityResponse.Builder} from the given object and type reference.
	 */
	public static <T> EntityResponse.Builder<T> fromObject(T t, ParameterizedTypeReference<?> bodyType) {
		return new DefaultEntityResponseBuilder<>(t, bodyType.getType());
	}


	/**
	 * Default {@link EntityResponse} implementation for synchronous bodies.
	 */
	private static class DefaultEntityResponse<T> extends DefaultServerResponseBuilder.AbstractServerResponse
			implements EntityResponse<T> {

		private final T entity;

		private final Type entityType;

		public DefaultEntityResponse(int statusCode, HttpHeaders headers,
				MultiValueMap<String, Cookie> cookies, T entity, Type entityType) {

			super(statusCode, headers, cookies);
			this.entity = entity;
			this.entityType = entityType;
		}

		private static <T> boolean isResource(T entity) {
			return !(entity instanceof InputStreamResource) &&
					(entity instanceof Resource);
		}

		@Override
		public T entity() {
			return this.entity;
		}

		@Override
		protected ModelAndView writeToInternal(HttpServletRequest servletRequest,
				HttpServletResponse servletResponse, Context context)
				throws ServletException, IOException {

			writeEntityWithMessageConverters(this.entity, servletRequest,servletResponse, context);
			return null;
		}

		@SuppressWarnings({ "unchecked", "resource" })
		protected void writeEntityWithMessageConverters(Object entity, HttpServletRequest request,
				HttpServletResponse response, ServerResponse.Context context)
				throws ServletException, IOException {

			ServletServerHttpResponse serverResponse = new ServletServerHttpResponse(response);
			MediaType contentType = getContentType(response);
			Class<?> entityClass = entity.getClass();
			Type entityType = this.entityType;

			if (entityClass != InputStreamResource.class && Resource.class.isAssignableFrom(entityClass)) {
				serverResponse.getHeaders().set(HttpHeaders.ACCEPT_RANGES, "bytes");
				String rangeHeader = request.getHeader(HttpHeaders.RANGE);
				if (rangeHeader != null) {
					Resource resource = (Resource) entity;
					try {
						List<HttpRange> httpRanges = HttpRange.parseRanges(rangeHeader);
						serverResponse.getServletResponse().setStatus(HttpStatus.PARTIAL_CONTENT.value());
						entity = HttpRange.toResourceRegions(httpRanges, resource);
						entityClass = entity.getClass();
						entityType = RESOURCE_REGION_LIST_TYPE;
					}
					catch (IllegalArgumentException ex) {
						serverResponse.getHeaders().set(HttpHeaders.CONTENT_RANGE, "bytes */" + resource.contentLength());
						serverResponse.getServletResponse().setStatus(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.value());
					}
				}
			}

			for (HttpMessageConverter<?> messageConverter : context.messageConverters()) {
				if (messageConverter instanceof GenericHttpMessageConverter<?>) {
					GenericHttpMessageConverter<Object> genericMessageConverter =
							(GenericHttpMessageConverter<Object>) messageConverter;
					if (genericMessageConverter.canWrite(entityType, entityClass, contentType)) {
						genericMessageConverter.write(entity, entityType, contentType, serverResponse);
						return;
					}
				}
				if (messageConverter.canWrite(entityClass, contentType)) {
					((HttpMessageConverter<Object>)messageConverter).write(entity, contentType, serverResponse);
					return;
				}
			}

			List<MediaType> producibleMediaTypes = producibleMediaTypes(context.messageConverters(), entityClass);
			throw new HttpMediaTypeNotAcceptableException(producibleMediaTypes);
		}

		@Nullable
		private static MediaType getContentType(HttpServletResponse response) {
			try {
				return MediaType.parseMediaType(response.getContentType()).removeQualityValue();
			}
			catch (InvalidMediaTypeException ex) {
				return null;
			}
		}

		protected void tryWriteEntityWithMessageConverters(Object entity, HttpServletRequest request,
				HttpServletResponse response, ServerResponse.Context context) {
			try {
				writeEntityWithMessageConverters(entity, request, response, context);
			}
			catch (IOException | ServletException ex) {
				handleError(ex, request, response, context);
			}
		}

		private static List<MediaType> producibleMediaTypes(
				List<HttpMessageConverter<?>> messageConverters,
				Class<?> entityClass) {

			return messageConverters.stream()
					.filter(messageConverter -> messageConverter.canWrite(entityClass, null))
					.flatMap(messageConverter -> messageConverter.getSupportedMediaTypes().stream())
					.collect(Collectors.toList());
		}

	}


	/**
	 * {@link EntityResponse} implementation for asynchronous {@link CompletionStage} bodies.
	 */
	private static class CompletionStageEntityResponse<T> extends DefaultEntityResponse<CompletionStage<T>> {

		public CompletionStageEntityResponse(int statusCode, HttpHeaders headers,
				MultiValueMap<String, Cookie> cookies, CompletionStage<T> entity, Type entityType) {

			super(statusCode, headers, cookies, entity, entityType);
		}

		@Override
		protected ModelAndView writeToInternal(HttpServletRequest servletRequest,
				HttpServletResponse servletResponse, Context context) {

			AsyncContext asyncContext = servletRequest.startAsync(servletRequest, servletResponse);
			entity().whenComplete((entity, throwable) -> {
				try {
					if (entity != null) {

						tryWriteEntityWithMessageConverters(entity,
								(HttpServletRequest) asyncContext.getRequest(),
								(HttpServletResponse) asyncContext.getResponse(),
								context);
					}
					else if (throwable != null) {
						handleError(throwable, servletRequest, servletResponse, context);
					}
				}
				finally {
					asyncContext.complete();
				}
			});
			return null;
		}
	}


	private static class PublisherEntityResponse<T> extends DefaultEntityResponse<Publisher<T>> {

		public PublisherEntityResponse(int statusCode, HttpHeaders headers,
				MultiValueMap<String, Cookie> cookies, Publisher<T> entity, Type entityType) {

			super(statusCode, headers, cookies, entity, entityType);
		}

		@Override
		protected ModelAndView writeToInternal(HttpServletRequest servletRequest,
				HttpServletResponse servletResponse, Context context) {

			AsyncContext asyncContext = servletRequest.startAsync(servletRequest,
					new NoContentLengthResponseWrapper(servletResponse));
			entity().subscribe(new ProducingSubscriber(asyncContext, context));
			return null;
		}

		public static boolean isPublisher(Object entity) {
			return (entity instanceof Publisher);
		}


		@SuppressWarnings("SubscriberImplementation")
		private class ProducingSubscriber implements Subscriber<T> {

			private final AsyncContext asyncContext;

			private final Context context;

			@Nullable
			private Subscription subscription;

			public ProducingSubscriber(AsyncContext asyncContext, Context context) {
				this.asyncContext = asyncContext;
				this.context = context;
			}

			@Override
			public void onSubscribe(Subscription s) {
				if (this.subscription == null) {
					this.subscription = s;
					this.subscription.request(Long.MAX_VALUE);
				}
				else {
					s.cancel();
				}
			}

			@Override
			public void onNext(T element) {
				HttpServletRequest servletRequest = (HttpServletRequest) this.asyncContext.getRequest();
				HttpServletResponse servletResponse = (HttpServletResponse) this.asyncContext.getResponse();
				tryWriteEntityWithMessageConverters(element, servletRequest, servletResponse, this.context);
			}

			@Override
			public void onError(Throwable t) {
				handleError(t, (HttpServletRequest) this.asyncContext.getRequest(),
						(HttpServletResponse) this.asyncContext.getResponse(), this.context);
				this.asyncContext.complete();
			}

			@Override
			public void onComplete() {
				this.asyncContext.complete();
			}
		}


		private static class NoContentLengthResponseWrapper extends HttpServletResponseWrapper {

			public NoContentLengthResponseWrapper(HttpServletResponse response) {
				super(response);
			}

			@Override
			public void addIntHeader(String name, int value) {
				if (!HttpHeaders.CONTENT_LENGTH.equals(name)) {
					super.addIntHeader(name, value);
				}
			}

			@Override
			public void addHeader(String name, String value) {
				if (!HttpHeaders.CONTENT_LENGTH.equals(name)) {
					super.addHeader(name, value);
				}
			}

			@Override
			public void setContentLength(int len) {
			}

			@Override
			public void setContentLengthLong(long len) {
			}
		}
	}

}
