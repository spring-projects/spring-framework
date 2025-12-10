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

package org.springframework.core;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import kotlin.coroutines.AbstractCoroutineContextElement;
import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.ThreadContextElement;
import kotlinx.coroutines.reactor.ReactorContext;
import org.jspecify.annotations.Nullable;
import reactor.util.context.ContextView;

import org.springframework.util.ClassUtils;

/**
 * {@link ThreadContextElement} that ensures that contexts registered with the
 * Micrometer Context Propagation library are captured and restored when
 * a coroutine is resumed on a thread. This is typically being used for
 * Micrometer Tracing support in Kotlin suspended functions.
 *
 * <p>It requires the {@code io.micrometer:context-propagation} library. If the
 * {@code org.jetbrains.kotlinx:kotlinx-coroutines-reactor} dependency is also
 * on the classpath, this element also supports Reactor {@code Context}.
 *
 * <p>{@code PropagationContextElement} can be used like this:
 *
 * <pre class="code">
 * fun main() {
 *     runBlocking(Dispatchers.IO + PropagationContextElement()) {
 *         suspendingFunction()
 *     }
 * }
 *
 * suspend fun suspendingFunction() {
 *     delay(1)
 *     logger.info("Log statement with traceId")
 * }
 * </pre>
 *
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @since 7.0
 */
public final class PropagationContextElement extends AbstractCoroutineContextElement implements ThreadContextElement<ContextSnapshot.Scope> {

	/**
	 * {@code PropagationContextElement} key.
	 */
	public static final Key Key = new Key();

	private static final ContextSnapshotFactory contextSnapshotFactory = ContextSnapshotFactory.builder()
			.contextRegistry(ContextRegistry.getInstance()).build();

	private static final boolean coroutinesReactorPresent = ClassUtils.isPresent("kotlinx.coroutines.reactor.ReactorContext",
			PropagationContextElement.class.getClassLoader());

	private final ContextSnapshot threadLocalContextSnapshot;


	public PropagationContextElement() {
		super(Key);
		this.threadLocalContextSnapshot = contextSnapshotFactory.captureAll();
	}

	public void restoreThreadContext(CoroutineContext context, ContextSnapshot.Scope oldState) {
		oldState.close();
	}

	public ContextSnapshot.Scope updateThreadContext(CoroutineContext context) {
		ContextSnapshot contextSnapshot;
		if (coroutinesReactorPresent) {
			contextSnapshot = ReactorDelegate.captureFrom(context);
			if (contextSnapshot == null) {
				contextSnapshot = this.threadLocalContextSnapshot;
			}
		}
		else {
			contextSnapshot = this.threadLocalContextSnapshot;
		}
		return contextSnapshot.setThreadLocals();
	}

	public static final class Key implements CoroutineContext.Key<PropagationContextElement> {
	}

	private static final class ReactorDelegate {

		@Nullable
		@SuppressWarnings({"unchecked", "rawtypes"})
		public static ContextSnapshot captureFrom(CoroutineContext context) {
			ReactorContext reactorContext = (ReactorContext)context.get((CoroutineContext.Key)ReactorContext.Key);
			ContextView contextView = reactorContext != null ? reactorContext.getContext() : null;
			if (contextView != null) {
				return contextSnapshotFactory.captureFrom(contextView);
			}
			else {
				return null;
			}
		}
	}
}
