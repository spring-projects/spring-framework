/*
 * Copyright 2002-2019 the original author or authors.
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
import java.util.Arrays;
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
 * <p>{@code "application/json"} can be supported with the official {@code "com.google.protobuf:protobuf-java-util"} 3.x
 *  with 3.3 or higher recommended
 *
 * @author Parviz Rozikov
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
	 * Construct a new {@code ProtobufMessageConverter}.
	 */
	public ProtobufMessageConverter() {
		this((ProtobufFormatSupport) null, (ExtensionRegistry) null);
	}

	/**
	 * Construct a new {@code ProtobufMessageConverter} with a registry that specifies
	 * protocol message extensions.
	 *
	 * @param extensionRegistry the registry to populate
	 */
	public ProtobufMessageConverter(@Nullable ExtensionRegistry extensionRegistry) {
		this(null, extensionRegistry);
	}

	/**
	 * Construct a new {@code ProtobufMessageConverter} with the given
	 * {@code JsonFormat.Parser} and {@code JsonFormat.Printer} configuration.
	 *
	 * @param parser  the JSON parser configuration
	 * @param printer the JSON printer configuration
	 */
	public ProtobufMessageConverter(@Nullable JsonFormat.Parser parser, @Nullable JsonFormat.Printer printer) {
		this(new ProtobufJavaUtilSupport(parser, printer), (ExtensionRegistry) null);
	}

	/**
	 * Construct a new {@code ProtobufMessageConverter} with the given
	 * {@code JsonFormat.Parser} and {@code JsonFormat.Printer} configuration, also
	 * accepting a registry that specifies protocol message extensions.
	 *
	 * @param parser            the JSON parser configuration
	 * @param printer           the JSON printer configuration
	 * @param extensionRegistry the registry to populate
	 */
	public ProtobufMessageConverter(@Nullable JsonFormat.Parser parser,
									@Nullable JsonFormat.Printer printer, @Nullable ExtensionRegistry extensionRegistry) {

		this(new ProtobufJavaUtilSupport(parser, printer), extensionRegistry);
	}

	/**
	 * Construct a new {@code ProtobufMessageConverter} with the given
	 * {@code ProtobufFormatSupport}  configuration, also
	 * accepting a registry that specifies protocol message extensions.
	 *
	 * @param formatSupport     support third party
	 * @param extensionRegistry the registry to populate
	 */
	public ProtobufMessageConverter(@Nullable ProtobufFormatSupport formatSupport,
									@Nullable ExtensionRegistry extensionRegistry) {
		super(PROTOBUF);

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
			setSupportedMimeTypes(Arrays.asList(protobufFormatSupport.supportedMediaTypes()));
		}

		this.extensionRegistry = (extensionRegistry == null ? ExtensionRegistry.newInstance() : extensionRegistry);
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		return Message.class.isAssignableFrom(clazz);
	}

	@Override
	protected boolean canConvertTo(Object payload, @Nullable MessageHeaders headers) {
		MimeType mimeType = getMimeType(headers);
		return (super.canConvertTo(payload, headers) ||
				this.protobufFormatSupport != null && this.protobufFormatSupport.supportsWriteOnly(mimeType));
	}

	@Override
	protected Object convertFromInternal(org.springframework.messaging.Message<?> message, Class<?> targetClass, @Nullable Object conversionHint) {
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
			else if (protobufFormatSupport != null) {
				this.protobufFormatSupport.merge(
						message, charset, contentType, this.extensionRegistry, builder);
			}
		}
		catch (IOException e) {
			throw new MessageConversionException(message, "Could not read proto message" + e.getMessage(), e);
		}

		return builder.build();
	}


	@Override
	protected Object convertToInternal(Object payload, @Nullable MessageHeaders headers, @Nullable Object conversionHint) {
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
				payload = new String(outputStream.toByteArray(), charset);
			}
		}
		catch (IOException e) {
			throw new MessageConversionException("Could not write proto message" + e.getMessage(), e);

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

		void merge(org.springframework.messaging.Message<?> message, Charset charset, MimeType contentType,
				   ExtensionRegistry extensionRegistry, Message.Builder builder)
				throws IOException, MessageConversionException;

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
			return new MimeType[]{PROTOBUF, TEXT_PLAIN, APPLICATION_JSON};
		}

		@Override
		public boolean supportsWriteOnly(@Nullable MimeType mimeType) {
			return false;
		}

		@Override
		public void merge(org.springframework.messaging.Message<?> message, Charset charset, MimeType contentType,
						  ExtensionRegistry extensionRegistry, Message.Builder builder)
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
			} else {
				throw new MessageConversionException(
						"protobuf-java-util does not support printing " + contentType);
			}
		}
	}


}
