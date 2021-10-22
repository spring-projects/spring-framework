/*
 * Copyright 2002-2021 the original author or authors.
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
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.ModelAndView;

/**
 * Default {@link EntityResponse.Builder} implementation.
 *
 * @author Arjen Poutsma
 * @since 5.2
 * @param <T> the entity type
 */
final class DefaultEntityResponseBuilder<T> implements EntityResponse.Builder<T> {

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
		if (this.entity instanceof CompletionStage completionStage) {
			return new CompletionStageEntityResponse(this.status, this.headers, this.cookies,
					completionStage, this.entityType);
		}
		else if (DefaultAsyncServerResponse.reactiveStreamsPresent) {
			ReactiveAdapter adapter = ReactiveAdapterRegistry.getSharedInstance().getAdapter(this.entity.getClass());
			if (adapter != null) {
				Publisher<T> publisher = adapter.toPublisher(this.entity);
				return new PublisherEntityResponse(this.status, this.headers, this.cookies, publisher, this.entityType);
			}
		}
		return new DefaultEntityResponse<>(this.status, this.headers, this.cookies, this.entity, this.entityType);
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
	private static class DefaultEntityResponse<T> extends AbstractServerResponse implements EntityResponse<T> {

		private final T entity;

		private final Type entityType;

		public DefaultEntityResponse(int statusCode, HttpHeaders headers,
				MultiValueMap<String, Cookie> cookies, T entity, Type entityType) {

			super(statusCode, headers, cookies);
			this.entity = entity;
			this.entityType = entityType;
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

		@SuppressWarnings({ "unchecked", "resource", "rawtypes" })
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
				if (messageConverter instanceof GenericHttpMessageConverter genericMessageConverter) {
					if (genericMessageConverter.canWrite(entityType, entityClass, contentType)) {
						genericMessageConverter.write(entity, entityType, contentType, serverResponse);
						return;
					}
				}
				if (messageConverter.canWrite(entityClass, contentType)) {
					((HttpMessageConverter<Object>) messageConverter).write(entity, contentType, serverResponse);
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
				HttpServletResponse response, ServerResponse.Context context) throws ServletException, IOException {
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
					.flatMap(messageConverter -> messageConverter.getSupportedMediaTypes(entityClass).stream())
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
		protected ModelAndView writeToInternal(HttpServletRequest servletRequest, HttpServletResponse servletResponse,
				Context context) throws ServletException, IOException {

			DeferredResult<ServerResponse> deferredResult = createDeferredResult(servletRequest, servletResponse, context);
			DefaultAsyncServerResponse.writeAsync(servletRequest, servletResponse, deferredResult);
			return null;
		}

		private DeferredResult<ServerResponse> createDeferredResult(HttpServletRequest request, HttpServletResponse response,
				Context context) {

			DeferredResult<ServerResponse> result = new DeferredResult<>();
			entity().handle((value, ex) -> {
				if (ex != null) {
					if (ex instanceof CompletionException && ex.getCause() != null) {
						ex = ex.getCause();
					}
					ServerResponse errorResponse = errorResponse(ex, request);
					if (errorResponse != null) {
						result.setResult(errorResponse);
					}
					else {
						result.setErrorResult(ex);
					}
				}
				else {
					try {
						tryWriteEntityWithMessageConverters(value, request, response, context);
						result.setResult(null);
					}
					catch (ServletException | IOException writeException) {
						result.setErrorResult(writeException);
					}
				}
				return null;
			});
			return result;
		}

	}


	/**
	 * {@link EntityResponse} implementation for asynchronous {@link Publisher} bodies.
	 */
	private static class PublisherEntityResponse<T> extends DefaultEntityResponse<Publisher<T>> {

		public PublisherEntityResponse(int statusCode, HttpHeaders headers,
				MultiValueMap<String, Cookie> cookies, Publisher<T> entity, Type entityType) {

			super(statusCode, headers, cookies, entity, entityType);
		}

		@Override
		protected ModelAndView writeToInternal(HttpServletRequest servletRequest, HttpServletResponse servletResponse,
				Context context) throws ServletException, IOException {

			DeferredResult<?> deferredResult = new DeferredResult<>();
			DefaultAsyncServerResponse.writeAsync(servletRequest, servletResponse, deferredResult);

			entity().subscribe(new DeferredResultSubscriber(servletRequest, servletResponse, context, deferredResult));
			return null;
		}


		private class DeferredResultSubscriber implements Subscriber<T> {

			private final HttpServletRequest servletRequest;

			private final HttpServletResponse servletResponse;

			private final Context context;

			private final DeferredResult<?> deferredResult;

			@Nullable
			private Subscription subscription;


			public DeferredResultSubscriber(HttpServletRequest servletRequest,
					HttpServletResponse servletResponse, Context context,
					DeferredResult<?> deferredResult) {

				this.servletRequest = servletRequest;
				this.servletResponse = new NoContentLengthResponseWrapper(servletResponse);
				this.context = context;
				this.deferredResult = deferredResult;
			}

			@Override
			public void onSubscribe(Subscription s) {
				if (this.subscription == null) {
					this.subscription = s;
					this.subscription.request(1);
				}
				else {
					s.cancel();
				}
			}

			@Override
			public void onNext(T t) {
				Assert.state(this.subscription != null, "No subscription");
				try {
					tryWriteEntityWithMessageConverters(t, this.servletRequest, this.servletResponse, this.context);
					this.servletResponse.getOutputStream().flush();
					this.subscription.request(1);
				}
				catch (ServletException | IOException ex) {
					this.subscription.cancel();
					this.deferredResult.setErrorResult(ex);
				}
			}

			@Override
			public void onError(Throwable t) {
				try {
					handleError(t, this.servletRequest, this.servletResponse, this.context);
				}
				catch (ServletException | IOException handlingThrowable) {
					this.deferredResult.setErrorResult(handlingThrowable);
				}
			}

			@Override
			public void onComplete() {
				try {
					this.servletResponse.getOutputStream().flush();
					this.deferredResult.setResult(null);
				}
				catch (IOException ex) {
					this.deferredResult.setErrorResult(ex);
				}

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
