/*
 * Copyright 2002-2020 the original author or authors.
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
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.MimeType;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON;
import static org.springframework.util.MimeTypeUtils.TEXT_PLAIN;

/**
 * An {@code MessageConverter} that reads and writes
 * {@link com.google.protobuf.Message com.google.protobuf.Messages} using
 * <a href="https://developers.google.com/protocol-buffers/">Google Protocol Buffers</a>.
 *
 * <p>To generate {@code Message} Java classes, you need to install the {@code protoc} binary.
 *
 * <p>This converter supports by default {@code "application/x-protobuf"} with the official
 * {@code "com.google.protobuf:protobuf-java"} library.
 *
 * <p>{@code "application/json"} can be supported with the official
 * {@code "com.google.protobuf:protobuf-java-util"} 3.x, with 3.3 or higher recommended.
 *
 * @author Parviz Rozikov
 * @author Rossen Stoyanchev
 * @since 5.2.2
 */
public class ProtobufMessageConverter extends AbstractMessageConverter {

	/**
	 * The default charset used by the converter.
	 */
	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	/**
	 * The mime-type for protobuf {@code application/x-protobuf}.
	 */
	public static final MimeType PROTOBUF = new MimeType("application", "x-protobuf", DEFAULT_CHARSET);


	private static final Map<Class<?>, Method> methodCache = new ConcurrentReferenceHashMap<>();

	final ExtensionRegistry extensionRegistry;

	@Nullable
	private final ProtobufFormatSupport protobufFormatSupport;


	/**
	 * Constructor with a default instance of {@link ExtensionRegistry}.
	 */
	public ProtobufMessageConverter() {
		this(null, null);
	}

	/**
	 * Constructor with a given {@code ExtensionRegistry}.
	 */
	public ProtobufMessageConverter(ExtensionRegistry extensionRegistry) {
		this(null, extensionRegistry);
	}

	ProtobufMessageConverter(@Nullable ProtobufFormatSupport formatSupport,
			@Nullable ExtensionRegistry extensionRegistry) {

		super(PROTOBUF, TEXT_PLAIN);

		if (formatSupport != null) {
			this.protobufFormatSupport = formatSupport;
		}
		else if (ClassUtils.isPresent("com.google.protobuf.util.JsonFormat", getClass().getClassLoader())) {
			this.protobufFormatSupport = new ProtobufJavaUtilSupport(null, null);
		}
		else {
			this.protobufFormatSupport = null;
		}

		if (this.protobufFormatSupport != null) {
			addSupportedMimeTypes(this.protobufFormatSupport.supportedMediaTypes());
		}

		this.extensionRegistry = (extensionRegistry == null ? ExtensionRegistry.newInstance() : extensionRegistry);
	}


	@Override
	protected boolean supports(Class<?> clazz) {
		return Message.class.isAssignableFrom(clazz);
	}

	@Override
	protected boolean canConvertTo(Object payload, @Nullable MessageHeaders headers) {
		MimeType contentType = getMimeType(headers);
		return (super.canConvertTo(payload, headers) ||
				this.protobufFormatSupport != null && this.protobufFormatSupport.supportsWriteOnly(contentType));
	}

	@Override
	protected Object convertFromInternal(org.springframework.messaging.Message<?> message,
			Class<?> targetClass, @Nullable Object conversionHint) {

		MimeType contentType = getMimeType(message.getHeaders());
		final Object payload = message.getPayload();

		if (contentType == null) {
			contentType = PROTOBUF;
		}

		Charset charset = contentType.getCharset();
		if (charset == null) {
			charset = DEFAULT_CHARSET;
		}

		Message.Builder builder = getMessageBuilder(targetClass);
		try {
			if (PROTOBUF.isCompatibleWith(contentType)) {
				builder.mergeFrom((byte[]) payload, this.extensionRegistry);
			}
			else if (this.protobufFormatSupport != null) {
				this.protobufFormatSupport.merge(message, charset, contentType, this.extensionRegistry, builder);
			}
		}
		catch (IOException ex) {
			throw new MessageConversionException(message, "Could not read proto message" + ex.getMessage(), ex);
		}

		return builder.build();
	}


