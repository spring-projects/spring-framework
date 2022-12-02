/*
 * Copyright 2002-2022 the original author or authors.
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
import java.nio.charset.StandardCharsets;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.http.MockHttpInputMessage;
import org.springframework.http.MockHttpOutputMessage;
import org.springframework.protobuf.Msg;
import org.springframework.protobuf.SecondMsg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test suite for {@link ProtobufHttpMessageConverter}.
 *
 * @author Alex Antonov
 * @author Juergen Hoeller
 * @author Andreas Ahlenstorf
 * @author Sebastien Deleuze
 * @author Sam Brannen
 */
@SuppressWarnings("deprecation")
class ProtobufHttpMessageConverterTests {

	private ExtensionRegistry extensionRegistry = mock(ExtensionRegistry.class);

	private ExtensionRegistryInitializer registryInitializer = mock(ExtensionRegistryInitializer.class);

	private ProtobufHttpMessageConverter converter = new ProtobufHttpMessageConverter(this.registryInitializer);

	private Msg testMsg = Msg.newBuilder().setFoo("Foo").setBlah(SecondMsg.newBuilder().setBlah(123).build()).build();


	@Test
	void extensionRegistryInitialized() {
		verify(this.registryInitializer, times(1)).initializeExtensionRegistry(any());
	}

	@Test
	void extensionRegistryInitializerNull() {
		ProtobufHttpMessageConverter converter = new ProtobufHttpMessageConverter((ExtensionRegistryInitializer)null);
		assertThat(converter.extensionRegistry).isNotNull();
	}

	@Test
	void extensionRegistryNull() {
		ProtobufHttpMessageConverter converter = new ProtobufHttpMessageConverter((ExtensionRegistry)null);
		assertThat(converter.extensionRegistry).isNotNull();
	}

	@Test
	void canRead() {
		assertThat(this.converter.canRead(Msg.class, null)).isTrue();
		assertThat(this.converter.canRead(Msg.class, ProtobufHttpMessageConverter.PROTOBUF)).isTrue();
		assertThat(this.converter.canRead(Msg.class, MediaType.APPLICATION_JSON)).isTrue();
		assertThat(this.converter.canRead(Msg.class, MediaType.APPLICATION_XML)).isTrue();
		assertThat(this.converter.canRead(Msg.class, MediaType.TEXT_PLAIN)).isTrue();

		// only supported as an output format
		assertThat(this.converter.canRead(Msg.class, MediaType.TEXT_HTML)).isFalse();
	}

	@Test
	void canWrite() {
		assertThat(this.converter.canWrite(Msg.class, null)).isTrue();
		assertThat(this.converter.canWrite(Msg.class, ProtobufHttpMessageConverter.PROTOBUF)).isTrue();
		assertThat(this.converter.canWrite(Msg.class, MediaType.APPLICATION_JSON)).isTrue();
		assertThat(this.converter.canWrite(Msg.class, MediaType.APPLICATION_XML)).isTrue();
		assertThat(this.converter.canWrite(Msg.class, MediaType.TEXT_PLAIN)).isTrue();
		assertThat(this.converter.canWrite(Msg.class, MediaType.TEXT_HTML)).isTrue();
	}

	@Test
	void read() throws IOException {
		byte[] body = this.testMsg.toByteArray();
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
		inputMessage.getHeaders().setContentType(ProtobufHttpMessageConverter.PROTOBUF);
		Message result = this.converter.read(Msg.class, inputMessage);
		assertThat(result).isEqualTo(this.testMsg);
	}

	@Test
	void readNoContentType() throws IOException {
		byte[] body = this.testMsg.toByteArray();
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
		Message result = this.converter.read(Msg.class, inputMessage);
		assertThat(result).isEqualTo(this.testMsg);
	}

	@Test
	void writeProtobuf() throws IOException {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		MediaType contentType = ProtobufHttpMessageConverter.PROTOBUF;
		this.converter.write(this.testMsg, contentType, outputMessage);
		assertThat(outputMessage.getHeaders().getContentType()).isEqualTo(contentType);
		assertThat(outputMessage.getBodyAsBytes().length > 0).isTrue();
		Message result = Msg.parseFrom(outputMessage.getBodyAsBytes());
		assertThat(result).isEqualTo(this.testMsg);

		String messageHeader =
				outputMessage.getHeaders().getFirst(ProtobufHttpMessageConverter.X_PROTOBUF_MESSAGE_HEADER);
		assertThat(messageHeader).isEqualTo("Msg");
		String schemaHeader =
				outputMessage.getHeaders().getFirst(ProtobufHttpMessageConverter.X_PROTOBUF_SCHEMA_HEADER);
		assertThat(schemaHeader).isEqualTo("sample.proto");
	}

	@Test
	void writeJsonWithGoogleProtobuf() throws IOException {
		this.converter = new ProtobufHttpMessageConverter(
				new ProtobufHttpMessageConverter.ProtobufJavaUtilSupport(null, null),
				this.extensionRegistry);
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		MediaType contentType = MediaType.APPLICATION_JSON;
		this.converter.write(this.testMsg, contentType, outputMessage);

		assertThat(outputMessage.getHeaders().getContentType()).isEqualTo(contentType);

		final String body = outputMessage.getBodyAsString(StandardCharsets.UTF_8);
		assertThat(body.isEmpty()).as("body is empty").isFalse();

		Msg.Builder builder = Msg.newBuilder();
		JsonFormat.parser().merge(body, builder);
		assertThat(builder.build()).isEqualTo(this.testMsg);

		assertThat(outputMessage.getHeaders().getFirst(
				ProtobufHttpMessageConverter.X_PROTOBUF_MESSAGE_HEADER)).isNull();
		assertThat(outputMessage.getHeaders().getFirst(
				ProtobufHttpMessageConverter.X_PROTOBUF_SCHEMA_HEADER)).isNull();
	}

	@Test
	void writeJsonWithJavaFormat() throws IOException {
		this.converter = new ProtobufHttpMessageConverter(
				new ProtobufHttpMessageConverter.ProtobufJavaFormatSupport(),
				this.extensionRegistry);
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		MediaType contentType = MediaType.APPLICATION_JSON_UTF8;
		this.converter.write(this.testMsg, contentType, outputMessage);

		assertThat(outputMessage.getHeaders().getContentType()).isEqualTo(contentType);

		final String body = outputMessage.getBodyAsString(StandardCharsets.UTF_8);
		assertThat(body.isEmpty()).as("body is empty").isFalse();

		Msg.Builder builder = Msg.newBuilder();
		JsonFormat.parser().merge(body, builder);
		assertThat(builder.build()).isEqualTo(this.testMsg);

		assertThat(outputMessage.getHeaders().getFirst(
				ProtobufHttpMessageConverter.X_PROTOBUF_MESSAGE_HEADER)).isNull();
		assertThat(outputMessage.getHeaders().getFirst(
				ProtobufHttpMessageConverter.X_PROTOBUF_SCHEMA_HEADER)).isNull();
	}

	@Test
	void defaultContentType() throws Exception {
		assertThat(this.converter.getDefaultContentType(this.testMsg))
				.isEqualTo(ProtobufHttpMessageConverter.PROTOBUF);
	}

	@Test
	void getContentLength() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		MediaType contentType = ProtobufHttpMessageConverter.PROTOBUF;
		this.converter.write(this.testMsg, contentType, outputMessage);
		assertThat(outputMessage.getHeaders().getContentLength()).isEqualTo(-1);
	}

}
