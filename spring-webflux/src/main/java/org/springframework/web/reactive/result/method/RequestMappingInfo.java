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

package org.springframework.web.reactive.result.method;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.accept.ApiVersionStrategy;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.result.condition.ConsumesRequestCondition;
import org.springframework.web.reactive.result.condition.HeadersRequestCondition;
import org.springframework.web.reactive.result.condition.ParamsRequestCondition;
import org.springframework.web.reactive.result.condition.PatternsRequestCondition;
import org.springframework.web.reactive.result.condition.ProducesRequestCondition;
import org.springframework.web.reactive.result.condition.RequestCondition;
import org.springframework.web.reactive.result.condition.RequestConditionHolder;
import org.springframework.web.reactive.result.condition.RequestMethodsRequestCondition;
import org.springframework.web.reactive.result.condition.VersionRequestCondition;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Request mapping information. Encapsulates the following request mapping conditions:
 * <ol>
 * <li>{@link PatternsRequestCondition}
 * <li>{@link RequestMethodsRequestCondition}
 * <li>{@link ParamsRequestCondition}
 * <li>{@link HeadersRequestCondition}
 * <li>{@link ConsumesRequestCondition}
 * <li>{@link ProducesRequestCondition}
 * <li>{@code RequestCondition} (optional, custom request condition)
 * </ol>
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public final class RequestMappingInfo implements RequestCondition<RequestMappingInfo> {

	private static final PatternsRequestCondition EMPTY_PATTERNS = new PatternsRequestCondition();

	private static final RequestMethodsRequestCondition EMPTY_REQUEST_METHODS = new RequestMethodsRequestCondition();

	private static final ParamsRequestCondition EMPTY_PARAMS = new ParamsRequestCondition();

	private static final HeadersRequestCondition EMPTY_HEADERS = new HeadersRequestCondition();

	private static final ConsumesRequestCondition EMPTY_CONSUMES = new ConsumesRequestCondition();

	private static final ProducesRequestCondition EMPTY_PRODUCES = new ProducesRequestCondition();

	private static final RequestConditionHolder EMPTY_CUSTOM = new RequestConditionHolder(null);


	private final @Nullable String name;

	private final PatternsRequestCondition patternsCondition;

	private final RequestMethodsRequestCondition methodsCondition;

	private final ParamsRequestCondition paramsCondition;

	private final HeadersRequestCondition headersCondition;

	private final ConsumesRequestCondition consumesCondition;

	private final ProducesRequestCondition producesCondition;

	private final VersionRequestCondition versionCondition;

	private final RequestConditionHolder customConditionHolder;

	private final int hashCode;

	private final BuilderConfiguration options;


	private RequestMappingInfo(@Nullable String name, @Nullable PatternsRequestCondition patterns,
			@Nullable RequestMethodsRequestCondition methods, @Nullable ParamsRequestCondition params,
			@Nullable HeadersRequestCondition headers, @Nullable ConsumesRequestCondition consumes,
			@Nullable ProducesRequestCondition produces, @Nullable VersionRequestCondition version,
			@Nullable RequestCondition<?> custom, BuilderConfiguration options) {

		this.name = (StringUtils.hasText(name) ? name : null);
		this.patternsCondition = (patterns != null ? patterns : EMPTY_PATTERNS);
		this.methodsCondition = (methods != null ? methods : EMPTY_REQUEST_METHODS);
		this.paramsCondition = (params != null ? params : EMPTY_PARAMS);
		this.headersCondition = (headers != null ? headers : EMPTY_HEADERS);
		this.consumesCondition = (consumes != null ? consumes : EMPTY_CONSUMES);
		this.producesCondition = (produces != null ? produces : EMPTY_PRODUCES);
		this.versionCondition = (version != null ? version : new VersionRequestCondition(null, null));
		this.customConditionHolder = (custom != null ? new RequestConditionHolder(custom) : EMPTY_CUSTOM);
		this.options = options;

		this.hashCode = calculateHashCode(
				this.patternsCondition, this.methodsCondition, this.paramsCondition, this.headersCondition,
				this.consumesCondition, this.producesCondition, this.versionCondition, this.customConditionHolder);
	}


	/**
	 * Return the name for this mapping, or {@code null}.
	 */
	public @Nullable String getName() {
		return this.name;
	}

	/**
	 * Returns the URL patterns of this {@link RequestMappingInfo};
	 * or instance with 0 patterns, never {@code null}.
	 */
	public PatternsRequestCondition getPatternsCondition() {
		return this.patternsCondition;
	}

	/**
	 * Return the mapping paths that are not patterns.
	 * @since 5.3
	 */
	public Set<String> getDirectPaths() {
		return this.patternsCondition.getDirectPaths();
	}

	/**
	 * Returns the HTTP request methods of this {@link RequestMappingInfo};
	 * or instance with 0 request methods, never {@code null}.
	 */
	public RequestMethodsRequestCondition getMethodsCondition() {
		return this.methodsCondition;
	}

	/**
	 * Returns the "parameters" condition of this {@link RequestMappingInfo};
	 * or instance with 0 parameter expressions, never {@code null}.
	 */
	public ParamsRequestCondition getParamsCondition() {
		return this.paramsCondition;
	}

	/**
	 * Returns the "headers" condition of this {@link RequestMappingInfo};
	 * or instance with 0 header expressions, never {@code null}.
	 */
	public HeadersRequestCondition getHeadersCondition() {
		return this.headersCondition;
	}

	/**
	 * Returns the "consumes" condition of this {@link RequestMappingInfo};
	 * or instance with 0 consumes expressions, never {@code null}.
	 */
	public ConsumesRequestCondition getConsumesCondition() {
		return this.consumesCondition;
	}

	/**
	 * Returns the "produces" condition of this {@link RequestMappingInfo};
	 * or instance with 0 produces expressions, never {@code null}.
	 */
	public ProducesRequestCondition getProducesCondition() {
		return this.producesCondition;
	}

	/**
	 * Returns the version condition of this {@link RequestMappingInfo},
	 * or an instance without a version.
	 * @since 7.0
	 */
	public VersionRequestCondition getVersionCondition() {
		return this.versionCondition;
	}

	/**
	 * Returns the "custom" condition of this {@link RequestMappingInfo}; or {@code null}.
	 */
	public @Nullable RequestCondition<?> getCustomCondition() {
		return this.customConditionHolder.getCondition();
	}


	/**
	 * Combines "this" request mapping info (i.e. the current instance) with another request mapping info instance.
	 * <p>Example: combine type- and method-level request mappings.
	 * @return a new request mapping info instance; never {@code null}
	 */
	@Override
	public RequestMappingInfo combine(RequestMappingInfo other) {
		String name = combineNames(other);
		PatternsRequestCondition patterns = this.patternsCondition.combine(other.patternsCondition);
		RequestMethodsRequestCondition methods = this.methodsCondition.combine(other.methodsCondition);
		ParamsRequestCondition params = this.paramsCondition.combine(other.paramsCondition);
		HeadersRequestCondition headers = this.headersCondition.combine(other.headersCondition);
		ConsumesRequestCondition consumes = this.consumesCondition.combine(other.consumesCondition);
		ProducesRequestCondition produces = this.producesCondition.combine(other.producesCondition);
		VersionRequestCondition version = this.versionCondition.combine(other.versionCondition);
		RequestConditionHolder custom = this.customConditionHolder.combine(other.customConditionHolder);

		return new RequestMappingInfo(name, patterns,
				methods, params, headers, consumes, produces, version, custom.getCondition(), this.options);
	}

	private @Nullable String combineNames(RequestMappingInfo other) {
		if (this.name != null && other.name != null) {
			return this.name + "#" + other.name;
		}
		else if (this.name != null) {
			return this.name;
		}
		else {
			return other.name;
		}
	}

	/**
	 * Checks if all conditions in this request mapping info match the provided request and returns
	 * a potentially new request mapping info with conditions tailored to the current request.
	 * <p>For example the returned instance may contain the subset of URL patterns that match to
	 * the current request, sorted with best matching patterns on top.
	 * @return a new instance in case all conditions match; or {@code null} otherwise
	 */
	@Override
	public @Nullable RequestMappingInfo getMatchingCondition(ServerWebExchange exchange) {
		RequestMethodsRequestCondition methods = this.methodsCondition.getMatchingCondition(exchange);
		if (methods == null) {
			return null;
		}
		ParamsRequestCondition params = this.paramsCondition.getMatchingCondition(exchange);
		if (params == null) {
			return null;
		}
		HeadersRequestCondition headers = this.headersCondition.getMatchingCondition(exchange);
		if (headers == null) {
			return null;
		}
		// Match "Content-Type" and "Accept" (parsed ones and cached) before patterns
		ConsumesRequestCondition consumes = this.consumesCondition.getMatchingCondition(exchange);
		if (consumes == null) {
			return null;
		}
		ProducesRequestCondition produces = this.producesCondition.getMatchingCondition(exchange);
		if (produces == null) {
			return null;
		}
		VersionRequestCondition version = this.versionCondition.getMatchingCondition(exchange);
		if (version == null) {
			return null;
		}
		PatternsRequestCondition patterns = this.patternsCondition.getMatchingCondition(exchange);
		if (patterns == null) {
			return null;
		}
		RequestConditionHolder custom = this.customConditionHolder.getMatchingCondition(exchange);
		if (custom == null) {
			return null;
		}
		return new RequestMappingInfo(this.name, patterns,
				methods, params, headers, consumes, produces, version, custom.getCondition(), this.options);
	}

	/**
	 * Compares "this" info (i.e. the current instance) with another info in the context of a request.
	 * <p>Note: It is assumed both instances have been obtained via
	 * {@link #getMatchingCondition(ServerWebExchange)} to ensure they have conditions with
	 * content relevant to current request.
	 */
	@Override
	public int compareTo(RequestMappingInfo other, ServerWebExchange exchange) {
		int result = this.patternsCondition.compareTo(other.getPatternsCondition(), exchange);
		if (result != 0) {
			return result;
		}
		result = this.paramsCondition.compareTo(other.getParamsCondition(), exchange);
		if (result != 0) {
			return result;
		}
		result = this.headersCondition.compareTo(other.getHeadersCondition(), exchange);
		if (result != 0) {
			return result;
		}
		result = this.consumesCondition.compareTo(other.getConsumesCondition(), exchange);
		if (result != 0) {
			return result;
		}
		result = this.producesCondition.compareTo(other.getProducesCondition(), exchange);
		if (result != 0) {
			return result;
		}
		result = this.versionCondition.compareTo(other.getVersionCondition(), exchange);
		if (result != 0) {
			return result;
		}
		result = this.methodsCondition.compareTo(other.getMethodsCondition(), exchange);
		if (result != 0) {
			return result;
		}
		result = this.customConditionHolder.compareTo(other.customConditionHolder, exchange);
		if (result != 0) {
			return result;
		}
		return 0;
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof RequestMappingInfo that &&
				this.patternsCondition.equals(that.patternsCondition) &&
				this.methodsCondition.equals(that.methodsCondition) &&
				this.paramsCondition.equals(that.paramsCondition) &&
				this.headersCondition.equals(that.headersCondition) &&
				this.consumesCondition.equals(that.consumesCondition) &&
				this.producesCondition.equals(that.producesCondition) &&
				this.versionCondition.equals(that.versionCondition) &&
				this.customConditionHolder.equals(that.customConditionHolder)));
	}

	@Override
	public int hashCode() {
		return this.hashCode;
	}

	private static int calculateHashCode(
			PatternsRequestCondition patterns, RequestMethodsRequestCondition methods,
			ParamsRequestCondition params, HeadersRequestCondition headers,
			ConsumesRequestCondition consumes, ProducesRequestCondition produces,
			VersionRequestCondition version, RequestConditionHolder custom) {

		return patterns.hashCode() * 31 + methods.hashCode() + params.hashCode() +
				headers.hashCode() + consumes.hashCode() + produces.hashCode() +
				version.hashCode() + custom.hashCode();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("{");
		if (!this.methodsCondition.isEmpty()) {
			Set<RequestMethod> httpMethods = this.methodsCondition.getMethods();
			builder.append(httpMethods.size() == 1 ? httpMethods.iterator().next() : httpMethods);
		}
		if (!this.patternsCondition.isEmpty()) {
			Set<PathPattern> patterns = this.patternsCondition.getPatterns();
			builder.append(' ').append(patterns.size() == 1 ? patterns.iterator().next() : patterns);
		}
		if (!this.paramsCondition.isEmpty()) {
			builder.append(", params ").append(this.paramsCondition);
		}
		if (!this.headersCondition.isEmpty()) {
			builder.append(", headers ").append(this.headersCondition);
		}
		if (!this.consumesCondition.isEmpty()) {
			builder.append(", consumes ").append(this.consumesCondition);
		}
		if (!this.producesCondition.isEmpty()) {
			builder.append(", produces ").append(this.producesCondition);
		}
		if (!this.versionCondition.isEmpty()) {
			builder.append(", version ").append(this.versionCondition);
		}
		if (!this.customConditionHolder.isEmpty()) {
			builder.append(", and ").append(this.customConditionHolder);
		}
		builder.append('}');
		return builder.toString();
	}


	/**
	 * Return a builder to create a new RequestMappingInfo by modifying this one.
	 * @return a builder to create a new, modified instance
	 * @since 5.3.4
	 */
	public Builder mutate() {
		return new MutateBuilder(this);
	}

	/**
	 * Create a new {@code RequestMappingInfo.Builder} with the given paths.
	 * @param paths the paths to use
	 */
	public static Builder paths(String... paths) {
		return new DefaultBuilder(paths);
	}


	/**
	 * Defines a builder for creating a RequestMappingInfo.
	 */
	public interface Builder {

		/**
		 * Set the path patterns.
		 */
		Builder paths(String... paths);

		/**
		 * Set the request method conditions.
		 */
		Builder methods(RequestMethod... methods);

		/**
		 * Set the request param conditions.
		 */
		Builder params(String... params);

		/**
		 * Set the header conditions.
		 * <p>By default this is not set.
		 */
		Builder headers(String... headers);

		/**
		 * Set the consumes conditions.
		 */
		Builder consumes(String... consumes);

		/**
		 * Set the produces conditions.
		 */
		Builder produces(String... produces);

		/**
		 * Set the API version condition.
		 * @since 7.0
		 */
		Builder version(String version);

		/**
		 * Set the mapping name.
		 */
		Builder mappingName(String name);

		/**
		 * Set a custom condition to use.
		 */
		Builder customCondition(RequestCondition<?> condition);

		/**
		 * Provide additional configuration needed for request mapping purposes.
		 */
		Builder options(BuilderConfiguration options);

		/**
		 * Build the RequestMappingInfo.
		 */
		RequestMappingInfo build();
	}


	private static class DefaultBuilder implements Builder {

		private String[] paths;

		private RequestMethod @Nullable [] methods;

		private String @Nullable [] params;

		private String @Nullable [] headers;

		private String @Nullable [] consumes;

		private String @Nullable [] produces;

		private @Nullable String version;

		private boolean hasContentType;

		private boolean hasAccept;

		private @Nullable String mappingName;

		private @Nullable RequestCondition<?> customCondition;

		private BuilderConfiguration options = new BuilderConfiguration();

		public DefaultBuilder(String... paths) {
			this.paths = paths;
		}

		@Override
		public Builder paths(String... paths) {
			this.paths = paths;
			return this;
		}

		@Override
		public DefaultBuilder methods(RequestMethod... methods) {
			this.methods = methods;
			return this;
		}

		@Override
		public DefaultBuilder params(String... params) {
			this.params = params;
			return this;
		}

		@Override
		public DefaultBuilder headers(String... headers) {
			for (String header : headers) {
				this.hasContentType = this.hasContentType ||
						header.contains("Content-Type") || header.contains("content-type");
				this.hasAccept = this.hasAccept ||
						header.contains("Accept") || header.contains("accept");
			}
			this.headers = headers;
			return this;
		}

		@Override
		public DefaultBuilder consumes(String... consumes) {
			this.consumes = consumes;
			return this;
		}

		@Override
		public DefaultBuilder produces(String... produces) {
			this.produces = produces;
			return this;
		}

		@Override
		public Builder version(String version) {
			this.version = version;
			return this;
		}

		@Override
		public DefaultBuilder mappingName(String name) {
			this.mappingName = name;
			return this;
		}

		@Override
		public DefaultBuilder customCondition(RequestCondition<?> condition) {
			this.customCondition = condition;
			return this;
		}

		@Override
		public Builder options(BuilderConfiguration options) {
			this.options = options;
			return this;
		}

		@Override
		public RequestMappingInfo build() {
			PathPatternParser parser = (this.options.getPatternParser() != null ?
					this.options.getPatternParser() : PathPatternParser.defaultInstance);

			RequestedContentTypeResolver contentTypeResolver = this.options.getContentTypeResolver();

			ApiVersionStrategy strategy = this.options.getApiVersionStrategy();
			Assert.state(strategy != null || !StringUtils.hasText(this.version),
					"API version specified, but no ApiVersionStrategy configured");
			VersionRequestCondition versionCondition = new VersionRequestCondition(this.version, strategy);

			return new RequestMappingInfo(this.mappingName,
					isEmpty(this.paths) ? null : new PatternsRequestCondition(parse(this.paths, parser)),
					ObjectUtils.isEmpty(this.methods) ?
							null : new RequestMethodsRequestCondition(this.methods),
					ObjectUtils.isEmpty(this.params) ?
							null : new ParamsRequestCondition(this.params),
					ObjectUtils.isEmpty(this.headers) ?
							null : new HeadersRequestCondition(this.headers),
					ObjectUtils.isEmpty(this.consumes) && !this.hasContentType ?
							null : new ConsumesRequestCondition(this.consumes, this.headers),
					ObjectUtils.isEmpty(this.produces) && !this.hasAccept ?
							null : new ProducesRequestCondition(this.produces, this.headers, contentTypeResolver),
					versionCondition,
					this.customCondition,
					this.options);
		}

		static List<PathPattern> parse(String[] patterns, PathPatternParser parser) {
			if (isEmpty(patterns)) {
				return Collections.emptyList();
			}
			List<PathPattern> result = new ArrayList<>(patterns.length);
			for (String pattern : patterns) {
				pattern = parser.initFullPathPattern(pattern);
				result.add(parser.parse(pattern));
			}
			return result;
		}

		static boolean isEmpty(String[] patterns) {
			if (!ObjectUtils.isEmpty(patterns)) {
				for (String pattern : patterns) {
					if (StringUtils.hasText(pattern)) {
						return false;
					}
				}
			}
			return true;
		}
	}


	private static class MutateBuilder implements Builder {

		private @Nullable String name;

		private @Nullable PatternsRequestCondition patternsCondition;

		private RequestMethodsRequestCondition methodsCondition;

		private ParamsRequestCondition paramsCondition;

		private HeadersRequestCondition headersCondition;

		private ConsumesRequestCondition consumesCondition;

		private ProducesRequestCondition producesCondition;

		private VersionRequestCondition versionCondition;

		private RequestConditionHolder customConditionHolder;

		private BuilderConfiguration options;

		public MutateBuilder(RequestMappingInfo other) {
			this.name = other.name;
			this.patternsCondition = other.patternsCondition;
			this.methodsCondition = other.methodsCondition;
			this.paramsCondition = other.paramsCondition;
			this.headersCondition = other.headersCondition;
			this.consumesCondition = other.consumesCondition;
			this.producesCondition = other.producesCondition;
			this.versionCondition = other.versionCondition;
			this.customConditionHolder = other.customConditionHolder;
			this.options = other.options;
		}

		@Override
		public Builder paths(String... paths) {
			PathPatternParser parser = (this.options.getPatternParser() != null ?
					this.options.getPatternParser() : PathPatternParser.defaultInstance);
			this.patternsCondition = (DefaultBuilder.isEmpty(paths) ?
					null : new PatternsRequestCondition(DefaultBuilder.parse(paths, parser)));
			return this;
		}

		@Override
		public Builder methods(RequestMethod... methods) {
			this.methodsCondition = (ObjectUtils.isEmpty(methods) ?
					EMPTY_REQUEST_METHODS : new RequestMethodsRequestCondition(methods));
			return this;
		}

		@Override
		public Builder params(String... params) {
			this.paramsCondition = (ObjectUtils.isEmpty(params) ?
					EMPTY_PARAMS : new ParamsRequestCondition(params));
			return this;
		}

		@Override
		public Builder headers(String... headers) {
			this.headersCondition = (ObjectUtils.isEmpty(headers) ?
					EMPTY_HEADERS : new HeadersRequestCondition(headers));
			return this;
		}

		@Override
		public Builder consumes(String... consumes) {
			this.consumesCondition = (ObjectUtils.isEmpty(consumes) ?
					EMPTY_CONSUMES : new ConsumesRequestCondition(consumes));
			return this;
		}

		@Override
		public Builder produces(String... produces) {
			this.producesCondition = (ObjectUtils.isEmpty(produces) ?
					EMPTY_PRODUCES :
					new ProducesRequestCondition(produces, null, this.options.getContentTypeResolver()));
			return this;
		}

		@Override
		public Builder version(@Nullable String version) {
			ApiVersionStrategy strategy = this.options.getApiVersionStrategy();
			Assert.state(strategy != null || !StringUtils.hasText(version),
					"API version specified, but no ApiVersionStrategy configured");
			this.versionCondition = new VersionRequestCondition(version, strategy);
			return this;
		}

		@Override
		public Builder mappingName(String name) {
			this.name = name;
			return this;
		}

		@Override
		public Builder customCondition(RequestCondition<?> condition) {
			this.customConditionHolder = new RequestConditionHolder(condition);
			return this;
		}

		@Override
		public Builder options(BuilderConfiguration options) {
			this.options = options;
			return this;
		}

		@Override
		public RequestMappingInfo build() {
			return new RequestMappingInfo(this.name, this.patternsCondition,
					this.methodsCondition, this.paramsCondition, this.headersCondition,
					this.consumesCondition, this.producesCondition, this.versionCondition,
					this.customConditionHolder, this.options);
		}
	}


	/**
	 * Container for configuration options used for request mapping purposes.
	 * Such configuration is required to create RequestMappingInfo instances but
	 * is typically used across all RequestMappingInfo instances.
	 * @see Builder#options
	 */
	public static class BuilderConfiguration {

		private @Nullable PathPatternParser patternParser;

		private @Nullable RequestedContentTypeResolver contentTypeResolver;

		private @Nullable ApiVersionStrategy apiVersionStrategy;

		public void setPatternParser(PathPatternParser patternParser) {
			this.patternParser = patternParser;
		}

		public @Nullable PathPatternParser getPatternParser() {
			return this.patternParser;
		}

		/**
		 * Set the ContentNegotiationManager to use for the ProducesRequestCondition.
		 * <p>By default this is not set.
		 */
		public void setContentTypeResolver(RequestedContentTypeResolver resolver) {
			this.contentTypeResolver = resolver;
		}

		public @Nullable RequestedContentTypeResolver getContentTypeResolver() {
			return this.contentTypeResolver;
		}

		/**
		 * Set the strategy for API versioning.
		 * @param apiVersionStrategy the strategy to use
		 * @since 7.0
		 */
		public void setApiVersionStrategy(@Nullable ApiVersionStrategy apiVersionStrategy) {
			this.apiVersionStrategy = apiVersionStrategy;
		}

		/**
		 * Return the configured strategy for API versioning.
		 * @since 7.0
		 */
		public @Nullable ApiVersionStrategy getApiVersionStrategy() {
			return this.apiVersionStrategy;
		}
	}

}
