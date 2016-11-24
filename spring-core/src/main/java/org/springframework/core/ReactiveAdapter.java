/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Contract for adapting to and from {@link Flux} and {@link Mono}.
 *
 * <p>An adapter supports a specific adaptee type whose stream semantics
 * can be checked via {@link #getDescriptor()}.
 *
 * <p>Use the {@link ReactiveAdapterRegistry} to obtain an adapter for a
 * supported adaptee type or to register additional adapters.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface ReactiveAdapter {

	/**
	 * Return a descriptor with further information about the adaptee.
	 */
	ReactiveTypeDescriptor getDescriptor();

	/**
	 * Adapt the given Object to a {@link Mono}
	 * @param source the source object to adapt
	 * @return the resulting {@link Mono} possibly empty
	 */
	<T> Mono<T> toMono(Object source);

	/**
	 * Adapt the given Object to a {@link Flux}.
	 * @param source the source object to adapt
	 * @return the resulting {@link Flux} possibly empty
	 */
	<T> Flux<T> toFlux(Object source);

	/**
	 * Adapt the given Object to a Publisher.
	 * @param source the source object to adapt
	 * @return the resulting {@link Mono} or {@link Flux} possibly empty
	 */
	<T> Publisher<T> toPublisher(Object source);

	/**
	 * Adapt the given Publisher to the target adaptee.
	 * @param publisher the publisher to adapt
	 * @return the resulting adaptee
	 */
	Object fromPublisher(Publisher<?> publisher);

}
