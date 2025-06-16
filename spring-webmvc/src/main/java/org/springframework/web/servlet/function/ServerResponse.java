/*
 * Copyright 2002-2025 the original author or authors.
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
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.MultiValueMap;
import org.springframework.web.ErrorResponse;
import org.springframework.web.servlet.ModelAndView;

/**
 * Represents a typed server-side HTTP response, as returned
 * by a {@linkplain HandlerFunction handler function} or
 * {@linkplain HandlerFilterFunction filter function}.
 *
 * @author Arjen Poutsma
 * @since 5.2
 */
public interface ServerResponse {

	/**
	 * Return the status code of this response.
	 * @return the status as an HttpStatusCode value
	 */
	HttpStatusCode statusCode();

	/**
	 * Return the headers of this response.
	 */
	HttpHeaders headers();

	/**
	 * Return the cookies of this response.
	 */
	MultiValueMap<String, Cookie> cookies();

	/**
	 * Write this response to the given servlet response.
	 * @param request the current request
	 * @param response the response to write to
	 * @param context the context to use when writing
	 * @return a {@code ModelAndView} to render, or {@code null} if handled directly
	 */
	@Nullable ModelAndView writeTo(HttpServletRequest request, HttpServletResponse response, Context context)
		throws ServletException, IOException;


	// Static methods

	/**
	 * Create a builder with the status code and headers of the given response.
	 * @param other the response to copy the status and headers from
	 * @return the created builder
	 */
	static BodyBuilder from(ServerResponse other) {
		return new DefaultServerResponseBuilder(other);
	}

	/**
	 * Create a {@code ServerResponse} from the given {@link ErrorResponse}.
	 * @param response the {@link ErrorResponse} to initialize from
	 * @return the built response
	 * @since 6.0
	 */
	static ServerResponse from(ErrorResponse response) {
		return status(response.getStatusCode())
				.headers(headers -> headers.putAll(response.getHeaders()))
				.body(response.getBody());
	}

	/**
	 * Create a builder with the given HTTP status.
	 * @param status the response status
	 * @return the created builder
	 */
	static BodyBuilder status(HttpStatusCode status) {
		return new DefaultServerResponseBuilder(status);
	}

	/**
	 * Create a builder with the given HTTP status.
	 * @param status the response status
	 * @return the created builder
	 */
	static BodyBuilder status(int status) {
		return new DefaultServerResponseBuilder(HttpStatusCode.valueOf(status));
	}

	/**
	 * Create a builder with the status set to {@linkplain HttpStatus#OK 200 OK}.
	 * @return the created builder
	 */
	static BodyBuilder ok() {
		return status(HttpStatus.OK);
	}

	/**
	 * Create a builder with a {@linkplain HttpStatus#CREATED 201 Created} status
	 * and a location header set to the given URI.
	 * @param location the location URI
	 * @return the created builder
	 */
	static BodyBuilder created(URI location) {
		BodyBuilder builder = status(HttpStatus.CREATED);
		return builder.location(location);
	}

	/**
	 * Create a builder with a {@linkplain HttpStatus#ACCEPTED 202 Accepted} status.
	 * @return the created builder
	 */
	static BodyBuilder accepted() {
		return status(HttpStatus.ACCEPTED);
	}

	/**
	 * Create a builder with a {@linkplain HttpStatus#NO_CONTENT 204 No Content} status.
	 * @return the created builder
	 */
	static HeadersBuilder<?> noContent() {
		return status(HttpStatus.NO_CONTENT);
	}

	/**
	 * Create a builder with a {@linkplain HttpStatus#SEE_OTHER 303 See Other}
	 * status and a location header set to the given URI.
	 * @param location the location URI
	 * @return the created builder
	 */
	static BodyBuilder seeOther(URI location) {
		BodyBuilder builder = status(HttpStatus.SEE_OTHER);
		return builder.location(location);
	}

