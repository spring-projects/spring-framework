/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.messaging.converter;

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
		this.conversionService = new DefaultConversionService();
	}

	/**
	 * Create a new instance with the given {@link ConversionService}.
	 */
	public GenericMessageConverter(ConversionService conversionService) {
		Assert.notNull(conversionService, "ConversionService must not be null");
		this.conversionService = conversionService;
	}


	@Override
	public Object fromMessage(Message<?> message, Class<?> targetClass) {
		Object payload = message.getPayload();
		if (targetClass == null) {
			return payload;
		}
		if (payload != null && this.conversionService.canConvert(payload.getClass(), targetClass)) {
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
