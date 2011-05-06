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

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.servlet.handler.MappedInterceptors;
import org.springframework.web.servlet.mvc.method.condition.RequestConditionFactory;

/**
 * An {@link AbstractHandlerMethodMapping} variant that uses {@link RequestMappingInfo}s for the registration and
 * the lookup of {@link HandlerMethod}s.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class RequestMappingHandlerMapping extends AbstractHandlerMethodMapping<RequestMappingInfo> {

	private PathMatcher pathMatcher = new AntPathMatcher();

	private MappedInterceptors mappedInterceptors;

	/**
	 * Set the PathMatcher implementation to use for matching URL paths against registered URL patterns. Default is
	 * AntPathMatcher.
	 *
	 * @see org.springframework.util.AntPathMatcher
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
	}

	/**
	 * Set the {@link MappedInterceptor} instances to use to intercept handler method invocations.
	 */
	public void setMappedInterceptors(MappedInterceptor[] mappedInterceptors) {
		this.mappedInterceptors = new MappedInterceptors(mappedInterceptors);
	}

	@Override
	protected void initInterceptors() {
		super.initInterceptors();
		if (this.mappedInterceptors == null) {
			this.mappedInterceptors = MappedInterceptors.createFromDeclaredBeans(getApplicationContext());
		}
	}
	
	/**
	 * {@inheritDoc}
	 * The handler determination is made based on the presence of a type-level {@link Controller} or
	 * a type-level {@link RequestMapping} annotation.
	 */
	@Override
	protected boolean isHandler(String beanName) {
		return ((getApplicationContext().findAnnotationOnBean(beanName, RequestMapping.class) != null) ||
				(getApplicationContext().findAnnotationOnBean(beanName, Controller.class) != null));
	}

	/**
	 * Provides a {@link RequestMappingInfo} for the given method.
	 * <p>Only {@link RequestMapping @RequestMapping}-annotated methods are considered.
	 * Type-level {@link RequestMapping @RequestMapping} annotations are also detected and their
	 * attributes combined with method-level {@link RequestMapping @RequestMapping} attributes.
	 *
	 * @param beanName the name of the bean the method belongs to
	 * @param method the method to create a mapping for
	 * @return the mapping, or {@code null}
	 * @see RequestMappingInfo#combine(RequestMappingInfo, PathMatcher)
	 */
	@Override
	protected RequestMappingInfo getMappingForMethod(String beanName, Method method) {
		RequestMapping annotation = AnnotationUtils.findAnnotation(method, RequestMapping.class);
		if (annotation != null) {
			RequestMappingInfo methodMapping = createFromRequestMapping(annotation);
			RequestMapping typeAnnot = getApplicationContext().findAnnotationOnBean(beanName, RequestMapping.class);
			if (typeAnnot != null) {
				RequestMappingInfo typeMapping = createFromRequestMapping(typeAnnot);
				return typeMapping.combine(methodMapping, pathMatcher);
			}
			else {
				return methodMapping;
			}
		}
		else {
			return null;
		}
	}

	private static RequestMappingInfo createFromRequestMapping(RequestMapping annotation) {
		return new RequestMappingInfo(Arrays.asList(annotation.value()), Arrays.asList(annotation.method()),
						RequestConditionFactory.parseParams(annotation.params()),
						RequestConditionFactory.parseHeaders(annotation.headers()),
						RequestConditionFactory.parseConsumes());
	}
	
	@Override
	protected Set<String> getMappingPaths(RequestMappingInfo mapping) {
		return mapping.getPatterns();
	}

	/**
	 * Returns a new {@link RequestMappingInfo} with attributes matching to the current request or {@code null}.
	 * @see RequestMappingInfo#getMatchingRequestMapping(String, HttpServletRequest, PathMatcher)
	 */
	@Override
	protected RequestMappingInfo getMatchingMapping(RequestMappingInfo mapping, String lookupPath, HttpServletRequest request) {
		return mapping.getMatchingRequestMapping(lookupPath, request, pathMatcher);
	}

	/**
	 * Returns a {@link Comparator} that can be used to sort and select the best matching {@link RequestMappingInfo}.
	 */
	@Override
	protected Comparator<RequestMappingInfo> getMappingComparator(String lookupPath, HttpServletRequest request) {
		return new RequestMappingInfoComparator(lookupPath, request);
	}

	@Override
	protected void handleMatch(RequestMappingInfo mapping, String lookupPath, HttpServletRequest request) {
		super.handleMatch(mapping, lookupPath, request);
		String pattern = mapping.getPatterns().iterator().next();
		Map<String, String> uriTemplateVariables = pathMatcher.extractUriTemplateVariables(pattern, lookupPath);
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVariables);
	}

	/**
	 * Iterates all {@link RequestMappingInfo}s looking for mappings that match by URL but not by HTTP method.
	 * @exception HttpRequestMethodNotSupportedException if there are matches by URL but not by HTTP method
	 */
	@Override
	protected HandlerMethod handleNoMatch(Set<RequestMappingInfo> requestMappingInfos, String lookupPath, HttpServletRequest request)
			throws HttpRequestMethodNotSupportedException {
		Set<String> allowedMethods = new HashSet<String>(6);
		for (RequestMappingInfo info : requestMappingInfos) {
			for (String pattern : info.getPatterns()) {
				if (pathMatcher.match(pattern, lookupPath)) {
					for (RequestMethod method : info.getMethods()) {
						allowedMethods.add(method.name());
					}
				}
			}
		}
		if (!allowedMethods.isEmpty()) {
			throw new HttpRequestMethodNotSupportedException(request.getMethod(),
					allowedMethods.toArray(new String[allowedMethods.size()]));

		} else {
			return null;
		}
	}

	/**
	 * Adds mapped interceptors to the handler execution chain.
	 */
	@Override
	protected HandlerExecutionChain getHandlerExecutionChain(Object handler, HttpServletRequest request) {
		HandlerExecutionChain chain = super.getHandlerExecutionChain(handler, request);
		if (this.mappedInterceptors != null) {
			String lookupPath = getUrlPathHelper().getLookupPathForRequest(request);
			HandlerInterceptor[] handlerInterceptors = mappedInterceptors.getInterceptors(lookupPath, pathMatcher);
			if (handlerInterceptors.length > 0) {
				chain.addInterceptors(handlerInterceptors);
			}
		}
		return chain;
	}

	/**
	 * A comparator for {@link RequestMappingInfo}s. Effective comparison can only be done in the context of a
	 * specific request. For example not all {@link RequestMappingInfo} patterns may apply to the current request.
	 * Therefore an HttpServletRequest is required as input.
	 *
	 * <p>Furthermore, the following assumptions are made about the input RequestMappings:
	 * <ul><li>Each RequestMappingInfo has been fully matched to the request <li>The RequestMappingInfo contains
	 * matched patterns only <li>Patterns are ordered with the best matching pattern at the top </ul>
	 *
	 * @see RequestMappingHandlerMapping#getMatchingMapping(RequestMappingInfo, String, HttpServletRequest)
	 */
	private class RequestMappingInfoComparator implements Comparator<RequestMappingInfo> {

		private Comparator<String> patternComparator;

		private List<MediaType> requestAcceptHeader;

		public RequestMappingInfoComparator(String lookupPath, HttpServletRequest request) {
			this.patternComparator = pathMatcher.getPatternComparator(lookupPath);
			String acceptHeader = request.getHeader("Accept");
			this.requestAcceptHeader = MediaType.parseMediaTypes(acceptHeader);
			MediaType.sortByQualityValue(this.requestAcceptHeader);
		}

		public int compare(RequestMappingInfo mapping, RequestMappingInfo otherMapping) {
			int result = comparePatterns(mapping.getPatterns(), otherMapping.getPatterns());
			if (result != 0) {
				return result;
			}
			result = mapping.getParams().compareTo(otherMapping.getParams());
			if (result != 0) {
				return result;
			}
			result = mapping.getHeaders().compareTo(otherMapping.getHeaders());
			if (result != 0) {
				return result;
			}
/*
			TODO: fix
			result = compareAcceptHeaders(mapping.getAcceptHeaderMediaTypes(), otherMapping.getAcceptHeaderMediaTypes());
			if (result != 0) {
				return result;
			}
*/
			result = otherMapping.getMethods().size() - mapping.getMethods().size();
			if (result != 0) {
				return result;
			}
			return 0;
		}

		private int comparePatterns(Set<String> patterns, Set<String> otherPatterns) {
			Iterator<String> iterator = patterns.iterator();
			Iterator<String> iteratorOther = otherPatterns.iterator();
			while (iterator.hasNext() && iteratorOther.hasNext()) {
				int result = patternComparator.compare(iterator.next(), iteratorOther.next());
				if (result != 0) {
					return result;
				}
			}
			if (iterator.hasNext()) {
				return -1;
			}
			else if (iteratorOther.hasNext()) {
				return 1;
			}
			else {
				return 0;
			}
		}

		private int compareAcceptHeaders(List<MediaType> accept, List<MediaType> otherAccept) {
			for (MediaType requestAccept : this.requestAcceptHeader) {
				int pos1 = indexOfIncluded(requestAccept, accept);
				int pos2 = indexOfIncluded(requestAccept, otherAccept);
				if (pos1 != pos2) {
					return pos2 - pos1;
				}
			}
			return 0;
		}

		private int indexOfIncluded(MediaType requestAccept, List<MediaType> accept) {
			for (int i = 0; i < accept.size(); i++) {
				if (requestAccept.includes(accept.get(i))) {
					return i;
				}
			}
			return -1;
		}

	}

}
