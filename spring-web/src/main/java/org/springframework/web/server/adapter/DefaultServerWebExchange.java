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

package org.springframework.web.server.adapter;

import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Hints;
import org.springframework.http.ETag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.AbstractServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.i18n.LocaleContextResolver;
import org.springframework.web.server.session.WebSessionManager;

/**
 * Default implementation of {@link ServerWebExchange}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public class DefaultServerWebExchange implements ServerWebExchange {

	private static final Set<HttpMethod> SAFE_METHODS = Set.of(HttpMethod.GET, HttpMethod.HEAD);

	private static final ResolvableType FORM_DATA_TYPE =
			ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class);

	private static final ResolvableType MULTIPART_DATA_TYPE = ResolvableType.forClassWithGenerics(
			MultiValueMap.class, String.class, Part.class);

	private static final Mono<MultiValueMap<String, String>> EMPTY_FORM_DATA =
			Mono.just(CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<String, String>(0)))
					.cache();

	private static final Mono<MultiValueMap<String, Part>> EMPTY_MULTIPART_DATA =
			Mono.just(CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<String, Part>(0)))
					.cache();


	private final ServerHttpRequest request;

	private final ServerHttpResponse response;

	private final Map<String, Object> attributes = new ConcurrentHashMap<>();

	private final Mono<WebSession> sessionMono;

	private final LocaleContextResolver localeContextResolver;

	private final Mono<MultiValueMap<String, String>> formDataMono;

	private final Mono<MultiValueMap<String, Part>> multipartDataMono;

	private volatile boolean multipartRead = false;

	private final @Nullable ApplicationContext applicationContext;

	private volatile boolean notModified;

	private Function<String, String> urlTransformer = url -> url;

	private @Nullable Object logId;

	private String logPrefix = "";


	public DefaultServerWebExchange(ServerHttpRequest request, ServerHttpResponse response,
			WebSessionManager sessionManager, ServerCodecConfigurer codecConfigurer,
			LocaleContextResolver localeContextResolver) {

		this(request, response, sessionManager, codecConfigurer, localeContextResolver, null);
	}

	protected DefaultServerWebExchange(ServerHttpRequest request, ServerHttpResponse response,
			WebSessionManager sessionManager, ServerCodecConfigurer codecConfigurer,
			LocaleContextResolver localeContextResolver, @Nullable ApplicationContext applicationContext) {

		Assert.notNull(request, "'request' is required");
		Assert.notNull(response, "'response' is required");
		Assert.notNull(sessionManager, "'sessionManager' is required");
		Assert.notNull(codecConfigurer, "'codecConfigurer' is required");
		Assert.notNull(localeContextResolver, "'localeContextResolver' is required");

		// Initialize before first call to getLogPrefix()
		this.attributes.put(ServerWebExchange.LOG_ID_ATTRIBUTE, request.getId());

		this.request = request;
		this.response = response;
		this.sessionMono = sessionManager.getSession(this).cache();
		this.localeContextResolver = localeContextResolver;
		this.formDataMono = initFormData(request, codecConfigurer, getLogPrefix());
		this.multipartDataMono = initMultipartData(codecConfigurer, getLogPrefix());
		this.applicationContext = applicationContext;

		if (request instanceof AbstractServerHttpRequest abstractServerHttpRequest) {
			abstractServerHttpRequest.setAttributesSupplier(() -> this.attributes);
		}
	}

	private static Mono<MultiValueMap<String, String>> initFormData(ServerHttpRequest request,
			ServerCodecConfigurer configurer, String logPrefix) {

		MediaType contentType = getContentType(request);
		if (contentType == null || !contentType.isCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED)) {
			return EMPTY_FORM_DATA;
		}

		HttpMessageReader<MultiValueMap<String, String>> reader = getReader(configurer, MediaType.APPLICATION_FORM_URLENCODED, FORM_DATA_TYPE);
		if (reader == null) {
			return Mono.error(new IllegalStateException("No HttpMessageReader for " + contentType));
		}

		return reader
				.readMono(FORM_DATA_TYPE, request, Hints.from(Hints.LOG_PREFIX_HINT, logPrefix))
				.switchIfEmpty(EMPTY_FORM_DATA)
				.cache();
	}

	private Mono<MultiValueMap<String, Part>> initMultipartData(ServerCodecConfigurer configurer, String logPrefix) {

		MediaType contentType = getContentType(this.request);
		if (contentType == null || !contentType.getType().equalsIgnoreCase("multipart")) {
			return EMPTY_MULTIPART_DATA;
		}

		HttpMessageReader<MultiValueMap<String, Part>> reader = getReader(configurer, contentType, MULTIPART_DATA_TYPE);
		if (reader == null) {
			return Mono.error(new IllegalStateException("No HttpMessageReader for " + contentType));
		}

		return reader
				.readMono(MULTIPART_DATA_TYPE, this.request, Hints.from(Hints.LOG_PREFIX_HINT, logPrefix))
				.doOnNext(ignored -> this.multipartRead = true)
				.switchIfEmpty(EMPTY_MULTIPART_DATA)
				.cache();
	}

	private static @Nullable MediaType getContentType(ServerHttpRequest request) {
		MediaType contentType = null;
		try {
			contentType = request.getHeaders().getContentType();
		}
		catch (InvalidMediaTypeException ex) {
			// ignore
		}
		return contentType;
	}

	@SuppressWarnings("unchecked")
	private static <E> @Nullable HttpMessageReader<E> getReader(
			ServerCodecConfigurer configurer, MediaType contentType, ResolvableType targetType) {

		HttpMessageReader<E> result = null;
		for (HttpMessageReader<?> reader : configurer.getReaders()) {
			if (reader.canRead(targetType, contentType)) {
				result = (HttpMessageReader<E>) reader;
			}
		}
		return result;
	}


	@Override
	public ServerHttpRequest getRequest() {
		return this.request;
	}

	private HttpHeaders getRequestHeaders() {
		return getRequest().getHeaders();
	}

	@Override
	public ServerHttpResponse getResponse() {
		return this.response;
	}

	private HttpHeaders getResponseHeaders() {
		return getResponse().getHeaders();
	}

	@Override
	public Map<String, Object> getAttributes() {
		return this.attributes;
	}

	@Override
	public Mono<WebSession> getSession() {
		return this.sessionMono;
	}

	@Override
	public <T extends Principal> Mono<T> getPrincipal() {
		return Mono.empty();
	}

	@Override
	public Mono<MultiValueMap<String, String>> getFormData() {
		return this.formDataMono;
	}

	@Override
	public Mono<MultiValueMap<String, Part>> getMultipartData() {
		return this.multipartDataMono;
	}

	@Override
	public Mono<Void> cleanupMultipart() {
		return Mono.defer(() -> {
			if (this.multipartRead) {
				return Mono.usingWhen(getMultipartData().onErrorComplete().map(this::collectParts),
						parts -> Mono.empty(),
						parts -> Flux.fromIterable(parts).flatMap(part -> part.delete().onErrorComplete())
				);
			}
			else {
				return Mono.empty();
			}
		});
	}

	private List<Part> collectParts(MultiValueMap<String, Part> multipartData) {
		return multipartData.values().stream().flatMap(List::stream).collect(Collectors.toList());
	}

	@Override
	public LocaleContext getLocaleContext() {
		return this.localeContextResolver.resolveLocaleContext(this);
	}

	@Override
	public @Nullable ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	@Override
	public boolean isNotModified() {
		return this.notModified;
	}

	@Override
	public boolean checkNotModified(Instant lastModified) {
		return checkNotModified(null, lastModified);
	}

	@Override
	public boolean checkNotModified(String etag) {
		return checkNotModified(etag, Instant.MIN);
	}

	@Override
	public boolean checkNotModified(@Nullable String eTag, Instant lastModified) {
		HttpStatusCode status = getResponse().getStatusCode();
		if (this.notModified || (status != null && !HttpStatus.OK.equals(status))) {
			return this.notModified;
		}
		// Evaluate conditions in order of precedence.
		// See https://datatracker.ietf.org/doc/html/rfc9110#section-13.2.2
		// 1) If-Match
		if (validateIfMatch(eTag)) {
			updateResponseStateChanging(eTag, lastModified);
			return this.notModified;
		}
		// 2) If-Unmodified-Since
		else if (validateIfUnmodifiedSince(lastModified)) {
			updateResponseStateChanging(eTag, lastModified);
			return this.notModified;
		}
		// 3) If-None-Match
		if (!validateIfNoneMatch(eTag)) {
			// 4) If-Modified-Since
			validateIfModifiedSince(lastModified);
		}
		updateResponseIdempotent(eTag, lastModified);
		return this.notModified;
	}

	private boolean validateIfMatch(@Nullable String eTag) {
		try {
			if (SAFE_METHODS.contains(getRequest().getMethod())) {
				return false;
			}
			List<String> values = getRequestHeaders().getOrEmpty(HttpHeaders.IF_MATCH);
			if (CollectionUtils.isEmpty(values)) {
				return false;
			}
			this.notModified = matchRequestedETags(values, eTag, false);
		}
		catch (IllegalArgumentException ex) {
			return false;
		}
		return true;
	}

	private boolean matchRequestedETags(List<String> requestedETagValues, @Nullable String tag, boolean weakCompare) {
		if (StringUtils.hasLength(tag)) {
			ETag eTag = ETag.create(tag);
			boolean isNotSafeMethod = !SAFE_METHODS.contains(getRequest().getMethod());
			for (String eTagValue : requestedETagValues) {
				for (ETag requestedETag : ETag.parse(eTagValue)) {
					// only consider "lost updates" checks for unsafe HTTP methods
					if (requestedETag.isWildcard() && isNotSafeMethod) {
						return false;
					}
					if (requestedETag.compare(eTag, !weakCompare)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private void updateResponseStateChanging(@Nullable String eTag, Instant lastModified) {
		if (this.notModified) {
			getResponse().setStatusCode(HttpStatus.PRECONDITION_FAILED);
		}
		else {
			addCachingResponseHeaders(eTag, lastModified);
		}
	}

	private boolean validateIfNoneMatch(@Nullable String eTag) {
		try {
			if (CollectionUtils.isEmpty(getRequestHeaders().get(HttpHeaders.IF_NONE_MATCH))) {
				return false;
			}
			List<String> values = getRequestHeaders().getOrEmpty(HttpHeaders.IF_NONE_MATCH);
			this.notModified = !matchRequestedETags(values, eTag, true);
		}
		catch (IllegalArgumentException ex) {
			return false;
		}
		return true;
	}

	private void updateResponseIdempotent(@Nullable String eTag, Instant lastModified) {
		boolean isSafeMethod = SAFE_METHODS.contains(getRequest().getMethod());
		if (this.notModified) {
			getResponse().setStatusCode(isSafeMethod ?
					HttpStatus.NOT_MODIFIED : HttpStatus.PRECONDITION_FAILED);
		}
		addCachingResponseHeaders(eTag, lastModified);
	}

	private void addCachingResponseHeaders(@Nullable String tag, Instant lastModified) {
		if (SAFE_METHODS.contains(getRequest().getMethod())) {
			if (lastModified.isAfter(Instant.EPOCH) && getResponseHeaders().getLastModified() == -1) {
				getResponseHeaders().setLastModified(lastModified.toEpochMilli());
			}
			if (StringUtils.hasLength(tag) && getResponseHeaders().getETag() == null) {
				getResponseHeaders().setETag(tag);
			}
		}
	}

	private boolean validateIfUnmodifiedSince(Instant lastModified) {
		if (lastModified.isBefore(Instant.EPOCH)) {
			return false;
		}
		long ifUnmodifiedSince = getRequestHeaders().getIfUnmodifiedSince();
		if (ifUnmodifiedSince == -1) {
			return false;
		}
		Instant sinceInstant = Instant.ofEpochMilli(ifUnmodifiedSince);
		this.notModified = sinceInstant.isBefore(lastModified.truncatedTo(ChronoUnit.SECONDS));
		return true;
	}

	private void validateIfModifiedSince(Instant lastModified) {
		if (lastModified.isBefore(Instant.EPOCH)) {
			return;
		}
		long ifModifiedSince = getRequestHeaders().getIfModifiedSince();
		if (ifModifiedSince != -1) {
			// We will perform this validation...
			this.notModified = ChronoUnit.SECONDS.between(lastModified, Instant.ofEpochMilli(ifModifiedSince)) >= 0;
		}
	}

	@Override
	public String transformUrl(String url) {
		return this.urlTransformer.apply(url);
	}

	@Override
	public void addUrlTransformer(Function<String, String> transformer) {
		Assert.notNull(transformer, "'encoder' must not be null");
		this.urlTransformer = this.urlTransformer.andThen(transformer);
	}

	@Override
	public String getLogPrefix() {
		Object value = getAttribute(LOG_ID_ATTRIBUTE);
		if (this.logId != value) {
			this.logId = value;
			this.logPrefix = value != null ? "[" + value + "] " : "";
		}
		return this.logPrefix;
	}

}