	@Override
	protected Object convertToInternal(
			Object payload, @Nullable MessageHeaders headers, @Nullable Object conversionHint) {

		final Message message = (Message) payload;

		MimeType contentType = getMimeType(headers);
		if (contentType == null) {
			contentType = PROTOBUF;
		}

		Charset charset = contentType.getCharset();
		if (charset == null) {
			charset = DEFAULT_CHARSET;
		}

		try {
			if (PROTOBUF.isCompatibleWith(contentType)) {
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				message.writeTo(byteArrayOutputStream);
				payload = byteArrayOutputStream.toByteArray();
			}
			else if (this.protobufFormatSupport != null) {
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				this.protobufFormatSupport.print(message, outputStream, contentType, charset);
				payload = outputStream.toString(charset);
			}
		}
		catch (IOException ex) {
			throw new MessageConversionException("Failed to print Protobuf message: " + ex.getMessage(), ex);

		}
		return payload;
	}

	/**
	 * Create a new {@code Message.Builder} instance for the given class.
	 * <p>This method uses a ConcurrentReferenceHashMap for caching method lookups.
	 */
	private Message.Builder getMessageBuilder(Class<?> clazz) {
		try {
			Method method = methodCache.get(clazz);
			if (method == null) {
				method = clazz.getMethod("newBuilder");
				methodCache.put(clazz, method);
			}
			return (Message.Builder) method.invoke(clazz);
		}
		catch (Exception ex) {
			throw new MessageConversionException(
					"Invalid Protobuf Message type: no invocable newBuilder() method on " + clazz, ex);
		}
	}


	/**
	 * Protobuf format support.
	 */
	interface ProtobufFormatSupport {

		MimeType[] supportedMediaTypes();

		boolean supportsWriteOnly(@Nullable MimeType mediaType);

		void merge(org.springframework.messaging.Message<?> message,
				Charset charset, MimeType contentType, ExtensionRegistry extensionRegistry,
				Message.Builder builder) throws IOException, MessageConversionException;

		void print(Message message, OutputStream output, MimeType contentType, Charset charset)
				throws IOException, MessageConversionException;
	}


	/**
	 * {@link ProtobufFormatSupport} implementation used when
	 * {@code com.google.protobuf.util.JsonFormat} is available.
	 */
	static class ProtobufJavaUtilSupport implements ProtobufFormatSupport {

		private final JsonFormat.Parser parser;

		private final JsonFormat.Printer printer;

		public ProtobufJavaUtilSupport(@Nullable JsonFormat.Parser parser, @Nullable JsonFormat.Printer printer) {
			this.parser = (parser != null ? parser : JsonFormat.parser());
			this.printer = (printer != null ? printer : JsonFormat.printer());
		}

		@Override
		public MimeType[] supportedMediaTypes() {
			return new MimeType[]{APPLICATION_JSON};
		}

		@Override
		public boolean supportsWriteOnly(@Nullable MimeType mimeType) {
			return false;
		}

		@Override
		public void merge(org.springframework.messaging.Message<?> message, Charset charset,
				MimeType contentType, ExtensionRegistry extensionRegistry, Message.Builder builder)
				throws IOException, MessageConversionException {

			if (contentType.isCompatibleWith(APPLICATION_JSON)) {
				this.parser.merge(message.getPayload().toString(), builder);
			}
			else {
				throw new MessageConversionException(
						"protobuf-java-util does not support parsing " + contentType);
			}
		}

		@Override
		public void print(Message message, OutputStream output, MimeType contentType, Charset charset)
				throws IOException, MessageConversionException {

			if (contentType.isCompatibleWith(APPLICATION_JSON)) {
				OutputStreamWriter writer = new OutputStreamWriter(output, charset);
				this.printer.appendTo(message, writer);
				writer.flush();
			}
			else {
				throw new MessageConversionException(
						"protobuf-java-util does not support printing " + contentType);
			}
		}
	}

}
