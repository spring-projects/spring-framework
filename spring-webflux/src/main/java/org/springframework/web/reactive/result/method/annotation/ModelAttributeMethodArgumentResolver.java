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

package org.springframework.web.reactive.result.method.annotation;

import java.lang.annotation.Annotation;
import java.util.Map;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import org.springframework.beans.BeanUtils;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.ValidationAnnotationUtils;
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
 * {@code @jakarta.validation.Valid} or Spring's own
 * {@code @org.springframework.validation.annotation.Validated}.
 *
 * <p>When this handler is created with {@code useDefaultResolution=true}
 * any non-simple type argument and return value is regarded as a model
 * attribute with or without the presence of an {@code @ModelAttribute}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
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

		Class<?> resolvedType = parameter.getParameterType();
		ReactiveAdapter adapter = getAdapterRegistry().getAdapter(resolvedType);
		Assert.state(adapter == null || !adapter.isMultiValue(), "Multi-value publisher is not supported");

		String name = ModelInitializer.getNameForParameter(parameter);

		Mono<WebExchangeDataBinder> dataBinderMono = initDataBinder(
				name, (adapter != null ? parameter.nested() : parameter), context, exchange);

		// unsafe() is OK: source is Reactive Streams Publisher
		Sinks.One<BindingResult> bindingResultSink = Sinks.unsafe().one();

		Map<String, Object> model = context.getModel().asMap();
		model.put(BindingResult.MODEL_KEY_PREFIX + name, bindingResultSink.asMono());

		return dataBinderMono
				.flatMap(binder -> {
					Object attribute = binder.getTarget();
					Assert.state(attribute != null, "Expected model attribute instance");
					return (!bindingDisabled(parameter) ? bindRequestParameters(binder, exchange) : Mono.empty())
							.doOnError(bindingResultSink::tryEmitError)
							.doOnSuccess(aVoid -> {
								validateIfApplicable(binder, parameter, exchange);
								BindingResult bindingResult = binder.getBindingResult();
								model.put(BindingResult.MODEL_KEY_PREFIX + name, bindingResult);
								model.put(name, attribute);
								// Ignore result: serialized and buffered (should never fail)
								bindingResultSink.tryEmitValue(bindingResult);
							})
							.then(Mono.fromCallable(() -> {
								BindingResult errors = binder.getBindingResult();
								if (adapter != null) {
									Mono<Object> mono = (errors.hasErrors() ?
											Mono.error(new WebExchangeBindException(parameter, errors)) :
											Mono.just(attribute));
									return adapter.fromPublisher(mono);
								}
								else {
									if (errors.hasErrors() && !hasErrorsArgument(parameter)) {
										throw new WebExchangeBindException(parameter, errors);
									}
									return attribute;
								}
							}));
				});
	}

	private Mono<WebExchangeDataBinder> initDataBinder(
			String name, MethodParameter parameter, BindingContext context, ServerWebExchange exchange) {

		Object value = context.getModel().asMap().get(name);
		if (value == null) {
			value = removeReactiveAttribute(name, context.getModel());
		}
		ResolvableType type = ResolvableType.forMethodParameter(parameter);
		if (value != null) {
			ReactiveAdapter adapter = getAdapterRegistry().getAdapter(null, value);
			Assert.isTrue(adapter == null || !adapter.isMultiValue(), "Multi-value publisher is not supported");
			return (adapter != null ? Mono.from(adapter.toPublisher(value)) : Mono.just(value))
					.map(attr -> context.createDataBinder(exchange, attr, name, type));
		}
		else {
			WebExchangeDataBinder binder = context.createDataBinder(exchange, null, name, type);
			return constructAttribute(binder, exchange).thenReturn(binder);
		}
	}

	@Nullable
	private Object removeReactiveAttribute(String name, Model model) {
		for (Map.Entry<String, Object> entry : model.asMap().entrySet()) {
			if (entry.getKey().startsWith(name)) {
				ReactiveAdapter adapter = getAdapterRegistry().getAdapter(null, entry.getValue());
				if (adapter != null) {
					if (entry.getKey().equals(name + ClassUtils.getShortName(adapter.getReactiveType()))) {
						// Remove since we will be re-inserting the resolved attribute value
						model.asMap().remove(entry.getKey());
						return entry.getValue();
					}
				}
			}
		}
		return null;
	}

	/**
	 * Extension point to create the attribute, binding the request to constructor args.
	 * @param binder the data binder instance to use for the binding
	 * @param exchange the current exchange
	 * @since 6.1
	 */
	protected Mono<Void> constructAttribute(WebExchangeDataBinder binder, ServerWebExchange exchange) {
		return binder.construct(exchange);
	}

	/**
	 * Determine if binding should be disabled for the supplied {@link MethodParameter},
	 * based on the {@link ModelAttribute#binding} annotation attribute.
	 * @since 5.2.15
	 */
	private boolean bindingDisabled(MethodParameter parameter) {
		ModelAttribute modelAttribute = parameter.getParameterAnnotation(ModelAttribute.class);
		return (modelAttribute != null && !modelAttribute.binding());
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

	private boolean hasErrorsArgument(MethodParameter parameter) {
		int i = parameter.getParameterIndex();
		Class<?>[] paramTypes = parameter.getExecutable().getParameterTypes();
		return (paramTypes.length > i + 1 && Errors.class.isAssignableFrom(paramTypes[i + 1]));
	}

	private void validateIfApplicable(WebExchangeDataBinder binder, MethodParameter parameter, ServerWebExchange exchange) {
		LocaleContext localeContext = null;
		try {
			for (Annotation ann : parameter.getParameterAnnotations()) {
				Object[] validationHints = ValidationAnnotationUtils.determineValidationHints(ann);
				if (validationHints != null) {
					if (localeContext == null) {
						localeContext = exchange.getLocaleContext();
						LocaleContextHolder.setLocaleContext(localeContext);
					}
					binder.validate(validationHints);
				}
			}
		}
		finally {
			if (localeContext != null) {
				LocaleContextHolder.resetLocaleContext();
			}
		}
	}

}
