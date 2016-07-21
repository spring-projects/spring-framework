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

package org.springframework.http.converter.protobuf;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import com.googlecode.protobuf.format.FormatFactory;
import com.googlecode.protobuf.format.ProtobufFormatter;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.ClassUtils;

/**
 * An {@code HttpMessageConverter} that reads and writes {@link com.google.protobuf.Message}s
 * using <a href="https://developers.google.com/protocol-buffers/">Google Protocol Buffers</a>.
 *
 * <p>This converter supports by default {@code "application/x-protobuf"} and {@code "text/plain"}
 * with the official {@code "com.google.protobuf:protobuf-java"} library.
 *
 * <p>Other formats can be supported with additional libraries:
 * <ul>
 *     <li>{@code "application/json"} with the official library
 *     {@code "com.google.protobuf:protobuf-java-util"}
 *     <li>{@code "application/json"}, {@code "application/xml"} and {@code "text/html"} (write only)
 *     can be supported with the 3rd party library
 *     {@code "com.googlecode.protobuf-java-format:protobuf-java-format"}
 * </ul>
 *
 * <p>To generate {@code Message} Java classes, you need to install the {@code protoc} binary.
 *
 * <p>Requires Protobuf 2.6 and Protobuf Java Format 1.4, as of Spring 4.3.
 * Supports up to Protobuf 3.0.0.
 *
 * @author Alex Antonov
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @since 4.1
 */
public class ProtobufHttpMessageConverter extends AbstractHttpMessageConverter<Message> {

	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	public static final MediaType PROTOBUF = new MediaType("application", "x-protobuf", DEFAULT_CHARSET);

	public static final String X_PROTOBUF_SCHEMA_HEADER = "X-Protobuf-Schema";

	public static final String X_PROTOBUF_MESSAGE_HEADER = "X-Protobuf-Message";

	private static final boolean isProtobufJavaUtilPresent =
			ClassUtils.isPresent("com.google.protobuf.util.JsonFormat", ProtobufHttpMessageConverter.class.getClassLoader());

	private static final boolean isProtobufJavaFormatPresent =
			ClassUtils.isPresent("com.googlecode.protobuf.format.JsonFormat", ProtobufHttpMessageConverter.class.getClassLoader());

	private static final MediaType[] SUPPORTED_MEDIATYPES;

	private final ProtobufFormatsSupport protobufFormatsSupport;

	private static final ConcurrentHashMap<Class<?>, Method> methodCache = new ConcurrentHashMap<>();

	private final ExtensionRegistry extensionRegistry = ExtensionRegistry.newInstance();

	static {
		if (isProtobufJavaFormatPresent) {
			SUPPORTED_MEDIATYPES = new MediaType[] {PROTOBUF, MediaType.TEXT_PLAIN, MediaType.APPLICATION_XML,
					MediaType.APPLICATION_JSON};
		}
		else if (isProtobufJavaUtilPresent) {
			SUPPORTED_MEDIATYPES = new MediaType[] {PROTOBUF, MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON};
		}
		else {
			SUPPORTED_MEDIATYPES = new MediaType[] {PROTOBUF, MediaType.TEXT_PLAIN};
		}
	}

	/**
	 * Construct a new instance.
	 */
	public ProtobufHttpMessageConverter() {
		this(null);
	}

	/**
	 * Construct a new instance with an {@link ExtensionRegistryInitializer}
	 * that allows the registration of message extensions.
	 */
	public ProtobufHttpMessageConverter(ExtensionRegistryInitializer registryInitializer) {
		super(SUPPORTED_MEDIATYPES);
		if (isProtobufJavaFormatPresent) {
			this.protobufFormatsSupport = new ProtobufJavaFormatSupport();
		}
		else if (isProtobufJavaUtilPresent) {
			this.protobufFormatsSupport = new ProtobufJavaUtilSupport();
		}
		else {
			this.protobufFormatsSupport = null;
		}
		if (registryInitializer != null) {
			registryInitializer.initializeExtensionRegistry(this.extensionRegistry);
		}
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

		try {
			Message.Builder builder = getMessageBuilder(clazz);
			if (PROTOBUF.isCompatibleWith(contentType)) {
				builder.mergeFrom(inputMessage.getBody(), this.extensionRegistry);
			}
			else if (MediaType.TEXT_PLAIN.isCompatibleWith(contentType)) {
				InputStreamReader reader = new InputStreamReader(inputMessage.getBody(), charset);
				TextFormat.merge(reader, this.extensionRegistry, builder);
			}
			else if (isProtobufJavaUtilPresent || isProtobufJavaFormatPresent) {
				this.protobufFormatsSupport.merge(inputMessage.getBody(), charset, contentType,
						this.extensionRegistry, builder);
			}
			return builder.build();
		}
		catch (Exception ex) {
			throw new HttpMessageNotReadableException("Could not read Protobuf message: " + ex.getMessage(), ex);
		}
	}

