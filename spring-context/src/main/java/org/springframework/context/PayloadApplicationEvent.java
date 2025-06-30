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

package org.springframework.context;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;
import org.springframework.util.Assert;

/**
 * An {@link ApplicationEvent} that carries an arbitrary payload.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author Qimiao Chen
 * @since 4.2
 * @param <T> the payload type of the event
 * @see ApplicationEventPublisher#publishEvent(Object)
 * @see ApplicationListener#forPayload(Consumer)
 */
@SuppressWarnings("serial")
public class PayloadApplicationEvent<T> extends ApplicationEvent implements ResolvableTypeProvider {

	private final T payload;

	private final ResolvableType payloadType;


	/**
	 * Create a new PayloadApplicationEvent, using the instance to infer its type.
	 * @param source the object on which the event initially occurred (never {@code null})
	 * @param payload the payload object (never {@code null})
	 */
	public PayloadApplicationEvent(Object source, T payload) {
		this(source, payload, null);
	}

	/**
	 * Create a new PayloadApplicationEvent based on the provided payload type.
	 * @param source the object on which the event initially occurred (never {@code null})
	 * @param payload the payload object (never {@code null})
	 * @param payloadType the type object of payload object (can be {@code null}).
	 * Note that this is meant to indicate the payload type (for example, {@code String}),
	 * not the full event type (such as {@code PayloadApplicationEvent<&lt;String&gt;}).
	 * @since 6.0
	 */
	public PayloadApplicationEvent(Object source, T payload, @Nullable ResolvableType payloadType) {
		super(source);
		Assert.notNull(payload, "Payload must not be null");
		this.payload = payload;
		this.payloadType = (payloadType != null ? payloadType : ResolvableType.forInstance(payload));
	}


	@Override
	public ResolvableType getResolvableType() {
		return ResolvableType.forClassWithGenerics(getClass(), this.payloadType);
	}

	/**
	 * Return the payload of the event.
	 */
	public T getPayload() {
		return this.payload;
	}

}
