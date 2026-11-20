/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
