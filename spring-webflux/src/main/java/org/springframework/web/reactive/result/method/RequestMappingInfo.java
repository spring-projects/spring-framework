/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.reactive.result.method;

import java.util.Set;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.accept.MappingContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.result.condition.ConsumesRequestCondition;
import org.springframework.web.reactive.result.condition.HeadersRequestCondition;
import org.springframework.web.reactive.result.condition.ParamsRequestCondition;
import org.springframework.web.reactive.result.condition.PatternsRequestCondition;
import org.springframework.web.reactive.result.condition.ProducesRequestCondition;
import org.springframework.web.reactive.result.condition.RequestCondition;
import org.springframework.web.reactive.result.condition.RequestConditionHolder;
import org.springframework.web.reactive.result.condition.RequestMethodsRequestCondition;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.support.HttpRequestPathHelper;
import org.springframework.web.util.patterns.PathPatternRegistry;

/**
 * Encapsulates the following request mapping conditions:
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

	private final String name;

	private final PatternsRequestCondition patternsCondition;

	private final RequestMethodsRequestCondition methodsCondition;

	private final ParamsRequestCondition paramsCondition;

	private final HeadersRequestCondition headersCondition;

	private final ConsumesRequestCondition consumesCondition;

	private final ProducesRequestCondition producesCondition;

	private final RequestConditionHolder customConditionHolder;


	public RequestMappingInfo(String name, PatternsRequestCondition patterns, RequestMethodsRequestCondition methods,
			ParamsRequestCondition params, HeadersRequestCondition headers, ConsumesRequestCondition consumes,
			ProducesRequestCondition produces, RequestCondition<?> custom) {

		this.name = (StringUtils.hasText(name) ? name : null);
		this.patternsCondition = (patterns != null ? patterns : new PatternsRequestCondition());
		this.methodsCondition = (methods != null ? methods : new RequestMethodsRequestCondition());
		this.paramsCondition = (params != null ? params : new ParamsRequestCondition());
		this.headersCondition = (headers != null ? headers : new HeadersRequestCondition());
		this.consumesCondition = (consumes != null ? consumes : new ConsumesRequestCondition());
		this.producesCondition = (produces != null ? produces : new ProducesRequestCondition());
		this.customConditionHolder = new RequestConditionHolder(custom);
	}

	/**
	 * Creates a new instance with the given request conditions.
	 */
	public RequestMappingInfo(PatternsRequestCondition patterns, RequestMethodsRequestCondition methods,
			ParamsRequestCondition params, HeadersRequestCondition headers, ConsumesRequestCondition consumes,
			ProducesRequestCondition produces, RequestCondition<?> custom) {

		this(null, patterns, methods, params, headers, consumes, produces, custom);
	}

	/**
	 * Re-create a RequestMappingInfo with the given custom request condition.
	 */
	public RequestMappingInfo(RequestMappingInfo info, RequestCondition<?> customRequestCondition) {
		this(info.name, info.patternsCondition, info.methodsCondition, info.paramsCondition, info.headersCondition,
				info.consumesCondition, info.producesCondition, customRequestCondition);
	}


	/**
	 * Return the name for this mapping, or {@code null}.
	 */
	public String getName() {
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
	 * Returns the "custom" condition of this {@link RequestMappingInfo}; or {@code null}.
	 */
	public RequestCondition<?> getCustomCondition() {
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
		RequestConditionHolder custom = this.customConditionHolder.combine(other.customConditionHolder);

		return new RequestMappingInfo(name, patterns,
				methods, params, headers, consumes, produces, custom.getCondition());
	}

	private String combineNames(RequestMappingInfo other) {
		if (this.name != null && other.name != null) {
			String separator = "#";
			return this.name + separator + other.name;
		}
		else if (this.name != null) {
			return this.name;
		}
		else {
			return (other.name != null ? other.name : null);
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
	public RequestMappingInfo getMatchingCondition(ServerWebExchange exchange) {
		RequestMethodsRequestCondition methods = this.methodsCondition.getMatchingCondition(exchange);
		ParamsRequestCondition params = this.paramsCondition.getMatchingCondition(exchange);
		HeadersRequestCondition headers = this.headersCondition.getMatchingCondition(exchange);
		ConsumesRequestCondition consumes = this.consumesCondition.getMatchingCondition(exchange);
		ProducesRequestCondition produces = this.producesCondition.getMatchingCondition(exchange);

		if (methods == null || params == null || headers == null || consumes == null || produces == null) {
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
				methods, params, headers, consumes, produces, custom.getCondition());
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
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof RequestMappingInfo)) {
			return false;
		}
		RequestMappingInfo otherInfo = (RequestMappingInfo) other;
		return (this.patternsCondition.equals(otherInfo.patternsCondition) &&
				this.methodsCondition.equals(otherInfo.methodsCondition) &&
				this.paramsCondition.equals(otherInfo.paramsCondition) &&
				this.headersCondition.equals(otherInfo.headersCondition) &&
				this.consumesCondition.equals(otherInfo.consumesCondition) &&
				this.producesCondition.equals(otherInfo.producesCondition) &&
				this.customConditionHolder.equals(otherInfo.customConditionHolder));
	}

	@Override
	public int hashCode() {
		return (this.patternsCondition.hashCode() * 31 +  // primary differentiation
				this.methodsCondition.hashCode() + this.paramsCondition.hashCode() +
				this.headersCondition.hashCode() + this.consumesCondition.hashCode() +
				this.producesCondition.hashCode() + this.customConditionHolder.hashCode());
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("{");
		builder.append(this.patternsCondition);
		if (!this.methodsCondition.isEmpty()) {
			builder.append(",methods=").append(this.methodsCondition);
		}
		if (!this.paramsCondition.isEmpty()) {
			builder.append(",params=").append(this.paramsCondition);
		}
		if (!this.headersCondition.isEmpty()) {
			builder.append(",headers=").append(this.headersCondition);
		}
		if (!this.consumesCondition.isEmpty()) {
			builder.append(",consumes=").append(this.consumesCondition);
		}
		if (!this.producesCondition.isEmpty()) {
			builder.append(",produces=").append(this.producesCondition);
		}
		if (!this.customConditionHolder.isEmpty()) {
			builder.append(",custom=").append(this.customConditionHolder);
		}
		builder.append('}');
		return builder.toString();
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

		private RequestMethod[] methods;

		private String[] params;

		private String[] headers;

		private String[] consumes;

		private String[] produces;

		private String mappingName;

		private RequestCondition<?> customCondition;

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
			RequestedContentTypeResolver contentTypeResolver = this.options.getContentTypeResolver();

			PathPatternRegistry pathPatternRegistry = this.options.getPathPatternRegistry();
			PathPatternRegistry conditionRegistry = new PathPatternRegistry();
			conditionRegistry.setUseTrailingSlashMatch(pathPatternRegistry.useTrailingSlashMatch());
			conditionRegistry.setUseSuffixPatternMatch(pathPatternRegistry.useSuffixPatternMatch());
			conditionRegistry.setFileExtensions(pathPatternRegistry.getFileExtensions());

			
			PatternsRequestCondition patternsCondition = new PatternsRequestCondition(
					this.paths, this.options.getPathHelper(), conditionRegistry);

			return new RequestMappingInfo(this.mappingName, patternsCondition,
					new RequestMethodsRequestCondition(methods),
					new ParamsRequestCondition(this.params),
					new HeadersRequestCondition(this.headers),
					new ConsumesRequestCondition(this.consumes, this.headers),
					new ProducesRequestCondition(this.produces, this.headers, contentTypeResolver),
					this.customCondition);
		}
	}


	/**
	 * Container for configuration options used for request mapping purposes.
	 * Such configuration is required to create RequestMappingInfo instances but
	 * is typically used across all RequestMappingInfo instances.
	 * @see Builder#options
	 */
	public static class BuilderConfiguration {

		private HttpRequestPathHelper pathHelper;

		private PathPatternRegistry pathPatternRegistry;

		private boolean registeredSuffixPatternMatch = false;

		private RequestedContentTypeResolver contentTypeResolver;

		/**
		 * Set a custom UrlPathHelper to use for the PatternsRequestCondition.
		 * <p>By default this is not set.
		 */
		public void setPathHelper(HttpRequestPathHelper pathHelper) {
			this.pathHelper = pathHelper;
		}

		public HttpRequestPathHelper getPathHelper() {
			return this.pathHelper;
		}

		public PathPatternRegistry getPathPatternRegistry() {
			if(this.pathPatternRegistry == null) {
				this.pathPatternRegistry = new PathPatternRegistry();
				this.pathPatternRegistry.setUseTrailingSlashMatch(true);
			}
			if(this.registeredSuffixPatternMatch) {
				RequestedContentTypeResolver resolver = getContentTypeResolver();
				if (resolver != null && resolver instanceof MappingContentTypeResolver) {
					if (resolver instanceof MappingContentTypeResolver) {
						Set<String> fileExtensions = ((MappingContentTypeResolver) resolver).getKeys();
						this.pathPatternRegistry.setFileExtensions(fileExtensions);
					}

				}
			}
			return this.pathPatternRegistry;
		}

		/**
		 * Whether suffix pattern matching should be restricted to registered
		 * file extensions only. Setting this property also sets
		 * suffixPatternMatch=true and requires that a
		 * {@link #setContentTypeResolver} is also configured in order to
		 * obtain the registered file extensions.
		 */
		public void setRegisteredSuffixPatternMatch(boolean registeredSuffixPatternMatch) {
			this.registeredSuffixPatternMatch = registeredSuffixPatternMatch;
		}

		public boolean useRegisteredSuffixPatternMatch() {
			return this.registeredSuffixPatternMatch;
		}

		/**
		 * Set the PathPatternRegistry to use for parsing and matching path patterns
		 * <p>By default, a new instance of {@link PathPatternRegistry} with
		 * {@link PathPatternRegistry#setUseTrailingSlashMatch(boolean)} set to {@code true}
		 */
		public void setPathPatternRegistry(PathPatternRegistry pathPatternRegistry) {
			this.pathPatternRegistry = pathPatternRegistry;
		}

		/**
		 * Set the ContentNegotiationManager to use for the ProducesRequestCondition.
		 * <p>By default this is not set.
		 */
		public void setContentTypeResolver(RequestedContentTypeResolver resolver) {
			this.contentTypeResolver = resolver;
		}

		public RequestedContentTypeResolver getContentTypeResolver() {
			return this.contentTypeResolver;
		}
	}

}