	/**
	 * Create a builder with a {@linkplain HttpStatus#TEMPORARY_REDIRECT 307 Temporary Redirect}
	 * status and a location header set to the given URI.
	 * @param location the location URI
	 * @return the created builder
	 */
	static BodyBuilder temporaryRedirect(URI location) {
		BodyBuilder builder = status(HttpStatus.TEMPORARY_REDIRECT);
		return builder.location(location);
	}

	/**
	 * Create a builder with a {@linkplain HttpStatus#PERMANENT_REDIRECT 308 Permanent Redirect}
	 * status and a location header set to the given URI.
	 * @param location the location URI
	 * @return the created builder
	 */
	static BodyBuilder permanentRedirect(URI location) {
		BodyBuilder builder = status(HttpStatus.PERMANENT_REDIRECT);
		return builder.location(location);
	}

	/**
	 * Create a builder with a {@linkplain HttpStatus#BAD_REQUEST 400 Bad Request} status.
	 * @return the created builder
	 */
	static BodyBuilder badRequest() {
		return status(HttpStatus.BAD_REQUEST);
	}

	/**
	 * Create a builder with a {@linkplain HttpStatus#NOT_FOUND 404 Not Found} status.
	 * @return the created builder
	 */
	static HeadersBuilder<?> notFound() {
		return status(HttpStatus.NOT_FOUND);
	}

	/**
	 * Create a builder with a
	 * {@linkplain HttpStatus#UNPROCESSABLE_ENTITY 422 Unprocessable Entity} status.
	 * @return the created builder
	 */
	static BodyBuilder unprocessableEntity() {
		return status(HttpStatus.UNPROCESSABLE_ENTITY);
	}

	/**
	 * Create a (built) response with the given asynchronous response.
	 * Parameter {@code asyncResponse} can be a
	 * {@link CompletableFuture CompletableFuture&lt;ServerResponse&gt;} or
	 * {@link Publisher Publisher&lt;ServerResponse&gt;} (or any
	 * asynchronous producer of a single {@code ServerResponse} that can be
	 * adapted via the {@link ReactiveAdapterRegistry}).
	 *
	 * <p>This method can be used to set the response status code, headers, and
	 * body based on an asynchronous result. If only the body is asynchronous,
	 * {@link BodyBuilder#body(Object)} can be used instead.
	 * @param asyncResponse a {@code CompletableFuture<ServerResponse>} or
	 * {@code Publisher<ServerResponse>}
	 * @return the asynchronous response
	 * @since 5.3
	 */
	static ServerResponse async(Object asyncResponse) {
		return AsyncServerResponse.create(asyncResponse);
	}

	/**
	 * Create a (built) response with the given asynchronous response.
	 * Parameter {@code asyncResponse} can be a
	 * {@link CompletableFuture CompletableFuture&lt;ServerResponse&gt;} or
	 * {@link Publisher Publisher&lt;ServerResponse&gt;} (or any
	 * asynchronous producer of a single {@code ServerResponse} that can be
	 * adapted via the {@link ReactiveAdapterRegistry}).
	 *
	 * <p>This method can be used to set the response status code, headers, and
	 * body based on an asynchronous result. If only the body is asynchronous,
	 * {@link BodyBuilder#body(Object)} can be used instead.
	 * @param asyncResponse a {@code CompletableFuture<ServerResponse>} or
	 * {@code Publisher<ServerResponse>}
	 * @param timeout maximum time period to wait for before timing out
	 * @return the asynchronous response
	 * @since 5.3.2
	 */
	static ServerResponse async(Object asyncResponse, Duration timeout) {
		return AsyncServerResponse.create(asyncResponse, timeout);
	}

