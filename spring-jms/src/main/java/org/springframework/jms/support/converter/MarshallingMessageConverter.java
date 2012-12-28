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

package org.springframework.jms.support.converter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.util.Assert;

/**
 * Spring JMS {@link MessageConverter} that uses a {@link Marshaller} and {@link Unmarshaller}.
 * Marshals an object to a {@link BytesMessage}, or to a {@link TextMessage} if the
 * {@link #setTargetType targetType} is set to {@link MessageType#TEXT}.
 * Unmarshals from a {@link TextMessage} or {@link BytesMessage} to an object.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 * @see org.springframework.jms.core.JmsTemplate#convertAndSend
 * @see org.springframework.jms.core.JmsTemplate#receiveAndConvert
 */
public class MarshallingMessageConverter implements MessageConverter, InitializingBean {

	private Marshaller marshaller;

	private Unmarshaller unmarshaller;

	private MessageType targetType = MessageType.BYTES;


	/**
	 * Construct a new {@code MarshallingMessageConverter} with no {@link Marshaller}
	 * or {@link Unmarshaller} set. The marshaller must be set after construction by invoking
	 * {@link #setMarshaller(Marshaller)} and {@link #setUnmarshaller(Unmarshaller)} .
	 */
	public MarshallingMessageConverter() {
	}

	/**
	 * Construct a new {@code MarshallingMessageConverter} with the given {@link Marshaller} set.
	 * <p>If the given {@link Marshaller} also implements the {@link Unmarshaller} interface,
	 * it is used for both marshalling and unmarshalling. Otherwise, an exception is thrown.
	 * <p>Note that all {@link Marshaller} implementations in Spring also implement the
	 * {@link Unmarshaller} interface, so that you can safely use this constructor.
	 * @param marshaller object used as marshaller and unmarshaller
	 * @throws IllegalArgumentException when {@code marshaller} does not implement the
	 * {@link Unmarshaller} interface as well
	 */
	public MarshallingMessageConverter(Marshaller marshaller) {
		Assert.notNull(marshaller, "Marshaller must not be null");
		if (!(marshaller instanceof Unmarshaller)) {
			throw new IllegalArgumentException(
					"Marshaller [" + marshaller + "] does not implement the Unmarshaller " +
					"interface. Please set an Unmarshaller explicitly by using the " +
					"MarshallingMessageConverter(Marshaller, Unmarshaller) constructor.");
		}
		else {
			this.marshaller = marshaller;
			this.unmarshaller = (Unmarshaller) marshaller;
		}
	}

	/**
	 * Construct a new {@code MarshallingMessageConverter} with the
	 * given Marshaller and Unmarshaller.
	 * @param marshaller the Marshaller to use
	 * @param unmarshaller the Unmarshaller to use
	 */
	public MarshallingMessageConverter(Marshaller marshaller, Unmarshaller unmarshaller) {
		Assert.notNull(marshaller, "Marshaller must not be null");
		Assert.notNull(unmarshaller, "Unmarshaller must not be null");
		this.marshaller = marshaller;
		this.unmarshaller = unmarshaller;
	}


	/**
	 * Set the {@link Marshaller} to be used by this message converter.
	 */
	public void setMarshaller(Marshaller marshaller) {
		this.marshaller = marshaller;
	}

	/**
	 * Set the {@link Unmarshaller} to be used by this message converter.
	 */
	public void setUnmarshaller(Unmarshaller unmarshaller) {
		this.unmarshaller = unmarshaller;
	}

	/**
	 * Specify whether {@link #toMessage(Object, Session)} should marshal to
	 * a {@link BytesMessage} or a {@link TextMessage}.
	 * <p>The default is {@link MessageType#BYTES}, i.e. this converter marshals
	 * to a {@link BytesMessage}. Note that the default version of this converter
	 * supports {@link MessageType#BYTES} and {@link MessageType#TEXT} only.
	 * @see MessageType#BYTES
	 * @see MessageType#TEXT
	 */
	public void setTargetType(MessageType targetType) {
		Assert.notNull(targetType, "MessageType must not be null");
		this.targetType = targetType;
	}

	@Override
	public void afterPropertiesSet() {
		Assert.notNull(this.marshaller, "Property 'marshaller' is required");
		Assert.notNull(this.unmarshaller, "Property 'unmarshaller' is required");
	}


	/**
	 * This implementation marshals the given object to a {@link javax.jms.TextMessage} or
	 * {@link javax.jms.BytesMessage}. The desired message type can be defined by setting
	 * the {@link #setTargetType "marshalTo"} property.
	 * @see #marshalToTextMessage
	 * @see #marshalToBytesMessage
	 */
	@Override
	public Message toMessage(Object object, Session session) throws JMSException, MessageConversionException {
		try {
			switch (this.targetType) {
				case TEXT:
					return marshalToTextMessage(object, session, this.marshaller);
				case BYTES:
					return marshalToBytesMessage(object, session, this.marshaller);
				default:
					return marshalToMessage(object, session, this.marshaller, this.targetType);
			}
		}
		catch (XmlMappingException ex) {
			throw new MessageConversionException("Could not marshal [" + object + "]", ex);
		}
		catch (IOException ex) {
			throw new MessageConversionException("Could not marshal [" + object + "]", ex);
		}
	}

