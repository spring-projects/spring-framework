package org.springframework.http.codec;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.ByteBufferEncoder;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.tests.TestSubscriber;
import org.springframework.util.MimeTypeUtils;

/**
 * Unit tests for {@link EncoderHttpMessageWriter}.
 *
 * @author Marcin Kamionowski
 */
public class EncoderHttpMessageWriterTest {

	private EncoderHttpMessageWriter<ByteBuffer> writer = new EncoderHttpMessageWriter<>(new ByteBufferEncoder());

	private MockServerHttpResponse response = new MockServerHttpResponse();

	@Test
	public void writableMediaTypes() throws Exception {
		assertThat(writer.getWritableMediaTypes(), containsInAnyOrder(MimeTypeUtils.ALL));
	}

	@Test
	public void supportedMediaTypes() throws Exception {
		assertTrue(writer.canWrite(ResolvableType.forClass(ByteBuffer.class),
				MediaType.ALL));
		assertTrue(writer.canWrite(ResolvableType.forClass(ByteBuffer.class),
				MediaType.TEXT_PLAIN));
	}

	@Test
	public void encodeByteBuffer(){
		String payload = "Buffer payload";
		Mono<ByteBuffer> source = Mono.just(ByteBuffer.wrap(payload.getBytes(StandardCharsets.UTF_8)));

		writer.write(source, ResolvableType.forClass(ByteBuffer.class),
				MediaType.APPLICATION_OCTET_STREAM, response, Collections.emptyMap());

		assertThat(this.response.getHeaders().getContentType(), is(MediaType.APPLICATION_OCTET_STREAM));
		TestSubscriber.subscribe(response.getBodyAsString()).assertComplete().assertValues(payload);
	}
}