	/**
	 * Create a server-sent event response. The {@link SseBuilder} provided to
	 * {@code consumer} can be used to build and send events.
	 *
	 * <p>For example:
	 * <pre class="code">
	 * public ServerResponse handleSse(ServerRequest request) {
	 *     return ServerResponse.sse(sse -&gt; sse.send("Hello World!"));
	 * }
	 * </pre>
	 *
	 * <p>or, to set both the id and event type:
	 * <pre class="code">
	 * public ServerResponse handleSse(ServerRequest request) {
	 *     return ServerResponse.sse(sse -&gt; sse
	 *         .id("42)
	 *         .event("event")
	 *         .send("Hello World!"));
	 * }
	 * </pre>
	 * @param consumer the consumer that will be provided with an event builder
	 * @return the server-side event response
	 * @since 5.3.2
	 * @see <a href="https://html.spec.whatwg.org/multipage/server-sent-events.html">Server-Sent Events</a>
	 */
	static ServerResponse sse(Consumer<SseBuilder> consumer) {
		return SseServerResponse.create(consumer, null);
	}

	/**
	 * Create a server-sent event response. The {@link SseBuilder} provided to
	 * {@code consumer} can be used to build and send events.
	 *
	 * <p>For example:
	 * <pre class="code">
	 * public ServerResponse handleSse(ServerRequest request) {
	 *     return ServerResponse.sse(sse -&gt; sse.send("Hello World!"));
	 * }
	 * </pre>
	 *
	 * <p>or, to set both the id and event type:
	 * <pre class="code">
	 * public ServerResponse handleSse(ServerRequest request) {
	 *     return ServerResponse.sse(sse -&gt; sse
	 *         .id("42)
	 *         .event("event")
	 *         .send("Hello World!"));
	 * }
	 * </pre>
	 * @param consumer the consumer that will be provided with an event builder
	 * @param timeout maximum time period to wait before timing out
	 * @return the server-side event response
	 * @since 5.3.2
	 * @see <a href="https://html.spec.whatwg.org/multipage/server-sent-events.html">Server-Sent Events</a>
	 */
	static ServerResponse sse(Consumer<SseBuilder> consumer, Duration timeout) {
		return SseServerResponse.create(consumer, timeout);
	}


	/**
	 * Defines a builder that adds headers to the response.
	 * @param <B> the builder subclass
	 */
	interface HeadersBuilder<B extends HeadersBuilder<B>> {

		/**
		 * Add the given header value(s) under the given name.
		 * @param headerName the header name
		 * @param headerValues the header value(s)
		 * @return this builder
		 * @see HttpHeaders#add(String, String)
		 */
		B header(String headerName, @Nullable String... headerValues);

		/**
		 * Manipulate this response's headers with the given consumer. The
		 * headers provided to the consumer are "live", so that the consumer can be used to
		 * {@linkplain HttpHeaders#set(String, String) overwrite} existing header values,
		 * {@linkplain HttpHeaders#remove(String) remove} values, or use any of the other
		 * {@link HttpHeaders} methods.
		 * @param headersConsumer a function that consumes the {@code HttpHeaders}
		 * @return this builder
		 */
		B headers(Consumer<HttpHeaders> headersConsumer);

		/**
		 * Add the given cookie to the response.
		 * @param cookie the cookie to add
		 * @return this builder
		 */
		B cookie(Cookie cookie);

		/**
		 * Manipulate this response's cookies with the given consumer. The
		 * cookies provided to the consumer are "live", so that the consumer can be used to
		 * {@linkplain MultiValueMap#set(Object, Object) overwrite} existing cookies,
		 * {@linkplain MultiValueMap#remove(Object) remove} cookies, or use any of the other
		 * {@link MultiValueMap} methods.
		 * @param cookiesConsumer a function that consumes the cookies
		 * @return this builder
		 */
		B cookies(Consumer<MultiValueMap<String, Cookie>> cookiesConsumer);

		/**
		 * Set the set of allowed {@link HttpMethod HTTP methods}, as specified
		 * by the {@code Allow} header.
		 * @param allowedMethods the allowed methods
		 * @return this builder
		 * @see HttpHeaders#setAllow(Set)
		 */
		B allow(HttpMethod... allowedMethods);

		/**
		 * Set the set of allowed {@link HttpMethod HTTP methods}, as specified
		 * by the {@code Allow} header.
		 * @param allowedMethods the allowed methods
		 * @return this builder
		 * @see HttpHeaders#setAllow(Set)
		 */
		B allow(Set<HttpMethod> allowedMethods);

