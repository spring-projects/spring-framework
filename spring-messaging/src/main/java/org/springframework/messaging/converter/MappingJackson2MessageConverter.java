/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.converter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeType;

/**
 * A Jackson 2 based {@link MessageConverter} implementation.
 *
 * <p>Tested against Jackson 2.2 and 2.3; compatible with Jackson 2.0 and higher.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public class MappingJackson2MessageConverter extends AbstractMessageConverter {

	// Check for Jackson 2.3's overloaded canDeserialize/canSerialize variants with cause reference
	private static final boolean jackson23Available =
			ClassUtils.hasMethod(ObjectMapper.class, "canDeserialize", JavaType.class, AtomicReference.class);


	private ObjectMapper objectMapper = new ObjectMapper();

	private Boolean prettyPrint;


	public MappingJackson2MessageConverter() {
		super(new MimeType("application", "json", Charset.forName("UTF-8")));
	}


	/**
	 * Set the {@code ObjectMapper} for this converter.
	 * If not set, a default {@link ObjectMapper#ObjectMapper() ObjectMapper} is used.
	 * <p>Setting a custom-configured {@code ObjectMapper} is one way to take further
	 * control of the JSON serialization process. For example, an extended
	 * {@link com.fasterxml.jackson.databind.ser.SerializerFactory} can be
	 * configured that provides custom serializers for specific types. The other
	 * option for refining the serialization process is to use Jackson's provided
	 * annotations on the types to be serialized, in which case a custom-configured
	 * ObjectMapper is unnecessary.
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "ObjectMapper must not be null");
		this.objectMapper = objectMapper;
		configurePrettyPrint();
	}

	/**
	 * Return the underlying {@code ObjectMapper} for this converter.
	 */
	public ObjectMapper getObjectMapper() {
		return this.objectMapper;
	}

	/**
	 * Whether to use the {@link DefaultPrettyPrinter} when writing JSON.
	 * This is a shortcut for setting up an {@code ObjectMapper} as follows:
	 * <pre class="code">
	 * ObjectMapper mapper = new ObjectMapper();
	 * mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
	 * converter.setObjectMapper(mapper);
	 * </pre>
	 */
	public void setPrettyPrint(boolean prettyPrint) {
		this.prettyPrint = prettyPrint;
		configurePrettyPrint();
	}

	private void configurePrettyPrint() {
		if (this.prettyPrint != null) {
			this.objectMapper.configure(SerializationFeature.INDENT_OUTPUT, this.prettyPrint);
		}
	}

	@Override
	protected boolean canConvertFrom(Message<?> message, Class<?> targetClass) {
		if (targetClass == null) {
			return false;
		}
		JavaType javaType = this.objectMapper.constructType(targetClass);
		if (!jackson23Available || !logger.isWarnEnabled()) {
			return (this.objectMapper.canDeserialize(javaType) && supportsMimeType(message.getHeaders()));
		}
		AtomicReference<Throwable> causeRef = new AtomicReference<Throwable>();
		if (this.objectMapper.canDeserialize(javaType, causeRef) && supportsMimeType(message.getHeaders())) {
			return true;
		}
		Throwable cause = causeRef.get();
		if (cause != null) {
			logger.warn("Failed to evaluate deserialization for type: " + javaType);
		}
		return false;
	}

	@Override
	protected boolean canConvertTo(Object payload, MessageHeaders headers) {
		if (!jackson23Available || !logger.isWarnEnabled()) {
			return (this.objectMapper.canSerialize(payload.getClass()) && supportsMimeType(headers));
		}
		AtomicReference<Throwable> causeRef = new AtomicReference<Throwable>();
		if (this.objectMapper.canSerialize(payload.getClass(), causeRef) && supportsMimeType(headers)) {
			return true;
		}
		Throwable cause = causeRef.get();
		if (cause != null) {
			logger.warn("Failed to evaluate serialization for type: " + payload.getClass());
		}
		return false;
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		// should not be called, since we override canConvertFrom/canConvertTo instead
		throw new UnsupportedOperationException();
	}

	@Override
	public Object convertFromInternal(Message<?> message, Class<?> targetClass) {
		JavaType javaType = this.objectMapper.constructType(targetClass);
		Object payload = message.getPayload();
		try {
			if (payload instanceof byte[]) {
				return this.objectMapper.readValue((byte[]) payload, javaType);
			}
			else {
				return this.objectMapper.readValue((String) payload, javaType);
			}
		}
		catch (IOException ex) {
			throw new MessageConversionException(message, "Could not read JSON: " + ex.getMessage(), ex);
		}
	}

	@Override
	public Object convertToInternal(Object payload, MessageHeaders headers) {
		try {
			if (byte[].class.equals(getSerializedPayloadClass())) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				JsonEncoding encoding = getJsonEncoding(getMimeType(headers));

				// The following has been deprecated as late as Jackson 2.2 (April 2013);
				// preserved for the time being, for Jackson 2.0/2.1 compatibility.
				@SuppressWarnings("deprecation")
				JsonGenerator generator = this.objectMapper.getJsonFactory().createJsonGenerator(out, encoding);

				// A workaround for JsonGenerators not applying serialization features
				// https://github.com/FasterXML/jackson-databind/issues/12
				if (this.objectMapper.isEnabled(SerializationFeature.INDENT_OUTPUT)) {
					generator.useDefaultPrettyPrinter();
				}

				this.objectMapper.writeValue(generator, payload);
				payload = out.toByteArray();
			}
			else {
				Writer writer = new StringWriter();
				this.objectMapper.writeValue(writer, payload);
				payload = writer.toString();
			}
		}
		catch (IOException ex) {
			throw new MessageConversionException("Could not write JSON: " + ex.getMessage(), ex);
		}
		return payload;
	}

	/**
	 * Determine the JSON encoding to use for the given content type.
	 * @param contentType the MIME type from the MessageHeaders, if any
	 * @return the JSON encoding to use (never {@code null})
	 */
	protected JsonEncoding getJsonEncoding(MimeType contentType) {
		if ((contentType != null) && (contentType.getCharSet() != null)) {
			Charset charset = contentType.getCharSet();
			for (JsonEncoding encoding : JsonEncoding.values()) {
				if (charset.name().equals(encoding.getJavaName())) {
					return encoding;
				}
			}
		}
		return JsonEncoding.UTF8;
	}

}
