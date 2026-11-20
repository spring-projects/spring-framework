/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.converter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;

/**
 * A {@link MessageConverter} that delegates to a list of registered converters
 * to be invoked until one of them returns a non-null result.
 *
 * <p>As of 4.2.1, this composite converter implements {@link SmartMessageConverter}
 * in order to support the delegation of conversion hints.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public class CompositeMessageConverter implements SmartMessageConverter {

	private final List<MessageConverter> converters;


	/**
	 * Create an instance with the given converters.
	 */
	public CompositeMessageConverter(Collection<MessageConverter> converters) {
		Assert.notEmpty(converters, "Converters must not be empty");
		this.converters = new ArrayList<>(converters);
	}


	@Override
	public @Nullable Object fromMessage(Message<?> message, Class<?> targetClass) {
		for (MessageConverter converter : getConverters()) {
			Object result = converter.fromMessage(message, targetClass);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	@Override
	public @Nullable Object fromMessage(Message<?> message, Class<?> targetClass, @Nullable Object conversionHint) {
		for (MessageConverter converter : getConverters()) {
			Object result = (converter instanceof SmartMessageConverter smartMessageConverter ?
					smartMessageConverter.fromMessage(message, targetClass, conversionHint) :
					converter.fromMessage(message, targetClass));
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	@Override
	public @Nullable Message<?> toMessage(Object payload, @Nullable MessageHeaders headers) {
		for (MessageConverter converter : getConverters()) {
			Message<?> result = converter.toMessage(payload, headers);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	@Override
	public @Nullable Message<?> toMessage(Object payload, @Nullable MessageHeaders headers, @Nullable Object conversionHint) {
		for (MessageConverter converter : getConverters()) {
			Message<?> result = (converter instanceof SmartMessageConverter smartMessageConverter ?
					smartMessageConverter.toMessage(payload, headers, conversionHint) :
					converter.toMessage(payload, headers));
			if (result != null) {
				return result;
			}
		}
		return null;
	}


	/**
	 * Return the underlying list of delegate converters.
	 */
	public List<MessageConverter> getConverters() {
		return this.converters;
	}

	@Override
	public String toString() {
		return "CompositeMessageConverter[converters=" + getConverters() + "]";
	}

}
