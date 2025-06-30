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

package org.springframework.web.method.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import kotlin.Unit;
import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KClass;
import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.KType;
import kotlin.reflect.full.KClasses;
import kotlin.reflect.jvm.KCallablesJvm;
import kotlin.reflect.jvm.ReflectJvmMapping;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;

import org.springframework.context.MessageSource;
import org.springframework.core.CoroutinesUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.method.MethodValidator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;

/**
 * Extension of {@link HandlerMethod} that invokes the underlying method with
 * argument values resolved from the current HTTP request through a list of
 * {@link HandlerMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 3.1
 */
public class InvocableHandlerMethod extends HandlerMethod {

	private static final Object[] EMPTY_ARGS = new Object[0];

	private static final Class<?>[] EMPTY_GROUPS = new Class<?>[0];


	private HandlerMethodArgumentResolverComposite resolvers = new HandlerMethodArgumentResolverComposite();

	private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	private @Nullable WebDataBinderFactory dataBinderFactory;

	private @Nullable MethodValidator methodValidator;

	private Class<?>[] validationGroups = EMPTY_GROUPS;


	/**
	 * Create an instance from a {@code HandlerMethod}.
	 */
	public InvocableHandlerMethod(HandlerMethod handlerMethod) {
		super(handlerMethod);
	}

	/**
	 * Create an instance from a bean instance and a method.
	 */
	public InvocableHandlerMethod(Object bean, Method method) {
		super(bean, method);
	}

	/**
	 * Variant of {@link #InvocableHandlerMethod(Object, Method)} that
	 * also accepts a {@link MessageSource}, for use in subclasses.
	 * @since 5.3.10
	 */
	protected InvocableHandlerMethod(Object bean, Method method, @Nullable MessageSource messageSource) {
		super(bean, method, messageSource);
	}

	/**
	 * Construct a new handler method with the given bean instance, method name and parameters.
	 * @param bean the object bean
	 * @param methodName the method name
	 * @param parameterTypes the method parameter types
	 * @throws NoSuchMethodException when the method cannot be found
	 */
	public InvocableHandlerMethod(Object bean, String methodName, Class<?>... parameterTypes)
			throws NoSuchMethodException {

		super(bean, methodName, parameterTypes);
	}


	/**
	 * Set {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}
	 * to use for resolving method argument values.
	 */
	public void setHandlerMethodArgumentResolvers(HandlerMethodArgumentResolverComposite argumentResolvers) {
		this.resolvers = argumentResolvers;
	}

