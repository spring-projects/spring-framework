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

package org.springframework.messaging.tcp.reactor;

import reactor.core.publisher.Mono;

/**
 * A Mono-to-ListenableFuture adapter where the source and the target from
 * the Promise and the ListenableFuture respectively are of the same type.
 *
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 * @since 5.0
 */
class MonoToListenableFutureAdapter<T> extends AbstractMonoToListenableFutureAdapter<T, T> {

	public MonoToListenableFutureAdapter(Mono<T> mono) {
		super(mono);
	}

	@Override
	protected T adapt(T result) {
		return result;
	}

}
