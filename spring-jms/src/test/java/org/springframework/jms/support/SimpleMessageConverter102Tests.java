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

package org.springframework.jms.support;

import junit.framework.TestCase;
import org.easymock.ArgumentsMatcher;
import org.easymock.MockControl;
import org.springframework.jms.support.converter.SimpleMessageConverter102;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import java.util.Arrays;

/**
 * Unit tests for the {@link SimpleMessageConverter102} class.
 *
 * @author Juergen Hoeller
 * @author Rick Evans
 */
public final class SimpleMessageConverter102Tests extends TestCase {

	public void testByteArrayConversion102() throws JMSException {
		MockControl sessionControl = MockControl.createControl(Session.class);
		Session session = (Session) sessionControl.getMock();
		MockControl messageControl = MockControl.createControl(BytesMessage.class);
		BytesMessage message = (BytesMessage) messageControl.getMock();

		byte[] content = new byte[5000];

		session.createBytesMessage();
		sessionControl.setReturnValue(message, 1);
		message.writeBytes(content);
		messageControl.setVoidCallable(1);
		message.readBytes(new byte[SimpleMessageConverter102.BUFFER_SIZE]);
		messageControl.setMatcher(new ArgumentsMatcher() {
			@Override
			public boolean matches(Object[] arg0, Object[] arg1) {
				byte[] one = (byte[]) arg0[0];
				byte[] two = (byte[]) arg1[0];
				return Arrays.equals(one, two);
			}

			@Override
			public String toString(Object[] arg0) {
				return "bla";
			}
		});
		messageControl.setReturnValue(SimpleMessageConverter102.BUFFER_SIZE, 1);
		message.readBytes(new byte[SimpleMessageConverter102.BUFFER_SIZE]);
		messageControl.setReturnValue(5000 - SimpleMessageConverter102.BUFFER_SIZE, 1);
		sessionControl.replay();
		messageControl.replay();

		SimpleMessageConverter102 converter = new SimpleMessageConverter102();
		Message msg = converter.toMessage(content, session);
		assertEquals(content.length, ((byte[]) converter.fromMessage(msg)).length);

		sessionControl.verify();
		messageControl.verify();
	}

}
