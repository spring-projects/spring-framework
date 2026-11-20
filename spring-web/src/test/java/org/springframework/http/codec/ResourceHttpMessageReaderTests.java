/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.testfixture.io.buffer.AbstractLeakCheckingTests;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.testfixture.http.client.reactive.MockClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ResourceHttpMessageReader}.
 *
 * @author Rossen Stoyanchev
 */
class ResourceHttpMessageReaderTests extends AbstractLeakCheckingTests {

	private final ResourceHttpMessageReader reader = new ResourceHttpMessageReader();

	@Test
	void readResourceAsMono() throws IOException {
		String filename = "test.txt";
		String body = "Test resource content";

		ContentDisposition contentDisposition =
				ContentDisposition.attachment().name("file").filename(filename).build();

		MockClientHttpResponse response = new MockClientHttpResponse(HttpStatus.OK);
		response.getHeaders().setContentType(MediaType.TEXT_PLAIN);
		response.getHeaders().setContentDisposition(contentDisposition);
		response.setBody(Mono.just(stringBuffer(body)));

		Resource resource = reader.readMono(
				ResolvableType.forClass(ByteArrayResource.class), response, Collections.emptyMap()).block();

		assertThat(resource).isNotNull();
		assertThat(resource.getFilename()).isEqualTo(filename);
		assertThat(resource.getInputStream()).hasContent(body);
	}

	private DataBuffer stringBuffer(String value) {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		DataBuffer buffer = this.bufferFactory.allocateBuffer(bytes.length);
		buffer.write(bytes);
		return buffer;
	}

}
