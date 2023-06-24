/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.service.invoker;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * {@link HttpRequestValuesResolver} implementation for resolving {@link HttpExchange} based annotations.
 * @author Freeman Lau
 * @since 6.1.0
 */
public class HttpExchangeRequestValuesResolver implements HttpRequestValuesResolver {

	@Nullable
	private final StringValueResolver embeddedValueResolver;

	public HttpExchangeRequestValuesResolver(
			@Nullable StringValueResolver embeddedValueResolver) {
		this.embeddedValueResolver = embeddedValueResolver;
	}

	@Override
	public boolean supports(Method method) {
		return AnnotatedElementUtils.hasAnnotation(method, HttpExchange.class);
	}

	@Override
	public HttpRequestValuesInitializer resolve(Method method, Class<?> serviceType) {
		HttpExchange annot1 = AnnotatedElementUtils.findMergedAnnotation(serviceType,
				HttpExchange.class);
		HttpExchange annot2 = AnnotatedElementUtils.findMergedAnnotation(method,
				HttpExchange.class);

		Assert.notNull(annot2, "Expected HttpRequest annotation");

		HttpMethod httpMethod = initHttpMethod(annot1, annot2);
		String url = initUrl(annot1, annot2, this.embeddedValueResolver);
		MediaType contentType = initContentType(annot1, annot2);
		List<MediaType> acceptableMediaTypes = initAccept(annot1, annot2);

		return new HttpRequestValuesInitializer(httpMethod, url, contentType,
				acceptableMediaTypes);
	}

	@Nullable
	private static HttpMethod initHttpMethod(@Nullable HttpExchange typeAnnot,
			HttpExchange annot) {

		String value1 = (typeAnnot != null ? typeAnnot.method() : null);
		String value2 = annot.method();

		if (StringUtils.hasText(value2)) {
			return HttpMethod.valueOf(value2);
		}

		if (StringUtils.hasText(value1)) {
			return HttpMethod.valueOf(value1);
		}

		return null;
	}

	@Nullable
	private static String initUrl(@Nullable HttpExchange typeAnnot, HttpExchange annot,
			@Nullable StringValueResolver embeddedValueResolver) {

		String url1 = (typeAnnot != null ? typeAnnot.url() : null);
		String url2 = annot.url();

		if (embeddedValueResolver != null) {
			url1 = (url1 != null ? embeddedValueResolver.resolveStringValue(url1) : null);
			url2 = embeddedValueResolver.resolveStringValue(url2);
		}

		boolean hasUrl1 = StringUtils.hasText(url1);
		boolean hasUrl2 = StringUtils.hasText(url2);

		if (hasUrl1 && hasUrl2) {
			return (url1 + (!url1.endsWith("/") && !url2.startsWith("/") ? "/" : "")
					+ url2);
		}

		if (!hasUrl1 && !hasUrl2) {
			return null;
		}

		return (hasUrl2 ? url2 : url1);
	}

	@Nullable
	private static MediaType initContentType(@Nullable HttpExchange typeAnnot,
			HttpExchange annot) {

		String value1 = (typeAnnot != null ? typeAnnot.contentType() : null);
		String value2 = annot.contentType();

		if (StringUtils.hasText(value2)) {
			return MediaType.parseMediaType(value2);
		}

		if (StringUtils.hasText(value1)) {
			return MediaType.parseMediaType(value1);
		}

		return null;
	}

	@Nullable
	private static List<MediaType> initAccept(@Nullable HttpExchange typeAnnot,
			HttpExchange annot) {

		String[] value1 = (typeAnnot != null ? typeAnnot.accept() : null);
		String[] value2 = annot.accept();

		if (!ObjectUtils.isEmpty(value2)) {
			return MediaType.parseMediaTypes(Arrays.asList(value2));
		}

		if (!ObjectUtils.isEmpty(value1)) {
			return MediaType.parseMediaTypes(Arrays.asList(value1));
		}

		return null;
	}
}
