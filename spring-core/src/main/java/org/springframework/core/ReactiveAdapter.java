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
 *  <p> {@link Flux} and {@link Mono} 适配器之间的沟通类
 *     使用ReactiveAdapterRegistry 去获取一个支持视频类型的适配器,或者注册额外的适配器
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface ReactiveAdapter {

	/**
	 * Return a descriptor with further information about the adaptee.
	 * 返回adaptee的更多信息的描述符
	 */
	Descriptor getDescriptor();

	/**
	 * Adapt the given Object to a {@link Mono}
	 * 
	 * 适配给定对象给{@link Mono}
	 * @param source the source object to adapt
	 * @return the resulting {@link Mono} possibly empty
	 */
	<T> Mono<T> toMono(Object source);

	/**
	 * Adapt the given Object to a {@link Flux}.
	 * 适配给定对象给{@link Flux}
	 * @param source the source object to adapt
	 * @return the resulting {@link Flux} possibly empty
	 */
	<T> Flux<T> toFlux(Object source);

	/**
	 * Adapt the given Object to a Publisher.
	 * 适配给定对象给Publisher
	 * @param source the source object to adapt
	 * @return the resulting {@link Mono} or {@link Flux} possibly empty
	 */
	<T> Publisher<T> toPublisher(Object source);

	/**
	 * Adapt the given Publisher to the target adaptee.
	 * 适配给定Publisher到目标对象adaptee
	 * @param publisher the publisher to adapt
	 * @return the resulting adaptee
	 */
	Object fromPublisher(Publisher<?> publisher);


	/**
	 * A descriptor with information about the adaptee stream semantics.
	 * adaptee对象的描述符
	 */
	class Descriptor {

		private final boolean isMultiValue;

		private final boolean supportsEmpty;

		private final boolean isNoValue;

		public Descriptor(boolean isMultiValue, boolean canBeEmpty, boolean isNoValue) {
			this.isMultiValue = isMultiValue;
			this.supportsEmpty = canBeEmpty;
			this.isNoValue = isNoValue;
		}

		/**
		 * Return {@code true} if the adaptee implies 0..N values can be produced
		 * and is therefore a good fit to adapt to {@link Flux}. A {@code false}
		 * return value implies the adaptee will produce 1 value at most and is
		 * therefore a good fit for {@link Mono}.
		 * 
		 * 如果适配器能生成0..N个值则适合适配给 {@link Flux}返回true
		 * 如果适配器能生成1个值最多,那么适合适配给{@link Mono}返回false
		 */
		public boolean isMultiValue() {
			return this.isMultiValue;
		}

		/**
		 * Return {@code true} if the adaptee can complete without values.
		 * 如果适配器可以不需要值完成操作，返回true
		 */
		public boolean supportsEmpty() {
			return this.supportsEmpty;
		}

		/**
		 * Return {@code true} if the adaptee implies no values will be produced,
		 * i.e. providing only completion or error signal.
		 * 
		 * 如果适配器没有值被生成,返回true,
		 */
		public boolean isNoValue() {
			return this.isNoValue;
		}
	}

}
