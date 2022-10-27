/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.util.concurrent;

import reactor.core.publisher.Mono;

/**
 * Adapts a {@link Mono} into a {@link ListenableFuture} by obtaining a
 * {@code CompletableFuture} from the {@code Mono} via {@link Mono#toFuture()}
 * and then adapting it with {@link CompletableToListenableFutureAdapter}.
 *
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 * @since 5.1
 * @param <T> the object type
 * @deprecated as of 6.0, in favor of {@link Mono#toFuture()}
 */
@Deprecated(since = "6.0")
public class MonoToListenableFutureAdapter<T> extends CompletableToListenableFutureAdapter<T> {

	public MonoToListenableFutureAdapter(Mono<T> mono) {
		super(mono.toFuture());
	}

}
