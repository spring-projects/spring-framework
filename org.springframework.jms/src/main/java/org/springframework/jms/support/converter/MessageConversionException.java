/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.jms.support.converter;

import org.springframework.jms.JmsException;

/**
 * Thrown by {@link MessageConverter} implementations when the conversion
 * of an object to/from a {@link javax.jms.Message} fails.
 *
 * @author Mark Pollack
 * @since 1.1
 * @see MessageConverter
 */
public class MessageConversionException extends JmsException {

	/**
	 * Create a new MessageConversionException.
	 * @param msg the detail message
	 */
	public MessageConversionException(String msg) {
		super(msg);
	}

	/**
	 * Create a new MessageConversionException.
	 * @param msg the detail message
	 * @param cause the root cause (if any)
	 */
	public MessageConversionException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