		/**
		 * Set the entity tag of the body, as specified by the {@code ETag} header.
		 * @param eTag the new entity tag
		 * @return this builder
		 * @see HttpHeaders#setETag(String)
		 */
		B eTag(String eTag);

		/**
		 * Set the time the resource was last changed, as specified by the
		 * {@code Last-Modified} header.
		 * @param lastModified the last modified date
		 * @return this builder
		 * @see HttpHeaders#setLastModified(long)
		 */
		B lastModified(ZonedDateTime lastModified);

		/**
		 * Set the time the resource was last changed, as specified by the
		 * {@code Last-Modified} header.
		 * @param lastModified the last modified date
		 * @return this builder
		 * @see HttpHeaders#setLastModified(long)
		 */
		B lastModified(Instant lastModified);
		/**
		 * Set the location of a resource, as specified by the {@code Location} header.
		 * @param location the location
		 * @return this builder
		 * @see HttpHeaders#setLocation(URI)
		 */
		B location(URI location);

		/**
		 * Set the caching directives for the resource, as specified by the HTTP 1.1
		 * {@code Cache-Control} header.
		 * <p>A {@code CacheControl} instance can be built like
		 * {@code CacheControl.maxAge(3600).cachePublic().noTransform()}.
		 * @param cacheControl a builder for cache-related HTTP response headers
		 * @return this builder
		 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2">RFC-7234 Section 5.2</a>
		 */
		B cacheControl(CacheControl cacheControl);

		/**
		 * Configure one or more request header names (for example, "Accept-Language") to
		 * add to the "Vary" response header to inform clients that the response is
		 * subject to content negotiation and variances based on the value of the
		 * given request headers. The configured request header names are added only
		 * if not already present in the response "Vary" header.
		 * @param requestHeaders request header names
		 * @return this builder
		 */
		B varyBy(String... requestHeaders);

		/**
		 * Build the response entity with no body.
		 */
		ServerResponse build();

		/**
		 * Build the response entity with a custom write function.
		 * @param writeFunction the function used to write to the {@link HttpServletResponse}
		 */
		ServerResponse build(WriteFunction writeFunction);


		/**
		 * Defines the contract for {@link #build(WriteFunction)}.
		 * @since 6.1
		 */
		@FunctionalInterface
		interface WriteFunction {

			/**
			 * Write to the given {@code servletResponse}, or return a
			 * {@code ModelAndView} to be rendered.
			 * @param servletRequest the HTTP request
			 * @param servletResponse  the HTTP response to write to
			 * @return a {@code ModelAndView} to render, or {@code null} if handled directly
			 * @throws Exception in case of Servlet errors
			 */
			@Nullable ModelAndView write(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception;

		}

	}


	/**
	 * Defines a builder that adds a body to the response.
	 */
	interface BodyBuilder extends HeadersBuilder<BodyBuilder> {

		/**
		 * Set the length of the body in bytes, as specified by the
		 * {@code Content-Length} header.
		 * @param contentLength the content length
		 * @return this builder
		 * @see HttpHeaders#setContentLength(long)
		 */
		BodyBuilder contentLength(long contentLength);

		/**
		 * Set the {@linkplain MediaType media type} of the body, as specified by the
		 * {@code Content-Type} header.
		 * @param contentType the content type
		 * @return this builder
		 * @see HttpHeaders#setContentType(MediaType)
		 */
		BodyBuilder contentType(MediaType contentType);

		/**
		 * Set the body of the response to the given {@code Object} and return
		 * it.
		 *
		 * <p>Asynchronous response bodies are supported by providing a
		 * {@link CompletionStage} or {@link Publisher} as body (or any
		 * asynchronous producer of a single entity that can be adapted via the
		 * {@link ReactiveAdapterRegistry}).
		 * @param body the body of the response
		 * @return the built response
		 */
		ServerResponse body(Object body);

