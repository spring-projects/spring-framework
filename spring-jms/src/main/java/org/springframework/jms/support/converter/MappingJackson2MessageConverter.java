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

package org.springframework.jms.support.converter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Message converter that uses Jackson 2.x to convert messages to and from JSON.
 * Maps an object to a {@link BytesMessage}, or to a {@link TextMessage} if the
 * {@link #setTargetType targetType} is set to {@link MessageType#TEXT}.
 * Converts from a {@link TextMessage} or {@link BytesMessage} to an object.
 *
 * <p>Tested against Jackson 2.2; compatible with Jackson 2.0 and higher.
 *
 * @author Mark Pollack
 * @author Dave Syer
 * @author Juergen Hoeller
 * @since 3.1.4
 */
public class MappingJackson2MessageConverter implements MessageConverter, BeanClassLoaderAware {

	/**
	 * The default encoding used for writing to text messages: UTF-8.
	 */
	public static final String DEFAULT_ENCODING = "UTF-8";


	private ObjectMapper objectMapper = new ObjectMapper();

	private MessageType targetType = MessageType.BYTES;

	private String encoding = DEFAULT_ENCODING;

	private String encodingPropertyName;

	private String typeIdPropertyName;

	private Map<String, Class<?>> idClassMappings = new HashMap<String, Class<?>>();

	private Map<Class<?>, String> classIdMappings = new HashMap<Class<?>, String>();

	private ClassLoader beanClassLoader;


	/**
	 * Specify the {@link ObjectMapper} to use instead of using the default.
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "ObjectMapper must not be null");
		this.objectMapper = objectMapper;
	}

	/**
	 * Specify whether {@link #toMessage(Object, Session)} should marshal to a
	 * {@link BytesMessage} or a {@link TextMessage}.
	 * <p>The default is {@link MessageType#BYTES}, i.e. this converter marshals to
	 * a {@link BytesMessage}. Note that the default version of this converter
	 * supports {@link MessageType#BYTES} and {@link MessageType#TEXT} only.
	 * @see MessageType#BYTES
	 * @see MessageType#TEXT
	 */
	public void setTargetType(MessageType targetType) {
		Assert.notNull(targetType, "MessageType must not be null");
		this.targetType = targetType;
	}

	/**
	 * Specify the encoding to use when converting to and from text-based
	 * message body content. The default encoding will be "UTF-8".
	 * <p>When reading from a a text-based message, an encoding may have been
	 * suggested through a special JMS property which will then be preferred
	 * over the encoding set on this MessageConverter instance.
	 * @see #setEncodingPropertyName
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * Specify the name of the JMS message property that carries the encoding from
	 * bytes to String and back is BytesMessage is used during the conversion process.
	 * <p>Default is none. Setting this property is optional; if not set, UTF-8 will
	 * be used for decoding any incoming bytes message.
	 * @see #setEncoding
	 */
	public void setEncodingPropertyName(String encodingPropertyName) {
		this.encodingPropertyName = encodingPropertyName;
	}

	/**
	 * Specify the name of the JMS message property that carries the type id for the
	 * contained object: either a mapped id value or a raw Java class name.
	 * <p>Default is none. <b>NOTE: This property needs to be set in order to allow
	 * for converting from an incoming message to a Java object.</b>
	 * @see #setTypeIdMappings
	 */
	public void setTypeIdPropertyName(String typeIdPropertyName) {
		this.typeIdPropertyName = typeIdPropertyName;
	}

	/**
	 * Specify mappings from type ids to Java classes, if desired.
	 * This allows for synthetic ids in the type id message property,
	 * instead of transferring Java class names.
	 * <p>Default is no custom mappings, i.e. transferring raw Java class names.
	 * @param typeIdMappings a Map with type id values as keys and Java classes as values
	 */
	public void setTypeIdMappings(Map<String, Class<?>> typeIdMappings) {
		this.idClassMappings = new HashMap<String, Class<?>>();
		for (Map.Entry<String, Class<?>> entry : typeIdMappings.entrySet()) {
			String id = entry.getKey();
			Class<?> clazz = entry.getValue();
			this.idClassMappings.put(id, clazz);
			this.classIdMappings.put(clazz, id);
		}
	}

	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}


	public Message toMessage(Object object, Session session) throws JMSException, MessageConversionException {
		Message message;
		try {
			switch (this.targetType) {
				case TEXT:
					message = mapToTextMessage(object, session, this.objectMapper);
					break;
				case BYTES:
					message = mapToBytesMessage(object, session, this.objectMapper);
					break;
				default:
					message = mapToMessage(object, session, this.objectMapper, this.targetType);
			}
		}
		catch (IOException ex) {
			throw new MessageConversionException("Could not map JSON object [" + object + "]", ex);
		}
		setTypeIdOnMessage(object, message);
		return message;
	}

	public Object fromMessage(Message message) throws JMSException, MessageConversionException {
		try {
			JavaType targetJavaType = getJavaTypeForMessage(message);
			return convertToObject(message, targetJavaType);
		}
		catch (IOException ex) {
			throw new MessageConversionException("Failed to convert JSON message content", ex);
		}
	}


	/**
	 * Map the given object to a {@link TextMessage}.
	 * @param object the object to be mapped
	 * @param session current JMS session
	 * @param objectMapper the mapper to use
	 * @return the resulting message
	 * @throws JMSException if thrown by JMS methods
	 * @throws IOException in case of I/O errors
	 * @see Session#createBytesMessage
	 */
	protected TextMessage mapToTextMessage(Object object, Session session, ObjectMapper objectMapper)
			throws JMSException, IOException {

		StringWriter writer = new StringWriter();
		objectMapper.writeValue(writer, object);
		return session.createTextMessage(writer.toString());
	}

	/**
	 * Map the given object to a {@link BytesMessage}.
	 * @param object the object to be mapped
	 * @param session current JMS session
	 * @param objectMapper the mapper to use
	 * @return the resulting message
	 * @throws JMSException if thrown by JMS methods
	 * @throws IOException in case of I/O errors
	 * @see Session#createBytesMessage
	 */
	protected BytesMessage mapToBytesMessage(Object object, Session session, ObjectMapper objectMapper)
			throws JMSException, IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		OutputStreamWriter writer = new OutputStreamWriter(bos, this.encoding);
		objectMapper.writeValue(writer, object);

		BytesMessage message = session.createBytesMessage();
		message.writeBytes(bos.toByteArray());
		if (this.encodingPropertyName != null) {
			message.setStringProperty(this.encodingPropertyName, this.encoding);
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
	 * @param objectMapper the mapper to use
	 * @param targetType the target message type (other than TEXT or BYTES)
	 * @return the resulting message
	 * @throws JMSException if thrown by JMS methods
	 * @throws IOException in case of I/O errors
	 */
	protected Message mapToMessage(Object object, Session session, ObjectMapper objectMapper, MessageType targetType)
			throws JMSException, IOException {

		throw new IllegalArgumentException("Unsupported message type [" + targetType +
				"]. MappingJackson2MessageConverter by default only supports TextMessages and BytesMessages.");
	}

	/**
	 * Set a type id for the given payload object on the given JMS Message.
	 * <p>The default implementation consults the configured type id mapping and
	 * sets the resulting value (either a mapped id or the raw Java class name)
	 * into the configured type id message property.
	 * @param object the payload object to set a type id for
	 * @param message the JMS Message to set the type id on
	 * @throws JMSException if thrown by JMS methods
	 * @see #getJavaTypeForMessage(javax.jms.Message)
	 * @see #setTypeIdPropertyName(String)
	 * @see #setTypeIdMappings(java.util.Map)
	 */
	protected void setTypeIdOnMessage(Object object, Message message) throws JMSException {
		if (this.typeIdPropertyName != null) {
			String typeId = this.classIdMappings.get(object.getClass());
			if (typeId == null) {
				typeId = object.getClass().getName();
			}
			message.setStringProperty(this.typeIdPropertyName, typeId);
		}
	}


	/**
	 * Convenience method to dispatch to converters for individual message types.
	 */
	private Object convertToObject(Message message, JavaType targetJavaType) throws JMSException, IOException {
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
	protected Object convertFromTextMessage(TextMessage message, JavaType targetJavaType)
			throws JMSException, IOException {

		String body = message.getText();
		return this.objectMapper.readValue(body, targetJavaType);
	}

	/**
	 * Convert a BytesMessage to a Java Object with the specified type.
	 * @param message the input message
	 * @param targetJavaType the target type
	 * @return the message converted to an object
	 * @throws JMSException if thrown by JMS
	 * @throws IOException in case of I/O errors
	 */
	protected Object convertFromBytesMessage(BytesMessage message, JavaType targetJavaType)
			throws JMSException, IOException {

		String encoding = this.encoding;
		if (this.encodingPropertyName != null && message.propertyExists(this.encodingPropertyName)) {
			encoding = message.getStringProperty(this.encodingPropertyName);
		}
		byte[] bytes = new byte[(int) message.getBodyLength()];
		message.readBytes(bytes);
		try {
			String body = new String(bytes, encoding);
			return this.objectMapper.readValue(body, targetJavaType);
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
	protected Object convertFromMessage(Message message, JavaType targetJavaType)
			throws JMSException, IOException {

		throw new IllegalArgumentException("Unsupported message type [" + message.getClass() +
				"]. MappingJacksonMessageConverter by default only supports TextMessages and BytesMessages.");
	}

	/**
	 * Determine a Jackson JavaType for the given JMS Message,
	 * typically parsing a type id message property.
	 * <p>The default implementation parses the configured type id property name
	 * and consults the configured type id mapping. This can be overridden with
	 * a different strategy, e.g. doing some heuristics based on message origin.
	 * @param message the JMS Message to set the type id on
	 * @throws JMSException if thrown by JMS methods
	 * @see #setTypeIdOnMessage(Object, javax.jms.Message)
	 * @see #setTypeIdPropertyName(String)
	 * @see #setTypeIdMappings(java.util.Map)
	 */
	protected JavaType getJavaTypeForMessage(Message message) throws JMSException {
		String typeId = message.getStringProperty(this.typeIdPropertyName);
		if (typeId == null) {
			throw new MessageConversionException("Could not find type id property [" + this.typeIdPropertyName + "]");
		}
		Class<?> mappedClass = this.idClassMappings.get(typeId);
		if (mappedClass != null) {
			return this.objectMapper.getTypeFactory().constructType(mappedClass);
		}
		try {
			Class<?> typeClass = ClassUtils.forName(typeId, this.beanClassLoader);
			return this.objectMapper.getTypeFactory().constructType(typeClass);
		}
		catch (Throwable ex) {
			throw new MessageConversionException("Failed to resolve type id [" + typeId + "]", ex);
		}
	}

}
