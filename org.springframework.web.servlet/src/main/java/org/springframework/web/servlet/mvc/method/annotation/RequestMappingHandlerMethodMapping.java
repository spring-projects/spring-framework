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
import org.springframework.web.util.UrlPathHelper;

/**
 * An {@link AbstractHandlerMethodMapping} variant that uses {@link RequestKey}s for the registration and the lookup 
 * of {@link HandlerMethod}s.
 * 
 * <p>A {@link RequestKey} for an incoming request contains the URL and the HTTP method of the request. 
 * A {@link RequestKey} for a handler method contains all conditions found in the method @{@link RequestMapping} 
 * annotation combined with all conditions found in the type @{@link RequestMapping} annotation, if present. 
 * 
 * <p>An incoming request matches to a handler method directly when a @{@link RequestMapping} annotation contains 
 * a single, non-pattern URL and a single HTTP method. When a {@link RequestKey} contains additional conditions 
 * (e.g. more URL patterns, request parameters, headers, etc) those conditions must be checked against the 
 * request rather than against the key that represents it. This results in the creation of a new handler method 
 * {@link RequestKey} with the subset of conditions relevant to the current request (see 
 * {@link RequestKey#getMatchingKey(HttpServletRequest, PathMatcher, UrlPathHelper)}).
 * Such keys can then be compared against each other, in the context of the current request, making it possible 
 * to select to the best matching {@link RequestKey} in case of multiple matches and also the best matching 
 * pattern within the selected key.   
 * 
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1.0
 */
public class RequestMappingHandlerMethodMapping extends AbstractHandlerMethodMapping<RequestKey> {

	private UrlPathHelper urlPathHelper = new UrlPathHelper();

	private PathMatcher pathMatcher = new AntPathMatcher();

	private MappedInterceptors mappedInterceptors;