	/**
	 * Set the ParameterNameDiscoverer for resolving parameter names when needed
	 * (for example, default request attribute name).
	 * <p>Default is a {@link org.springframework.core.DefaultParameterNameDiscoverer}.
	 */
	public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}

	/**
	 * Set the {@link WebDataBinderFactory} to be passed to argument resolvers allowing them
	 * to create a {@link WebDataBinder} for data binding and type conversion purposes.
	 */
	public void setDataBinderFactory(WebDataBinderFactory dataBinderFactory) {
		this.dataBinderFactory = dataBinderFactory;
	}

	/**
	 * Set the {@link MethodValidator} to perform method validation with if the
	 * controller method {@link #shouldValidateArguments()} or
	 * {@link #shouldValidateReturnValue()}.
	 * @since 6.1
	 */
	public void setMethodValidator(@Nullable MethodValidator methodValidator) {
		this.methodValidator = methodValidator;
		this.validationGroups = (methodValidator != null ?
				methodValidator.determineValidationGroups(getBean(), getBridgedMethod()) : EMPTY_GROUPS);
	}


	/**
	 * Invoke the method after resolving its argument values in the context of the given request.
	 * <p>Argument values are commonly resolved through
	 * {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}.
	 * The {@code providedArgs} parameter however may supply argument values to be used directly,
	 * i.e. without argument resolution. Examples of provided argument values include a
	 * {@link WebDataBinder}, a {@link SessionStatus}, or a thrown exception instance.
	 * Provided argument values are checked before argument resolvers.
	 * <p>Delegates to {@link #getMethodArgumentValues} and calls {@link #doInvoke} with the
	 * resolved arguments.
	 * @param request the current request
	 * @param mavContainer the ModelAndViewContainer for this request
	 * @param providedArgs "given" arguments matched by type, not resolved
	 * @return the raw value returned by the invoked method
	 * @throws Exception raised if no suitable argument resolver can be found,
	 * or if the method raised an exception
	 * @see #getMethodArgumentValues
	 * @see #doInvoke
	 */
	public @Nullable Object invokeForRequest(NativeWebRequest request, @Nullable ModelAndViewContainer mavContainer,
			@Nullable Object... providedArgs) throws Exception {

		@Nullable Object[] args = getMethodArgumentValues(request, mavContainer, providedArgs);
		if (logger.isTraceEnabled()) {
			logger.trace("Arguments: " + Arrays.toString(args));
		}

		if (shouldValidateArguments() && this.methodValidator != null) {
			this.methodValidator.applyArgumentValidation(
					getBean(), getBridgedMethod(), getMethodParameters(), args, this.validationGroups);
		}

		Object returnValue = doInvoke(args);

		if (shouldValidateReturnValue() && this.methodValidator != null) {
			this.methodValidator.applyReturnValueValidation(
					getBean(), getBridgedMethod(), getReturnType(), returnValue, this.validationGroups);
		}

		return returnValue;
	}

	/**
	 * Get the method argument values for the current request, checking the provided
	 * argument values and falling back to the configured argument resolvers.
	 * <p>The resulting array will be passed into {@link #doInvoke}.
	 * @since 5.1.2
	 */
	protected @Nullable Object[] getMethodArgumentValues(NativeWebRequest request, @Nullable ModelAndViewContainer mavContainer,
			@Nullable Object... providedArgs) throws Exception {

		MethodParameter[] parameters = getMethodParameters();
		if (ObjectUtils.isEmpty(parameters)) {
			return EMPTY_ARGS;
		}

		@Nullable Object[] args = new Object[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			MethodParameter parameter = parameters[i];
			parameter.initParameterNameDiscovery(this.parameterNameDiscoverer);
			args[i] = findProvidedArgument(parameter, providedArgs);
			if (args[i] != null) {
				continue;
			}
			if (!this.resolvers.supportsParameter(parameter)) {
				throw new IllegalStateException(formatArgumentError(parameter, "No suitable resolver"));
			}
			try {
				args[i] = this.resolvers.resolveArgument(parameter, mavContainer, request, this.dataBinderFactory);
			}
			catch (Exception ex) {
				// Leave stack trace for later, exception may actually be resolved and handled...
				if (logger.isDebugEnabled()) {
					String exMsg = ex.getMessage();
					if (exMsg != null && !exMsg.contains(parameter.getExecutable().toGenericString())) {
						logger.debug(formatArgumentError(parameter, exMsg));
					}
				}
				throw ex;
			}
		}
		return args;
	}

	/**
	 * Invoke the handler method with the given argument values.
	 */
	protected @Nullable Object doInvoke(@Nullable Object... args) throws Exception {
		Method method = getBridgedMethod();
		try {
			if (KotlinDetector.isKotlinType(method.getDeclaringClass())) {
				if (KotlinDetector.isSuspendingFunction(method)) {
					return invokeSuspendingFunction(method, getBean(), args);
				}
				return KotlinDelegate.invokeFunction(method, getBean(), args);
			}
			return method.invoke(getBean(), args);
		}
		catch (IllegalArgumentException ex) {
			assertTargetBean(method, getBean(), args);
			String text = (ex.getMessage() == null || ex.getCause() instanceof NullPointerException) ?
					"Illegal argument" : ex.getMessage();
			throw new IllegalStateException(formatInvokeError(text, args), ex);
		}
		catch (InvocationTargetException ex) {
			// Unwrap for HandlerExceptionResolvers ...
			Throwable targetException = ex.getCause();
			if (targetException instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			else if (targetException instanceof Error error) {
				throw error;
			}
			else if (targetException instanceof Exception exception) {
				throw exception;
			}
			else {
				throw new IllegalStateException(formatInvokeError("Invocation failure", args), targetException);
			}
		}
	}

	/**
	 * Invoke the given Kotlin coroutine suspended function.
	 * <p>The default implementation invokes
	 * {@link CoroutinesUtils#invokeSuspendingFunction(Method, Object, Object...)},
	 * but subclasses can override this method to use
	 * {@link CoroutinesUtils#invokeSuspendingFunction(kotlin.coroutines.CoroutineContext, Method, Object, Object...)}
	 * instead.
	 * @since 6.0
	 */
	protected Object invokeSuspendingFunction(Method method, Object target, @Nullable Object[] args) {
		Object result = CoroutinesUtils.invokeSuspendingFunction(method, target, args);
		return (result instanceof Mono<?> mono ? mono.handle(KotlinDelegate::handleResult) : result);
	}


	/**
	 * Inner class to avoid a hard dependency on Kotlin at runtime.
	 */
	private static class KotlinDelegate {

		@SuppressWarnings("DataFlowIssue")
		public static @Nullable Object invokeFunction(Method method, Object target, @Nullable Object[] args) throws
				InvocationTargetException, IllegalAccessException, NoSuchMethodException {

			KFunction<?> function = ReflectJvmMapping.getKotlinFunction(method);
			// For property accessors
			if (function == null) {
				return method.invoke(target, args);
			}
			if (!KCallablesJvm.isAccessible(function)) {
				KCallablesJvm.setAccessible(function, true);
			}
			Map<KParameter, Object> argMap = CollectionUtils.newHashMap(args.length + 1);
			int index = 0;
			for (KParameter parameter : function.getParameters()) {
				switch (parameter.getKind()) {
					case INSTANCE -> argMap.put(parameter, target);
					case VALUE, EXTENSION_RECEIVER -> {
						Object arg = args[index];
						if (!(parameter.isOptional() && arg == null)) {
							KType type = parameter.getType();
							if (!(type.isMarkedNullable() && arg == null) &&
									type.getClassifier() instanceof KClass<?> kClass &&
									KotlinDetector.isInlineClass(JvmClassMappingKt.getJavaClass(kClass))) {
								arg = box(kClass, arg);
							}
							argMap.put(parameter, arg);
						}
						index++;
					}
				}
			}
			Object result = function.callBy(argMap);
			if (result != null && KotlinDetector.isInlineClass(result.getClass())) {
				result = unbox(result);
			}
			return (result == Unit.INSTANCE ? null : result);
		}

		private static Object box(KClass<?> kClass, @Nullable Object arg) {
			KFunction<?> constructor = Objects.requireNonNull(KClasses.getPrimaryConstructor(kClass));
			KType type = constructor.getParameters().get(0).getType();
			if (!(type.isMarkedNullable() && arg == null) &&
					type.getClassifier() instanceof KClass<?> parameterClass &&
					KotlinDetector.isInlineClass(JvmClassMappingKt.getJavaClass(parameterClass))) {
				arg = box(parameterClass, arg);
			}
			if (!KCallablesJvm.isAccessible(constructor)) {
				KCallablesJvm.setAccessible(constructor, true);
			}
			return constructor.call(arg);
		}

		private static void handleResult(Object result, SynchronousSink<Object> sink) {
			if (KotlinDetector.isInlineClass(result.getClass())) {
				try {
					Object unboxed = unbox(result);
					if (unboxed != Unit.INSTANCE) {
						sink.next(unboxed);
					}
					sink.complete();
				}
				catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
					sink.error(ex);
				}
			}
			else {
				sink.next(result);
				sink.complete();
			}
		}

		private static Object unbox(Object result) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
			Object unboxed = result.getClass().getDeclaredMethod("unbox-impl").invoke(result);
			if (KotlinDetector.isInlineClass(unboxed.getClass())) {
				return unbox(unboxed);
			}
			return unboxed;
		}
	}

}
