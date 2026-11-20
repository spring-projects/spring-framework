/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.converter;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * An extended {@link MessageConverter} SPI with conversion hint support.
 *
 * <p>In case of a conversion hint being provided, the framework will call
 * these extended methods if a converter implements this interface, instead
 * of calling the regular {@code fromMessage} / {@code toMessage} variants.
 *
 * @author Juergen Hoeller
 * @since 4.2.1
 */
public interface SmartMessageConverter extends MessageConverter {

	/**
	 * A variant of {@link #fromMessage(Message, Class)} which takes an extra
	 * conversion context as an argument, allowing to take, for example, annotations
	 * on a payload parameter into account.
	 * @param message the input message
	 * @param targetClass the target class for the conversion
	 * @param conversionHint an extra object passed to the {@link MessageConverter},
	 * for example, the associated {@code MethodParameter} (may be {@code null})
	 * @return the result of the conversion, or {@code null} if the converter cannot
	 * perform the conversion
	 * @see #fromMessage(Message, Class)
	 */
	@Nullable Object fromMessage(Message<?> message, Class<?> targetClass, @Nullable Object conversionHint);

	/**
	 * A variant of {@link #toMessage(Object, MessageHeaders)} which takes an extra
	 * conversion context as an argument, allowing to take, for example, annotations
	 * on a return type into account.
	 * @param payload the Object to convert
	 * @param headers optional headers for the message (may be {@code null})
	 * @param conversionHint an extra object passed to the {@link MessageConverter},
	 * for example, the associated {@code MethodParameter} (may be {@code null})
	 * @return the new message, or {@code null} if the converter does not support the
	 * Object type or the target media type
	 * @see #toMessage(Object, MessageHeaders)
	 */
	@Nullable Message<?> toMessage(Object payload, @Nullable MessageHeaders headers, @Nullable Object conversionHint);

}