		/**
		 * Set the body of the response to the given {@code Object} and return it. The parameter
		 * {@code bodyType} is used to capture the generic type.
		 * @param body the body of the response
		 * @param bodyType the type of the body, used to capture the generic type
		 * @return the built response
		 */
		<T> ServerResponse body(T body, ParameterizedTypeReference<T> bodyType);

		/**
		 * Render the template with the given {@code name} using the given {@code modelAttributes}.
		 * The model attributes are mapped under a
		 * {@linkplain org.springframework.core.Conventions#getVariableName generated name}.
		 * <p><em>Note: Empty {@link Collection Collections} are not added to
		 * the model when using this method because we cannot correctly determine
		 * the true convention name.</em>
		 * @param name the name of the template to be rendered
		 * @param modelAttributes the modelAttributes used to render the template
		 * @return the built response
		 */
		ServerResponse render(String name, Object... modelAttributes);

		/**
		 * Render the template with the given {@code name} using the given {@code model}.
		 * @param name the name of the template to be rendered
		 * @param model the model used to render the template
		 * @return the built response
		 */
		ServerResponse render(String name, Map<String, ?> model);

		/**
		 * Create a low-level streaming response; for SSE support, see {@link #sse(Consumer)}.
		 * <p>The {@link StreamBuilder} provided to the {@code streamConsumer} can
		 * be used to write to the response in a streaming fashion. Note, the builder is
		 * responsible for flushing the buffered content to the network.
		 * <p>For example:
		 * <pre class="code">
		 * public ServerResponse handleStream(ServerRequest request) {
		 *     return ServerResponse.ok()
		 *       .contentType(MediaType.APPLICATION_ND_JSON)
		 *       .stream(stream -&gt; {
		 *         try {
		 *           // Write and flush a first item
		 *           stream.write(new Person("John", 51), MediaType.APPLICATION_JSON)
		 *             .write(new byte[]{'\n'})
		 *             .flush();
		 *           // Write and complete with the last item
		 *           stream.write(new Person("Jane", 42), MediaType.APPLICATION_JSON)
		 *             .write(new byte[]{'\n'})
		 *             .complete();
		 *         }
		 *         catch (IOException ex) {
		 *           throw new UncheckedIOException(ex);
		 *         }
		 *     });
		 * }
		 * </pre>
		 * @param streamConsumer consumer that will be provided with a stream builder
		 * @return the server-side streaming response
		 * @since 6.2
		 */
		ServerResponse stream(Consumer<StreamBuilder> streamConsumer);

	}

	/**
	 * Defines a builder for a body that sends server-sent events.
	 *
	 * @since 5.3.2
	 */
	interface SseBuilder {

		/**
		 * Completes the stream with the given error.
		 *
		 * <p>The throwable is dispatched back into Spring MVC, and passed to
		 * its exception handling mechanism. Since the response has
		 * been committed by this point, the response status can not change.
		 * @param t the throwable to dispatch
		 */
		void error(Throwable t);

		/**
		 * Completes the stream.
		 */
		void complete();

		/**
		 * Register a callback to be invoked when a request times
		 * out.
		 * @param onTimeout the callback to invoke on timeout
		 * @return this builder
		 */
		SseBuilder onTimeout(Runnable onTimeout);

		/**
		 * Register a callback to be invoked when an error occurs during
		 * processing.
		 * @param onError the callback to invoke on error
		 * @return this builder
		 */
		SseBuilder onError(Consumer<Throwable> onError);

		/**
		 * Register a callback to be invoked when the request completes.
		 * @param onCompletion the callback to invoked on completion
		 * @return this builder
		 */
		SseBuilder onComplete(Runnable onCompletion);

		/**
		 * Sends the given object as a server-sent event.
		 * Strings will be sent as UTF-8 encoded bytes, and other objects will
		 * be converted into JSON using
		 * {@linkplain HttpMessageConverter message converters}.
		 *
		 * <p>This convenience method has the same effect as
		 * {@link #data(Object)}.
		 * @param object the object to send
		 * @throws IOException in case of I/O errors
		 */
		void send(Object object) throws IOException;

