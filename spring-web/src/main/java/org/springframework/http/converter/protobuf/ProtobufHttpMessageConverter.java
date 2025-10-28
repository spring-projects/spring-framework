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

package org.springframework.http.converter.protobuf;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import com.google.protobuf.util.JsonFormat;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN;

/**
 * An {@code HttpMessageConverter} that reads and writes
 * {@link com.google.protobuf.Message com.google.protobuf.Messages} using
 * <a href="https://developers.google.com/protocol-buffers/">Google Protocol Buffers</a>.
 *
 * <p>To generate {@code Message} Java classes, you need to install the {@code protoc} binary.
 *
 * <p>This converter supports by default {@code "application/x-protobuf"}, {@code "application/*+x-protobuf"}
 * and {@code "text/plain"} with the official {@code "com.google.protobuf:protobuf-java"} library.
 * The {@code "application/json"} format is also supported with the {@code "com.google.protobuf:protobuf-java-util"}
 * dependency. See {@link ProtobufJsonFormatHttpMessageConverter} for a configurable variant.
 *
 * <p>This converter requires Protobuf 3 or higher.
 *
 * @author Alex Antonov
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author Kamil Doroszkiewicz
 * @since 4.1
 * @see JsonFormat
 * @see ProtobufJsonFormatHttpMessageConverter
 */
public class ProtobufHttpMessageConverter extends AbstractHttpMessageConverter<Message> {

	/**
	 * The default charset used by the converter.
	 */
	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	/**
	 * The media-type for protobuf {@code application/x-protobuf}.
	 */
	public static final MediaType PROTOBUF = new MediaType("application", "x-protobuf", DEFAULT_CHARSET);

	/**
	 * The media-type for protobuf {@code application/*+x-protobuf}.
	 */
	public static final MediaType PLUS_PROTOBUF = new MediaType("application", "*+x-protobuf", DEFAULT_CHARSET);

	/**
	 * The HTTP header containing the protobuf schema.
	 */
	public static final String X_PROTOBUF_SCHEMA_HEADER = "X-Protobuf-Schema";

	/**
	 * The HTTP header containing the protobuf message.
	 */
	public static final String X_PROTOBUF_MESSAGE_HEADER = "X-Protobuf-Message";

	private static final boolean PROTOBUF_JSON_FORMAT_PRESENT =
			ClassUtils.isPresent("com.google.protobuf.util.JsonFormat", ProtobufHttpMessageConverter.class.getClassLoader());

	private static final Map<Class<?>, Method> methodCache = new ConcurrentReferenceHashMap<>();


	final ExtensionRegistry extensionRegistry;

	private final ProtobufHttpMessageConverter.@Nullable ProtobufFormatDelegate protobufFormatDelegate;


	/**
	 * Construct a new {@code ProtobufHttpMessageConverter}.
	 */
	public ProtobufHttpMessageConverter() {
		this(null, null);
	}

	/**
	 * Construct a new {@code ProtobufHttpMessageConverter} with a registry that specifies
	 * protocol message extensions.
	 * @param extensionRegistry the registry to populate
	 */
	public ProtobufHttpMessageConverter(ExtensionRegistry extensionRegistry) {
		this(null, extensionRegistry);
	}

	/**
	 * Constructor for a subclass that supports additional formats.
	 * @param formatDelegate delegate to read and write additional formats
	 * @param extensionRegistry the registry to populate
	 */
	protected ProtobufHttpMessageConverter(
			ProtobufHttpMessageConverter.@Nullable ProtobufFormatDelegate formatDelegate,
			@Nullable ExtensionRegistry extensionRegistry) {

		if (formatDelegate != null) {
			this.protobufFormatDelegate = formatDelegate;
		}
		else if (PROTOBUF_JSON_FORMAT_PRESENT) {
			this.protobufFormatDelegate = new ProtobufJavaUtilDelegate(null, null);
		}
		else {
			this.protobufFormatDelegate = null;
		}

		setSupportedMediaTypes(Arrays.asList(this.protobufFormatDelegate != null ?
				this.protobufFormatDelegate.supportedMediaTypes() : new MediaType[] {PROTOBUF, PLUS_PROTOBUF, TEXT_PLAIN}));

		this.extensionRegistry = (extensionRegistry == null ? ExtensionRegistry.newInstance() : extensionRegistry);
	}


	@Override
	protected boolean supports(Class<?> clazz) {
		return Message.class.isAssignableFrom(clazz);
	}

	@Override
	protected MediaType getDefaultContentType(Message message) {
		return PROTOBUF;
	}

	@Override
	protected Message readInternal(Class<? extends Message> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		MediaType contentType = inputMessage.getHeaders().getContentType();
		if (contentType == null) {
			contentType = PROTOBUF;
		}
		Charset charset = contentType.getCharset();
		if (charset == null) {
			charset = DEFAULT_CHARSET;
		}

		Message.Builder builder = getMessageBuilder(clazz);
		if (PROTOBUF.isCompatibleWith(contentType) ||
				PLUS_PROTOBUF.isCompatibleWith(contentType)) {
			builder.mergeFrom(inputMessage.getBody(), this.extensionRegistry);
		}
		else if (MediaType.TEXT_PLAIN.isCompatibleWith(contentType)) {
			InputStreamReader reader = new InputStreamReader(inputMessage.getBody(), charset);
			TextFormat.merge(reader, this.extensionRegistry, builder);
		}
		else if (this.protobufFormatDelegate != null) {
			this.protobufFormatDelegate.merge(
					inputMessage, contentType, charset, builder, this.extensionRegistry);
		}
		return builder.build();
	}

