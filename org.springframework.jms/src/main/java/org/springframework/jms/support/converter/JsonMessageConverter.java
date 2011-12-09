/*
 * Copyright 2002-2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.jms.support.converter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.transform.Result;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;
import org.springframework.oxm.Marshaller;

/**
 * Message converter that uses the Jackson library to convert messages to and
 * from JSON. Maps an object to a {@link BytesMessage}, or to a
 * {@link TextMessage} if the {@link #setTargetType marshalTo} is set to
 * {@link MessageType#TEXT}. Converts from a {@link TextMessage} or
 * {@link BytesMessage} to an object.
 * 
 * @author Mark Pollack
 * @author James Carr
 * @author Dave Syer
 */
public class JsonMessageConverter implements MessageConverter {

	public static final String DEFAULT_CHARSET = "UTF-8";

	public static final String DEFAULT_ENCODING_PROPERTY_NAME = "__Encoding__";

	private volatile String defaultCharset = DEFAULT_CHARSET;

	private MessageType targetType = MessageType.BYTES;

	private ObjectMapper jsonObjectMapper = new ObjectMapper();

	private JavaTypeMapper javaTypeMapper = new DefaultJavaTypeMapper();

	private String encodingPropertyName = DEFAULT_ENCODING_PROPERTY_NAME;

	public JsonMessageConverter() {
		super();
		initializeJsonObjectMapper();
	}

	/**
	 * Specify whether {@link #toMessage(Object, Session)} should marshal to a
	 * {@link BytesMessage} or a {@link TextMessage}.
	 * <p>
	 * The default is {@link MessageType#BYTES}, i.e. this converter marshals to
	 * a {@link BytesMessage}. Note that the default version of this converter
	 * supports {@link MessageType#BYTES} and {@link MessageType#TEXT} only.
	 * 
	 * @see MessageType#BYTES
	 * @see MessageType#TEXT
	 */
	public void setTargetType(MessageType targetType) {
		this.targetType = targetType;
	}

	/**
	 * A mapper to extract a Jackson {@link JavaType} from a message.
	 * 
	 * @param javaTypeMapper
	 *            the javaTypeMapper to set
	 */
	public void setJavaTypeMapper(JavaTypeMapper javaTypeMapper) {
		this.javaTypeMapper = javaTypeMapper;
	}

	/**
	 * Specify the default charset to use when converting from and from
	 * text-based Message body content. If not specified, the charset will be
	 * "UTF-8".
	 */
	public void setDefaultCharset(String defaultCharset) {
		this.defaultCharset = (defaultCharset != null) ? defaultCharset
				: DEFAULT_CHARSET;
	}

	/**
	 * Specify the name of the JMS message property that carries the encoding
	 * from bytes to String and back is BytesMessage is used during the
	 * conversion process.
	 * 
	 * @param encodingPropertyName
	 *            the name of the message property
	 */
	public void setEncodingPropertyName(String encodingPropertyName) {
		this.encodingPropertyName = encodingPropertyName;
	}

	/**
	 * The {@link ObjectMapper} to use instead of using the default. An
	 * alternative to injecting a mapper is to extend this class and override
	 * {@link #initializeJsonObjectMapper()}.
	 * 
	 * @param jsonObjectMapper
	 *            the object mapper to set
	 */
	public void setJsonObjectMapper(ObjectMapper jsonObjectMapper) {
		this.jsonObjectMapper = jsonObjectMapper;
	}

	/**
	 * Subclass and override to customize the mapper.
	 */
	protected void initializeJsonObjectMapper() {
		jsonObjectMapper
				.configure(
						DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES,
						false);
	}

	public Message toMessage(Object object, Session session)
			throws JMSException, MessageConversionException {
		Message message;
		try {
			switch (this.targetType) {
			case TEXT:
				message = mapToTextMessage(object, session,
						this.jsonObjectMapper);
				break;
			case BYTES:
				message = mapToBytesMessage(object, session,
						this.jsonObjectMapper);
				break;
			default:
				message = mapToMessage(object, session, this.jsonObjectMapper,
						this.targetType);
			}
		} catch (JsonMappingException ex) {
			throw new MessageConversionException("Could not map [" + object
					+ "]", ex);
		} catch (IOException ex) {
			throw new MessageConversionException("Could not map  [" + object
					+ "]", ex);
		}
		javaTypeMapper.fromJavaType(TypeFactory.type(object.getClass()),
				message);
		return message;
	}

	/**
	 * Map the given object to a {@link TextMessage}.
	 * @param object the object to be mapped
	 * @param session current JMS session
	 * @param jsonObjectMapper the mapper to use
	 * @return the resulting message
	 * @throws JMSException if thrown by JMS methods
	 * @throws IOException in case of I/O errors
	 * @throws JsonMappingException in case of Jackson mapping errors
	 * @see Session#createBytesMessage
	 * @see Marshaller#marshal(Object, Result)
	 */
	protected Message mapToTextMessage(Object object, Session session,
			ObjectMapper jsonObjectMapper) throws JsonMappingException,
			IOException, JMSException {
		StringWriter writer = new StringWriter();
		jsonObjectMapper.writeValue(writer, object);
		return session.createTextMessage(writer.toString());
	}

