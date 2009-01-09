/*
 * Copyright 2007 the original author or authors.
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
import org.springframework.oxm.MarshallingFailureException;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.UnmarshallingFailureException;
import org.springframework.util.Assert;

/**
 * Spring JMS {@link MessageConverter} that uses a {@link Marshaller} and {@link Unmarshaller}. Marshals an object to a
 * {@link BytesMessage}, or to a {@link TextMessage} if the {@link #setMarshalTo marshalTo} is set to {@link
 * #MARSHAL_TO_TEXT_MESSAGE}. Unmarshals from a {@link TextMessage} or {@link BytesMessage} to an object.
 *
 * @author Arjen Poutsma
 * @see org.springframework.jms.core.JmsTemplate#convertAndSend
 * @see org.springframework.jms.core.JmsTemplate#receiveAndConvert
 * @since 3.0
 */
public class MarshallingMessageConverter implements MessageConverter, InitializingBean {

	/** Constant that indicates that {@link #toMessage(Object, Session)} should marshal to a {@link BytesMessage}. */
	public static final int MARSHAL_TO_BYTES_MESSAGE = 1;

	/** Constant that indicates that {@link #toMessage(Object, Session)} should marshal to a {@link TextMessage}. */
	public static final int MARSHAL_TO_TEXT_MESSAGE = 2;

	private Marshaller marshaller;

	private Unmarshaller unmarshaller;

	private int marshalTo = MARSHAL_TO_BYTES_MESSAGE;

	/**
	 * Constructs a new <code>MarshallingMessageConverter</code> with no {@link Marshaller} set. The marshaller must be set
	 * after construction by invoking {@link #setMarshaller(Marshaller)}.
	 */
	public MarshallingMessageConverter() {
	}

	/**
	 * Constructs a new <code>MarshallingMessageConverter</code> with the given {@link Marshaller} set.  If the given
	 * {@link Marshaller} also implements the {@link Unmarshaller} interface, it is used for both marshalling and
	 * unmarshalling. Otherwise, an exception is thrown. <p/> Note that all {@link Marshaller} implementations in Spring-WS
	 * also implement the {@link Unmarshaller} interface, so that you can safely use this constructor.
	 *
	 * @param marshaller object used as marshaller and unmarshaller
	 * @throws IllegalArgumentException when <code>marshaller</code> does not implement the {@link Unmarshaller} interface
	 */
	public MarshallingMessageConverter(Marshaller marshaller) {
		Assert.notNull(marshaller, "marshaller must not be null");
		if (!(marshaller instanceof Unmarshaller)) {
			throw new IllegalArgumentException("Marshaller [" + marshaller + "] does not implement the Unmarshaller " +
					"interface. Please set an Unmarshaller explicitely by using the " +
					"AbstractMarshallingPayloadEndpoint(Marshaller, Unmarshaller) constructor.");
		}
		else {
			this.marshaller = marshaller;
			this.unmarshaller = (Unmarshaller) marshaller;
		}
	}

	/**
	 * Creates a new <code>MarshallingMessageConverter</code> with the given marshaller and unmarshaller.
	 *
	 * @param marshaller   the marshaller to use
	 * @param unmarshaller the unmarshaller to use
	 */
	public MarshallingMessageConverter(Marshaller marshaller, Unmarshaller unmarshaller) {
		Assert.notNull(marshaller, "marshaller must not be null");
		Assert.notNull(unmarshaller, "unmarshaller must not be null");
		this.marshaller = marshaller;
		this.unmarshaller = unmarshaller;
	}

	/**
	 * Indicates whether {@link #toMessage(Object,Session)} should marshal to a {@link BytesMessage} or a {@link
	 * TextMessage}. The default is {@link #MARSHAL_TO_BYTES_MESSAGE}, i.e. this converter marshals to a {@link
	 * BytesMessage}.
	 *
	 * @see #MARSHAL_TO_BYTES_MESSAGE
	 * @see #MARSHAL_TO_TEXT_MESSAGE
	 */
	public void setMarshalTo(int marshalTo) {
		this.marshalTo = marshalTo;
	}

	/** Sets the {@link Marshaller} to be used by this message converter. */
	public void setMarshaller(Marshaller marshaller) {
		this.marshaller = marshaller;
	}

	/** Sets the {@link Unmarshaller} to be used by this message converter. */
	public void setUnmarshaller(Unmarshaller unmarshaller) {
		this.unmarshaller = unmarshaller;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(marshaller, "Property 'marshaller' is required");
		Assert.notNull(unmarshaller, "Property 'unmarshaller' is required");
	}

	/**
	 * Marshals the given object to a {@link TextMessage} or {@link javax.jms.BytesMessage}. The desired message type can
	 * be defined by setting the {@link #setMarshalTo(int) marshalTo} property.
	 *
	 * @see #marshalToTextMessage
	 * @see #marshalToBytesMessage
	 */
	public Message toMessage(Object object, Session session) throws JMSException, MessageConversionException {
		try {
			switch (marshalTo) {
				case MARSHAL_TO_TEXT_MESSAGE:
					return marshalToTextMessage(object, session, marshaller);
				case MARSHAL_TO_BYTES_MESSAGE:
					return marshalToBytesMessage(object, session, marshaller);
				default:
					return marshalToMessage(object, session, marshaller);
			}
		}
		catch (MarshallingFailureException ex) {
			throw new MessageConversionException("Could not marshal [" + object + "]", ex);
		}
		catch (IOException ex) {
			throw new MessageConversionException("Could not marshal  [" + object + "]", ex);
		}
	}

