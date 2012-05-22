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

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.beans.ITestBean;
import org.springframework.beans.TestBean;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.SimpleMessageConverter;

/**
 * @author Juergen Hoeller
 */
public class JmsInvokerTests extends TestCase {

	private MockControl connectionFactoryControl;
	private QueueConnectionFactory mockConnectionFactory;

	private MockControl connectionControl;
	private QueueConnection mockConnection;

	private MockControl sessionControl;
	private QueueSession mockSession;

	private MockControl queueControl;
	private Queue mockQueue;


	protected void setUp() throws Exception {
		connectionFactoryControl = MockControl.createControl(QueueConnectionFactory.class);
		mockConnectionFactory = (QueueConnectionFactory) connectionFactoryControl.getMock();

		connectionControl = MockControl.createControl(QueueConnection.class);
		mockConnection = (QueueConnection) connectionControl.getMock();

		sessionControl = MockControl.createControl(QueueSession.class);
		mockSession = (QueueSession) sessionControl.getMock();

		queueControl = MockControl.createControl(Queue.class);
		mockQueue = (Queue) queueControl.getMock();

		mockConnectionFactory.createConnection();
		connectionFactoryControl.setReturnValue(mockConnection, 8);

		mockConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		connectionControl.setReturnValue(mockSession, 8);

		mockConnection.start();
		connectionControl.setVoidCallable(8);

		connectionFactoryControl.replay();
		connectionControl.replay();
	}


	public void testJmsInvokerProxyFactoryBeanAndServiceExporter() throws Throwable {
		sessionControl.replay();

		doTestJmsInvokerProxyFactoryBeanAndServiceExporter(false);
	}

	public void testJmsInvokerProxyFactoryBeanAndServiceExporterWithDynamicQueue() throws Throwable {
		mockSession.createQueue("myQueue");
		sessionControl.setReturnValue(mockQueue, 8);
		sessionControl.replay();

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
			protected Message doExecuteRequest(Session session, Queue queue, Message requestMessage) throws JMSException {
				MockControl exporterSessionControl = MockControl.createControl(Session.class);
				Session mockExporterSession = (Session) exporterSessionControl.getMock();
				ResponseStoringProducer mockProducer = new ResponseStoringProducer();
				mockExporterSession.createProducer(requestMessage.getJMSReplyTo());
				exporterSessionControl.setReturnValue(mockProducer);
				exporterSessionControl.replay();
				exporter.onMessage(requestMessage, mockExporterSession);
				exporterSessionControl.verify();
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

		connectionFactoryControl.verify();
		connectionControl.verify();
		sessionControl.verify();
	}


	private static class ResponseStoringProducer implements MessageProducer {

		public Message response;

		public boolean closed = false;

		public void setDisableMessageID(boolean b) throws JMSException {
		}

		public boolean getDisableMessageID() throws JMSException {
			return false;
		}

		public void setDisableMessageTimestamp(boolean b) throws JMSException {
		}

		public boolean getDisableMessageTimestamp() throws JMSException {
			return false;
		}

		public void setDeliveryMode(int i) throws JMSException {
		}

		public int getDeliveryMode() throws JMSException {
			return 0;
		}

		public void setPriority(int i) throws JMSException {
		}

		public int getPriority() throws JMSException {
			return 0;
		}

		public void setTimeToLive(long l) throws JMSException {
		}

		public long getTimeToLive() throws JMSException {
			return 0;
		}

		public Destination getDestination() throws JMSException {
			return null;
		}

		public void close() throws JMSException {
			this.closed = true;
		}

		public void send(Message message) throws JMSException {
			this.response = message;
		}

		public void send(Message message, int i, int i1, long l) throws JMSException {
		}

		public void send(Destination destination, Message message) throws JMSException {
		}

		public void send(Destination destination, Message message, int i, int i1, long l) throws JMSException {
		}
	}


	private static class MockObjectMessage implements ObjectMessage {

		private Serializable serializable;

		private Destination replyTo;

		public MockObjectMessage(Serializable serializable) {
			this.serializable = serializable;
		}

		public void setObject(Serializable serializable) throws JMSException {
			this.serializable = serializable;
		}

		public Serializable getObject() throws JMSException {
			return serializable;
		}

		public String getJMSMessageID() throws JMSException {
			return null;
		}

		public void setJMSMessageID(String string) throws JMSException {
		}

		public long getJMSTimestamp() throws JMSException {
			return 0;
		}

		public void setJMSTimestamp(long l) throws JMSException {
		}

		public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
			return new byte[0];
		}

		public void setJMSCorrelationIDAsBytes(byte[] bytes) throws JMSException {
		}

		public void setJMSCorrelationID(String string) throws JMSException {
		}

		public String getJMSCorrelationID() throws JMSException {
			return null;
		}

		public Destination getJMSReplyTo() throws JMSException {
			return replyTo;
		}

		public void setJMSReplyTo(Destination destination) throws JMSException {
			this.replyTo = destination;
		}

		public Destination getJMSDestination() throws JMSException {
			return null;
		}

		public void setJMSDestination(Destination destination) throws JMSException {
		}

		public int getJMSDeliveryMode() throws JMSException {
			return 0;
		}

		public void setJMSDeliveryMode(int i) throws JMSException {
		}

		public boolean getJMSRedelivered() throws JMSException {
			return false;
		}

		public void setJMSRedelivered(boolean b) throws JMSException {
		}

		public String getJMSType() throws JMSException {
			return null;
		}

		public void setJMSType(String string) throws JMSException {
		}

		public long getJMSExpiration() throws JMSException {
			return 0;
		}

		public void setJMSExpiration(long l) throws JMSException {
		}

		public int getJMSPriority() throws JMSException {
			return 0;
		}

		public void setJMSPriority(int i) throws JMSException {
		}

		public void clearProperties() throws JMSException {
		}

		public boolean propertyExists(String string) throws JMSException {
			return false;
		}

		public boolean getBooleanProperty(String string) throws JMSException {
			return false;
		}

		public byte getByteProperty(String string) throws JMSException {
			return 0;
		}

		public short getShortProperty(String string) throws JMSException {
			return 0;
		}

		public int getIntProperty(String string) throws JMSException {
			return 0;
		}

		public long getLongProperty(String string) throws JMSException {
			return 0;
		}

		public float getFloatProperty(String string) throws JMSException {
			return 0;
		}

		public double getDoubleProperty(String string) throws JMSException {
			return 0;
		}

		public String getStringProperty(String string) throws JMSException {
			return null;
		}

		public Object getObjectProperty(String string) throws JMSException {
			return null;
		}

		public Enumeration getPropertyNames() throws JMSException {
			return null;
		}

		public void setBooleanProperty(String string, boolean b) throws JMSException {
		}

		public void setByteProperty(String string, byte b) throws JMSException {
		}

		public void setShortProperty(String string, short i) throws JMSException {
		}

		public void setIntProperty(String string, int i) throws JMSException {
		}

		public void setLongProperty(String string, long l) throws JMSException {
		}

		public void setFloatProperty(String string, float v) throws JMSException {
		}

		public void setDoubleProperty(String string, double v) throws JMSException {
		}

		public void setStringProperty(String string, String string1) throws JMSException {
		}

		public void setObjectProperty(String string, Object object) throws JMSException {
		}

		public void acknowledge() throws JMSException {
		}

		public void clearBody() throws JMSException {
		}
	}


	private static class MockSimpleMessageConverter extends SimpleMessageConverter {

		public Message toMessage(Object object, Session session) throws JMSException, MessageConversionException {
			return new MockObjectMessage((Serializable) object);
		}
	}

}
