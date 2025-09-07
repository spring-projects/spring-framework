/*
 * Copyright 2002-present the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import com.fasterxml.jackson.annotation.JsonView;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonEncoding;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeType;

/**
 * A Jackson 3.x based {@link MessageConverter} implementation.
 *
 * <p>The default constructor loads {@link tools.jackson.databind.JacksonModule}s
 * found by {@link MapperBuilder#findModules(ClassLoader)}.
 *
 * @author Sebastien Deleuze
 * @since 7.0
 */
public class JacksonJsonMessageConverter extends AbstractMessageConverter {

	private static final MimeType[] DEFAULT_MIME_TYPES = new MimeType[] {
			new MimeType("application", "json"), new MimeType("application", "*+json")};

	private final JsonMapper jsonMapper;


	/**
	 * Construct a new instance with a {@link JsonMapper} customized with the
	 * {@link tools.jackson.databind.JacksonModule}s found by
	 * {@link MapperBuilder#findModules(ClassLoader)}.
	 */
	public JacksonJsonMessageConverter() {
		this(DEFAULT_MIME_TYPES);
	}

	/**
	 * Construct a new instance with a {@link JsonMapper} customized
	 * with the {@link tools.jackson.databind.JacksonModule}s found
	 * by {@link MapperBuilder#findModules(ClassLoader)} and the
	 * provided {@link MimeType}s.
	 * @param supportedMimeTypes the supported MIME types
	 */
	public JacksonJsonMessageConverter(MimeType... supportedMimeTypes) {
		super(supportedMimeTypes);
		this.jsonMapper = JsonMapper.builder().findAndAddModules(JacksonJsonMessageConverter.class.getClassLoader()).build();
	}

	/**
	 * Construct a new instance with the provided {@link JsonMapper}.
	 * @see JsonMapper#builder()
	 * @see MapperBuilder#findModules(ClassLoader)
	 */
	public JacksonJsonMessageConverter(JsonMapper jsonMapper) {
		this(jsonMapper, DEFAULT_MIME_TYPES);
	}

	/**
	 * Construct a new instance with the provided {@link JsonMapper} and the
	 * provided {@link MimeType}s.
	 * @see JsonMapper#builder()
	 * @see MapperBuilder#findModules(ClassLoader)
	 */
	public JacksonJsonMessageConverter(JsonMapper jsonMapper, MimeType... supportedMimeTypes) {
		super(supportedMimeTypes);
		Assert.notNull(jsonMapper, "JsonMapper must not be null");
		this.jsonMapper = jsonMapper;
	}

	/**
	 * Return the underlying {@code JsonMapper} for this converter.
	 */
	protected JsonMapper getJsonMapper() {
		return this.jsonMapper;
	}

	@Override
	protected boolean canConvertFrom(Message<?> message, @Nullable Class<?> targetClass) {
		return targetClass != null && supportsMimeType(message.getHeaders());
	}

	@Override
	protected boolean canConvertTo(Object payload, @Nullable MessageHeaders headers) {
		return supportsMimeType(headers);
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		// should not be called, since we override canConvertFrom/canConvertTo instead
		throw new UnsupportedOperationException();
	}

	@Override
	protected @Nullable Object convertFromInternal(Message<?> message, Class<?> targetClass, @Nullable Object conversionHint) {
		JavaType javaType = this.jsonMapper.constructType(getResolvedType(targetClass, conversionHint));
		Object payload = message.getPayload();
		Class<?> view = getSerializationView(conversionHint);
		try {
			if (ClassUtils.isAssignableValue(targetClass, payload)) {
				return payload;
			}
			else if (payload instanceof byte[] bytes) {
				if (view != null) {
					return this.jsonMapper.readerWithView(view).forType(javaType).readValue(bytes);
				}
				else {
					return this.jsonMapper.readValue(bytes, javaType);
				}
			}
			else {
				// Assuming a text-based source payload
				if (view != null) {
					return this.jsonMapper.readerWithView(view).forType(javaType).readValue(payload.toString());
				}
				else {
					return this.jsonMapper.readValue(payload.toString(), javaType);
				}
			}
		}
		catch (JacksonException ex) {
			throw new MessageConversionException(message, "Could not read JSON: " + ex.getMessage(), ex);
		}
	}

	@Override
	protected @Nullable Object convertToInternal(Object payload, @Nullable MessageHeaders headers,
			@Nullable Object conversionHint) {

		try {
			Class<?> view = getSerializationView(conversionHint);
			if (byte[].class == getSerializedPayloadClass()) {
				ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
				JsonEncoding encoding = getJsonEncoding(getMimeType(headers));
				try (JsonGenerator generator = this.jsonMapper.createGenerator(out, encoding)) {
					if (view != null) {
						this.jsonMapper.writerWithView(view).writeValue(generator, payload);
					}
					else {
						this.jsonMapper.writeValue(generator, payload);
					}
					payload = out.toByteArray();
				}
			}
			else {
				// Assuming a text-based target payload
				Writer writer = new StringWriter(1024);
				if (view != null) {
					this.jsonMapper.writerWithView(view).writeValue(writer, payload);
				}
				else {
					this.jsonMapper.writeValue(writer, payload);
				}
				payload = writer.toString();
			}
		}
		catch (JacksonException ex) {
			throw new MessageConversionException("Could not write JSON: " + ex.getMessage(), ex);
		}
		return payload;
	}

	/**
	 * Determine a Jackson serialization view based on the given conversion hint.
	 * @param conversionHint the conversion hint Object as passed into the
	 * converter for the current conversion attempt
	 * @return the serialization view class, or {@code null} if none
	 */
	protected @Nullable Class<?> getSerializationView(@Nullable Object conversionHint) {
		if (conversionHint instanceof MethodParameter param) {
			JsonView annotation = (param.getParameterIndex() >= 0 ?
					param.getParameterAnnotation(JsonView.class) : param.getMethodAnnotation(JsonView.class));
			if (annotation != null) {
				return extractViewClass(annotation, conversionHint);
			}
		}
		else if (conversionHint instanceof JsonView jsonView) {
			return extractViewClass(jsonView, conversionHint);
		}
		else if (conversionHint instanceof Class<?> clazz) {
			return clazz;
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
	protected JsonEncoding getJsonEncoding(@Nullable MimeType contentType) {
		if (contentType != null && contentType.getCharset() != null) {
			Charset charset = contentType.getCharset();
			for (JsonEncoding encoding : JsonEncoding.values()) {
				if (charset.name().equals(encoding.getJavaName())) {
					return encoding;
				}
			}
		}
		return JsonEncoding.UTF8;
	}

}
