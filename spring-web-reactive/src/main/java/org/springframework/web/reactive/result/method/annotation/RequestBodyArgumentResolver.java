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

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.Conventions;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.reactive.HttpMessageConverter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

/**
 * Resolves method arguments annotated with {@code @RequestBody} by reading and
 * decoding the body of the request through a compatible
 * {@code HttpMessageConverter}.
 *
 * @author Sebastien Deleuze
 * @author Stephane Maldini
 * @author Rossen Stoyanchev
 */
public class RequestBodyArgumentResolver implements HandlerMethodArgumentResolver {

	private static final TypeDescriptor MONO_TYPE = TypeDescriptor.valueOf(Mono.class);

	private static final TypeDescriptor FLUX_TYPE = TypeDescriptor.valueOf(Flux.class);


	private final List<HttpMessageConverter<?>> messageConverters;

	private final ConversionService conversionService;

	private final Validator validator;

	private final List<MediaType> supportedMediaTypes;


	/**
	 * Constructor with message converters and a ConversionService.
	 * @param converters converters for reading the request body with
	 * @param service for converting to other reactive types from Flux and Mono
	 */
	public RequestBodyArgumentResolver(List<HttpMessageConverter<?>> converters,
			ConversionService service) {

		this(converters, service, null);
	}

	/**
	 * Constructor with message converters and a ConversionService.
	 * @param converters converters for reading the request body with
	 * @param service for converting to other reactive types from Flux and Mono
	 * @param validator validator to validate decoded objects with
	 */
	public RequestBodyArgumentResolver(List<HttpMessageConverter<?>> converters,
			ConversionService service, Validator validator) {

		Assert.notEmpty(converters, "At least one message converter is required.");
		Assert.notNull(service, "'conversionService' is required.");
		this.messageConverters = converters;
		this.conversionService = service;
		this.validator = validator;
		this.supportedMediaTypes = converters.stream()
				.flatMap(converter -> converter.getReadableMediaTypes().stream())
				.collect(Collectors.toList());
	}


	/**
	 * Return the configured message converters.
	 */
	public List<HttpMessageConverter<?>> getMessageConverters() {
		return this.messageConverters;
	}

	/**
	 * Return the configured {@link ConversionService}.
	 */
	public ConversionService getConversionService() {
		return this.conversionService;
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(RequestBody.class);
	}

	@Override
	public Mono<Object> resolveArgument(MethodParameter parameter, ModelMap model, ServerWebExchange exchange) {

		TypeDescriptor typeDescriptor = new TypeDescriptor(parameter);
		boolean convertFromMono = getConversionService().canConvert(MONO_TYPE, typeDescriptor);
		boolean convertFromFlux = getConversionService().canConvert(FLUX_TYPE, typeDescriptor);

		ResolvableType type = ResolvableType.forMethodParameter(parameter);
		ResolvableType elementType = convertFromMono || convertFromFlux ? type.getGeneric(0) : type;

		ServerHttpRequest request = exchange.getRequest();
		MediaType mediaType = request.getHeaders().getContentType();
		if (mediaType == null) {
			mediaType = MediaType.APPLICATION_OCTET_STREAM;
		}

		for (HttpMessageConverter<?> converter : getMessageConverters()) {
			if (converter.canRead(elementType, mediaType)) {
				if (convertFromFlux) {
					Flux<?> flux = converter.read(elementType, request);
					if (this.validator != null) {
						flux = flux.map(applyValidationIfApplicable(parameter));
					}
					return Mono.just(getConversionService().convert(flux, FLUX_TYPE, typeDescriptor));
				}
				else {
					Mono<?> mono = converter.readMono(elementType, request);
					if (this.validator != null) {
						mono = mono.map(applyValidationIfApplicable(parameter));
					}
					if (convertFromMono) {
						return Mono.just(getConversionService().convert(mono, MONO_TYPE, typeDescriptor));
					}
					else {
						return Mono.from(mono);
					}
				}
			}
		}

		return Mono.error(new UnsupportedMediaTypeStatusException(mediaType, this.supportedMediaTypes));
	}

	protected <T> Function<T, T> applyValidationIfApplicable(MethodParameter methodParam) {
		Annotation[] annotations = methodParam.getParameterAnnotations();
		for (Annotation ann : annotations) {
			Validated validAnnot = AnnotationUtils.getAnnotation(ann, Validated.class);
			if (validAnnot != null || ann.annotationType().getSimpleName().startsWith("Valid")) {
				Object hints = (validAnnot != null ? validAnnot.value() : AnnotationUtils.getValue(ann));
				Object[] validHints = (hints instanceof Object[] ? (Object[]) hints : new Object[] {hints});
				return element -> {
					doValidate(element, validHints, methodParam);
					return element;
				};
			}
		}
		return element -> element;
	}

	/**
	 * TODO: replace with use of DataBinder
	 */
	private void doValidate(Object target, Object[] validationHints, MethodParameter methodParam) {
		String name = Conventions.getVariableNameForParameter(methodParam);
		Errors errors = new BeanPropertyBindingResult(target, name);
		if (!ObjectUtils.isEmpty(validationHints) && this.validator instanceof SmartValidator) {
			((SmartValidator) this.validator).validate(target, errors, validationHints);
		}
		else if (this.validator != null) {
			this.validator.validate(target, errors);
		}
		if (errors.hasErrors()) {
			throw new ServerWebInputException("Validation failed", methodParam);
		}
	}

}
