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

package org.springframework.messaging.converter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.springframework.beans.TypeMismatchException;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * Implementation of {@link MessageConverter} that can read and write XML using Spring's
 * {@link Marshaller} and {@link Unmarshaller} abstractions.
 *
 * <p>This converter requires a {@code Marshaller} and {@code Unmarshaller} before it can
 * be used. These can be injected by the {@linkplain #MarshallingMessageConverter(Marshaller)
 * constructor} or {@linkplain #setMarshaller(Marshaller) bean properties}.
 *
 * @author Arjen Poutsma
 * @since 4.2
 */
public class MarshallingMessageConverter extends AbstractMessageConverter {

	@Nullable
	private Marshaller marshaller;

	@Nullable
	private Unmarshaller unmarshaller;


	/**
	 * Default construct allowing for {@link #setMarshaller(Marshaller)} and/or
	 * {@link #setUnmarshaller(Unmarshaller)} to be invoked separately.
	 */
	public MarshallingMessageConverter() {
		this(new MimeType("application", "xml"), new MimeType("text", "xml"), new MimeType("application", "*+xml"));
	}

	/**
	 * Constructor with a given list of MIME types to support.
	 * @param supportedMimeTypes the MIME types
	 */
	public MarshallingMessageConverter(MimeType... supportedMimeTypes) {
		super(Arrays.asList(supportedMimeTypes));
	}

	/**
	 * Constructor with {@link Marshaller}. If the given {@link Marshaller} also
	 * implements {@link Unmarshaller}, it is also used for unmarshalling.
	 * <p>Note that all {@code Marshaller} implementations in Spring also implement
	 * {@code Unmarshaller} so that you can safely use this constructor.
	 * @param marshaller object used as marshaller and unmarshaller
	 */
	public MarshallingMessageConverter(Marshaller marshaller) {
		this();
		Assert.notNull(marshaller, "Marshaller must not be null");
		this.marshaller = marshaller;
		if (marshaller instanceof Unmarshaller) {
			this.unmarshaller = (Unmarshaller) marshaller;
		}
	}


	/**
	 * Set the {@link Marshaller} to be used by this message converter.
	 */
	public void setMarshaller(@Nullable Marshaller marshaller) {
		this.marshaller = marshaller;
	}

	/**
	 * Return the configured Marshaller.
	 */
	@Nullable
	public Marshaller getMarshaller() {
		return this.marshaller;
	}

	/**
	 * Set the {@link Unmarshaller} to be used by this message converter.
	 */
	public void setUnmarshaller(@Nullable Unmarshaller unmarshaller) {
		this.unmarshaller = unmarshaller;
	}

	/**
	 * Return the configured unmarshaller.
	 */
	@Nullable
	public Unmarshaller getUnmarshaller() {
		return this.unmarshaller;
	}


	@Override
	protected boolean canConvertFrom(Message<?> message, Class<?> targetClass) {
		return (supportsMimeType(message.getHeaders()) && this.unmarshaller != null &&
				this.unmarshaller.supports(targetClass));
	}

	@Override
	protected boolean canConvertTo(Object payload, @Nullable MessageHeaders headers) {
		return (supportsMimeType(headers) && this.marshaller != null &&
				this.marshaller.supports(payload.getClass()));
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		// should not be called, since we override canConvertFrom/canConvertTo instead
		throw new UnsupportedOperationException();
	}

	@Override
	@Nullable
	protected Object convertFromInternal(Message<?> message, Class<?> targetClass, @Nullable Object conversionHint) {
		Assert.notNull(this.unmarshaller, "Property 'unmarshaller' is required");
		try {
			Source source = getSource(message.getPayload());
			Object result = this.unmarshaller.unmarshal(source);
			if (!targetClass.isInstance(result)) {
				throw new TypeMismatchException(result, targetClass);
			}
			return result;
		}
		catch (Exception ex) {
			throw new MessageConversionException(message, "Could not unmarshal XML: " + ex.getMessage(), ex);
		}
	}

	private Source getSource(Object payload) {
		if (payload instanceof byte[]) {
			return new StreamSource(new ByteArrayInputStream((byte[]) payload));
		}
		else {
			return new StreamSource(new StringReader((String) payload));
		}
	}

	@Override
	@Nullable
	protected Object convertToInternal(Object payload, @Nullable MessageHeaders headers,
			@Nullable Object conversionHint) {

		Assert.notNull(this.marshaller, "Property 'marshaller' is required");
		try {
			if (byte[].class == getSerializedPayloadClass()) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				Result result = new StreamResult(out);
				this.marshaller.marshal(payload, result);
				payload = out.toByteArray();
			}
			else {
				Writer writer = new StringWriter();
				Result result = new StreamResult(writer);
				this.marshaller.marshal(payload, result);
				payload = writer.toString();
			}
		}
		catch (Throwable ex) {
			throw new MessageConversionException("Could not marshal XML: " + ex.getMessage(), ex);
		}
		return payload;
	}

}
