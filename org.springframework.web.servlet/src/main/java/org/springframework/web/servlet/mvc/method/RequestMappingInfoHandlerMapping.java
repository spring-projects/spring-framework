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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;

/**
 * An {@link AbstractHandlerMethodMapping} variant that uses {@link RequestMappingInfo} to represent request 
 * mapping conditions.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1.0
 */
public abstract class RequestMappingInfoHandlerMapping extends AbstractHandlerMethodMapping<RequestMappingInfo> {

	@Override
	protected void handlerMethodsInitialized(Map<RequestMappingInfo, HandlerMethod> handlerMethods) {
		List<RequestMappingInfo> infos = new ArrayList<RequestMappingInfo>(handlerMethods.keySet());
		while (infos.size() > 1) {
			RequestMappingInfo info1 = infos.remove(0);
			for (RequestMappingInfo info2 : infos) {
				// TODO: validate duplicate consumable and producible media types
			}
		}
	}

	/**
	 * Get the URL path patterns associated with this {@link RequestMappingInfo}.
	 */
	@Override
	protected Set<String> getMappingPathPatterns(RequestMappingInfo info) {
		return info.getPatternsCondition().getPatterns();
	}

	/**
	 * Checks if the given RequestMappingInfo matches the current request and returns a potentially new 
	 * RequestMappingInfo instances tailored to the current request, for example containing the subset
	 * of URL patterns or media types that match the request.
	 *  
	 * @returns a RequestMappingInfo instance in case of a match; or {@code null} in case of no match. 
	 */
	@Override
	protected RequestMappingInfo getMatchingMapping(RequestMappingInfo info, HttpServletRequest request) {
		return info.getMatchingInfo(request);
	}

	/**
	 * Returns a {@link Comparator} for sorting {@link RequestMappingInfo} in the context of the given request.
	 */
	@Override
	protected Comparator<RequestMappingInfo> getMappingComparator(final HttpServletRequest request) {
		return new Comparator<RequestMappingInfo>() {
			public int compare(RequestMappingInfo info1, RequestMappingInfo info2) {
				return info1.compareTo(info2, request);
			}
		};
	}

	/**
	 * Exposes URI template variables and producible media types as request attributes.
	 * 
	 * @see HandlerMapping#URI_TEMPLATE_VARIABLES_ATTRIBUTE
	 * @see HandlerMapping#PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE
	 */
	@Override
	protected void handleMatch(RequestMappingInfo info, String lookupPath, HttpServletRequest request) {
		super.handleMatch(info, lookupPath, request);

		String pattern = info.getPatternsCondition().getPatterns().iterator().next();
		Map<String, String> uriTemplateVariables = getPathMatcher().extractUriTemplateVariables(pattern, lookupPath);
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVariables);

		if (!info.getProducesCondition().isEmpty()) {
			Set<MediaType> mediaTypes = info.getProducesCondition().getMediaTypes();
			request.setAttribute(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, mediaTypes);
		}
	}

	/**
	 * Iterates all {@link RequestMappingInfo}s looking for mappings that match by URL but not by HTTP method.
	 *
	 * @throws HttpRequestMethodNotSupportedException 
	 * 		if there are matches by URL but not by HTTP method
	 * @throws HttpMediaTypeNotAcceptableException 
	 * 		if there are matches by URL but the consumable media types don't match the 'Content-Type' header
	 * @throws HttpMediaTypeNotAcceptableException 
	 * 		if there are matches by URL but the producible media types don't match the 'Accept' header
	 */
	@Override
	protected HandlerMethod handleNoMatch(Set<RequestMappingInfo> requestMappingInfos, 
										  String lookupPath, 
										  HttpServletRequest request) throws ServletException {
		Set<String> allowedMethods = new HashSet<String>(6);
		Set<MediaType> consumableMediaTypes = new HashSet<MediaType>();
		Set<MediaType> producibleMediaTypes = new HashSet<MediaType>();
		for (RequestMappingInfo info : requestMappingInfos) {
			if (info.getPatternsCondition().getMatchingCondition(request) != null) {
				if (info.getMethodsCondition().getMatchingCondition(request) == null) {
					for (RequestMethod method : info.getMethodsCondition().getMethods()) {
						allowedMethods.add(method.name());
					}
				}
				if (info.getConsumesCondition().getMatchingCondition(request) == null) {
					consumableMediaTypes.addAll(info.getConsumesCondition().getMediaTypes());
				}
				if (info.getProducesCondition().getMatchingCondition(request) == null) {
					producibleMediaTypes.addAll(info.getProducesCondition().getMediaTypes());
				}
			}
		}
		if (!allowedMethods.isEmpty()) {
			throw new HttpRequestMethodNotSupportedException(request.getMethod(), allowedMethods);
		}
		else if (!consumableMediaTypes.isEmpty()) {
			MediaType contentType = null;
			if (StringUtils.hasLength(request.getContentType())) {
				contentType = MediaType.parseMediaType(request.getContentType());
			}
			throw new HttpMediaTypeNotSupportedException(contentType, new ArrayList<MediaType>(consumableMediaTypes));
		}
		else if (!producibleMediaTypes.isEmpty()) {
			throw new HttpMediaTypeNotAcceptableException(new ArrayList<MediaType>(producibleMediaTypes));
		}
		else {
			return null;
		}
	}

}
