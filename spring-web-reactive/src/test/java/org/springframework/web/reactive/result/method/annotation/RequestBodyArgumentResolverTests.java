/*
 * Copyright 2002-2016 the original author or authors.
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
package org.springframework.web.reactive.result.method.annotation;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.convert.support.MonoToCompletableFutureConverter;
import org.springframework.core.convert.support.ReactorToRxJava1Converter;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.converter.reactive.CodecHttpMessageConverter;
import org.springframework.http.converter.reactive.HttpMessageConverter;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.result.ResolvableMethod;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.core.ResolvableType.forClassWithGenerics;

/**
 * Unit tests for {@link RequestBodyArgumentResolver}.When adding a test also
 * consider whether the logic under test is in a parent class, then see:
 * {@link MessageConverterArgumentResolverTests}.
 *
 * @author Rossen Stoyanchev
 */
public class RequestBodyArgumentResolverTests {


	@Test
	public void supports() throws Exception {

		ResolvableMethod testMethod = ResolvableMethod.on(getClass()).name("handle");
		RequestBodyArgumentResolver resolver = resolver();

		ResolvableType type = forClassWithGenerics(Mono.class, String.class);
		MethodParameter param = testMethod.resolveParam(type);
		assertTrue(resolver.supportsParameter(param));

		MethodParameter parameter = testMethod.resolveParam(p -> !p.hasParameterAnnotations());
		assertFalse(resolver.supportsParameter(parameter));
	}

	private RequestBodyArgumentResolver resolver() {
		List<HttpMessageConverter<?>> converters = new ArrayList<>();
		converters.add(new CodecHttpMessageConverter<>(new StringDecoder()));

		FormattingConversionService service = new DefaultFormattingConversionService();
		service.addConverter(new MonoToCompletableFutureConverter());
		service.addConverter(new ReactorToRxJava1Converter());

		return new RequestBodyArgumentResolver(converters, service);
	}


	@SuppressWarnings("unused")
	void handle(@RequestBody Mono<String> monoString, String paramWithoutAnnotation) {}

}
