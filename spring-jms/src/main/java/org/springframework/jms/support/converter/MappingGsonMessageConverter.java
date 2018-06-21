/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.jms.support.converter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import com.google.gson.Gson;

import org.springframework.util.Assert;


/**
 * Message converter that uses GSON to convert messages to and from JSON.
 * Maps an object to a {@link BytesMessage}, or to a {@link TextMessage} if the
 * {@link #setTargetType targetType} is set to {@link MessageType#TEXT}.
 * Converts from a {@link TextMessage} or {@link BytesMessage} to an object.
 *
 * @author Mark Pollack
 * @author Dave Syer
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Marten Deinum
 *
 * @since 5.1.0
 */
public class MappingGsonMessageConverter extends AbstractMappingMessageConverter {

	private Gson gson;

	public MappingGsonMessageConverter() {
		this.gson = new Gson();
	}

	/**
	 * Specify the {@link Gson} to use instead of using the default.
	 */
	public void setGson(Gson gson) {
		Assert.notNull(gson, "A Gson instance is required");
		this.gson = gson;
	}

	@Override
	public Object fromMessage(Message message) throws JMSException, MessageConversionException {
		try {
			Type targetJavaType = getTypeForMessage(message);
			return convertToObject(message, targetJavaType);
		}
		catch (IOException ex) {
			throw new MessageConversionException("Failed to convert JSON message content", ex);
		}
	}

	@Override
	public Message toMessage(Object object, Session session, Object conversionHint) throws JMSException, MessageConversionException {
		return this.toMessage(object, session);
	}

	@Override
	public Message toMessage(Object object, Session session)
			throws JMSException, MessageConversionException {

		Message message;
		try {
			switch (getTargetType()) {
				case TEXT:
					message = mapToTextMessage(object, session);
					break;
				case BYTES:
					message = mapToBytesMessage(object, session);
					break;
				default:
					message = mapToMessage(object, session, getTargetType());
			}
		}
		catch (IOException ex) {
			throw new MessageConversionException("Could not map JSON object [" + object + "]", ex);
		}
		setTypeIdOnMessage(object, message);
		return message;
	}


	/**
	 * Map the given object to a {@link TextMessage}.
	 * @param object the object to be mapped
	 * @param session current JMS session
	 * @return the resulting message
	 * @throws JMSException if thrown by JMS methods
	 * @since 4.3
	 * @see Session#createBytesMessage
	 */
	protected TextMessage mapToTextMessage(Object object, Session session)
			throws JMSException {
		return session.createTextMessage(this.gson.toJson(object));
	}

	/**
	 * Map the given object to a {@link BytesMessage}.
	 * @param object the object to be mapped
	 * @param session current JMS session
	 * @return the resulting message
	 * @throws JMSException if thrown by JMS methods
	 * @throws IOException in case of I/O errors
	 * @since 4.3
	 * @see Session#createBytesMessage
	 */
	protected BytesMessage mapToBytesMessage(Object object, Session session)
			throws JMSException, IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
		String encoding = getEncoding() != null ? getEncoding() : DEFAULT_ENCODING;
		OutputStreamWriter writer = new OutputStreamWriter(bos, encoding);

		this.gson.toJson(object, writer);

		BytesMessage message = session.createBytesMessage();
		message.writeBytes(bos.toByteArray());
		if (getEncodingPropertyName() != null) {
			message.setStringProperty(getEncodingPropertyName(), encoding);
		}
		return message;
	}

	/**
	 * Template method that allows for custom message mapping.
	 * Invoked when {@link #setTargetType} is not {@link MessageType#TEXT} or
	 * {@link MessageType#BYTES}.
	 * <p>The default implementation throws an {@link IllegalArgumentException}.
	 * @param object the object to marshal
	 * @param session the JMS Session
	 * @param targetType the target message type (other than TEXT or BYTES)
	 * @return the resulting message
	 * @throws JMSException if thrown by JMS methods
	 * @throws IOException in case of I/O errors
	 */
	protected Message mapToMessage(Object object, Session session, MessageType targetType)
			throws JMSException, IOException {

		throw new IllegalArgumentException("Unsupported message type [" + targetType +
				"]. MappingGsonMessageConverter by default only supports TextMessages and BytesMessages.");
	}

	/**
	 * Convenience method to dispatch to converters for individual message types.
	 */
	private Object convertToObject(Message message, Type targetJavaType) throws JMSException, IOException {
		if (message instanceof TextMessage) {
			return convertFromTextMessage((TextMessage) message, targetJavaType);
		}
		else if (message instanceof BytesMessage) {
			return convertFromBytesMessage((BytesMessage) message, targetJavaType);
		}
		else {
			return convertFromMessage(message, targetJavaType);
		}
	}

	/**
	 * Convert a TextMessage to a Java Object with the specified type.
	 * @param message the input message
	 * @param targetJavaType the target type
	 * @return the message converted to an object
	 * @throws JMSException if thrown by JMS
	 * @throws IOException in case of I/O errors
	 */
	protected Object convertFromTextMessage(TextMessage message, Type targetJavaType)
			throws JMSException, IOException {

		String body = message.getText();
		return this.gson.fromJson(body, targetJavaType);
	}

	/**
	 * Convert a BytesMessage to a Java Object with the specified type.
	 * @param message the input message
	 * @param targetJavaType the target type
	 * @return the message converted to an object
	 * @throws JMSException if thrown by JMS
	 * @throws IOException in case of I/O errors
	 */
	protected Object convertFromBytesMessage(BytesMessage message, Type targetJavaType)
			throws JMSException, IOException {

		String encoding = getEncoding();
		if (getEncodingPropertyName() != null && message.propertyExists(getEncodingPropertyName())) {
			encoding = message.getStringProperty(getEncodingPropertyName());
		}
		byte[] bytes = new byte[(int) message.getBodyLength()];
		message.readBytes(bytes);
		try {
			String body = new String(bytes, encoding != null ? encoding : DEFAULT_ENCODING);
			return this.gson.fromJson(body, targetJavaType);
		}
		catch (UnsupportedEncodingException ex) {
			throw new MessageConversionException("Cannot convert bytes to String", ex);
		}
	}

	/**
	 * Template method that allows for custom message mapping.
	 * Invoked when {@link #setTargetType} is not {@link MessageType#TEXT} or
	 * {@link MessageType#BYTES}.
	 * <p>The default implementation throws an {@link IllegalArgumentException}.
	 * @param message the input message
	 * @param targetJavaType the target type
	 * @return the message converted to an object
	 * @throws JMSException if thrown by JMS
	 * @throws IOException in case of I/O errors
	 */
	protected Object convertFromMessage(Message message, Type targetJavaType)
			throws JMSException, IOException {

		throw new IllegalArgumentException("Unsupported message type [" + message.getClass() +
				"]. MappingGsonMessageConverter by default only supports TextMessages and BytesMessages.");
	}

}
