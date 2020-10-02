/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.reactive.result.method.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.core.publisher.Sinks;

import org.springframework.beans.BeanUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.support.WebExchangeDataBinder;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport;
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
 * @author Juergen Hoeller
 * @since 5.0
 */
public class ModelAttributeMethodArgumentResolver extends HandlerMethodArgumentResolverSupport {

	private final boolean useDefaultResolution;


	/**
	 * Class constructor with a default resolution mode flag.
	 * @param adapterRegistry for adapting to other reactive types from and to Mono
	 * @param useDefaultResolution if "true", non-simple method arguments and
	 * return values are considered model attributes with or without a
	 * {@code @ModelAttribute} annotation present.
	 */
	public ModelAttributeMethodArgumentResolver(ReactiveAdapterRegistry adapterRegistry,
			boolean useDefaultResolution) {

		super(adapterRegistry);
		this.useDefaultResolution = useDefaultResolution;
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		if (parameter.hasParameterAnnotation(ModelAttribute.class)) {
			return true;
		}
		else if (this.useDefaultResolution) {
			return checkParameterType(parameter, type -> !BeanUtils.isSimpleProperty(type));
		}
		return false;
	}

	@Override
	public Mono<Object> resolveArgument(
			MethodParameter parameter, BindingContext context, ServerWebExchange exchange) {

		ResolvableType type = ResolvableType.forMethodParameter(parameter);
		Class<?> resolvedType = type.resolve();
		ReactiveAdapter adapter = (resolvedType != null ? getAdapterRegistry().getAdapter(resolvedType) : null);
		ResolvableType valueType = (adapter != null ? type.getGeneric() : type);

		Assert.state(adapter == null || !adapter.isMultiValue(),
				() -> getClass().getSimpleName() + " does not support multi-value reactive type wrapper: " +
						parameter.getGenericParameterType());

		String name = ModelInitializer.getNameForParameter(parameter);
		Mono<?> valueMono = prepareAttributeMono(name, valueType, context, exchange);

		Map<String, Object> model = context.getModel().asMap();
		MonoProcessor<BindingResult> bindingResultMono = MonoProcessor.fromSink(Sinks.one());
		model.put(BindingResult.MODEL_KEY_PREFIX + name, bindingResultMono);

		return valueMono.flatMap(value -> {
			WebExchangeDataBinder binder = context.createDataBinder(exchange, value, name);
			return bindRequestParameters(binder, exchange)
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
						if (adapter != null) {
							return adapter.fromPublisher(errors.hasErrors() ?
									Mono.error(new WebExchangeBindException(parameter, errors)) : valueMono);
						}
						else {
							if (errors.hasErrors() && !hasErrorsArgument(parameter)) {
								throw new WebExchangeBindException(parameter, errors);
							}
							return value;
						}
					}));
		});
	}

	/**
	 * Extension point to bind the request to the target object.
	 * @param binder the data binder instance to use for the binding
	 * @param exchange the current request
	 * @since 5.2.6
	 */
	protected Mono<Void> bindRequestParameters(WebExchangeDataBinder binder, ServerWebExchange exchange) {
		return binder.bind(exchange);
	}

	private Mono<?> prepareAttributeMono(String attributeName, ResolvableType attributeType,
			BindingContext context, ServerWebExchange exchange) {

		Object attribute = context.getModel().asMap().get(attributeName);

		if (attribute == null) {
			attribute = findAndRemoveReactiveAttribute(context.getModel(), attributeName);
		}

		if (attribute == null) {
			return createAttribute(attributeName, attributeType.toClass(), context, exchange);
		}

		ReactiveAdapter adapter = getAdapterRegistry().getAdapter(null, attribute);
		if (adapter != null) {
			Assert.isTrue(!adapter.isMultiValue(), "Data binding only supports single-value async types");
			return Mono.from(adapter.toPublisher(attribute));
		}
		else {
			return Mono.justOrEmpty(attribute);
		}
	}

	@Nullable
	private Object findAndRemoveReactiveAttribute(Model model, String attributeName) {
		return model.asMap().entrySet().stream()
				.filter(entry -> {
					if (!entry.getKey().startsWith(attributeName)) {
						return false;
					}
					ReactiveAdapter adapter = getAdapterRegistry().getAdapter(null, entry.getValue());
					if (adapter == null) {
						return false;
					}
					String name = attributeName + ClassUtils.getShortName(adapter.getReactiveType());
					return entry.getKey().equals(name);
				})
				.findFirst()
				.map(entry -> {
					// Remove since we will be re-inserting the resolved attribute value
					model.asMap().remove(entry.getKey());
					return entry.getValue();
				})
				.orElse(null);
	}

	private Mono<?> createAttribute(
			String attributeName, Class<?> clazz, BindingContext context, ServerWebExchange exchange) {

		Constructor<?> ctor = BeanUtils.getResolvableConstructor(clazz);
		return constructAttribute(ctor, attributeName, context, exchange);
	}

	private Mono<?> constructAttribute(Constructor<?> ctor, String attributeName,
			BindingContext context, ServerWebExchange exchange) {

		if (ctor.getParameterCount() == 0) {
			// A single default constructor -> clearly a standard JavaBeans arrangement.
			return Mono.just(BeanUtils.instantiateClass(ctor));
		}

		// A single data class constructor -> resolve constructor arguments from request parameters.
		WebExchangeDataBinder binder = context.createDataBinder(exchange, null, attributeName);
		return getValuesToBind(binder, exchange).map(bindValues -> {
			String[] paramNames = BeanUtils.getParameterNames(ctor);
			Class<?>[] paramTypes = ctor.getParameterTypes();
			Object[] args = new Object[paramTypes.length];
			String fieldDefaultPrefix = binder.getFieldDefaultPrefix();
			String fieldMarkerPrefix = binder.getFieldMarkerPrefix();
			for (int i = 0; i < paramNames.length; i++) {
				String paramName = paramNames[i];
				Class<?> paramType = paramTypes[i];
				Object value = bindValues.get(paramName);
				if (value == null) {
					if (fieldDefaultPrefix != null) {
						value = bindValues.get(fieldDefaultPrefix + paramName);
					}
					if (value == null && fieldMarkerPrefix != null) {
						if (bindValues.get(fieldMarkerPrefix + paramName) != null) {
							value = binder.getEmptyValue(paramType);
						}
					}
				}
				value = (value instanceof List ? ((List<?>) value).toArray() : value);
				MethodParameter methodParam = new MethodParameter(ctor, i);
				if (value == null && methodParam.isOptional()) {
					args[i] = (methodParam.getParameterType() == Optional.class ? Optional.empty() : null);
				}
				else {
					args[i] = binder.convertIfNecessary(value, paramTypes[i], methodParam);
				}
			}
			return BeanUtils.instantiateClass(ctor, args);
		});
	}

	/**
	 * Protected method to obtain the values for data binding. By default this
	 * method delegates to {@link WebExchangeDataBinder#getValuesToBind}.
	 * @param binder the data binder in use
	 * @param exchange the current exchange
	 * @return a map of bind values
	 * @since 5.3
	 */
	public Mono<Map<String, Object>> getValuesToBind(WebExchangeDataBinder binder, ServerWebExchange exchange) {
		return binder.getValuesToBind(exchange);
	}

	private boolean hasErrorsArgument(MethodParameter parameter) {
		int i = parameter.getParameterIndex();
		Class<?>[] paramTypes = parameter.getExecutable().getParameterTypes();
		return (paramTypes.length > i + 1 && Errors.class.isAssignableFrom(paramTypes[i + 1]));
	}

	private void validateIfApplicable(WebExchangeDataBinder binder, MethodParameter parameter) {
		for (Annotation ann : parameter.getParameterAnnotations()) {
			Validated validatedAnn = AnnotationUtils.getAnnotation(ann, Validated.class);
			if (validatedAnn != null || ann.annotationType().getSimpleName().startsWith("Valid")) {
				Object hints = (validatedAnn != null ? validatedAnn.value() : AnnotationUtils.getValue(ann));
				if (hints != null) {
					Object[] validationHints = (hints instanceof Object[] ? (Object[]) hints : new Object[] {hints});
					binder.validate(validationHints);
				}
				else {
					binder.validate();
				}
			}
		}
	}

}
