/*
 * Copyright 2002-2017 the original author or authors.
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

import java.util.Optional;
import java.util.function.Function;

import org.reactivestreams.Publisher;

import org.springframework.util.Assert;

/**
 * Adapt a Reactive Streams {@link Publisher} to and from an async/reactive type
 * such as {@code CompletableFuture}, an RxJava {@code Observable}, etc.
 *
 * <p>Use the {@link ReactiveAdapterRegistry} to register reactive types and
 * obtain adapters from.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ReactiveAdapter {

	private final ReactiveTypeDescriptor descriptor;

	private final Function<Object, Publisher<?>> toPublisherFunction;

	private final Function<Publisher<?>, Object> fromPublisherFunction;


	/**
	 * Constructor for an adapter with functions to convert the target reactive
	 * or async type to and from a Reactive Streams Publisher.
	 * @param descriptor the reactive type descriptor
	 * @param toPublisherFunction adapter to a Publisher
	 * @param fromPublisherFunction adapter from a Publisher
	 */
	public ReactiveAdapter(ReactiveTypeDescriptor descriptor,
			Function<Object, Publisher<?>> toPublisherFunction,
			Function<Publisher<?>, Object> fromPublisherFunction) {

		Assert.notNull(descriptor, "'descriptor' is required");
		Assert.notNull(toPublisherFunction, "'toPublisherFunction' is required");
		Assert.notNull(fromPublisherFunction, "'fromPublisherFunction' is required");

		this.descriptor = descriptor;
		this.toPublisherFunction = toPublisherFunction;
		this.fromPublisherFunction = fromPublisherFunction;
	}


	/**
	 * Return the descriptor of the reactive type for the adapter.
	 */
	public ReactiveTypeDescriptor getDescriptor() {
		return this.descriptor;
	}

	/**
	 * A shortcut for {@code getDescriptor().getReactiveType()}.
	 */
	public Class<?> getReactiveType() {
		return getDescriptor().getReactiveType();
	}

	/**
	 * A shortcut for {@code getDescriptor().isMultiValue()}.
	 */
	public boolean isMultiValue() {
		return getDescriptor().isMultiValue();
	}

	/**
	 * A shortcut for {@code getDescriptor().supportsEmpty()}.
	 */
	public boolean supportsEmpty() {
		return getDescriptor().supportsEmpty();
	}

	/**
	 * A shortcut for {@code getDescriptor().isNoValue()}.
	 */
	public boolean isNoValue() {
		return getDescriptor().isNoValue();
	}


	/**
	 * Adapt the given instance to a Reactive Streams Publisher.
	 * @param source the source object to adapt from
	 * @return the Publisher representing the adaptation
	 */
	@SuppressWarnings("unchecked")
	public <T> Publisher<T> toPublisher(Object source) {
		source = (source instanceof Optional ? ((Optional<?>) source).orElse(null) : source);
		if (source == null) {
			source = getDescriptor().getEmptyValue();
		}
		return (Publisher<T>) this.toPublisherFunction.apply(source);
	}

	/**
	 * Adapt from the given Reactive Streams Publisher.
	 * @param publisher the publisher to adapt from
	 * @return the reactive type instance representing the adapted publisher
	 */
	public Object fromPublisher(Publisher<?> publisher) {
		return (publisher != null ? this.fromPublisherFunction.apply(publisher) : null);
	}

}