	/**
	 * Unmarshals the given {@link Message} into an object.
	 *
	 * @see #unmarshalFromTextMessage
	 * @see #unmarshalFromBytesMessage
	 */
	public Object fromMessage(Message message) throws JMSException, MessageConversionException {
		try {
			if (message instanceof TextMessage) {
				TextMessage textMessage = (TextMessage) message;
				return unmarshalFromTextMessage(textMessage, unmarshaller);
			}
			else if (message instanceof BytesMessage) {
				BytesMessage bytesMessage = (BytesMessage) message;
				return unmarshalFromBytesMessage(bytesMessage, unmarshaller);
			}
			else {
				return unmarshalFromMessage(message, unmarshaller);
			}
		}
		catch (UnmarshallingFailureException ex) {
			throw new MessageConversionException("Could not unmarshal message [" + message + "]", ex);
		}
		catch (IOException ex) {
			throw new MessageConversionException("Could not unmarshal message [" + message + "]", ex);
		}
	}

	/**
	 * Marshals the given object to a {@link TextMessage}.
	 *
	 * @param object	 the object to be marshalled
	 * @param session	current JMS session
	 * @param marshaller the marshaller to use
	 * @return the resulting message
	 * @throws JMSException if thrown by JMS methods
	 * @throws IOException  in case of I/O errors
	 * @see Session#createTextMessage
	 * @see Marshaller#marshal(Object, Result)
	 */
	protected TextMessage marshalToTextMessage(Object object, Session session, Marshaller marshaller)
			throws JMSException, IOException {
		StringWriter writer = new StringWriter();
		Result result = new StreamResult(writer);
		marshaller.marshal(object, result);
		return session.createTextMessage(writer.toString());
	}

	/**
	 * Marshals the given object to a {@link BytesMessage}.
	 *
	 * @param object	 the object to be marshalled
	 * @param session	current JMS session
	 * @param marshaller the marshaller to use
	 * @return the resulting message
	 * @throws JMSException if thrown by JMS methods
	 * @throws IOException  in case of I/O errors
	 * @see Session#createBytesMessage
	 * @see Marshaller#marshal(Object, Result)
	 */
	protected BytesMessage marshalToBytesMessage(Object object, Session session, Marshaller marshaller)
			throws JMSException, IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		StreamResult streamResult = new StreamResult(bos);
		marshaller.marshal(object, streamResult);
		BytesMessage message = session.createBytesMessage();
		message.writeBytes(bos.toByteArray());
		return message;
	}

	/**
	 * Template method that allows for custom message marshalling. Invoked when {@link #setMarshalTo(int)} is not {@link
	 * #MARSHAL_TO_TEXT_MESSAGE} or {@link #MARSHAL_TO_BYTES_MESSAGE}. <p/> Default implemenetation throws a {@link
	 * MessageConversionException}.
	 *
	 * @param object	 the object to marshal
	 * @param session	the JMS session
	 * @param marshaller the marshaller to use
	 * @return the resulting message
	 * @throws JMSException if thrown by JMS methods
	 * @throws IOException  in case of I/O errors
	 */
	protected Message marshalToMessage(Object object, Session session, Marshaller marshaller)
			throws JMSException, IOException {
		throw new MessageConversionException(
				"Unknown 'marshalTo' value [" + marshalTo + "]. Cannot convert object to Message");
	}

	/**
	 * Unmarshals the given {@link TextMessage} into an object.
	 *
	 * @param message	  the message
	 * @param unmarshaller the unmarshaller to use
	 * @return the unmarshalled object
	 * @throws JMSException if thrown by JMS methods
	 * @throws IOException  in case of I/O errors
	 * @see Unmarshaller#unmarshal(Source)
	 */
	protected Object unmarshalFromTextMessage(TextMessage message, Unmarshaller unmarshaller)
			throws JMSException, IOException {
		Source source = new StreamSource(new StringReader(message.getText()));
		return unmarshaller.unmarshal(source);
	}

	/**
	 * Unmarshals the given {@link BytesMessage} into an object.
	 *
	 * @param message	  the message
	 * @param unmarshaller the unmarshaller to use
	 * @return the unmarshalled object
	 * @throws JMSException if thrown by JMS methods
	 * @throws IOException  in case of I/O errors
	 * @see Unmarshaller#unmarshal(Source)
	 */
	protected Object unmarshalFromBytesMessage(BytesMessage message, Unmarshaller unmarshaller)
			throws JMSException, IOException {
		byte[] bytes = new byte[(int) message.getBodyLength()];
		message.readBytes(bytes);
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		StreamSource source = new StreamSource(bis);
		return unmarshaller.unmarshal(source);
	}

	/**
	 * Template method that allows for custom message unmarshalling. Invoked when {@link #fromMessage(Message)} is invoked
	 * with a message that is not a {@link TextMessage} or {@link BytesMessage}. <p/> Default implemenetation throws a
	 * {@link MessageConversionException}.
	 *
	 * @param message	  the message
	 * @param unmarshaller the unmarshaller to use
	 * @return the unmarshalled object
	 * @throws JMSException if thrown by JMS methods
	 * @throws IOException  in case of I/O errors
	 */
	protected Object unmarshalFromMessage(Message message, Unmarshaller unmarshaller) throws JMSException, IOException {
		throw new MessageConversionException(
				"MarshallingMessageConverter only supports TextMessages and BytesMessages");
	}
}

