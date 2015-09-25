/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.messaging.converter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeType;

/**
 * A Jackson 2 based {@link MessageConverter} implementation.
 *
 * <p>It customizes Jackson's default properties with the following ones:
 * <ul>
 * <li>{@link MapperFeature#DEFAULT_VIEW_INCLUSION} is disabled</li>
 * <li>{@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} is disabled</li>
 * </ul>
 *
 * <p>Compatible with Jackson 2.1 and higher.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 4.0
 */
public class MappingJackson2MessageConverter extends AbstractMessageConverter {

	// Check for Jackson 2.3's overloaded canDeserialize/canSerialize variants with cause reference
	private static final boolean jackson23Available =
			ClassUtils.hasMethod(ObjectMapper.class, "canDeserialize", JavaType.class, AtomicReference.class);


	private ObjectMapper objectMapper;

	private Boolean prettyPrint;


	/**
	 * Construct a {@code MappingJackson2MessageConverter} supporting
	 * the {@code application/json} MIME type.
	 */
	public MappingJackson2MessageConverter() {
		super(new MimeType("application", "json", Charset.forName("UTF-8")));
		initObjectMapper();
	}

	/**
	 * Construct a {@code MappingJackson2MessageConverter} supporting
	 * one or more custom MIME types.
	 * @param supportedMimeTypes the supported MIME types
	 * @since 4.1.5
	 */
	public MappingJackson2MessageConverter(MimeType... supportedMimeTypes) {
		super(Arrays.asList(supportedMimeTypes));
		initObjectMapper();
	}


	private void initObjectMapper() {
		this.objectMapper = new ObjectMapper();
		this.objectMapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
		this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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
			String msg = "Failed to evaluate deserialization for type " + javaType;
			if (logger.isDebugEnabled()) {
				logger.warn(msg, cause);
			}
			else {
				logger.warn(msg + ": " + cause);
			}
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
			String msg = "Failed to evaluate serialization for type [" + payload.getClass() + "]";
			if (logger.isDebugEnabled()) {
				logger.warn(msg, cause);
			}
			else {
				logger.warn(msg + ": " + cause);
			}
		}
		return false;
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		// should not be called, since we override canConvertFrom/canConvertTo instead
		throw new UnsupportedOperationException();
	}

	@Override
	@SuppressWarnings("deprecation")
	protected Object convertFromInternal(Message<?> message, Class<?> targetClass, Object conversionHint) {
		JavaType javaType = this.objectMapper.constructType(targetClass);
		Object payload = message.getPayload();
		Class<?> view = getSerializationView(conversionHint);
		// Note: in the view case, calling withType instead of forType for compatibility with Jackson <2.5
		try {
			if (payload instanceof byte[]) {
				if (view != null) {
					return this.objectMapper.readerWithView(view).withType(javaType).readValue((byte[]) payload);
				}
				else {
					return this.objectMapper.readValue((byte[]) payload, javaType);
				}
			}
			else {
				if (view != null) {
					return this.objectMapper.readerWithView(view).withType(javaType).readValue(payload.toString());
				}
				else {
					return this.objectMapper.readValue(payload.toString(), javaType);
				}
			}
		}
		catch (IOException ex) {
			throw new MessageConversionException(message, "Could not read JSON: " + ex.getMessage(), ex);
		}
	}

	@Override
	protected Object convertToInternal(Object payload, MessageHeaders headers, Object conversionHint) {
		try {
			Class<?> view = getSerializationView(conversionHint);
			if (byte[].class == getSerializedPayloadClass()) {
				ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
				JsonEncoding encoding = getJsonEncoding(getMimeType(headers));
				JsonGenerator generator = this.objectMapper.getFactory().createGenerator(out, encoding);
				if (view != null) {
					this.objectMapper.writerWithView(view).writeValue(generator, payload);
				}
				else {
					this.objectMapper.writeValue(generator, payload);
				}
				payload = out.toByteArray();
			}
			else {
				Writer writer = new StringWriter();
				if (view != null) {
					this.objectMapper.writerWithView(view).writeValue(writer, payload);
				}
				else {
					this.objectMapper.writeValue(writer, payload);
				}
				payload = writer.toString();
			}
		}
		catch (IOException ex) {
			throw new MessageConversionException("Could not write JSON: " + ex.getMessage(), ex);
		}
		return payload;
	}

	/**
	 * Determine a Jackson serialization view based on the given conversion hint.
	 * @param conversionHint the conversion hint Object as passed into the
	 * converter for the current conversion attempt
	 * @return the serialization view class, or {@code null} if none
	 * @since 4.2
	 */
	protected Class<?> getSerializationView(Object conversionHint) {
		if (conversionHint instanceof MethodParameter) {
			MethodParameter param = (MethodParameter) conversionHint;
			JsonView annotation = (param.getParameterIndex() >= 0 ?
					param.getParameterAnnotation(JsonView.class) : param.getMethodAnnotation(JsonView.class));
			if (annotation != null) {
				return extractViewClass(annotation, conversionHint);
			}
		}
		else if (conversionHint instanceof JsonView) {
			return extractViewClass((JsonView) conversionHint, conversionHint);
		}
		else if (conversionHint instanceof Class) {
			return (Class) conversionHint;
		}

		// No JSON view specified...
		return null;
	}

	private Class<?> extractViewClass(JsonView annotation, Object conversionHint) {
		Class<?>[] classes = annotation.value();
		if (classes.length != 1) {
			throw new IllegalArgumentException(
					"@JsonView only supported for handler methods with exactly 1 class argument: " + conversionHint);
		}
		return classes[0];
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
