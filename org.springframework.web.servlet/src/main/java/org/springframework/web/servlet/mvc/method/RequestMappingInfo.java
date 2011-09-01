/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.servlet.mvc.method;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.mvc.condition.ConsumesRequestCondition;
import org.springframework.web.servlet.mvc.condition.HeadersRequestCondition;
import org.springframework.web.servlet.mvc.condition.ParamsRequestCondition;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.ProducesRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestConditionHolder;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;

/**
 * Encapsulates the following request mapping conditions:
 * <ol>
 * 	<li>{@link PatternsRequestCondition}
 * 	<li>{@link RequestMethodsRequestCondition}
 * 	<li>{@link ParamsRequestCondition}
 * 	<li>{@link HeadersRequestCondition}
 * 	<li>{@link ConsumesRequestCondition}
 * 	<li>{@link ProducesRequestCondition}
 * 	<li>{@code RequestCondition<?>} (optional, custom request condition)
 * </ol>
 * 
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class RequestMappingInfo implements RequestCondition<RequestMappingInfo> {

	private final PatternsRequestCondition patternsCondition;

	private final RequestMethodsRequestCondition methodsCondition;

	private final ParamsRequestCondition paramsCondition;

	private final HeadersRequestCondition headersCondition;

	private final ConsumesRequestCondition consumesCondition;

	private final ProducesRequestCondition producesCondition;
	
	private final RequestConditionHolder customConditionHolder;

	private int hash;

	/**
	 * Creates a new instance with the given request conditions.
	 */
	public RequestMappingInfo(PatternsRequestCondition patterns,
							  RequestMethodsRequestCondition methods, 
							  ParamsRequestCondition params,
							  HeadersRequestCondition headers, 
							  ConsumesRequestCondition consumes,
							  ProducesRequestCondition produces,
							  RequestCondition<?> custom) {
		this.patternsCondition = patterns != null ? patterns : new PatternsRequestCondition();
		this.methodsCondition = methods != null ? methods : new RequestMethodsRequestCondition();
		this.paramsCondition = params != null ? params : new ParamsRequestCondition();
		this.headersCondition = headers != null ? headers : new HeadersRequestCondition();
		this.consumesCondition = consumes != null ? consumes : new ConsumesRequestCondition();
		this.producesCondition = produces != null ? produces : new ProducesRequestCondition();
		this.customConditionHolder = new RequestConditionHolder(custom);
	}

	/**
	 * Re-create a RequestMappingInfo with the given custom request condition.
	 */
	public RequestMappingInfo(RequestMappingInfo info, RequestCondition<?> customRequestCondition) {
		this(info.patternsCondition, info.methodsCondition, info.paramsCondition, info.headersCondition,
				info.consumesCondition, info.producesCondition, customRequestCondition);
	}

	/**
	 * Returns the URL patterns of this {@link RequestMappingInfo}; 
	 * or instance with 0 patterns, never {@code null}
	 */
	public PatternsRequestCondition getPatternsCondition() {
		return patternsCondition;
	}

	/**
	 * Returns the HTTP request methods of this {@link RequestMappingInfo}; 
	 * or instance with 0 request methods, never {@code null}
	 */
	public RequestMethodsRequestCondition getMethodsCondition() {
		return methodsCondition;
	}

	/**
	 * Returns the "parameters" condition of this {@link RequestMappingInfo};
	 * or instance with 0 parameter expressions, never {@code null}
	 */
	public ParamsRequestCondition getParamsCondition() {
		return paramsCondition;
	}

	/**
	 * Returns the "headers" condition of this {@link RequestMappingInfo};
	 * or instance with 0 header expressions, never {@code null}
	 */
	public HeadersRequestCondition getHeadersCondition() {
		return headersCondition;
	}

	/**
	 * Returns the "consumes" condition of this {@link RequestMappingInfo};
	 * or instance with 0 consumes expressions, never {@code null}
	 */
	public ConsumesRequestCondition getConsumesCondition() {
		return consumesCondition;
	}

	/**
	 * Returns the "produces" condition of this {@link RequestMappingInfo};
	 * or instance with 0 produces expressions, never {@code null}
	 */
	public ProducesRequestCondition getProducesCondition() {
		return producesCondition;
	}

	/**
	 * Returns the "custom" condition of this {@link RequestMappingInfo}; or {@code null}
	 */
	public RequestCondition<?> getCustomCondition() {
		return customConditionHolder.getCondition();
	}

	/**
	 * Combines "this" request mapping info (i.e. the current instance) with another request mapping info instance.
	 * <p>Example: combine type- and method-level request mappings.
	 * @return a new request mapping info instance; never {@code null}
	 */
	public RequestMappingInfo combine(RequestMappingInfo other) {
		PatternsRequestCondition patterns = this.patternsCondition.combine(other.patternsCondition);
		RequestMethodsRequestCondition methods = this.methodsCondition.combine(other.methodsCondition);
		ParamsRequestCondition params = this.paramsCondition.combine(other.paramsCondition);
		HeadersRequestCondition headers = this.headersCondition.combine(other.headersCondition);
		ConsumesRequestCondition consumes = this.consumesCondition.combine(other.consumesCondition);
		ProducesRequestCondition produces = this.producesCondition.combine(other.producesCondition);
		RequestConditionHolder custom = this.customConditionHolder.combine(other.customConditionHolder);

		return new RequestMappingInfo(patterns, methods, params, headers, consumes, produces, custom.getCondition());
	}

	/**
	 * Checks if all conditions in this request mapping info match the provided request and returns 
	 * a potentially new request mapping info with conditions tailored to the current request. 
	 * <p>For example the returned instance may contain the subset of URL patterns that match to 
	 * the current request, sorted with best matching patterns on top.
	 * @return a new instance in case all conditions match; or {@code null} otherwise
	 */
	public RequestMappingInfo getMatchingCondition(HttpServletRequest request) {
		RequestMethodsRequestCondition methods = methodsCondition.getMatchingCondition(request);
		ParamsRequestCondition params = paramsCondition.getMatchingCondition(request);
		HeadersRequestCondition headers = headersCondition.getMatchingCondition(request);
		ConsumesRequestCondition consumes = consumesCondition.getMatchingCondition(request);
		ProducesRequestCondition produces = producesCondition.getMatchingCondition(request);
		
		if (methods == null || params == null || headers == null || consumes == null || produces == null) {
			return null;
		}
		
		PatternsRequestCondition patterns = patternsCondition.getMatchingCondition(request);
		if (patterns == null) {
			return null;
		}
		
		RequestConditionHolder custom = customConditionHolder.getMatchingCondition(request);
		if (custom == null) {
			return null;
		}
		
		return new RequestMappingInfo(patterns, methods, params, headers, consumes, produces, custom.getCondition());
	}
	
	/**
	 * Compares "this" info (i.e. the current instance) with another info in the context of a request. 
	 * <p>Note: it is assumed both instances have been obtained via 
	 * {@link #getMatchingCondition(HttpServletRequest)} to ensure they have conditions with
	 * content relevant to current request.
	 */
	public int compareTo(RequestMappingInfo other, HttpServletRequest request) {
		int result = patternsCondition.compareTo(other.getPatternsCondition(), request);
		if (result != 0) {
			return result;
		}
		result = paramsCondition.compareTo(other.getParamsCondition(), request);
		if (result != 0) {
			return result;
		}
		result = headersCondition.compareTo(other.getHeadersCondition(), request);
		if (result != 0) {
			return result;
		}
		result = consumesCondition.compareTo(other.getConsumesCondition(), request);
		if (result != 0) {
			return result;
		}
		result = producesCondition.compareTo(other.getProducesCondition(), request);
		if (result != 0) {
			return result;
		}
		result = methodsCondition.compareTo(other.getMethodsCondition(), request);
		if (result != 0) {
			return result;
		}
		result = customConditionHolder.compareTo(other.customConditionHolder, request);
		if (result != 0) {
			return result;
		}
		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj != null && obj instanceof RequestMappingInfo) {
			RequestMappingInfo other = (RequestMappingInfo) obj;
			return (this.patternsCondition.equals(other.patternsCondition) &&
					this.methodsCondition.equals(other.methodsCondition) &&
					this.paramsCondition.equals(other.paramsCondition) &&
					this.headersCondition.equals(other.headersCondition) &&
					this.consumesCondition.equals(other.consumesCondition) &&
					this.producesCondition.equals(other.producesCondition) && 
					this.customConditionHolder.equals(other.customConditionHolder));
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = hash;
		if (result == 0) {
			result = patternsCondition.hashCode();
			result = 31 * result + methodsCondition.hashCode();
			result = 31 * result + paramsCondition.hashCode();
			result = 31 * result + headersCondition.hashCode();
			result = 31 * result + consumesCondition.hashCode();
			result = 31 * result + producesCondition.hashCode();
			result = 31 * result + customConditionHolder.hashCode();
			hash = result;
		}
		return result;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("{");
		builder.append(patternsCondition);
		builder.append(",methods=").append(methodsCondition);
		builder.append(",params=").append(paramsCondition);
		builder.append(",headers=").append(headersCondition);
		builder.append(",consumes=").append(consumesCondition);
		builder.append(",produces=").append(producesCondition);
		builder.append(",custom=").append(customConditionHolder);
		builder.append('}');
		return builder.toString();
	}

}
