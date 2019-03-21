/*
 * Copyright 2002-2016 the original author or authors.
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
import java.util.concurrent.ConcurrentHashMap;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import com.googlecode.protobuf.format.HtmlFormat;
import com.googlecode.protobuf.format.JsonFormat;
import com.googlecode.protobuf.format.ProtobufFormatter;
import com.googlecode.protobuf.format.XmlFormat;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.FileCopyUtils;

/**
 * An {@code HttpMessageConverter} that reads and writes {@link com.google.protobuf.Message}s
 * using <a href="https://developers.google.com/protocol-buffers/">Google Protocol Buffers</a>.
 *
 * <p>By default, it supports {@code "application/x-protobuf"}, {@code "text/plain"},
 * {@code "application/json"}, {@code "application/xml"}, while also writing {@code "text/html"}.
 *
 * <p>To generate {@code Message} Java classes, you need to install the {@code protoc} binary.
 *
 * <p>Requires Protobuf 2.6 and Protobuf Java Format 1.4, as of Spring 4.3.
 *
 * @author Alex Antonov
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @since 4.1
 */
public class ProtobufHttpMessageConverter extends AbstractHttpMessageConverter<Message> {

	public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	public static final MediaType PROTOBUF = new MediaType("application", "x-protobuf", DEFAULT_CHARSET);

	public static final String X_PROTOBUF_SCHEMA_HEADER = "X-Protobuf-Schema";

	public static final String X_PROTOBUF_MESSAGE_HEADER = "X-Protobuf-Message";


	private static final ProtobufFormatter JSON_FORMAT = new JsonFormat();

	private static final ProtobufFormatter XML_FORMAT = new XmlFormat();

	private static final ProtobufFormatter HTML_FORMAT = new HtmlFormat();


	private static final ConcurrentHashMap<Class<?>, Method> methodCache = new ConcurrentHashMap<Class<?>, Method>();

	private final ExtensionRegistry extensionRegistry = ExtensionRegistry.newInstance();


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
		super(PROTOBUF, MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML);
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
			if (MediaType.TEXT_PLAIN.isCompatibleWith(contentType)) {
				InputStreamReader reader = new InputStreamReader(inputMessage.getBody(), charset);
				TextFormat.merge(reader, this.extensionRegistry, builder);
			}
			else if (MediaType.APPLICATION_JSON.isCompatibleWith(contentType)) {
				JSON_FORMAT.merge(inputMessage.getBody(), charset, this.extensionRegistry, builder);
			}
			else if (MediaType.APPLICATION_XML.isCompatibleWith(contentType)) {
				XML_FORMAT.merge(inputMessage.getBody(), charset, this.extensionRegistry, builder);
			}
			else {
				builder.mergeFrom(inputMessage.getBody(), this.extensionRegistry);
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
		return (super.canWrite(mediaType) || MediaType.TEXT_HTML.isCompatibleWith(mediaType));
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

		if (MediaType.TEXT_PLAIN.isCompatibleWith(contentType)) {
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputMessage.getBody(), charset);
			TextFormat.print(message, outputStreamWriter);
			outputStreamWriter.flush();
		}
		else if (MediaType.APPLICATION_JSON.isCompatibleWith(contentType)) {
			JSON_FORMAT.print(message, outputMessage.getBody(), charset);
		}
		else if (MediaType.APPLICATION_XML.isCompatibleWith(contentType)) {
			XML_FORMAT.print(message, outputMessage.getBody(), charset);
		}
		else if (MediaType.TEXT_HTML.isCompatibleWith(contentType)) {
			HTML_FORMAT.print(message, outputMessage.getBody(), charset);
		}
		else if (PROTOBUF.isCompatibleWith(contentType)) {
			setProtoHeader(outputMessage, message);
			FileCopyUtils.copy(message.toByteArray(), outputMessage.getBody());
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

}
