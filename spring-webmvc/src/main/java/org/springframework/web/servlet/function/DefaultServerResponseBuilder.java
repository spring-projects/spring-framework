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
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.ModelAndView;

/**
 * Default {@link ServerResponse.BodyBuilder} implementation.
 *
 * @author Arjen Poutsma
 * @since 5.2
 */
class DefaultServerResponseBuilder implements ServerResponse.BodyBuilder {

	private final int statusCode;

	private final HttpHeaders headers = new HttpHeaders();

	private final MultiValueMap<String, Cookie> cookies = new LinkedMultiValueMap<>();


	public DefaultServerResponseBuilder(ServerResponse other) {
		Assert.notNull(other, "ServerResponse must not be null");
		this.statusCode = (other instanceof AbstractServerResponse ?
				((AbstractServerResponse) other).statusCode : other.statusCode().value());
		this.headers.addAll(other.headers());
		this.cookies.addAll(other.cookies());
	}

	public DefaultServerResponseBuilder(HttpStatus status) {
		Assert.notNull(status, "HttpStatus must not be null");
		this.statusCode = status.value();
	}

	public DefaultServerResponseBuilder(int statusCode) {
		this.statusCode = statusCode;
	}

	@Override
	public ServerResponse.BodyBuilder header(String headerName, String... headerValues) {
		for (String headerValue : headerValues) {
			this.headers.add(headerName, headerValue);
		}
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder headers(Consumer<HttpHeaders> headersConsumer) {
		headersConsumer.accept(this.headers);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder cookie(Cookie cookie) {
		Assert.notNull(cookie, "Cookie must not be null");
		this.cookies.add(cookie.getName(), cookie);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder cookies(Consumer<MultiValueMap<String, Cookie>> cookiesConsumer) {
		cookiesConsumer.accept(this.cookies);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder allow(HttpMethod... allowedMethods) {
		this.headers.setAllow(new LinkedHashSet<>(Arrays.asList(allowedMethods)));
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder allow(Set<HttpMethod> allowedMethods) {
		this.headers.setAllow(allowedMethods);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder contentLength(long contentLength) {
		this.headers.setContentLength(contentLength);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder contentType(MediaType contentType) {
		this.headers.setContentType(contentType);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder eTag(String etag) {
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
	public ServerResponse.BodyBuilder lastModified(ZonedDateTime lastModified) {
		this.headers.setLastModified(lastModified);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder lastModified(Instant lastModified) {
		this.headers.setLastModified(lastModified);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder location(URI location) {
		this.headers.setLocation(location);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder cacheControl(CacheControl cacheControl) {
		this.headers.setCacheControl(cacheControl);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder varyBy(String... requestHeaders) {
		this.headers.setVary(Arrays.asList(requestHeaders));
		return this;
	}

	@Override
	public ServerResponse build() {
		return build((request, response) -> null);
	}

	@Override
	public ServerResponse build(
			BiFunction<HttpServletRequest, HttpServletResponse, ModelAndView> writeFunction) {

		return new WriterFunctionResponse(this.statusCode, this.headers, this.cookies, writeFunction);
	}

	@Override
	public ServerResponse body(Object body) {
		return DefaultEntityResponseBuilder.fromObject(body)
				.status(this.statusCode)
				.headers(headers -> headers.putAll(this.headers))
				.cookies(cookies -> cookies.addAll(this.cookies))
				.build();
	}

	@Override
	public <T> ServerResponse body(T body, ParameterizedTypeReference<T> bodyType) {
		return DefaultEntityResponseBuilder.fromObject(body, bodyType)
				.status(this.statusCode)
				.headers(headers -> headers.putAll(this.headers))
				.cookies(cookies -> cookies.addAll(this.cookies))
				.build();
	}

	@Override
	public ServerResponse render(String name, Object... modelAttributes) {
		return new DefaultRenderingResponseBuilder(name)
				.status(this.statusCode)
				.headers(headers -> headers.putAll(this.headers))
				.cookies(cookies -> cookies.addAll(this.cookies))
				.modelAttributes(modelAttributes)
				.build();
	}

	@Override
	public ServerResponse render(String name, Map<String, ?> model) {
		return new DefaultRenderingResponseBuilder(name)
				.status(this.statusCode)
				.headers(headers -> headers.putAll(this.headers))
				.cookies(cookies -> cookies.addAll(this.cookies))
				.modelAttributes(model)
				.build();
	}


	/**
	 * Abstract base class for {@link ServerResponse} implementations.
	 */
	abstract static class AbstractServerResponse implements ServerResponse {

		private static final Set<HttpMethod> SAFE_METHODS =	EnumSet.of(HttpMethod.GET, HttpMethod.HEAD);

		final int statusCode;

		private final HttpHeaders headers;

		private final MultiValueMap<String, Cookie> cookies;

		private final List<ErrorHandler<?>> errorHandlers = new ArrayList<>();

		protected AbstractServerResponse(
				int statusCode, HttpHeaders headers, MultiValueMap<String, Cookie> cookies) {

			this.statusCode = statusCode;
			this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
			this.cookies =
					CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<>(cookies));
		}

		protected <T extends ServerResponse> void addErrorHandler(Predicate<Throwable> predicate,
				BiFunction<Throwable, ServerRequest, T> errorHandler) {

			Assert.notNull(predicate, "Predicate must not be null");
			Assert.notNull(errorHandler, "ErrorHandler must not be null");
			this.errorHandlers.add(new ErrorHandler<>(predicate, errorHandler));
		}


		@Override
		public final HttpStatus statusCode() {
			return HttpStatus.valueOf(this.statusCode);
		}

		@Override
		public int rawStatusCode() {
			return this.statusCode;
		}

		@Override
		public final HttpHeaders headers() {
			return this.headers;
		}

		@Override
		public MultiValueMap<String, Cookie> cookies() {
			return this.cookies;
		}

		@Override
		public ModelAndView writeTo(HttpServletRequest request, HttpServletResponse response,
				Context context) throws ServletException, IOException {

			try {
				writeStatusAndHeaders(response);

				long lastModified = headers().getLastModified();
				ServletWebRequest servletWebRequest = new ServletWebRequest(request, response);
				HttpMethod httpMethod = HttpMethod.resolve(request.getMethod());
				if (SAFE_METHODS.contains(httpMethod) &&
						servletWebRequest.checkNotModified(headers().getETag(), lastModified)) {
					return null;
				}
				else {
					return writeToInternal(request, response, context);
				}
			}
			catch (Throwable throwable) {
				return handleError(throwable, request, response, context);
			}
		}

		private void writeStatusAndHeaders(HttpServletResponse response) {
			response.setStatus(this.statusCode);
			writeHeaders(response);
			writeCookies(response);
		}

		private void writeHeaders(HttpServletResponse servletResponse) {
			this.headers.forEach((headerName, headerValues) -> {
				for (String headerValue : headerValues) {
					servletResponse.addHeader(headerName, headerValue);
				}
			});
			// HttpServletResponse exposes some headers as properties: we should include those if not already present
			if (servletResponse.getContentType() == null && this.headers.getContentType() != null) {
				servletResponse.setContentType(this.headers.getContentType().toString());
			}
			if (servletResponse.getCharacterEncoding() == null &&
					this.headers.getContentType() != null &&
					this.headers.getContentType().getCharset() != null) {
				servletResponse.setCharacterEncoding(this.headers.getContentType().getCharset().name());
			}
		}

		private void writeCookies(HttpServletResponse servletResponse) {
			this.cookies.values().stream()
					.flatMap(Collection::stream)
					.forEach(servletResponse::addCookie);
		}

		@Nullable
		protected abstract ModelAndView writeToInternal(
				HttpServletRequest request, HttpServletResponse response, Context context)
				throws ServletException, IOException;

		@Nullable
		protected ModelAndView handleError(Throwable t, HttpServletRequest servletRequest,
				HttpServletResponse servletResponse, Context context) {

			return this.errorHandlers.stream()
					.filter(errorHandler -> errorHandler.test(t))
					.findFirst()
					.map(errorHandler -> {
						ServerRequest serverRequest = (ServerRequest)
								servletRequest.getAttribute(RouterFunctions.REQUEST_ATTRIBUTE);
						ServerResponse serverResponse = errorHandler.handle(t, serverRequest);
						try {
							return serverResponse.writeTo(servletRequest, servletResponse, context);
						}
						catch (ServletException ex) {
							throw new IllegalStateException(ex);
						}
						catch (IOException ex) {
							throw new UncheckedIOException(ex);
						}
					})
					.orElseThrow(() -> new IllegalStateException(t));
		}


		private static class ErrorHandler<T extends ServerResponse> {

			private final Predicate<Throwable> predicate;

			private final BiFunction<Throwable, ServerRequest, T>
					responseProvider;

			public ErrorHandler(Predicate<Throwable> predicate,
					BiFunction<Throwable, ServerRequest, T> responseProvider) {

				Assert.notNull(predicate, "Predicate must not be null");
				Assert.notNull(responseProvider, "ResponseProvider must not be null");
				this.predicate = predicate;
				this.responseProvider = responseProvider;
			}

			public boolean test(Throwable t) {
				return this.predicate.test(t);
			}

			public T handle(Throwable t, ServerRequest serverRequest) {
				return this.responseProvider.apply(t, serverRequest);
			}
		}
	}


	private static class WriterFunctionResponse extends AbstractServerResponse {

		private final BiFunction<HttpServletRequest, HttpServletResponse, ModelAndView> writeFunction;

		public WriterFunctionResponse(
				int statusCode, HttpHeaders headers, MultiValueMap<String, Cookie> cookies,
				BiFunction<HttpServletRequest, HttpServletResponse, ModelAndView> writeFunction) {

			super(statusCode, headers, cookies);
			Assert.notNull(writeFunction, "WriteFunction must not be null");
			this.writeFunction = writeFunction;
		}

		@Override
		protected ModelAndView writeToInternal(
				HttpServletRequest request, HttpServletResponse response, Context context) {

			return this.writeFunction.apply(request, response);
		}
	}

}
