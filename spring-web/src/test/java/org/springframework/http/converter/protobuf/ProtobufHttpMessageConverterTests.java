/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.converter.protobuf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.protobuf.Msg;
import org.springframework.protobuf.SecondMsg;
import org.springframework.web.testfixture.http.MockHttpInputMessage;
import org.springframework.web.testfixture.http.MockHttpOutputMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ProtobufHttpMessageConverter}.
 *
 * @author Alex Antonov
 * @author Juergen Hoeller
 * @author Andreas Ahlenstorf
 * @author Sebastien Deleuze
 */
class ProtobufHttpMessageConverterTests {

	private ProtobufHttpMessageConverter converter = new ProtobufHttpMessageConverter();

	private final ExtensionRegistry extensionRegistry = mock();

	private final Msg testMsg = Msg.newBuilder().setFoo("Foo").setBlah(SecondMsg.newBuilder().setBlah(123).build()).build();
	private final MediaType testPlusProtoMediaType = MediaType.parseMediaType("application/vnd.example.public.v1+x-protobuf");


	@Test
	void canRead() {
		assertThat(this.converter.canRead(Msg.class, null)).isTrue();
		assertThat(this.converter.canRead(Msg.class, ProtobufHttpMessageConverter.PROTOBUF)).isTrue();
		assertThat(this.converter.canRead(Msg.class, this.testPlusProtoMediaType)).isTrue();
		assertThat(this.converter.canRead(Msg.class, MediaType.APPLICATION_JSON)).isTrue();
		assertThat(this.converter.canRead(Msg.class, MediaType.TEXT_PLAIN)).isTrue();
	}

	@Test
	void canWrite() {
		assertThat(this.converter.canWrite(Msg.class, null)).isTrue();
		assertThat(this.converter.canWrite(Msg.class, ProtobufHttpMessageConverter.PROTOBUF)).isTrue();
		assertThat(this.converter.canWrite(Msg.class, this.testPlusProtoMediaType)).isTrue();
		assertThat(this.converter.canWrite(Msg.class, MediaType.APPLICATION_JSON)).isTrue();
		assertThat(this.converter.canWrite(Msg.class, MediaType.TEXT_PLAIN)).isTrue();
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
		assertThat(outputMessage.getBodyAsBytes().length).isGreaterThan(0);
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
				new ProtobufHttpMessageConverter.ProtobufJavaUtilDelegate(null, null),
				this.extensionRegistry);
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		MediaType contentType = MediaType.APPLICATION_JSON;
		this.converter.write(this.testMsg, contentType, outputMessage);

		assertThat(outputMessage.getHeaders().getContentType()).isEqualTo(contentType);

		final String body = outputMessage.getBodyAsString(StandardCharsets.UTF_8);
		assertThat(body).as("body is empty").isNotEmpty();

		Msg.Builder builder = Msg.newBuilder();
		JsonFormat.parser().merge(body, builder);
		assertThat(builder.build()).isEqualTo(this.testMsg);

		assertThat(outputMessage.getHeaders().getFirst(
				ProtobufHttpMessageConverter.X_PROTOBUF_MESSAGE_HEADER)).isNull();
		assertThat(outputMessage.getHeaders().getFirst(
				ProtobufHttpMessageConverter.X_PROTOBUF_SCHEMA_HEADER)).isNull();
	}

	@Test
	void defaultContentType() {
		assertThat(this.converter.getDefaultContentType(this.testMsg))
				.isEqualTo(ProtobufHttpMessageConverter.PROTOBUF);
	}

	@Test
	void getContentLength() throws IOException {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		MediaType contentType = ProtobufHttpMessageConverter.PROTOBUF;
		this.converter.write(this.testMsg, contentType, outputMessage);
		assertThat(outputMessage.getHeaders().getContentLength()).isEqualTo(-1);
	}

}
