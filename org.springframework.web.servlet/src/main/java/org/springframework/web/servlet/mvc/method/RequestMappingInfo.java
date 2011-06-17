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

import org.springframework.util.PathMatcher;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.condition.ConsumesRequestCondition;
import org.springframework.web.servlet.mvc.method.condition.HeadersRequestCondition;
import org.springframework.web.servlet.mvc.method.condition.ParamsRequestCondition;
import org.springframework.web.servlet.mvc.method.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.condition.ProducesRequestCondition;
import org.springframework.web.servlet.mvc.method.condition.RequestMethodsRequestCondition;

/**
 * Contains request mapping conditions to be matched to a given request.
 * 
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class RequestMappingInfo {

	private final PatternsRequestCondition patternsCondition;

	private final RequestMethodsRequestCondition methodsCondition;

	private final ParamsRequestCondition paramsCondition;

	private final HeadersRequestCondition headersCondition;

	private final ConsumesRequestCondition consumesCondition;

	private final ProducesRequestCondition producesCondition;

	private int hash;

	/**
	 * Creates a new {@code RequestMappingInfo} instance.
	 */
	public RequestMappingInfo(PatternsRequestCondition patternsCondition,
							  RequestMethodsRequestCondition methodsCondition,
							  ParamsRequestCondition paramsCondition,
							  HeadersRequestCondition headersCondition,
							  ConsumesRequestCondition consumesCondition,
							  ProducesRequestCondition producesCondition) {
		this.patternsCondition = patternsCondition != null ? patternsCondition : new PatternsRequestCondition();
		this.methodsCondition = methodsCondition != null ? methodsCondition : new RequestMethodsRequestCondition();
		this.paramsCondition = paramsCondition != null ? paramsCondition : new ParamsRequestCondition();
		this.headersCondition = headersCondition != null ? headersCondition : new HeadersRequestCondition();
		this.consumesCondition = consumesCondition != null ? consumesCondition : new ConsumesRequestCondition();
		this.producesCondition = producesCondition != null ? producesCondition : new ProducesRequestCondition();
	}

	/**
	 * Package protected, used for testing.
	 */
	RequestMappingInfo(String[] patterns, RequestMethod... methods) {
		this(new PatternsRequestCondition(patterns), new RequestMethodsRequestCondition(methods), null, null, null, null);
	}

	/**
	 * Returns the patterns of this request mapping info.
	 */
	public PatternsRequestCondition getPatternsCondition() {
		return patternsCondition;
	}

	/**
	 * Returns the request method condition of this request mapping info.
	 */
	public RequestMethodsRequestCondition getMethodsCondition() {
		return methodsCondition;
	}

	/**
	 * Returns the request parameters condition of this request mapping info.
	 */
	public ParamsRequestCondition getParamsCondition() {
		return paramsCondition;
	}

	/**
	 * Returns the request headers condition of this request mapping info.
	 */
	public HeadersRequestCondition getHeadersCondition() {
		return headersCondition;
	}

	/**
	 * Returns the request consumes condition of this request mapping info.
	 */
	public ConsumesRequestCondition getConsumesCondition() {
		return consumesCondition;
	}

	/**
	 * Returns the request produces condition of this request mapping info.
	 */
	public ProducesRequestCondition getProducesCondition() {
		return producesCondition;
	}

	/**
	 * Combines this {@code RequestMappingInfo} with another as follows:
	 * <ul>
	 * <li>URL patterns:
	 * 	<ul>
	 * 	  <li>If both have patterns combine them according to the rules of the given {@link PathMatcher}
	 * 	  <li>If either contains patterns, but not both, use the available pattern
	 * 	  <li>If neither contains patterns use ""
	 * 	</ul>
	 * <li>HTTP methods are combined as union of all HTTP methods listed in both keys.
	 * <li>Request parameters are combined as per {@link ParamsRequestCondition#combine(ParamsRequestCondition)}.
	 * <li>Request headers are combined as per {@link HeadersRequestCondition#combine(HeadersRequestCondition)}.
	 * <li>Consumes are combined as per {@link ConsumesRequestCondition#combine(ConsumesRequestCondition)}.
	 * </ul>
	 * @param methodKey the key to combine with
	 * @return a new request mapping info containing conditions from both keys
	 */
	public RequestMappingInfo combine(RequestMappingInfo methodKey) {
		PatternsRequestCondition patterns = this.patternsCondition.combine(methodKey.patternsCondition);
		RequestMethodsRequestCondition methods = this.methodsCondition.combine(methodKey.methodsCondition);
		ParamsRequestCondition params = this.paramsCondition.combine(methodKey.paramsCondition);
		HeadersRequestCondition headers = this.headersCondition.combine(methodKey.headersCondition);
		ConsumesRequestCondition consumes = this.consumesCondition.combine(methodKey.consumesCondition);
		ProducesRequestCondition produces = this.producesCondition.combine(methodKey.producesCondition);
		
		return new RequestMappingInfo(patterns, methods, params, headers, consumes, produces);
	}

	/**
	 * Returns a new {@code RequestMappingInfo} with conditions relevant to the current request.
	 * For example the list of URL path patterns is trimmed to contain the patterns that match the URL.
	 * @param request the current request
	 * @return a new request mapping info that contains all matching attributes, or {@code null} if not all conditions match
	 */
	public RequestMappingInfo getMatchingRequestMapping(HttpServletRequest request) {
		RequestMethodsRequestCondition matchingMethod = methodsCondition.getMatchingCondition(request);
		ParamsRequestCondition matchingParams = paramsCondition.getMatchingCondition(request);
		HeadersRequestCondition matchingHeaders = headersCondition.getMatchingCondition(request);
		ConsumesRequestCondition matchingConsumes = consumesCondition.getMatchingCondition(request);
		ProducesRequestCondition matchingProduces = producesCondition.getMatchingCondition(request);

		if (matchingMethod == null || matchingParams == null || matchingHeaders == null ||
				matchingConsumes == null || matchingProduces == null)  {
			return null;
		}
		
		PatternsRequestCondition matchingPatterns = patternsCondition.getMatchingCondition(request);
		if (matchingPatterns != null) {
			return new RequestMappingInfo(matchingPatterns, matchingMethod,
					matchingParams, matchingHeaders, matchingConsumes,
					matchingProduces);
		}

		return null;
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
					this.producesCondition.equals(other.producesCondition));
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
		builder.append('}');
		return builder.toString();
	}

}
