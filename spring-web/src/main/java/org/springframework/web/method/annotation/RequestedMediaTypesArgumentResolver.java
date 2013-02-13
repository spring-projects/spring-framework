/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.method.annotation;

import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;


/**
 * Resolves {@link Collection} or {@link List} of {@link MediaType}s method arguments.
 * 
 * Delegates the resolution of requested media types to a {@link ContentNegotiationStrategy}.
 * The commonly used implementation is {@link ContentNegociationManager}.
 * 
 * @author Arnaud Cogoluègnes
 * @since 3.2
 */
public class RequestedMediaTypesArgumentResolver implements HandlerMethodArgumentResolver {
	
	private final ContentNegotiationStrategy contentNegotiationStrategy;
	
	/**
	 * @param contentNegotiationStrategy
	 */
	public RequestedMediaTypesArgumentResolver(
			ContentNegotiationStrategy contentNegotiationStrategy) {
		super();
		this.contentNegotiationStrategy = contentNegotiationStrategy;
	}

	/* (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#supportsParameter(org.springframework.core.MethodParameter)
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return isCollectionOfMediaTypes(parameter);
	}

	/* (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#resolveArgument(org.springframework.core.MethodParameter, org.springframework.web.method.support.ModelAndViewContainer, org.springframework.web.context.request.NativeWebRequest, org.springframework.web.bind.support.WebDataBinderFactory)
	 */
	@Override
	public Object resolveArgument(MethodParameter parameter,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest,
			WebDataBinderFactory binderFactory) throws Exception {
		List<MediaType> mediaTypes = this.contentNegotiationStrategy.resolveMediaTypes(webRequest);
		return mediaTypes.isEmpty() ? Collections.singletonList(MediaType.ALL) : mediaTypes;
	}

	private boolean isCollectionOfMediaTypes(MethodParameter parameter) {
		return isCollection(parameter) && isMediaTypeParameterized(parameter);
	}
	
	private boolean isCollection(MethodParameter parameter) {
		return Collection.class.isAssignableFrom(parameter.getParameterType());
	}
	
	private boolean isMediaTypeParameterized(MethodParameter parameter) {
		if(parameter.getGenericParameterType() instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) parameter.getGenericParameterType();
			if(parameterizedType.getActualTypeArguments().length > 0) {
				if(parameterizedType.getActualTypeArguments()[0] instanceof Class) {
					Class<?> genericTypeOfTheCollection = (Class<?> )parameterizedType.getActualTypeArguments()[0];
					return MediaType.class.isAssignableFrom(genericTypeOfTheCollection);
				}
			}
		}
		return false;
	}

}
