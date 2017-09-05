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

package org.springframework.jms.support.converter;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.springframework.util.ObjectUtils;

/**
 * A simple message converter which is able to handle TextMessages, BytesMessages,
 * MapMessages, and ObjectMessages. Used as default conversion strategy
 * by {@link org.springframework.jms.core.JmsTemplate}, for
 * {@code convertAndSend} and {@code receiveAndConvert} operations.
 *
 * <p>Converts a String to a {@link javax.jms.TextMessage}, a byte array to a
 * {@link javax.jms.BytesMessage}, a Map to a {@link javax.jms.MapMessage}, and
 * a Serializable object to a {@link javax.jms.ObjectMessage} (or vice versa).
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see org.springframework.jms.core.JmsTemplate#convertAndSend
 * @see org.springframework.jms.core.JmsTemplate#receiveAndConvert
 */
public class SimpleMessageConverter implements MessageConverter {

	/**
	 * This implementation creates a TextMessage for a String, a
	 * BytesMessage for a byte array, a MapMessage for a Map,
	 * and an ObjectMessage for a Serializable object.
	 * @see #createMessageForString
	 * @see #createMessageForByteArray
	 * @see #createMessageForMap
	 * @see #createMessageForSerializable
	 */
	@Override
	public Message toMessage(Object object, Session session) throws JMSException, MessageConversionException {
		if (object instanceof Message) {
			return (Message) object;
		}
		else if (object instanceof String) {
			return createMessageForString((String) object, session);
		}
		else if (object instanceof byte[]) {
			return createMessageForByteArray((byte[]) object, session);
		}
		else if (object instanceof Map) {
			return createMessageForMap((Map<? ,?>) object, session);
		}
		else if (object instanceof Serializable) {
			return createMessageForSerializable(((Serializable) object), session);
		}
		else {
			throw new MessageConversionException("Cannot convert object of type [" +
					ObjectUtils.nullSafeClassName(object) + "] to JMS message. Supported message " +
					"payloads are: String, byte array, Map<String,?>, Serializable object.");
		}
	}

	/**
	 * This implementation converts a TextMessage back to a String, a
	 * ByteMessage back to a byte array, a MapMessage back to a Map,
	 * and an ObjectMessage back to a Serializable object. Returns
	 * the plain Message object in case of an unknown message type.
	 * @see #extractStringFromMessage
	 * @see #extractByteArrayFromMessage
	 * @see #extractMapFromMessage
	 * @see #extractSerializableFromMessage
	 */
	@Override
	public Object fromMessage(Message message) throws JMSException, MessageConversionException {
		if (message instanceof TextMessage) {
			return extractStringFromMessage((TextMessage) message);
		}
		else if (message instanceof BytesMessage) {
			return extractByteArrayFromMessage((BytesMessage) message);
		}
		else if (message instanceof MapMessage) {
			return extractMapFromMessage((MapMessage) message);
		}
		else if (message instanceof ObjectMessage) {
			return extractSerializableFromMessage((ObjectMessage) message);
		}
		else {
			return message;
		}
	}


	/**
	 * Create a JMS TextMessage for the given String.
	 * @param text the String to convert
	 * @param session current JMS session
	 * @return the resulting message
	 * @throws JMSException if thrown by JMS methods
	 * @see javax.jms.Session#createTextMessage
	 */
	protected TextMessage createMessageForString(String text, Session session) throws JMSException {
		return session.createTextMessage(text);
	}

	/**
	 * Create a JMS BytesMessage for the given byte array.
	 * @param bytes the byyte array to convert
	 * @param session current JMS session
	 * @return the resulting message
	 * @throws JMSException if thrown by JMS methods
	 * @see javax.jms.Session#createBytesMessage
	 */
	protected BytesMessage createMessageForByteArray(byte[] bytes, Session session) throws JMSException {
		BytesMessage message = session.createBytesMessage();
		message.writeBytes(bytes);
		return message;
	}

	/**
	 * Create a JMS MapMessage for the given Map.
	 * @param map the Map to convert
	 * @param session current JMS session
	 * @return the resulting message
	 * @throws JMSException if thrown by JMS methods
	 * @see javax.jms.Session#createMapMessage
	 */
	protected MapMessage createMessageForMap(Map<?, ?> map, Session session) throws JMSException {
		MapMessage message = session.createMapMessage();
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			if (!(entry.getKey() instanceof String)) {
				throw new MessageConversionException("Cannot convert non-String key of type [" +
						ObjectUtils.nullSafeClassName(entry.getKey()) + "] to JMS MapMessage entry");
			}
			message.setObject((String) entry.getKey(), entry.getValue());
		}
		return message;
	}

	/**
	 * Create a JMS ObjectMessage for the given Serializable object.
	 * @param object the Serializable object to convert
	 * @param session current JMS session
	 * @return the resulting message
	 * @throws JMSException if thrown by JMS methods
	 * @see javax.jms.Session#createObjectMessage
	 */
	protected ObjectMessage createMessageForSerializable(Serializable object, Session session) throws JMSException {
		return session.createObjectMessage(object);
	}


	/**
	 * Extract a String from the given TextMessage.
	 * @param message the message to convert
	 * @return the resulting String
	 * @throws JMSException if thrown by JMS methods
	 */
	protected String extractStringFromMessage(TextMessage message) throws JMSException {
		return message.getText();
	}

	/**
	 * Extract a byte array from the given {@link BytesMessage}.
	 * @param message the message to convert
	 * @return the resulting byte array
	 * @throws JMSException if thrown by JMS methods
	 */
	protected byte[] extractByteArrayFromMessage(BytesMessage message) throws JMSException {
		byte[] bytes = new byte[(int) message.getBodyLength()];
		message.readBytes(bytes);
		return bytes;
	}

	/**
	 * Extract a Map from the given {@link MapMessage}.
	 * @param message the message to convert
	 * @return the resulting Map
	 * @throws JMSException if thrown by JMS methods
	 */
	@SuppressWarnings("unchecked")
	protected Map<String, Object> extractMapFromMessage(MapMessage message) throws JMSException {
		Map<String, Object> map = new HashMap<>();
		Enumeration<String> en = message.getMapNames();
		while (en.hasMoreElements()) {
			String key = en.nextElement();
			map.put(key, message.getObject(key));
		}
		return map;
	}

	/**
	 * Extract a Serializable object from the given {@link ObjectMessage}.
	 * @param message the message to convert
	 * @return the resulting Serializable object
	 * @throws JMSException if thrown by JMS methods
	 */
	protected Serializable extractSerializableFromMessage(ObjectMessage message) throws JMSException {
		return message.getObject();
	}

}
