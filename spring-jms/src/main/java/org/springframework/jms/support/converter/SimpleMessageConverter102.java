/*
 * Copyright 2002-2012 the original author or authors.
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

import java.io.ByteArrayOutputStream;

import javax.jms.BytesMessage;
import javax.jms.JMSException;

/**
 * A subclass of {@link SimpleMessageConverter} for the JMS 1.0.2 specification,
 * not relying on JMS 1.1 methods like SimpleMessageConverter itself.
 * This class can be used for JMS 1.0.2 providers, offering the same functionality
 * as SimpleMessageConverter does for JMS 1.1 providers.
 *
 * <p>The only difference to the default SimpleMessageConverter is that BytesMessage
 * is handled differently: namely, without using the {@code getBodyLength()}
 * method which has been introduced in JMS 1.1 and is therefore not available on a
 * JMS 1.0.2 provider.
 *
 * @author Juergen Hoeller
 * @since 1.1.1
 * @see javax.jms.BytesMessage#getBodyLength()
 * @deprecated as of Spring 3.0, in favor of the JMS 1.1 based {@link SimpleMessageConverter}
 */
@Deprecated
public class SimpleMessageConverter102 extends SimpleMessageConverter {

	public static final int BUFFER_SIZE = 4096;


	/**
	 * Overrides superclass method to copy bytes from the message into a
	 * ByteArrayOutputStream, using a buffer, to avoid using the
	 * {@code getBodyLength()} method which has been introduced in
	 * JMS 1.1 and is therefore not available on a JMS 1.0.2 provider.
	 * @see javax.jms.BytesMessage#getBodyLength()
	 */
	protected byte[] extractByteArrayFromMessage(BytesMessage message) throws JMSException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFFER_SIZE);
		byte[] buffer = new byte[BUFFER_SIZE];
		int bufferCount = -1;
		while ((bufferCount = message.readBytes(buffer)) >= 0) {
			baos.write(buffer, 0, bufferCount);
			if (bufferCount < BUFFER_SIZE) {
				break;
			}
		}
		return baos.toByteArray();
	}

}
