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
import org.springframework.util.ClassUtils
import reactor.util.context.ContextView
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext


/**
 * [ThreadContextElement] that ensures that contexts registered with the
 * Micrometer Context Propagation library are captured and restored when
 * a coroutine is resumed on a thread. This is typically being used for
 * Micrometer Tracing support in Kotlin suspended functions.
 *
 * It requires the `io.micrometer:context-propagation` library. If the
 * `org.jetbrains.kotlinx:kotlinx-coroutines-reactor` dependency is also
 * on the classpath, this element also supports Reactor `Context`.
 *
 * `PropagationContextElement` can be used like this:
 *
 * ```kotlin
 *   fun main() {
 * 		runBlocking(Dispatchers.IO + PropagationContextElement()) {
 * 			suspendingFunction()
 * 		}
 *   }
 *
 *  suspend fun suspendingFunction() {
 *      delay(1)
 *      logger.info("Log statement with traceId")
 *  }
 * ```
 *
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @since 7.0
 */
class PropagationContextElement : ThreadContextElement<ContextSnapshot.Scope>,
	AbstractCoroutineContextElement(Key) {

	companion object Key : CoroutineContext.Key<PropagationContextElement> {

		private val contextSnapshotFactory =
			ContextSnapshotFactory.builder().contextRegistry(ContextRegistry.getInstance()).build()

		private val coroutinesReactorPresent =
			ClassUtils.isPresent("kotlinx.coroutines.reactor.ReactorContext",
				PropagationContextElement::class.java.classLoader);
	}

	// Context captured from the the ThreadLocal where the PropagationContextElement is instantiated
	private val threadLocalContextSnapshot: ContextSnapshot = contextSnapshotFactory.captureAll()

	override fun restoreThreadContext(context: CoroutineContext, oldState: ContextSnapshot.Scope) {
		oldState.close()
	}

	override fun updateThreadContext(context: CoroutineContext): ContextSnapshot.Scope {
		val contextSnapshot = if (coroutinesReactorPresent) {
			ReactorDelegate().captureFrom(context) ?: threadLocalContextSnapshot
		} else {
			threadLocalContextSnapshot
		}
		return contextSnapshot.setThreadLocals()
	}

	private class ReactorDelegate {

		fun captureFrom(context: CoroutineContext): ContextSnapshot? {
			val contextView: ContextView? = context[ReactorContext]?.context
			if (contextView != null) {
				return contextSnapshotFactory.captureFrom(contextView)
			}
			return null;
		}
	}
}
