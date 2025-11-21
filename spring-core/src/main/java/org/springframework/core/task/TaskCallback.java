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

package org.springframework.core.task;

import java.util.concurrent.Callable;

/**
 * Variant of {@link Callable} with a flexible exception signature
 * that can be adapted in the {@link SyncTaskExecutor#execute(TaskCallback)}
 * method signature for propagating specific exception types only.
 *
 * <p>An implementation of this interface can also be passed into any
 * {@code Callback}-based method such as {@link AsyncTaskExecutor#submit(Callable)}
 * or {@link AsyncTaskExecutor#submitCompletable(Callable)}. It is just capable
 * of adapting to flexible exception propagation in caller signatures as well.
 *
 * @author Juergen Hoeller
 * @since 7.0
 * @param <V> the returned value type, if any
 * @param <E> the exception propagated, if any
 * @see SyncTaskExecutor#execute(TaskCallback)
 */
public interface TaskCallback<V, E extends Exception> extends Callable<V> {

	@Override
	V call() throws E;

}
