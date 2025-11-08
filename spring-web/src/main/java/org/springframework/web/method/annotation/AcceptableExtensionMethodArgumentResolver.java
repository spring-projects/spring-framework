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

package org.springframework.web.method.annotation;

import java.util.Arrays;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.AcceptableExtension;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.MultipartResolutionDelegate;

/**
 * Resolves method arguments annotated with @AcceptableExtension and validates
 * file extensions for MultipartFile parameters.
 *
 * @author Aleksei Iakhnenko
 * @since 7.0
 * @see AcceptableExtension
 */
public class AcceptableExtensionMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(AcceptableExtension.class);
	}

	@Override
	@Nullable
	public Object resolveArgument(
			MethodParameter parameter,
			@Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest,
			@Nullable WebDataBinderFactory binderFactory) throws Exception {

		AcceptableExtension annotation = parameter.getParameterAnnotation(AcceptableExtension.class);
		if (annotation == null) {
			return null;
		}

		HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
		if (servletRequest == null) {
			return null;
		}

		String paramName = getParameterName(parameter);
		if (paramName == null) {
			return null;
		}

		Object resolvedArgument = MultipartResolutionDelegate.resolveMultipartArgument(
				paramName, parameter, servletRequest);

		MultipartFile file = (resolvedArgument instanceof MultipartFile) ?
				(MultipartFile) resolvedArgument :
				null;

		if (file != null && !file.isEmpty()) {
			String filename = file.getOriginalFilename();
			if (StringUtils.hasText(filename)) {
				String extension = StringUtils.getFilenameExtension(filename);
				if (extension != null && !isAcceptableExtension(extension, annotation.extensions())) {
					throw new MultipartException(annotation.message() +
							". Allowed: " + Arrays.toString(annotation.extensions()) +
							", received: " + extension);
				}
			}
		}

		return file;
	}

	/**
	 * Determine the name for the given method parameter.
	 * @param parameter the method parameter
	 * @return the parameter name, or {@code null} if not resolvable
	 */
	@Nullable
	private String getParameterName(MethodParameter parameter) {
		org.springframework.web.bind.annotation.RequestParam requestParam =
				parameter.getParameterAnnotation(org.springframework.web.bind.annotation.RequestParam.class);

		if (requestParam != null) {
			String paramName = requestParam.value();
			if (StringUtils.hasText(paramName)) {
				return paramName;
			}
			paramName = requestParam.name();
			if (StringUtils.hasText(paramName)) {
				return paramName;
			}
		}

		// Fallback to actual parameter name if available
		return parameter.getParameterName();
	}

	private boolean isAcceptableExtension(String extension, String[] acceptableExtensions) {
		if (acceptableExtensions.length == 0) {
			return true;
		}
		for (String acceptable : acceptableExtensions) {
			if (acceptable.equalsIgnoreCase(extension)) {
				return true;
			}
		}
		return false;
	}

}