	/**
	 * Map the given object to a {@link BytesMessage}.
	 * @param object the object to be mapped
	 * @param session current JMS session
	 * @param jsonObjectMapper the mapper to use
	 * @return the resulting message
	 * @throws JMSException if thrown by JMS methods
	 * @throws IOException in case of I/O errors
	 * @throws JsonMappingException in case of Jackson mapping errors
	 * @see Session#createBytesMessage
	 * @see Marshaller#marshal(Object, Result)
	 */
	protected Message mapToBytesMessage(Object object, Session session,
			ObjectMapper jsonObjectMapper) throws JsonMappingException,
			IOException, JMSException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		OutputStreamWriter writer = new OutputStreamWriter(bos, defaultCharset);
		jsonObjectMapper.writeValue(writer, object);
		BytesMessage message = session.createBytesMessage();
		message.writeBytes(bos.toByteArray());
		message.setStringProperty(encodingPropertyName, defaultCharset);
		return message;
	}

	/**
	 * Template method that allows for custom message mapping. Invoked when
	 * {@link #setTargetType} is not {@link MessageType#TEXT} or
	 * {@link MessageType#BYTES}.
	 * <p>
	 * The default implementation throws an {@link IllegalArgumentException}.
	 * 
	 * @param object
	 *            the object to marshal
	 * @param session
	 *            the JMS session
	 * @param jsonObjectMapper
	 *            the mapper to use
	 * @param targetType
	 *            the target message type (other than TEXT or BYTES)
	 * @return the resulting message
	 * @throws JMSException
	 *             if thrown by JMS methods
	 * @throws IOException
	 *             in case of I/O errors
	 * @throws JsonMappingException
	 *             in case of Jackson mapping errors
	 */
	protected Message mapToMessage(Object object, Session session,
			ObjectMapper jsonObjectMapper, MessageType targetType)
			throws JsonMappingException, IOException, JMSException {
		throw new IllegalArgumentException("Unsupported message type ["
				+ targetType + "]. Cannot map to the specified message type.");
	}

	public Object fromMessage(Message message) throws JMSException,
			MessageConversionException {
		Object content = null;
		try {
			JavaType targetJavaType = javaTypeMapper.toJavaType(message);
			content = convertToObject(message, targetJavaType);
		} catch (JsonParseException e) {
			throw new MessageConversionException(
					"Failed to convert Message content", e);
		} catch (JsonMappingException e) {
			throw new MessageConversionException(
					"Failed to convert Message content", e);
		} catch (IOException e) {
			throw new MessageConversionException(
					"Failed to convert Message content", e);
		}
		return content;
	}

	/**
	 * Convenience method to dispatch to converters for individual message
	 * types.
	 */
	private Object convertToObject(Message message, JavaType targetJavaType)
			throws JMSException, JsonParseException, JsonMappingException,
			IOException {
		if (message instanceof TextMessage) {
			return convertFromTextMessage((TextMessage) message, targetJavaType);
		} else if (message instanceof BytesMessage) {
			return convertFromBytesMessage((BytesMessage) message,
					targetJavaType);
		} else {
			return convertFromMessage(message, targetJavaType);
		}
	}

	/**
	 * Convert a generic Message to a Java Object with the specified type.
	 * Default implementation throws IllegalArgumentException.
	 * 
	 * @param message
	 *            the input message
	 * @param targetJavaType
	 *            the target type
	 * @return the message converted to an object
	 * @throws JsonParseException
	 *             if thrown by Jackson
	 * @throws JsonMappingException
	 *             if thrown by Jackson
	 * @throws IOException
	 *             in case of I/O errors
	 * @throws JMSException
	 *             if thrown by JMS
	 */
	protected Object convertFromMessage(Message message, JavaType targetJavaType) {
		throw new IllegalArgumentException(
				"JsonMessageConverter only supports TextMessages and BytesMessages");
	}

	/**
	 * Convert a TextMessage to a Java Object with the specified type.
	 * 
	 * @param message
	 *            the input message
	 * @param targetJavaType
	 *            the target type
	 * @return the message converted to an object
	 * @throws JsonParseException
	 *             if thrown by Jackson
	 * @throws JsonMappingException
	 *             if thrown by Jackson
	 * @throws IOException
	 *             in case of I/O errors
	 * @throws JMSException
	 *             if thrown by JMS
	 */
	protected Object convertFromTextMessage(TextMessage message,
			JavaType targetJavaType) throws JsonParseException,
			JsonMappingException, IOException, JMSException {
		String body = message.getText();
		return jsonObjectMapper.readValue(body, targetJavaType);
	}

	/**
	 * Convert a BytesMessage to a Java Object with the specified type.
	 * 
	 * @param message
	 *            the input message
	 * @param targetJavaType
	 *            the target type
	 * @return the message converted to an object
	 * @throws JsonParseException
	 *             if thrown by Jackson
	 * @throws JsonMappingException
	 *             if thrown by Jackson
	 * @throws IOException
	 *             in case of I/O errors
	 * @throws JMSException
	 *             if thrown by JMS
	 */
	protected Object convertFromBytesMessage(BytesMessage message,
			JavaType targetJavaType) throws JsonParseException,
			JsonMappingException, IOException, JMSException {
		String encoding = defaultCharset;
		if (message.propertyExists(encodingPropertyName)) {
			encoding = message.getStringProperty(encodingPropertyName);
		}
		byte[] bytes = new byte[(int) message.getBodyLength()];
		message.readBytes(bytes);
		try {
			String body = new String(bytes, encoding);
			return jsonObjectMapper.readValue(body, targetJavaType);
		} catch (UnsupportedEncodingException e) {
			throw new MessageConversionException(
					"Cannot convert bytes to String", e);
		}
	}

}
