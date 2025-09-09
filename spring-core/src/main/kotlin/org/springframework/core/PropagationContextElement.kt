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

package org.springframework.core

import io.micrometer.context.ContextRegistry
import io.micrometer.context.ContextSnapshot
import io.micrometer.context.ContextSnapshotFactory
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.reactor.ReactorContext
import reactor.util.context.ContextView
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext


/**
 * [ThreadContextElement] that restores `ThreadLocals` from the Reactor [ContextSnapshot]
 * every time the coroutine with this element in the context is resumed on a thread.
 *
 * This effectively ensures that Kotlin Coroutines, Reactor and Micrometer Context Propagation
 * work together in an application, typically for observability purposes.
 *
 * Applications need to have both `"io.micrometer:context-propagation"` and
 * `"org.jetbrains.kotlinx:kotlinx-coroutines-reactor"` on the classpath to use this context element.
 *
 * The `PropagationContextElement` can be used like this:
 * 
 * ```kotlin
 *   suspend fun suspendable() {
 *     withContext(PropagationContextElement(coroutineContext)) {
 *       logger.info("Log statement with traceId")
 *     }
 *   }
 * ```
 *
 * @author Brian Clozel
 * @since 7.0
 */
class PropagationContextElement(private val context: CoroutineContext) : ThreadContextElement<ContextSnapshot.Scope>,
	AbstractCoroutineContextElement(Key) {

	companion object Key : CoroutineContext.Key<PropagationContextElement>

	val contextSnapshot: ContextSnapshot
		get() {
			val contextView: ContextView? = context[ReactorContext]?.context
			val contextSnapshotFactory =
				ContextSnapshotFactory.builder().contextRegistry(ContextRegistry.getInstance()).build()
			if (contextView != null) {
				return contextSnapshotFactory.captureFrom(contextView)
			}
			return contextSnapshotFactory.captureAll()
		}

	override fun restoreThreadContext(context: CoroutineContext, oldState: ContextSnapshot.Scope) {
		oldState.close()
	}

	override fun updateThreadContext(context: CoroutineContext): ContextSnapshot.Scope {
		return contextSnapshot.setThreadLocals()
	}
}