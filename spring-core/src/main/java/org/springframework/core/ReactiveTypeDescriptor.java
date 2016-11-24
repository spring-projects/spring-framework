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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.util.Assert;

/**
 * Descriptor for a reactive type with information its stream semantics, i.e.
 * how many values it can produce.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ReactiveTypeDescriptor {

	private final Class<?> reactiveType;

	private final boolean multiValue;

	private final boolean supportsEmpty;

	private final boolean noValue;


	/**
	 * Private constructor. See static factory methods.
	 */
	private ReactiveTypeDescriptor(Class<?> reactiveType, boolean multiValue,
			boolean canBeEmpty, boolean noValue) {

		Assert.notNull(reactiveType, "'reactiveType' must not be null");
		this.reactiveType = reactiveType;
		this.multiValue = multiValue;
		this.supportsEmpty = canBeEmpty;
		this.noValue = noValue;
	}


	/**
	 * Return the reactive type the descriptor was created for.
	 */
	public Class<?> getReactiveType() {
		return this.reactiveType;
	}

	/**
	 * Return {@code true} if the reactive type can produce more than 1 value
	 * can be produced and is therefore a good fit to adapt to {@link Flux}.
	 * A {@code false} return value implies the reactive type can produce 1
	 * value at most and is therefore a good fit to adapt to {@link Mono}.
	 */
	public boolean isMultiValue() {
		return this.multiValue;
	}

	/**
	 * Return {@code true} if the reactive type can complete with no values.
	 */
	public boolean supportsEmpty() {
		return this.supportsEmpty;
	}

	/**
	 * Return {@code true} if the reactive type does not produce any values and
	 * only provides completion and error signals.
	 */
	public boolean isNoValue() {
		return this.noValue;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		return this.reactiveType.equals(((ReactiveTypeDescriptor) other).reactiveType);
	}

	@Override
	public int hashCode() {
		return this.reactiveType.hashCode();
	}


	/**
	 * Descriptor for a reactive type that can produce 0..N values.
	 */
	public static ReactiveTypeDescriptor multiValue(Class<?> reactiveType) {
		return new ReactiveTypeDescriptor(reactiveType, true, true, false);
	}

	/**
	 * Descriptor for a reactive type that can produce 0..1 values.
	 */
	public static ReactiveTypeDescriptor singleOptionalValue(Class<?> reactiveType) {
		return new ReactiveTypeDescriptor(reactiveType, false, true, false);
	}

	/**
	 * Descriptor for a reactive type that must produce 1 value to complete.
	 */
	public static ReactiveTypeDescriptor singleRequiredValue(Class<?> reactiveType) {
		return new ReactiveTypeDescriptor(reactiveType, false, false, false);
	}

	/**
	 * Descriptor for a reactive type that does not produce any values.
	 */
	public static ReactiveTypeDescriptor noValue(Class<?> reactiveType) {
		return new ReactiveTypeDescriptor(reactiveType, false, true, true);
	}

}