	/**
	 * This method overrides the parent implementation, since this HttpMessageConverter
	 * can also produce {@code MediaType.HTML "text/html"} ContentType.
	 */
	@Override
	protected boolean canWrite(MediaType mediaType) {
		return (super.canWrite(mediaType) ||
				(isProtobufJavaFormatPresent && MediaType.TEXT_HTML.isCompatibleWith(mediaType)));
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

		if (PROTOBUF.isCompatibleWith(contentType)) {
			setProtoHeader(outputMessage, message);
			CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(outputMessage.getBody());
			message.writeTo(codedOutputStream);
			codedOutputStream.flush();
		}
		else if (MediaType.TEXT_PLAIN.isCompatibleWith(contentType)) {
			final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputMessage.getBody(), charset);
			TextFormat.print(message, outputStreamWriter);
			outputStreamWriter.flush();
			outputMessage.getBody().flush();
		}
		else if (isProtobufJavaUtilPresent || isProtobufJavaFormatPresent) {
			this.protobufFormatsSupport.print(message, outputMessage.getBody(), contentType, charset);
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


	/**
	 * Create a new {@code Message.Builder} instance for the given class.
	 * <p>This method uses a ConcurrentHashMap for caching method lookups.
	 */
	private static Message.Builder getMessageBuilder(Class<? extends Message> clazz) throws Exception {
		Method method = methodCache.get(clazz);
		if (method == null) {
			method = clazz.getMethod("newBuilder");
			methodCache.put(clazz, method);
		}
		return (Message.Builder) method.invoke(clazz);
	}

	private interface ProtobufFormatsSupport {

		void merge(InputStream input, Charset cs, MediaType contentType, ExtensionRegistry extensionRegistry,
				Message.Builder builder) throws IOException;

		void print(Message message, OutputStream output, MediaType contentType, Charset cs) throws IOException;
	}

	private class ProtobufJavaUtilSupport implements ProtobufFormatsSupport {

		private final com.google.protobuf.util.JsonFormat.Parser parser;

		private final com.google.protobuf.util.JsonFormat.Printer printer;

		public ProtobufJavaUtilSupport() {
			this.parser = com.google.protobuf.util.JsonFormat.parser();
			this.printer = com.google.protobuf.util.JsonFormat.printer();
		}

		@Override
		public void merge(InputStream input, Charset cs, MediaType contentType,
				ExtensionRegistry extensionRegistry, Message.Builder builder) throws IOException {

			if (contentType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
				InputStreamReader reader = new InputStreamReader(input, cs);
				this.parser.merge(reader, builder);
			}
			else {
				throw new UnsupportedOperationException("com.googlecode.protobuf:protobuf-java-util does not support "
						+ contentType.toString() + " format");
			}
		}

		@Override
		public void print(Message message, OutputStream output, MediaType contentType, Charset cs) throws IOException {
			if (contentType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
				this.printer.appendTo(message, new OutputStreamWriter(output, cs));
			}
			else {
				throw new UnsupportedOperationException("com.googlecode.protobuf:protobuf-java-util does not support "
						+ contentType.toString() + " format");
			}
		}

	}

	private class ProtobufJavaFormatSupport implements ProtobufFormatsSupport {

		private final FormatFactory FORMAT_FACTORY;

		private final ProtobufFormatter JSON_FORMATTER;

		private final ProtobufFormatter XML_FORMATTER;

		private final ProtobufFormatter HTML_FORMATTER;

		public ProtobufJavaFormatSupport() {
			FORMAT_FACTORY = new FormatFactory();
			JSON_FORMATTER = FORMAT_FACTORY.createFormatter(FormatFactory.Formatter.JSON);
			XML_FORMATTER = FORMAT_FACTORY.createFormatter(FormatFactory.Formatter.XML);
			HTML_FORMATTER = FORMAT_FACTORY.createFormatter(FormatFactory.Formatter.HTML);
		}

		@Override
		public void merge(InputStream input, Charset cs, MediaType contentType,
				ExtensionRegistry extensionRegistry, Message.Builder builder) throws IOException {
			if (contentType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
				JSON_FORMATTER.merge(input, cs, extensionRegistry, builder);
			}
			else if (contentType.isCompatibleWith(MediaType.APPLICATION_XML)) {
				XML_FORMATTER.merge(input, cs, extensionRegistry, builder);
			}
			else {
				throw new UnsupportedOperationException("com.google.protobuf.util does not support "
						+ contentType.toString() + " format");
			}
		}

		@Override
		public void print(Message message, OutputStream output, MediaType contentType, Charset cs) throws IOException {
			if (contentType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
				JSON_FORMATTER.print(message, output, cs);
			}
			else if (contentType.isCompatibleWith(MediaType.APPLICATION_XML)) {
				XML_FORMATTER.print(message, output, cs);
			}
			else if (contentType.isCompatibleWith(MediaType.TEXT_HTML)) {
				HTML_FORMATTER.print(message, output, cs);
			}
			else {
				throw new UnsupportedOperationException("com.google.protobuf.util does not support "
						+ contentType.toString() + " format");
			}
		}
	}

}
