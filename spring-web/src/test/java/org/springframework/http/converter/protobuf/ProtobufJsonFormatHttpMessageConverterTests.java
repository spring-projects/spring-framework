/*
 * Copyright 2002-2021 the original author or authors.
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

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.http.MockHttpInputMessage;
import org.springframework.http.MockHttpOutputMessage;
import org.springframework.protobuf.Msg;
import org.springframework.protobuf.SecondMsg;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test suite for {@link ProtobufJsonFormatHttpMessageConverter}.
 *
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 */
public class ProtobufJsonFormatHttpMessageConverterTests {

	private final ProtobufHttpMessageConverter converter = new ProtobufJsonFormatHttpMessageConverter(
			JsonFormat.parser(), JsonFormat.printer());

	private final Msg testMsg = Msg.newBuilder().setFoo("Foo").setBlah(SecondMsg.newBuilder().setBlah(123).build()).build();


	@Test
	public void canRead() {
		assertThat(this.converter.canRead(Msg.class, null)).isTrue();
		assertThat(this.converter.canRead(Msg.class, ProtobufHttpMessageConverter.PROTOBUF)).isTrue();
		assertThat(this.converter.canRead(Msg.class, MediaType.APPLICATION_JSON)).isTrue();
		assertThat(this.converter.canRead(Msg.class, MediaType.TEXT_PLAIN)).isTrue();
	}

	@Test
	public void canWrite() {
		assertThat(this.converter.canWrite(Msg.class, null)).isTrue();
		assertThat(this.converter.canWrite(Msg.class, ProtobufHttpMessageConverter.PROTOBUF)).isTrue();
		assertThat(this.converter.canWrite(Msg.class, MediaType.APPLICATION_JSON)).isTrue();
		assertThat(this.converter.canWrite(Msg.class, MediaType.TEXT_PLAIN)).isTrue();
	}

	@Test
	public void read() throws IOException {
		byte[] body = this.testMsg.toByteArray();
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
		inputMessage.getHeaders().setContentType(ProtobufHttpMessageConverter.PROTOBUF);
		Message result = this.converter.read(Msg.class, inputMessage);
		assertThat(result).isEqualTo(this.testMsg);
	}

	@Test
	public void readNoContentType() throws IOException {
		byte[] body = this.testMsg.toByteArray();
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
		Message result = this.converter.read(Msg.class, inputMessage);
		assertThat(result).isEqualTo(this.testMsg);
	}

	@Test
	public void write() throws IOException {
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
	public void defaultContentType() throws Exception {
		assertThat(this.converter.getDefaultContentType(this.testMsg)).isEqualTo(ProtobufHttpMessageConverter.PROTOBUF);
	}

	@Test
	public void getContentLength() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		MediaType contentType = ProtobufHttpMessageConverter.PROTOBUF;
		this.converter.write(this.testMsg, contentType, outputMessage);
		assertThat(outputMessage.getHeaders().getContentLength()).isEqualTo(-1);
	}

}
