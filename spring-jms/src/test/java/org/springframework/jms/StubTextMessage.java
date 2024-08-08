/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;

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


	@Override
	public String getText() {
		return this.text;
	}

	@Override
	public void setText(String text) {
		this.text = text;
	}

	@Override
	public void acknowledge() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearBody() {
		this.text = null;
	}

	@Override
	public void clearProperties() {
		this.properties.clear();
	}

	@Override
	public boolean getBooleanProperty(String name) {
		Object value = this.properties.get(name);
		return (value instanceof Boolean b) ? b : false;
	}

	@Override
	public byte getByteProperty(String name) {
		Object value = this.properties.get(name);
		return (value instanceof Byte b) ? b : 0;
	}

	@Override
	public double getDoubleProperty(String name) {
		Object value = this.properties.get(name);
		return (value instanceof Double d) ? d : 0;
	}

	@Override
	public float getFloatProperty(String name) {
		Object value = this.properties.get(name);
		return (value instanceof Float f) ? f : 0;
	}

	@Override
	public int getIntProperty(String name) {
		Object value = this.properties.get(name);
		return (value instanceof Integer i) ? i : 0;
	}

	@Override
	public String getJMSCorrelationID() throws JMSException {
		return this.correlationId;
	}

	@Override
	public byte[] getJMSCorrelationIDAsBytes() {
		return this.correlationId.getBytes();
	}

	@Override
	public int getJMSDeliveryMode() throws JMSException {
		return this.deliveryMode;
	}

	@Override
	public Destination getJMSDestination() throws JMSException {
		return this.destination;
	}

	@Override
	public long getJMSExpiration() throws JMSException {
		return this.expiration;
	}

	@Override
	public String getJMSMessageID() throws JMSException {
		return this.messageId;
	}

	@Override
	public int getJMSPriority() throws JMSException {
		return this.priority;
	}

	@Override
	public boolean getJMSRedelivered() throws JMSException {
		return this.redelivered;
	}

	@Override
	public Destination getJMSReplyTo() throws JMSException {
		return this.replyTo;
	}

	@Override
	public long getJMSTimestamp() throws JMSException {
		return this.timestamp;
	}

	@Override
	public String getJMSType() throws JMSException {
		return this.type;
	}

	@Override
	public long getJMSDeliveryTime() {
		return this.deliveryTime;
	}

	@Override
	public long getLongProperty(String name) {
		Object value = this.properties.get(name);
		return (value instanceof Long l) ? l : 0;
	}

	@Override
	public Object getObjectProperty(String name) throws JMSException {
		return this.properties.get(name);
	}

	@Override
	public Enumeration<?> getPropertyNames() {
		return this.properties.keys();
	}

	@Override
	public short getShortProperty(String name) {
		Object value = this.properties.get(name);
		return (value instanceof Short s) ? s : 0;
	}

	@Override
	public String getStringProperty(String name) {
		Object value = this.properties.get(name);
		return (value instanceof String text) ? text : null;
	}

	@Override
	public boolean propertyExists(String name) {
		return this.properties.containsKey(name);
	}

	@Override
	public void setBooleanProperty(String name, boolean value) {
		this.properties.put(name, value);
	}

	@Override
	public void setByteProperty(String name, byte value) {
		this.properties.put(name, value);
	}

	@Override
	public void setDoubleProperty(String name, double value) {
		this.properties.put(name, value);
	}

	@Override
	public void setFloatProperty(String name, float value) {
		this.properties.put(name, value);
	}

	@Override
	public void setIntProperty(String name, int value) {
		this.properties.put(name, value);
	}

	@Override
	public void setJMSCorrelationID(String correlationId) throws JMSException {
		this.correlationId = correlationId;
	}

	@Override
	public void setJMSCorrelationIDAsBytes(byte[] correlationID) {
		this.correlationId = new String(correlationID);
	}

	@Override
	public void setJMSDeliveryMode(int deliveryMode) {
		this.deliveryMode = deliveryMode;
	}

	@Override
	public void setJMSDestination(Destination destination) {
		this.destination = destination;
	}

	@Override
	public void setJMSExpiration(long expiration) {
		this.expiration = expiration;
	}

	@Override
	public void setJMSMessageID(String id) {
		this.messageId = id;
	}

	@Override
	public void setJMSPriority(int priority) {
		this.priority = priority;
	}

	@Override
	public void setJMSRedelivered(boolean redelivered) {
		this.redelivered = redelivered;
	}

	@Override
	public void setJMSReplyTo(Destination replyTo) throws JMSException {
		this.replyTo = replyTo;
	}

	@Override
	public void setJMSTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public void setJMSType(String type) throws JMSException {
		this.type = type;
	}

	@Override
	public void setJMSDeliveryTime(long deliveryTime) {
		this.deliveryTime = deliveryTime;
	}

	@Override
	public void setLongProperty(String name, long value) {
		this.properties.put(name, value);
	}

	@Override
	public void setObjectProperty(String name, Object value) throws JMSException {
		this.properties.put(name, value);
	}

	@Override
	public void setShortProperty(String name, short value) {
		this.properties.put(name, value);
	}

	@Override
	public void setStringProperty(String name, String value) {
		this.properties.put(name, value);
	}

	@Override
	public <T> T getBody(Class<T> c) {
		return null;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public boolean isBodyAssignableTo(Class c) {
		return false;
	}

}
