/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import kotlin.Unit;
import kotlin.coroutines.CoroutineContext;
import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KClass;
import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.KType;
import kotlin.reflect.full.KCallables;
import kotlin.reflect.full.KClasses;
import kotlin.reflect.full.KClassifiers;
import kotlin.reflect.full.KTypes;
import kotlin.reflect.jvm.KCallablesJvm;
import kotlin.reflect.jvm.ReflectJvmMapping;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineStart;
import kotlinx.coroutines.Deferred;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.GlobalScope;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.reactor.MonoKt;
import kotlinx.coroutines.reactor.ReactorFlowKt;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Utilities for working with Kotlin Coroutines.
 *
 * @author Sebastien Deleuze
 * @author Phillip Webb
 * @since 5.2
 */
public abstract class CoroutinesUtils {

	private static final KType flowType = KClassifiers.getStarProjectedType(JvmClassMappingKt.getKotlinClass(Flow.class));

	private static final KType monoType = KClassifiers.getStarProjectedType(JvmClassMappingKt.getKotlinClass(Mono.class));

	private static final KType publisherType = KClassifiers.getStarProjectedType(JvmClassMappingKt.getKotlinClass(Publisher.class));


	/**
	 * Convert a {@link Deferred} instance to a {@link Mono}.
	 */
	public static <T> Mono<T> deferredToMono(Deferred<T> source) {
		return MonoKt.mono(Dispatchers.getUnconfined(),
				(scope, continuation) -> source.await(continuation));
	}

	/**
	 * Convert a {@link Mono} instance to a {@link Deferred}.
	 */
	public static <T> Deferred<T> monoToDeferred(Mono<T> source) {
		return BuildersKt.async(GlobalScope.INSTANCE, Dispatchers.getUnconfined(),
				CoroutineStart.DEFAULT,
				(scope, continuation) -> MonoKt.awaitSingleOrNull(source, continuation));
	}

	/**
	 * Invoke a suspending function and convert it to {@link Mono} or {@link Flux}.
	 * Uses an {@linkplain Dispatchers#getUnconfined() unconfined} dispatcher.
	 * @param method the suspending function to invoke
	 * @param target the target to invoke {@code method} on
	 * @param args the function arguments. If the {@code Continuation} argument is specified as the last argument
	 * (typically {@code null}), it is ignored.
	 * @return the method invocation result as reactive stream
	 * @throws IllegalArgumentException if {@code method} is not a suspending function
	 */
	public static Publisher<?> invokeSuspendingFunction(Method method, Object target, @Nullable Object... args) {
		return invokeSuspendingFunction(Dispatchers.getUnconfined(), method, target, args);
	}

	/**
	 * Invoke a suspending function and convert it to {@link Mono} or
	 * {@link Flux}.
	 * @param context the coroutine context to use
	 * @param method the suspending function to invoke
	 * @param target the target to invoke {@code method} on
	 * @param args the function arguments. If the {@code Continuation} argument is specified as the last argument
	 * (typically {@code null}), it is ignored.
	 * @return the method invocation result as reactive stream
	 * @throws IllegalArgumentException if {@code method} is not a suspending function
	 * @since 6.0
	 */
	@SuppressWarnings({"deprecation", "DataFlowIssue", "NullAway"})
	public static Publisher<?> invokeSuspendingFunction(
			CoroutineContext context, Method method, @Nullable Object target, @Nullable Object... args) {

		Assert.isTrue(KotlinDetector.isSuspendingFunction(method), "Method must be a suspending function");
		KFunction<?> function = ReflectJvmMapping.getKotlinFunction(method);
		Assert.notNull(function, () -> "Failed to get Kotlin function for method: " + method);
		if (!KCallablesJvm.isAccessible(function)) {
			KCallablesJvm.setAccessible(function, true);
		}
		Mono<Object> mono = MonoKt.mono(context, (scope, continuation) -> {
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
										KFunction<?> constructor = KClasses.getPrimaryConstructor(kClass);
										if (!KCallablesJvm.isAccessible(constructor)) {
											KCallablesJvm.setAccessible(constructor, true);
										}
										arg = constructor.call(arg);
									}
									argMap.put(parameter, arg);
								}
								index++;
							}
						}
					}
					return KCallables.callSuspendBy(function, argMap, continuation);
				})
				.handle(CoroutinesUtils::handleResult)
				.onErrorMap(InvocationTargetException.class, InvocationTargetException::getTargetException);

		KType returnType = function.getReturnType();
		if (KTypes.isSubtypeOf(returnType, flowType)) {
			return mono.flatMapMany(CoroutinesUtils::asFlux);
		}
		if (KTypes.isSubtypeOf(returnType, publisherType)) {
			if (KTypes.isSubtypeOf(returnType, monoType)) {
				return mono.flatMap(o -> ((Mono<?>)o));
			}
			return mono.flatMapMany(o -> ((Publisher<?>)o));
		}
		return mono;
	}

	private static Flux<?> asFlux(Object flow) {
		return ReactorFlowKt.asFlux(((Flow<?>) flow));
	}

	private static void handleResult(Object result, SynchronousSink<Object> sink) {
		if (result == Unit.INSTANCE) {
			sink.complete();
		}
		else if (KotlinDetector.isInlineClass(result.getClass())) {
			try {
				sink.next(result.getClass().getDeclaredMethod("unbox-impl").invoke(result));
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
}
