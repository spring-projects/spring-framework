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

package org.springframework.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

import kotlin.Unit;
import kotlin.coroutines.CoroutineContext;
import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KClass;
import kotlin.reflect.KClassifier;
import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.full.KCallables;
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
	public static Publisher<?> invokeSuspendingFunction(Method method, Object target,
			Object... args) {
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
	@SuppressWarnings("deprecation")
	public static Publisher<?> invokeSuspendingFunction(CoroutineContext context, Method method, Object target,
			Object... args) {
		Assert.isTrue(KotlinDetector.isSuspendingFunction(method), "'method' must be a suspending function");
		KFunction<?> function = Objects.requireNonNull(ReflectJvmMapping.getKotlinFunction(method));
		if (method.isAccessible() && !KCallablesJvm.isAccessible(function)) {
			KCallablesJvm.setAccessible(function, true);
		}
		Mono<Object> mono = MonoKt.mono(context, (scope, continuation) -> {
					Map<KParameter, Object> argMap = CollectionUtils.newHashMap(args.length + 1);
					int index = 0;
					for (KParameter parameter : function.getParameters()) {
						switch (parameter.getKind()) {
							case INSTANCE -> argMap.put(parameter, target);
							case VALUE -> {
								if (!parameter.isOptional() || args[index] != null) {
									argMap.put(parameter, args[index]);
								}
								index++;
							}
						}
					}
					return KCallables.callSuspendBy(function, argMap, continuation);
				})
				.filter(result -> !Objects.equals(result, Unit.INSTANCE))
				.onErrorMap(InvocationTargetException.class, InvocationTargetException::getTargetException);

		KClassifier returnType = function.getReturnType().getClassifier();
		if (returnType != null) {
			if (returnType.equals(JvmClassMappingKt.getKotlinClass(Flow.class))) {
				return mono.flatMapMany(CoroutinesUtils::asFlux);
			}
			else if (returnType.equals(JvmClassMappingKt.getKotlinClass(Mono.class))) {
				return mono.flatMap(o -> ((Mono<?>)o));
			}
			else if (returnType instanceof KClass<?> kClass &&
					Publisher.class.isAssignableFrom(JvmClassMappingKt.getJavaClass(kClass))) {
				return mono.flatMapMany(o -> ((Publisher<?>)o));
			}
		}
		return mono;
	}

	private static Flux<?> asFlux(Object flow) {
		return ReactorFlowKt.asFlux(((Flow<?>) flow));
	}

}
