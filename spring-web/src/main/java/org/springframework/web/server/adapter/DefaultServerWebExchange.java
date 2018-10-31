/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.server.adapter;

import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import reactor.core.publisher.Mono;

import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Hints;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
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
 * @since 5.0
 */
public class DefaultServerWebExchange implements ServerWebExchange {

	private static final List<HttpMethod> SAFE_METHODS = Arrays.asList(HttpMethod.GET, HttpMethod.HEAD);

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

	@Nullable
	private final ApplicationContext applicationContext;

	private volatile boolean notModified;

	private Function<String, String> urlTransformer = url -> url;

	@Nullable
	private Object logId;

	private String logPrefix = "";


	public DefaultServerWebExchange(ServerHttpRequest request, ServerHttpResponse response,
			WebSessionManager sessionManager, ServerCodecConfigurer codecConfigurer,
			LocaleContextResolver localeContextResolver) {

		this(request, response, sessionManager, codecConfigurer, localeContextResolver, null);
	}

	DefaultServerWebExchange(ServerHttpRequest request, ServerHttpResponse response,
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
		this.multipartDataMono = initMultipartData(request, codecConfigurer, getLogPrefix());
		this.applicationContext = applicationContext;
	}

	@SuppressWarnings("unchecked")
	private static Mono<MultiValueMap<String, String>> initFormData(ServerHttpRequest request,
			ServerCodecConfigurer configurer, String logPrefix) {

		try {
			MediaType contentType = request.getHeaders().getContentType();
			if (MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(contentType)) {
				return ((HttpMessageReader<MultiValueMap<String, String>>) configurer.getReaders().stream()
						.filter(reader -> reader.canRead(FORM_DATA_TYPE, MediaType.APPLICATION_FORM_URLENCODED))
						.findFirst()
						.orElseThrow(() -> new IllegalStateException("No form data HttpMessageReader.")))
						.readMono(FORM_DATA_TYPE, request, Hints.from(Hints.LOG_PREFIX_HINT, logPrefix))
						.switchIfEmpty(EMPTY_FORM_DATA)
						.cache();
			}
		}
		catch (InvalidMediaTypeException ex) {
			// Ignore
		}
		return EMPTY_FORM_DATA;
	}

	@SuppressWarnings("unchecked")
	private static Mono<MultiValueMap<String, Part>> initMultipartData(ServerHttpRequest request,
			ServerCodecConfigurer configurer, String logPrefix) {

		try {
			MediaType contentType = request.getHeaders().getContentType();
			if (MediaType.MULTIPART_FORM_DATA.isCompatibleWith(contentType)) {
				return ((HttpMessageReader<MultiValueMap<String, Part>>) configurer.getReaders().stream()
						.filter(reader -> reader.canRead(MULTIPART_DATA_TYPE, MediaType.MULTIPART_FORM_DATA))
						.findFirst()
						.orElseThrow(() -> new IllegalStateException("No multipart HttpMessageReader.")))
						.readMono(MULTIPART_DATA_TYPE, request, Hints.from(Hints.LOG_PREFIX_HINT, logPrefix))
						.switchIfEmpty(EMPTY_MULTIPART_DATA)
						.cache();
			}
		}
		catch (InvalidMediaTypeException ex) {
			// Ignore
		}
		return EMPTY_MULTIPART_DATA;
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
	public LocaleContext getLocaleContext() {
		return this.localeContextResolver.resolveLocaleContext(this);
	}

	@Override
	@Nullable
	public ApplicationContext getApplicationContext() {
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
	public boolean checkNotModified(@Nullable String etag, Instant lastModified) {
		HttpStatus status = getResponse().getStatusCode();
		if (this.notModified || (status != null && !HttpStatus.OK.equals(status))) {
			return this.notModified;
		}

		// Evaluate conditions in order of precedence.
		// See https://tools.ietf.org/html/rfc7232#section-6

		if (validateIfUnmodifiedSince(lastModified)) {
			if (this.notModified) {
				getResponse().setStatusCode(HttpStatus.PRECONDITION_FAILED);
			}
			return this.notModified;
		}

		boolean validated = validateIfNoneMatch(etag);
		if (!validated) {
			validateIfModifiedSince(lastModified);
		}

		// Update response

		boolean isHttpGetOrHead = SAFE_METHODS.contains(getRequest().getMethod());
		if (this.notModified) {
			getResponse().setStatusCode(isHttpGetOrHead ?
					HttpStatus.NOT_MODIFIED : HttpStatus.PRECONDITION_FAILED);
		}
		if (isHttpGetOrHead) {
			if (lastModified.isAfter(Instant.EPOCH) && getResponseHeaders().getLastModified() == -1) {
				getResponseHeaders().setLastModified(lastModified.toEpochMilli());
			}
			if (StringUtils.hasLength(etag) && getResponseHeaders().getETag() == null) {
				getResponseHeaders().setETag(padEtagIfNecessary(etag));
			}
		}

		return this.notModified;
	}

	private boolean validateIfUnmodifiedSince(Instant lastModified) {
		if (lastModified.isBefore(Instant.EPOCH)) {
			return false;
		}
		long ifUnmodifiedSince = getRequestHeaders().getIfUnmodifiedSince();
		if (ifUnmodifiedSince == -1) {
			return false;
		}
		// We will perform this validation...
		Instant sinceInstant = Instant.ofEpochMilli(ifUnmodifiedSince);
		this.notModified = sinceInstant.isBefore(lastModified.truncatedTo(ChronoUnit.SECONDS));
		return true;
	}

	private boolean validateIfNoneMatch(@Nullable String etag) {
		if (!StringUtils.hasLength(etag)) {
			return false;
		}
		List<String> ifNoneMatch;
		try {
			ifNoneMatch = getRequestHeaders().getIfNoneMatch();
		}
		catch (IllegalArgumentException ex) {
			return false;
		}
		if (ifNoneMatch.isEmpty()) {
			return false;
		}
		// We will perform this validation...
		etag = padEtagIfNecessary(etag);
		if (etag.startsWith("W/")) {
			etag = etag.substring(2);
		}
		for (String clientEtag : ifNoneMatch) {
			// Compare weak/strong ETags as per https://tools.ietf.org/html/rfc7232#section-2.3
			if (StringUtils.hasLength(clientEtag)) {
				if (clientEtag.startsWith("W/")) {
					clientEtag = clientEtag.substring(2);
				}
				if (clientEtag.equals(etag)) {
					this.notModified = true;
					break;
				}
			}
		}
		return true;
	}

	private String padEtagIfNecessary(String etag) {
		if (!StringUtils.hasLength(etag)) {
			return etag;
		}
		if ((etag.startsWith("\"") || etag.startsWith("W/\"")) && etag.endsWith("\"")) {
			return etag;
		}
		return "\"" + etag + "\"";
	}

	private boolean validateIfModifiedSince(Instant lastModified) {
		if (lastModified.isBefore(Instant.EPOCH)) {
			return false;
		}
		long ifModifiedSince = getRequestHeaders().getIfModifiedSince();
		if (ifModifiedSince == -1) {
			return false;
		}
		// We will perform this validation...
		this.notModified = ChronoUnit.SECONDS.between(lastModified, Instant.ofEpochMilli(ifModifiedSince)) >= 0;
		return true;
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
