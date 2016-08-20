/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.jms;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.TextMessage;

/**
 * Stub JMS Message implementation intended for testing purposes only.
 *
 * @author Mark Fisher
 * @since 4.1
 */
public class StubTextMessage implements TextMessage {

	private String messageId;

	private String text;

	private int deliveryMode = DEFAULT_DELIVERY_MODE;

	private Destination destination;

	private String correlationId;

	private Destination replyTo;

	private String type;

	private long deliveryTime;

	private long timestamp = 0L;

	private long expiration = 0L;

	private int priority = DEFAULT_PRIORITY;

	private boolean redelivered;

	private ConcurrentHashMap<String, Object> properties = new ConcurrentHashMap<>();


	public StubTextMessage() {
	}

	public StubTextMessage(String text) {
		this.text = text;
	}


	public String getText() throws JMSException {
		return this.text;
	}

	public void setText(String text) throws JMSException {
		this.text = text;
	}

	public void acknowledge() throws JMSException {
		throw new UnsupportedOperationException();
	}

	public void clearBody() throws JMSException {
		this.text = null;
	}

	public void clearProperties() throws JMSException {
		this.properties.clear();
	}

	public boolean getBooleanProperty(String name) throws JMSException {
		Object value = this.properties.get(name);
		return (value instanceof Boolean) ? ((Boolean) value).booleanValue() : false;
	}

	public byte getByteProperty(String name) throws JMSException {
		Object value = this.properties.get(name);
		return (value instanceof Byte) ? ((Byte) value).byteValue() : 0;
	}

	public double getDoubleProperty(String name) throws JMSException {
		Object value = this.properties.get(name);
		return (value instanceof Double) ? ((Double) value).doubleValue() : 0;
	}

	public float getFloatProperty(String name) throws JMSException {
		Object value = this.properties.get(name);
		return (value instanceof Float) ? ((Float) value).floatValue() : 0;
	}

	public int getIntProperty(String name) throws JMSException {
		Object value = this.properties.get(name);
		return (value instanceof Integer) ? ((Integer) value).intValue() : 0;
	}

	public String getJMSCorrelationID() throws JMSException {
		return this.correlationId;
	}

	public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
		return this.correlationId.getBytes();
	}

	public int getJMSDeliveryMode() throws JMSException {
		return this.deliveryMode;
	}

	public Destination getJMSDestination() throws JMSException {
		return this.destination;
	}

	public long getJMSExpiration() throws JMSException {
		return this.expiration;
	}

	public String getJMSMessageID() throws JMSException {
		return this.messageId;
	}

	public int getJMSPriority() throws JMSException {
		return this.priority;
	}

	public boolean getJMSRedelivered() throws JMSException {
		return this.redelivered;
	}

	public Destination getJMSReplyTo() throws JMSException {
		return this.replyTo;
	}

	public long getJMSTimestamp() throws JMSException {
		return this.timestamp;
	}

	public String getJMSType() throws JMSException {
		return this.type;
	}

	@Override
	public long getJMSDeliveryTime() throws JMSException {
		return this.deliveryTime;
	}

	public long getLongProperty(String name) throws JMSException {
		Object value = this.properties.get(name);
		return (value instanceof Long) ? ((Long) value).longValue() : 0;
	}

	public Object getObjectProperty(String name) throws JMSException {
		return this.properties.get(name);
	}

	public Enumeration<?> getPropertyNames() throws JMSException {
		return this.properties.keys();
	}

	public short getShortProperty(String name) throws JMSException {
		Object value = this.properties.get(name);
		return (value instanceof Short) ? ((Short) value).shortValue() : 0;
	}

	public String getStringProperty(String name) throws JMSException {
		Object value = this.properties.get(name);
		return (value instanceof String) ? (String) value : null;
	}

	public boolean propertyExists(String name) throws JMSException {
		return this.properties.containsKey(name);
	}

	public void setBooleanProperty(String name, boolean value) throws JMSException {
		this.properties.put(name, value);
	}

	public void setByteProperty(String name, byte value) throws JMSException {
		this.properties.put(name, value);
	}

	public void setDoubleProperty(String name, double value) throws JMSException {
		this.properties.put(name, value);
	}

	public void setFloatProperty(String name, float value) throws JMSException {
		this.properties.put(name, value);
	}

	public void setIntProperty(String name, int value) throws JMSException {
		this.properties.put(name, value);
	}

	public void setJMSCorrelationID(String correlationId) throws JMSException {
		this.correlationId = correlationId;
	}

	public void setJMSCorrelationIDAsBytes(byte[] correlationID) throws JMSException {
		this.correlationId = new String(correlationID);
	}

	public void setJMSDeliveryMode(int deliveryMode) throws JMSException {
		this.deliveryMode = deliveryMode;
	}

	public void setJMSDestination(Destination destination) throws JMSException {
		this.destination = destination;
	}

	public void setJMSExpiration(long expiration) throws JMSException {
		this.expiration = expiration;
	}

	public void setJMSMessageID(String id) throws JMSException {
		this.messageId = id;
	}

	public void setJMSPriority(int priority) throws JMSException {
		this.priority = priority;
	}

	public void setJMSRedelivered(boolean redelivered) throws JMSException {
		this.redelivered = redelivered;
	}

	public void setJMSReplyTo(Destination replyTo) throws JMSException {
		this.replyTo = replyTo;
	}

	public void setJMSTimestamp(long timestamp) throws JMSException {
		this.timestamp = timestamp;
	}

	public void setJMSType(String type) throws JMSException {
		this.type = type;
	}

	@Override
	public void setJMSDeliveryTime(long deliveryTime) throws JMSException {
		this.deliveryTime = deliveryTime;
	}

	public void setLongProperty(String name, long value) throws JMSException {
		this.properties.put(name, value);
	}

	public void setObjectProperty(String name, Object value) throws JMSException {
		this.properties.put(name, value);
	}

	public void setShortProperty(String name, short value) throws JMSException {
		this.properties.put(name, value);
	}

	public void setStringProperty(String name, String value) throws JMSException {
		this.properties.put(name, value);
	}

	@Override
	public <T> T getBody(Class<T> c) throws JMSException {
		return null;
	}

	@Override
	public boolean isBodyAssignableTo(Class c) throws JMSException {
		return false;
	}

}