	/**
	 * Set if URL lookup should always use the full path within the current servlet context. Else, the path within the
	 * current servlet mapping is used if applicable (that is, in the case of a ".../*" servlet mapping in web.xml).
	 * <p>Default is "false".
	 *
	 * @see org.springframework.web.util.UrlPathHelper#setAlwaysUseFullPath
	 */
	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
		this.urlPathHelper.setAlwaysUseFullPath(alwaysUseFullPath);
	}

	/**
	 * Set if context path and request URI should be URL-decoded. Both are returned <i>undecoded</i> by the Servlet API, in
	 * contrast to the servlet path. <p>Uses either the request encoding or the default encoding according to the Servlet
	 * spec (ISO-8859-1).
	 *
	 * @see org.springframework.web.util.UrlPathHelper#setUrlDecode
	 */
	public void setUrlDecode(boolean urlDecode) {
		this.urlPathHelper.setUrlDecode(urlDecode);
	}

	/**
	 * Set the UrlPathHelper to use for resolution of lookup paths. <p>Use this to override the default UrlPathHelper 
	 * with a custom subclass, or to share common UrlPathHelper settings across multiple HandlerMappings and
	 * MethodNameResolvers.
	 *
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
	}

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
	 * Returns a {@link RequestKey} instances that represents the given HTTP servlet request.
	 *
	 * @param request the request to look up the key for
	 * @return the key, never null
	 */
	@Override
	protected RequestKey getKeyForRequest(HttpServletRequest request) {
		return RequestKey.createFromServletRequest(request, urlPathHelper);
	}

	/**
	 * Provides a {@link RequestKey} for the given method. 
	 * <p>Only {@link RequestMapping @RequestMapping}-annotated methods are considered. 
	 * Type-level {@link RequestMapping @RequestMapping} annotations are also detected and their 
	 * attributes combined with method-level {@link RequestMapping @RequestMapping} attributes.
	 *
	 * @param beanName the name of the bean the method belongs to
	 * @param method the method to create a key for
	 * @return the key, or {@code null}
	 * @see RequestKey#combine(RequestKey, PathMatcher)
	 */
	@Override
	protected RequestKey getKeyForMethod(String beanName, Method method) {
		RequestMapping annotation = AnnotationUtils.findAnnotation(method, RequestMapping.class);
		if (annotation != null) {
			RequestKey methodKey = RequestKey.createFromRequestMapping(annotation);
			RequestMapping typeAnnot = getApplicationContext().findAnnotationOnBean(beanName, RequestMapping.class);
			if (typeAnnot != null) {
				RequestKey typeKey = RequestKey.createFromRequestMapping(typeAnnot);
				return typeKey.combine(methodKey, pathMatcher);
			}
			else {
				return methodKey;
			}
		}
		else {
			return null;
		}
	}

	/**
	 * Returns a new {@link RequestKey} with attributes matching to the current request or {@code null}.
	 * @see RequestKey#getMatchingKey(HttpServletRequest, PathMatcher, UrlPathHelper)
	 */
	@Override
	protected RequestKey getMatchingKey(RequestKey key, HttpServletRequest request) {
		return key.getMatchingKey(request, pathMatcher, urlPathHelper);
	}

	/**
	 * Returns a {@link Comparator} that can be used to sort and select the best matching {@link RequestKey}.
	 */
	@Override
	protected Comparator<RequestKey> getKeyComparator(HttpServletRequest request) {
		return new RequestKeyComparator(request);
	}

	@Override
	protected void handleMatch(RequestKey key, HttpServletRequest request) {
		String pattern = key.getPatterns().iterator().next();
		String lookupPath = urlPathHelper.getLookupPathForRequest(request);
		Map<String, String> uriTemplateVariables = pathMatcher.extractUriTemplateVariables(pattern, lookupPath);
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVariables);
	}

	/**
	 * Iterates all {@link RequestKey}s looking for keys that match by URL but not by HTTP method.
	 * @exception HttpRequestMethodNotSupportedException if there are matches by URL but not by HTTP method
	 */
	@Override
	protected HandlerMethod handleNoMatch(Set<RequestKey> requestKeys, HttpServletRequest request)
			throws HttpRequestMethodNotSupportedException {
		String lookupPath = urlPathHelper.getLookupPathForRequest(request);
		Set<String> allowedMethods = new HashSet<String>(6);
		for (RequestKey requestKey : requestKeys) {
			for (String pattern : requestKey.getPatterns()) {
				if (pathMatcher.match(pattern, lookupPath)) {
					for (RequestMethod method : requestKey.getMethods()) {
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
			String lookupPath = urlPathHelper.getLookupPathForRequest(request);
			HandlerInterceptor[] handlerInterceptors = mappedInterceptors.getInterceptors(lookupPath, pathMatcher);
			if (handlerInterceptors.length > 0) {
				chain.addInterceptors(handlerInterceptors);
			}
		}
		return chain;
	}

	/**
	 * A comparator for {@link RequestKey}s. Effective comparison can only be done in the context of a 
	 * specific request. For example not all {@link RequestKey} patterns may apply to the current request. 
	 * Therefore an HttpServletRequest is required as input.
	 *
	 * <p>Furthermore, the following assumptions are made about the input RequestKeys: 
	 * <ul><li>Each RequestKey has been fully matched to the request <li>The RequestKey contains matched 
	 * patterns only <li>Patterns are ordered with the best matching pattern at the top </ul>
	 *
	 * @see RequestMappingHandlerMethodMapping#getMatchingKey(RequestKey, HttpServletRequest)
	 */
	private class RequestKeyComparator implements Comparator<RequestKey> {

		private Comparator<String> patternComparator;

		private List<MediaType> requestAcceptHeader;

		public RequestKeyComparator(HttpServletRequest request) {
			String lookupPath = urlPathHelper.getLookupPathForRequest(request);
			this.patternComparator = pathMatcher.getPatternComparator(lookupPath);
			String acceptHeader = request.getHeader("Accept");
			this.requestAcceptHeader = MediaType.parseMediaTypes(acceptHeader);
			MediaType.sortByQualityValue(this.requestAcceptHeader);
		}

		public int compare(RequestKey key, RequestKey otherKey) {
			int result = comparePatterns(key.getPatterns(), otherKey.getPatterns());
			if (result != 0) {
				return result;
			}
			result = key.getParams().compareTo(otherKey.getParams());
			if (result != 0) {
				return result;
			}
			result = key.getHeaders().compareTo(otherKey.getHeaders());
			if (result != 0) {
				return result;
			}
/*
			TODO: fix
			result = compareAcceptHeaders(key.getAcceptHeaderMediaTypes(), otherKey.getAcceptHeaderMediaTypes());
			if (result != 0) {
				return result;
			}
*/
			result = otherKey.getMethods().size() - key.getMethods().size();
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
