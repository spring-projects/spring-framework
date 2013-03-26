/*
 * Copyright 2002-2013 the original author or authors.
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

import java.io.ByteArrayInputStream;
import java.util.Random;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.jms.support.converter.SimpleMessageConverter102;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for the {@link SimpleMessageConverter102} class.
 *
 * @author Juergen Hoeller
 * @author Rick Evans
 */
public final class SimpleMessageConverter102Tests {

	@Test
	public void testByteArrayConversion102() throws JMSException {
		Session session = mock(Session.class);
		BytesMessage message = mock(BytesMessage.class);

		byte[] content = new byte[5000];
		new Random().nextBytes(content);

		final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content);
		given(session.createBytesMessage()).willReturn(message);
		given(message.readBytes((byte[]) anyObject())).willAnswer(new Answer<Integer>() {
			@Override
			public Integer answer(InvocationOnMock invocation) throws Throwable {
				return byteArrayInputStream.read((byte[])invocation.getArguments()[0]);
			}
		});

		SimpleMessageConverter102 converter = new SimpleMessageConverter102();
		Message msg = converter.toMessage(content, session);
		assertThat((byte[])converter.fromMessage(msg), equalTo(content));
	}

}