		/**
		 * Sends the buffered content as a server-sent event, without data.
		 * Only the {@link #event(String) events} and {@link #comment(String) comments}
		 * will be sent.
		 * @throws IOException in case of I/O errors
		 * @since 6.1.4
		 */
		void send() throws IOException;

		/**
		 * Add an SSE "id" line.
		 * @param id the event identifier
		 * @return this builder
		 */
		SseBuilder id(String id);

		/**
		 * Add an SSE "event" line.
		 * @param eventName the event name
		 * @return this builder
		 */
		SseBuilder event(String eventName);

		/**
		 * Add an SSE "retry" line.
		 * @param duration the duration to convert into millis
		 * @return this builder
		 */
		SseBuilder retry(Duration duration);

		/**
		 * Add an SSE comment.
		 * @param comment the comment
		 * @return this builder
		 */
		SseBuilder comment(String comment);

		/**
		 * Add an SSE "data" line for the given object and sends the built
		 * server-sent event to the client.
		 * Strings will be sent as UTF-8 encoded bytes, and other objects will
		 * be converted into JSON using
		 * {@linkplain HttpMessageConverter message converters}.
		 * @param object the object to send as data
		 * @throws IOException in case of I/O errors
		 */
		void data(Object object) throws IOException;

	}

	/**
	 * Defines a builder for a streaming response body.
	 *
	 * @since 6.2
	 */
	interface StreamBuilder {

		/**
		 * Completes the stream with the given error.
		 *
		 * <p>The throwable is dispatched back into Spring MVC, and passed to
		 * its exception handling mechanism. Since the response has
		 * been committed by this point, the response status can not change.
		 * @param t the throwable to dispatch
		 */
		void error(Throwable t);

		/**
		 * Completes the stream.
		 */
		void complete();

		/**
		 * Register a callback to be invoked when a request times
		 * out.
		 * @param onTimeout the callback to invoke on timeout
		 * @return this builder
		 */
		StreamBuilder onTimeout(Runnable onTimeout);

		/**
		 * Register a callback to be invoked when an error occurs during
		 * processing.
		 * @param onError the callback to invoke on error
		 * @return this builder
		 */
		StreamBuilder onError(Consumer<Throwable> onError);

		/**
		 * Register a callback to be invoked when the request completes.
		 * @param onCompletion the callback to invoked on completion
		 * @return this builder
		 */
		StreamBuilder onComplete(Runnable onCompletion);

		/**
		 * Write the given object to the response stream, without flushing.
		 * Strings will be sent as UTF-8 encoded bytes, byte arrays will be sent as-is,
		 * and other objects will be converted into JSON using
		 * {@linkplain HttpMessageConverter message converters}.
		 * @param object the object to send as data
		 * @return this builder
		 * @throws IOException in case of I/O errors
		 */
		StreamBuilder write(Object object) throws IOException;

		/**
		 * Write the given object to the response stream, without flushing.
		 * Strings will be sent as UTF-8 encoded bytes, byte arrays will be sent as-is,
		 * and other objects will be converted into JSON using
		 * {@linkplain HttpMessageConverter message converters}.
		 * @param object the object to send as data
		 * @param mediaType the media type to use for encoding the provided data
		 * @return this builder
		 * @throws IOException in case of I/O errors
		 */
		StreamBuilder write(Object object, @Nullable MediaType mediaType) throws IOException;

		/**
		 * Flush the buffered response stream content to the network.
		 * @throws IOException in case of I/O errors
		 */
		void flush() throws IOException;

	}

	/**
	 * Defines the context used during the {@link #writeTo(HttpServletRequest, HttpServletResponse, Context)}.
	 */
	interface Context {

		/**
		 * Return the {@link HttpMessageConverter HttpMessageConverters} to be used for response body conversion.
		 * @return the list of message writers
		 */
		List<HttpMessageConverter<?>> messageConverters();

	}

}
