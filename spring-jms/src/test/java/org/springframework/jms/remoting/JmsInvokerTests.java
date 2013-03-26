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

package org.springframework.jms.remoting;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Enumeration;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;

import org.junit.Before;
import org.junit.Test;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.TestBean;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Juergen Hoeller
 */
public class JmsInvokerTests {

	private QueueConnectionFactory mockConnectionFactory;

	private QueueConnection mockConnection;

	private QueueSession mockSession;

	private Queue mockQueue;


	@Before
	public void setUpMocks() throws Exception {
		mockConnectionFactory = mock(QueueConnectionFactory.class);
		mockConnection = mock(QueueConnection.class);
		mockSession = mock(QueueSession.class);
		mockQueue = mock(Queue.class);

		given(mockConnectionFactory.createConnection()).willReturn(mockConnection);
		given(mockConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)).willReturn(mockSession);
	}


	@Test
	public void testJmsInvokerProxyFactoryBeanAndServiceExporter() throws Throwable {
		doTestJmsInvokerProxyFactoryBeanAndServiceExporter(false);
	}

	@Test
	public void testJmsInvokerProxyFactoryBeanAndServiceExporterWithDynamicQueue() throws Throwable {
		given(mockSession.createQueue("myQueue")).willReturn(mockQueue);
		doTestJmsInvokerProxyFactoryBeanAndServiceExporter(true);
	}

	private void doTestJmsInvokerProxyFactoryBeanAndServiceExporter(boolean dynamicQueue) throws Throwable {
		TestBean target = new TestBean("myname", 99);

		final JmsInvokerServiceExporter exporter = new JmsInvokerServiceExporter();
		exporter.setServiceInterface(ITestBean.class);
		exporter.setService(target);
		exporter.setMessageConverter(new MockSimpleMessageConverter());
		exporter.afterPropertiesSet();

		JmsInvokerProxyFactoryBean pfb = new JmsInvokerProxyFactoryBean() {
			@Override
			protected Message doExecuteRequest(Session session, Queue queue, Message requestMessage) throws JMSException {
				Session mockExporterSession = mock(Session.class);
				ResponseStoringProducer mockProducer = new ResponseStoringProducer();
				given(mockExporterSession.createProducer(requestMessage.getJMSReplyTo())).willReturn(mockProducer);
				exporter.onMessage(requestMessage, mockExporterSession);
				assertTrue(mockProducer.closed);
				return mockProducer.response;
			}
		};
		pfb.setServiceInterface(ITestBean.class);
		pfb.setConnectionFactory(this.mockConnectionFactory);
		if (dynamicQueue) {
			pfb.setQueueName("myQueue");
		}
		else {
			pfb.setQueue(this.mockQueue);
		}
		pfb.setMessageConverter(new MockSimpleMessageConverter());

		pfb.afterPropertiesSet();
		ITestBean proxy = (ITestBean) pfb.getObject();

		assertEquals("myname", proxy.getName());
		assertEquals(99, proxy.getAge());
		proxy.setAge(50);
		assertEquals(50, proxy.getAge());
		proxy.setStringArray(new String[] {"str1", "str2"});
		assertTrue(Arrays.equals(new String[] {"str1", "str2"}, proxy.getStringArray()));

		try {
			proxy.exceptional(new IllegalStateException());
			fail("Should have thrown IllegalStateException");
		}
		catch (IllegalStateException ex) {
			// expected
		}
		try {
			proxy.exceptional(new IllegalAccessException());
			fail("Should have thrown IllegalAccessException");
		}
		catch (IllegalAccessException ex) {
			// expected
		}
	}


	private static class ResponseStoringProducer implements MessageProducer {

		public Message response;

		public boolean closed = false;

		@Override
		public void setDisableMessageID(boolean b) throws JMSException {
		}

		@Override
		public boolean getDisableMessageID() throws JMSException {
			return false;
		}

		@Override
		public void setDisableMessageTimestamp(boolean b) throws JMSException {
		}

		@Override
		public boolean getDisableMessageTimestamp() throws JMSException {
			return false;
		}

		@Override
		public void setDeliveryMode(int i) throws JMSException {
		}

		@Override
		public int getDeliveryMode() throws JMSException {
			return 0;
		}

		@Override
		public void setPriority(int i) throws JMSException {
		}

		@Override
		public int getPriority() throws JMSException {
			return 0;
		}

		@Override
		public void setTimeToLive(long l) throws JMSException {
		}

		@Override
		public long getTimeToLive() throws JMSException {
			return 0;
		}

		@Override
		public Destination getDestination() throws JMSException {
			return null;
		}

		@Override
		public void close() throws JMSException {
			this.closed = true;
		}

		@Override
		public void send(Message message) throws JMSException {
			this.response = message;
		}

		@Override
		public void send(Message message, int i, int i1, long l) throws JMSException {
		}

		@Override
		public void send(Destination destination, Message message) throws JMSException {
		}

		@Override
		public void send(Destination destination, Message message, int i, int i1, long l) throws JMSException {
		}
	}


	private static class MockObjectMessage implements ObjectMessage {

		private Serializable serializable;

		private Destination replyTo;

		public MockObjectMessage(Serializable serializable) {
			this.serializable = serializable;
		}

		@Override
		public void setObject(Serializable serializable) throws JMSException {
			this.serializable = serializable;
		}

		@Override
		public Serializable getObject() throws JMSException {
			return serializable;
		}

		@Override
		public String getJMSMessageID() throws JMSException {
			return null;
		}

		@Override
		public void setJMSMessageID(String string) throws JMSException {
		}

		@Override
		public long getJMSTimestamp() throws JMSException {
			return 0;
		}

		@Override
		public void setJMSTimestamp(long l) throws JMSException {
		}

		@Override
		public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
			return new byte[0];
		}

		@Override
		public void setJMSCorrelationIDAsBytes(byte[] bytes) throws JMSException {
		}

		@Override
		public void setJMSCorrelationID(String string) throws JMSException {
		}

		@Override
		public String getJMSCorrelationID() throws JMSException {
			return null;
		}

		@Override
		public Destination getJMSReplyTo() throws JMSException {
			return replyTo;
		}

		@Override
		public void setJMSReplyTo(Destination destination) throws JMSException {
			this.replyTo = destination;
		}

		@Override
		public Destination getJMSDestination() throws JMSException {
			return null;
		}

		@Override
		public void setJMSDestination(Destination destination) throws JMSException {
		}

		@Override
		public int getJMSDeliveryMode() throws JMSException {
			return 0;
		}

		@Override
		public void setJMSDeliveryMode(int i) throws JMSException {
		}

		@Override
		public boolean getJMSRedelivered() throws JMSException {
			return false;
		}

		@Override
		public void setJMSRedelivered(boolean b) throws JMSException {
		}

		@Override
		public String getJMSType() throws JMSException {
			return null;
		}

		@Override
		public void setJMSType(String string) throws JMSException {
		}

		@Override
		public long getJMSExpiration() throws JMSException {
			return 0;
		}

		@Override
		public void setJMSExpiration(long l) throws JMSException {
		}

		@Override
		public int getJMSPriority() throws JMSException {
			return 0;
		}

		@Override
		public void setJMSPriority(int i) throws JMSException {
		}

		@Override
		public void clearProperties() throws JMSException {
		}

		@Override
		public boolean propertyExists(String string) throws JMSException {
			return false;
		}

		@Override
		public boolean getBooleanProperty(String string) throws JMSException {
			return false;
		}

		@Override
		public byte getByteProperty(String string) throws JMSException {
			return 0;
		}

		@Override
		public short getShortProperty(String string) throws JMSException {
			return 0;
		}

		@Override
		public int getIntProperty(String string) throws JMSException {
			return 0;
		}

		@Override
		public long getLongProperty(String string) throws JMSException {
			return 0;
		}

		@Override
		public float getFloatProperty(String string) throws JMSException {
			return 0;
		}

		@Override
		public double getDoubleProperty(String string) throws JMSException {
			return 0;
		}

		@Override
		public String getStringProperty(String string) throws JMSException {
			return null;
		}

		@Override
		public Object getObjectProperty(String string) throws JMSException {
			return null;
		}

		@Override
		public Enumeration getPropertyNames() throws JMSException {
			return null;
		}

		@Override
		public void setBooleanProperty(String string, boolean b) throws JMSException {
		}

		@Override
		public void setByteProperty(String string, byte b) throws JMSException {
		}

		@Override
		public void setShortProperty(String string, short i) throws JMSException {
		}

		@Override
		public void setIntProperty(String string, int i) throws JMSException {
		}

		@Override
		public void setLongProperty(String string, long l) throws JMSException {
		}

		@Override
		public void setFloatProperty(String string, float v) throws JMSException {
		}

		@Override
		public void setDoubleProperty(String string, double v) throws JMSException {
		}

		@Override
		public void setStringProperty(String string, String string1) throws JMSException {
		}

		@Override
		public void setObjectProperty(String string, Object object) throws JMSException {
		}

		@Override
		public void acknowledge() throws JMSException {
		}

		@Override
		public void clearBody() throws JMSException {
		}
	}


	private static class MockSimpleMessageConverter extends SimpleMessageConverter {

		@Override
		public Message toMessage(Object object, Session session) throws JMSException, MessageConversionException {
			return new MockObjectMessage((Serializable) object);
		}
	}

}
