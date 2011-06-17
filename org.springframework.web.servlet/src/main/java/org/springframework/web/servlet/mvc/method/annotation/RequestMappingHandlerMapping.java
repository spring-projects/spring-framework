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

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.PathMatcher;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import org.springframework.web.servlet.mvc.method.condition.ConsumesRequestCondition;
import org.springframework.web.servlet.mvc.method.condition.HeadersRequestCondition;
import org.springframework.web.servlet.mvc.method.condition.ParamsRequestCondition;
import org.springframework.web.servlet.mvc.method.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.condition.ProducesRequestCondition;
import org.springframework.web.servlet.mvc.method.condition.RequestMethodsRequestCondition;

/**
 * A sub-class of {@link RequestMappingInfoHandlerMapping} that prepares {@link RequestMappingInfo}s 
 * from @{@link RequestMapping} annotations on @{@link Controller} classes.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1.0
 */
public class RequestMappingHandlerMapping extends RequestMappingInfoHandlerMapping {

	/**
	 * {@inheritDoc} The handler determination in this method is made based on the presence of a type-level {@link
	 * Controller} annotation.
	 */
	@Override
	protected boolean isHandler(Class<?> beanType) {
		return AnnotationUtils.findAnnotation(beanType, Controller.class) != null;
	}

	/**
	 * Provides a {@link RequestMappingInfo} for the given method. <p>Only {@link RequestMapping @RequestMapping}-annotated
	 * methods are considered. Type-level {@link RequestMapping @RequestMapping} annotations are also detected and their
	 * attributes combined with method-level {@link RequestMapping @RequestMapping} attributes.
	 *
	 * @param method the method to create a mapping for
	 * @param handlerType the actual handler type, possibly a sub-type of {@code method.getDeclaringClass()}
	 * @return the mapping, or {@code null}
	 * @see RequestMappingInfo#combine(RequestMappingInfo, PathMatcher)
	 */
	@Override
	protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
		RequestMapping annotation = AnnotationUtils.findAnnotation(method, RequestMapping.class);
		if (annotation != null) {
			RequestMappingInfo methodMapping = createFromRequestMapping(annotation);
			RequestMapping typeAnnot = AnnotationUtils.findAnnotation(handlerType, RequestMapping.class);
			if (typeAnnot != null) {
				RequestMappingInfo typeMapping = createFromRequestMapping(typeAnnot);
				return typeMapping.combine(methodMapping);
			}
			else {
				return methodMapping;
			}
		}
		else {
			return null;
		}
	}

	private RequestMappingInfo createFromRequestMapping(RequestMapping annotation) {
		return new RequestMappingInfo(
				new PatternsRequestCondition(annotation.value(), getUrlPathHelper(), getPathMatcher()),
				new RequestMethodsRequestCondition(annotation.method()),
				new ParamsRequestCondition(annotation.params()),
				new HeadersRequestCondition(annotation.headers()),
				new ConsumesRequestCondition(annotation.consumes(), annotation.headers()),
				new ProducesRequestCondition(annotation.produces(), annotation.headers()));
	}

}
