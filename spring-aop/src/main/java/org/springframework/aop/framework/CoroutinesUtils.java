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

package org.springframework.aop.framework;

import kotlin.coroutines.Continuation;
import kotlinx.coroutines.reactive.ReactiveFlowKt;
import kotlinx.coroutines.reactor.MonoKt;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.lang.Nullable;

/**
 * Package-visible class designed to avoid a hard dependency on Kotlin and Coroutines dependency at runtime.
 *
 * @author Sebastien Deleuze
 * @since 6.1
 */
abstract class CoroutinesUtils {

	static Object asFlow(Object publisher) {
		return ReactiveFlowKt.asFlow((Publisher<?>) publisher);
	}

	@Nullable
	@SuppressWarnings({"unchecked", "rawtypes"})
	static Object awaitSingleOrNull(Object value, Object continuation) {
		return MonoKt.awaitSingleOrNull(value instanceof Mono mono ? mono : Mono.just(value),
				(Continuation<Object>) continuation);
	}

}
