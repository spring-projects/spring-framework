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
import java.util.Map;

import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

import org.springframework.beans.BeanUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapter.Descriptor;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebExchangeBindException;
import org.springframework.web.bind.WebExchangeDataBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * Resolve {@code @ModelAttribute} annotated method arguments.
 *
 * <p>Model attributes are sourced from the model, or created using a default
 * constructor and then added to the model. Once created the attribute is
 * populated via data binding to the request (form data, query params).
 * Validation also may be applied if the argument is annotated with
 * {@code @javax.validation.Valid} or Spring's own
 * {@code @org.springframework.validation.annotation.Validated}.
 *
 * <p>When this handler is created with {@code useDefaultResolution=true}
 * any non-simple type argument and return value is regarded as a model
 * attribute with or without the presence of an {@code @ModelAttribute}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ModelAttributeMethodArgumentResolver implements HandlerMethodArgumentResolver {

	private final ReactiveAdapterRegistry adapterRegistry;

	private final boolean useDefaultResolution;


	/**
	 * Class constructor.
	 * @param registry for adapting to other reactive types from and to Mono
	 */
	public ModelAttributeMethodArgumentResolver(ReactiveAdapterRegistry registry) {
		this(registry, false);
	}

	/**
	 * Class constructor with a default resolution mode flag.
	 * @param registry for adapting to other reactive types from and to Mono
	 * @param useDefaultResolution if "true", non-simple method arguments and
	 * return values are considered model attributes with or without a
	 * {@code @ModelAttribute} annotation present.
	 */
	public ModelAttributeMethodArgumentResolver(ReactiveAdapterRegistry registry,
			boolean useDefaultResolution) {

		Assert.notNull(registry, "'ReactiveAdapterRegistry' is required.");
		this.useDefaultResolution = useDefaultResolution;
		this.adapterRegistry = registry;
	}


	/**
	 * Return the configured {@link ReactiveAdapterRegistry}.
	 */
	public ReactiveAdapterRegistry getAdapterRegistry() {
		return this.adapterRegistry;
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		if (parameter.hasParameterAnnotation(ModelAttribute.class)) {
			return true;
		}
		if (this.useDefaultResolution) {
			Class<?> clazz = parameter.getParameterType();
			ReactiveAdapter adapter = getAdapterRegistry().getAdapterFrom(clazz);
			if (adapter != null) {
				Descriptor descriptor = adapter.getDescriptor();
				if (descriptor.isNoValue() || descriptor.isMultiValue()) {
					return false;
				}
				clazz = ResolvableType.forMethodParameter(parameter).getGeneric(0).getRawClass();
			}
			return !BeanUtils.isSimpleProperty(clazz);
		}
		return false;
	}

	@Override
	public Mono<Object> resolveArgument(MethodParameter parameter, BindingContext context,
			ServerWebExchange exchange) {

		ResolvableType type = ResolvableType.forMethodParameter(parameter);
		ReactiveAdapter adapterTo = getAdapterRegistry().getAdapterTo(type.resolve());
		Class<?> valueType = (adapterTo != null ? type.resolveGeneric(0) : parameter.getParameterType());
		String name = getAttributeName(valueType, parameter);
		Mono<?> valueMono = getAttributeMono(name, valueType, parameter, context, exchange);

		Map<String, Object> model = context.getModel().asMap();
		MonoProcessor<BindingResult> bindingResultMono = MonoProcessor.create();
		model.put(BindingResult.MODEL_KEY_PREFIX + name, bindingResultMono);

		return valueMono.then(value -> {
			WebExchangeDataBinder binder = context.createDataBinder(exchange, value, name);
			return binder.bind(exchange)
					.doOnError(bindingResultMono::onError)
					.doOnSuccess(aVoid -> {
						validateIfApplicable(binder, parameter);
						BindingResult errors = binder.getBindingResult();
						model.put(BindingResult.MODEL_KEY_PREFIX + name, errors);
						model.put(name, value);
						bindingResultMono.onNext(errors);
					})
					.then(Mono.fromCallable(() -> {
						BindingResult errors = binder.getBindingResult();
						if (adapterTo != null) {
							return adapterTo.fromPublisher(errors.hasErrors() ?
									Mono.error(new WebExchangeBindException(parameter, errors)) :
									Mono.just(value));
						}
						else {
							if (errors.hasErrors() && checkErrorsArgument(parameter)) {
								throw new WebExchangeBindException(parameter, errors);
							}
							return value;
						}
					}));
		});
	}

	private String getAttributeName(Class<?> valueType, MethodParameter parameter) {
		ModelAttribute annot = parameter.getParameterAnnotation(ModelAttribute.class);
		if (annot != null && StringUtils.hasText(annot.value())) {
			return annot.value();
		}
		// TODO: Conventions does not deal with async wrappers
		return ClassUtils.getShortNameAsProperty(valueType);
	}

	private Mono<?> getAttributeMono(String attributeName, Class<?> attributeType,
			MethodParameter param, BindingContext context, ServerWebExchange exchange) {

		Object attribute = context.getModel().asMap().get(attributeName);
		if (attribute == null) {
			attribute = createAttribute(attributeName, attributeType, param, context, exchange);
		}
		if (attribute != null) {
			ReactiveAdapter adapterFrom = getAdapterRegistry().getAdapterFrom(null, attribute);
			if (adapterFrom != null) {
				return adapterFrom.toMono(attribute);
			}
		}
		return Mono.justOrEmpty(attribute);
	}

	protected Object createAttribute(String attributeName, Class<?> attributeType,
			MethodParameter parameter, BindingContext context, ServerWebExchange exchange) {

		return BeanUtils.instantiateClass(attributeType);
	}

	protected boolean checkErrorsArgument(MethodParameter methodParam) {
		int i = methodParam.getParameterIndex();
		Class<?>[] paramTypes = methodParam.getMethod().getParameterTypes();
		return paramTypes.length <= (i + 1) || !Errors.class.isAssignableFrom(paramTypes[i + 1]);
	}

	protected void validateIfApplicable(WebExchangeDataBinder binder, MethodParameter parameter) {
		Annotation[] annotations = parameter.getParameterAnnotations();
		for (Annotation ann : annotations) {
			Validated validAnnot = AnnotationUtils.getAnnotation(ann, Validated.class);
			if (validAnnot != null || ann.annotationType().getSimpleName().startsWith("Valid")) {
				Object hints = (validAnnot != null ? validAnnot.value() : AnnotationUtils.getValue(ann));
				Object hintArray = (hints instanceof Object[] ? (Object[]) hints : new Object[] {hints});
				binder.validate(hintArray);
			}
		}
	}

}