	/**
	 * This implementation unmarshals the given {@link Message} into an object.
	 * @see #unmarshalFromTextMessage
	 * @see #unmarshalFromBytesMessage
	 */
	@Override
	public Object fromMessage(Message message) throws JMSException, MessageConversionException {
		try {
			if (message instanceof TextMessage) {
				TextMessage textMessage = (TextMessage) message;
				return unmarshalFromTextMessage(textMessage, this.unmarshaller);
			}
			else if (message instanceof BytesMessage) {
				BytesMessage bytesMessage = (BytesMessage) message;
				return unmarshalFromBytesMessage(bytesMessage, this.unmarshaller);
			}
			else {
				return unmarshalFromMessage(message, this.unmarshaller);
			}
		}
		catch (IOException ex) {
			throw new MessageConversionException("Could not access message content: " + message, ex);
		}
		catch (XmlMappingException ex) {
			throw new MessageConversionException("Could not unmarshal message: " + message, ex);
		}
	}


	/**
	 * Marshal the given object to a {@link TextMessage}.
	 * @param object the object to be marshalled
	 * @param session current JMS session
	 * @param marshaller the marshaller to use
	 * @return the resulting message
	 * @throws JMSException if thrown by JMS methods
	 * @throws IOException in case of I/O errors
	 * @throws XmlMappingException in case of OXM mapping errors
	 * @see Session#createTextMessage
	 * @see Marshaller#marshal(Object, Result)
	 */
	protected TextMessage marshalToTextMessage(Object object, Session session, Marshaller marshaller)
			throws JMSException, IOException, XmlMappingException {

		StringWriter writer = new StringWriter();
		Result result = new StreamResult(writer);
		marshaller.marshal(object, result);
		return session.createTextMessage(writer.toString());
	}

	/**
	 * Marshal the given object to a {@link BytesMessage}.
	 * @param object the object to be marshalled
	 * @param session current JMS session
	 * @param marshaller the marshaller to use
	 * @return the resulting message
	 * @throws JMSException if thrown by JMS methods
	 * @throws IOException in case of I/O errors
	 * @throws XmlMappingException in case of OXM mapping errors
	 * @see Session#createBytesMessage
	 * @see Marshaller#marshal(Object, Result)
	 */
	protected BytesMessage marshalToBytesMessage(Object object, Session session, Marshaller marshaller)
			throws JMSException, IOException, XmlMappingException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		StreamResult streamResult = new StreamResult(bos);
		marshaller.marshal(object, streamResult);
		BytesMessage message = session.createBytesMessage();
		message.writeBytes(bos.toByteArray());
		return message;
	}

	/**
	 * Template method that allows for custom message marshalling.
	 * Invoked when {@link #setTargetType} is not {@link MessageType#TEXT} or
	 * {@link MessageType#BYTES}.
	 * <p>The default implementation throws an {@link IllegalArgumentException}.
	 * @param object the object to marshal
	 * @param session the JMS session
	 * @param marshaller the marshaller to use
	 * @param targetType the target message type (other than TEXT or BYTES)
	 * @return the resulting message
	 * @throws JMSException if thrown by JMS methods
	 * @throws IOException in case of I/O errors
	 * @throws XmlMappingException in case of OXM mapping errors
	 */
	protected Message marshalToMessage(Object object, Session session, Marshaller marshaller, MessageType targetType)
			throws JMSException, IOException, XmlMappingException {

		throw new IllegalArgumentException("Unsupported message type [" + targetType +
				"]. MarshallingMessageConverter by default only supports TextMessages and BytesMessages.");
	}


	/**
	 * Unmarshal the given {@link TextMessage} into an object.
	 * @param message the message
	 * @param unmarshaller the unmarshaller to use
	 * @return the unmarshalled object
	 * @throws JMSException if thrown by JMS methods
	 * @throws IOException in case of I/O errors
	 * @throws XmlMappingException in case of OXM mapping errors
	 * @see Unmarshaller#unmarshal(Source)
	 */
	protected Object unmarshalFromTextMessage(TextMessage message, Unmarshaller unmarshaller)
			throws JMSException, IOException, XmlMappingException {

		Source source = new StreamSource(new StringReader(message.getText()));
		return unmarshaller.unmarshal(source);
	}

	/**
	 * Unmarshal the given {@link BytesMessage} into an object.
	 * @param message the message
	 * @param unmarshaller the unmarshaller to use
	 * @return the unmarshalled object
	 * @throws JMSException if thrown by JMS methods
	 * @throws IOException in case of I/O errors
	 * @throws XmlMappingException in case of OXM mapping errors
	 * @see Unmarshaller#unmarshal(Source)
	 */
	protected Object unmarshalFromBytesMessage(BytesMessage message, Unmarshaller unmarshaller)
			throws JMSException, IOException, XmlMappingException {

		byte[] bytes = new byte[(int) message.getBodyLength()];
		message.readBytes(bytes);
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		StreamSource source = new StreamSource(bis);
		return unmarshaller.unmarshal(source);
	}

	/**
	 * Template method that allows for custom message unmarshalling.
	 * Invoked when {@link #fromMessage(Message)} is invoked with a message
	 * that is not a {@link TextMessage} or {@link BytesMessage}.
	 * <p>The default implementation throws an {@link IllegalArgumentException}.
	 * @param message the message
	 * @param unmarshaller the unmarshaller to use
	 * @return the unmarshalled object
	 * @throws JMSException if thrown by JMS methods
	 * @throws IOException in case of I/O errors
	 * @throws XmlMappingException in case of OXM mapping errors
	 */
	protected Object unmarshalFromMessage(Message message, Unmarshaller unmarshaller)
			throws JMSException, IOException, XmlMappingException {

		throw new IllegalArgumentException("Unsupported message type [" + message.getClass() +
				"]. MarshallingMessageConverter by default only supports TextMessages and BytesMessages.");
	}

}
