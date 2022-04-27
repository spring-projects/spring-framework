/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import javax.servlet.ServletException;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.annotation.AbstractNamedValueMethodArgumentResolver;

/**
 * Resolves method arguments annotated with an @{@link RequestAttribute}.
 *
 * @author Rossen Stoyanchev
 * @since 4.3
 */
public class RequestAttributeMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(RequestAttribute.class);
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		RequestAttribute ann = parameter.getParameterAnnotation(RequestAttribute.class);
		Assert.state(ann != null, "No RequestAttribute annotation");
		return new NamedValueInfo(ann.name(), ann.required(), ValueConstants.DEFAULT_NONE);
	}

	@Override
	@Nullable
	protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest request){
		return request.getAttribute(name, RequestAttributes.SCOPE_REQUEST);
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) throws ServletException {
		throw new ServletRequestBindingException("Missing request attribute '" + name +
				"' of type " +  parameter.getNestedParameterType().getSimpleName());
	}

}
