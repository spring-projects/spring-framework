/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.converter;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * An extension of the {@link SimpleMessageConverter} that uses a
 * {@link ConversionService} to convert the payload of the message
 * to the requested type.
 *
 * <p>Return {@code null} if the conversion service cannot convert
 * from the payload type to the requested type.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see ConversionService
 */
public class GenericMessageConverter extends SimpleMessageConverter {

	private final ConversionService conversionService;


	/**
	 * Create a new instance with a default {@link ConversionService}.
	 */
	public GenericMessageConverter() {
		this.conversionService = DefaultConversionService.getSharedInstance();
	}

	/**
	 * Create a new instance with the given {@link ConversionService}.
	 */
	public GenericMessageConverter(ConversionService conversionService) {
		Assert.notNull(conversionService, "ConversionService must not be null");
		this.conversionService = conversionService;
	}


	@Override
	public @Nullable Object fromMessage(Message<?> message, Class<?> targetClass) {
		Object payload = message.getPayload();
		if (this.conversionService.canConvert(payload.getClass(), targetClass)) {
			try {
				return this.conversionService.convert(payload, targetClass);
			}
			catch (ConversionException ex) {
				throw new MessageConversionException(message, "Failed to convert message payload '" +
						payload + "' to '" + targetClass.getName() + "'", ex);
			}
		}
		return (ClassUtils.isAssignableValue(targetClass, payload) ? payload : null);
	}

}
