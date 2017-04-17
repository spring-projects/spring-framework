/*
 * Copyright 2002-2017 the original author or authors.
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

import java.beans.ConstructorProperties;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

import org.springframework.beans.BeanUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
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

	private static final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

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
		ReactiveAdapter adapter = getAdapterRegistry().getAdapter(type.resolve());
		ResolvableType valueType = (adapter != null ? type.getGeneric(0) : type);

		Assert.state(adapter == null || !adapter.isMultiValue(),
				() -> getClass().getSimpleName() + " doesn't support multi-value reactive type wrapper: " +
						parameter.getGenericParameterType());

		String name = getAttributeName(valueType, parameter);
		Mono<?> valueMono = getAttributeMono(name, valueType, context, exchange);

		Map<String, Object> model = context.getModel().asMap();
		MonoProcessor<BindingResult> bindingResultMono = MonoProcessor.create();
		model.put(BindingResult.MODEL_KEY_PREFIX + name, bindingResultMono);

		return valueMono.flatMap(value -> {
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
						if (adapter != null) {
							return adapter.fromPublisher(errors.hasErrors() ?
									Mono.error(new WebExchangeBindException(parameter, errors)) :
									valueMono);
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

	private String getAttributeName(ResolvableType valueType, MethodParameter parameter) {
		ModelAttribute ann = parameter.getParameterAnnotation(ModelAttribute.class);
		if (ann != null && StringUtils.hasText(ann.value())) {
			return ann.value();
		}
		// TODO: Conventions does not deal with async wrappers
		return ClassUtils.getShortNameAsProperty(valueType.getRawClass());
	}

	private Mono<?> getAttributeMono(
			String attributeName, ResolvableType attributeType, BindingContext context, ServerWebExchange exchange) {

		Object attribute = context.getModel().asMap().get(attributeName);
		if (attribute == null) {
			return createAttribute(attributeName, attributeType.getRawClass(), context, exchange);
		}

		ReactiveAdapter adapterFrom = getAdapterRegistry().getAdapter(null, attribute);
		if (adapterFrom != null) {
			Assert.isTrue(!adapterFrom.isMultiValue(), "Data binding only supports single-value async types");
			return Mono.from(adapterFrom.toPublisher(attribute));
		}
		else {
			return Mono.justOrEmpty(attribute);
		}
	}

	private Mono<?> createAttribute(
			String attributeName, Class<?> attributeType, BindingContext context, ServerWebExchange exchange) {

		Constructor<?>[] ctors = attributeType.getConstructors();
		if (ctors.length != 1) {
			// No standard data class or standard JavaBeans arrangement ->
			// defensively go with default constructor, expecting regular bean property bindings.
			return Mono.just(BeanUtils.instantiateClass(attributeType));
		}
		Constructor<?> ctor = ctors[0];
		if (ctor.getParameterCount() == 0) {
			// A single default constructor -> clearly a standard JavaBeans arrangement.
			return Mono.just(BeanUtils.instantiateClass(ctor));
		}

		// A single data class constructor -> resolve constructor arguments from request parameters.
		return exchange.getRequestParams().flatMap(requestParams -> {
			ConstructorProperties cp = ctor.getAnnotation(ConstructorProperties.class);
			String[] paramNames = (cp != null ? cp.value() : parameterNameDiscoverer.getParameterNames(ctor));
			Assert.state(paramNames != null, () -> "Cannot resolve parameter names for constructor " + ctor);
			Class<?>[] paramTypes = ctor.getParameterTypes();
			Assert.state(paramNames.length == paramTypes.length,
					() -> "Invalid number of parameter names: " + paramNames.length + " for constructor " + ctor);
			Object[] args = new Object[paramTypes.length];
			WebDataBinder binder = context.createDataBinder(exchange, null, attributeName);
			for (int i = 0; i < paramNames.length; i++) {
				List<String> paramValues = requestParams.get(paramNames[i]);
				Object paramValue = null;
				if (paramValues != null) {
					paramValue = (paramValues.size() == 1 ? paramValues.get(0) :
							paramValues.toArray(new String[paramValues.size()]));
				}
				args[i] = binder.convertIfNecessary(paramValue, paramTypes[i], new MethodParameter(ctor, i));
			}
			return Mono.fromSupplier(() -> BeanUtils.instantiateClass(ctor, args));
		});
	}

	private boolean hasErrorsArgument(MethodParameter parameter) {
		int i = parameter.getParameterIndex();
		Class<?>[] paramTypes = parameter.getMethod().getParameterTypes();
		return (paramTypes.length > i && Errors.class.isAssignableFrom(paramTypes[i + 1]));
	}

	private void validateIfApplicable(WebExchangeDataBinder binder, MethodParameter parameter) {
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