	/**
	 * Create a new {@code Message.Builder} instance for the given class.
	 * <p>This method uses a ConcurrentReferenceHashMap for caching method lookups.
	 */
	private Message.Builder getMessageBuilder(Class<? extends Message> clazz) {
		try {
			Method method = methodCache.get(clazz);
			if (method == null) {
				method = clazz.getMethod("newBuilder");
				methodCache.put(clazz, method);
			}
			return (Message.Builder) method.invoke(clazz);
		}
		catch (Exception ex) {
			throw new HttpMessageConversionException(
					"Invalid Protobuf Message type: no invocable newBuilder() method on " + clazz, ex);
		}
	}


	@Override
	protected boolean canWrite(@Nullable MediaType mediaType) {
		return (super.canWrite(mediaType) ||
				(this.protobufFormatDelegate != null && this.protobufFormatDelegate.supportsWriteOnly(mediaType)));
	}

	@Override
	protected void writeInternal(Message message, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		MediaType contentType = outputMessage.getHeaders().getContentType();
		if (contentType == null) {
			contentType = getDefaultContentType(message);
		}
		Charset charset = contentType.getCharset();
		if (charset == null) {
			charset = DEFAULT_CHARSET;
		}

		if (PROTOBUF.isCompatibleWith(contentType) ||
				PLUS_PROTOBUF.isCompatibleWith(contentType)) {
			setProtoHeader(outputMessage, message);
			CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(outputMessage.getBody());
			message.writeTo(codedOutputStream);
			codedOutputStream.flush();
		}
		else if (TEXT_PLAIN.isCompatibleWith(contentType)) {
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputMessage.getBody(), charset);
			TextFormat.printer().print(message, outputStreamWriter);
			outputStreamWriter.flush();
			outputMessage.getBody().flush();
		}
		else if (this.protobufFormatDelegate != null) {
			this.protobufFormatDelegate.print(message, outputMessage, contentType, charset);
			outputMessage.getBody().flush();
		}
	}

	/**
	 * Set the "X-Protobuf-*" HTTP headers when responding with a message of
	 * content type "application/x-protobuf"
	 * <p><b>Note:</b> <code>outputMessage.getBody()</code> should not have been called
	 * before because it writes HTTP headers (making them read only).</p>
	 */
	private void setProtoHeader(HttpOutputMessage response, Message message) {
		response.getHeaders().set(X_PROTOBUF_SCHEMA_HEADER, message.getDescriptorForType().getFile().getName());
		response.getHeaders().set(X_PROTOBUF_MESSAGE_HEADER, message.getDescriptorForType().getFullName());
	}

	@Override
	protected boolean supportsRepeatableWrites(Message message) {
		return true;
	}


	/**
	 * Contract to enable subclasses to plug in support for additional formats.
	 */
	protected interface ProtobufFormatDelegate {

		/**
		 * Return the supported media types for the converter.
		 * <p>Note that {@link #PROTOBUF}, {@link #PLUS_PROTOBUF}, and {@link MediaType#TEXT_PLAIN}
		 * have built-in support and can be listed in addition to formats
		 * specific to this delegate.
		 */
		MediaType[] supportedMediaTypes();

		/**
		 * Whether the media type is supported for writing.
		 */
		boolean supportsWriteOnly(@Nullable MediaType mediaType);

		/**
		 * Use merge methods on {@link Message.Builder} to read a message from
		 * the given {@code HttpInputMessage}.
		 */
		void merge(HttpInputMessage inputMessage, MediaType contentType, Charset charset,
				Message.Builder builder, ExtensionRegistry extensionRegistry)
				throws IOException, HttpMessageConversionException;

		/**
		 * Use print methods on {@link Message.Builder} to write the message to
		 * the given {@code HttpOutputMessage}.
		 */
		void print(Message message, HttpOutputMessage outputMessage, MediaType contentType, Charset charset)
				throws IOException, HttpMessageConversionException;
	}


	/**
	 * {@link ProtobufFormatDelegate} implementation used when
	 * {@code com.google.protobuf.util.JsonFormat} is available.
	 */
	static class ProtobufJavaUtilDelegate implements ProtobufFormatDelegate {

		private final JsonFormat.Parser parser;

		private final JsonFormat.Printer printer;

		public ProtobufJavaUtilDelegate(JsonFormat.@Nullable Parser parser, JsonFormat.@Nullable Printer printer) {
			this.parser = (parser != null ? parser : JsonFormat.parser());
			this.printer = (printer != null ? printer : JsonFormat.printer());
		}

		@Override
		public MediaType[] supportedMediaTypes() {
			return new MediaType[] {PROTOBUF, PLUS_PROTOBUF, TEXT_PLAIN, APPLICATION_JSON};
		}

		@Override
		public boolean supportsWriteOnly(@Nullable MediaType mediaType) {
			return false;
		}

		@Override
		public void merge(HttpInputMessage inputMessage, MediaType contentType, Charset charset,
				Message.Builder builder, ExtensionRegistry extensionRegistry)
				throws IOException, HttpMessageConversionException {

			if (contentType.isCompatibleWith(APPLICATION_JSON)) {
				InputStreamReader reader = new InputStreamReader(inputMessage.getBody(), charset);
				this.parser.merge(reader, builder);
			}
			else {
				throw new HttpMessageConversionException(
						"protobuf-java-util does not support parsing " + contentType);
			}
		}

		@Override
		public void print(Message message, HttpOutputMessage outputMessage, MediaType contentType, Charset charset)
				throws IOException, HttpMessageConversionException {

			if (contentType.isCompatibleWith(APPLICATION_JSON)) {
				OutputStreamWriter writer = new OutputStreamWriter(outputMessage.getBody(), charset);
				this.printer.appendTo(message, writer);
				writer.flush();
			}
			else {
				throw new HttpMessageConversionException(
						"protobuf-java-util does not support printing " + contentType);
			}
		}
	}

}
