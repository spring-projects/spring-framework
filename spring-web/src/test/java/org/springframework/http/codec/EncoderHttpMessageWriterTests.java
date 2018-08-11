/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.http.codec;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.codec.Encoder;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.core.ResolvableType.forClass;
import static org.springframework.http.MediaType.TEXT_HTML;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.http.MediaType.TEXT_XML;

/**
 * Unit tests for {@link EncoderHttpMessageWriter}.
 * @author Rossen Stoyanchev
 */
public class EncoderHttpMessageWriterTests {

	private static final Map<String, Object> NO_HINTS = Collections.emptyMap();

	private static final MediaType TEXT_PLAIN_UTF_8 = new MediaType("text", "plain", UTF_8);


	@Mock
	private Encoder<String> encoder;

	private ArgumentCaptor<MediaType> mediaTypeCaptor;

	private MockServerHttpResponse response;


	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		this.mediaTypeCaptor = ArgumentCaptor.forClass(MediaType.class);
		this.response = new MockServerHttpResponse();
	}


	@Test
	public void getWritableMediaTypes() throws Exception {
		HttpMessageWriter<?> writer = getWriter(MimeTypeUtils.TEXT_HTML, MimeTypeUtils.TEXT_XML);
		assertEquals(Arrays.asList(TEXT_HTML, TEXT_XML), writer.getWritableMediaTypes());
	}

	@Test
	public void canWrite() throws Exception {
		HttpMessageWriter<?> writer = getWriter(MimeTypeUtils.TEXT_HTML);
		when(this.encoder.canEncode(forClass(String.class), TEXT_HTML)).thenReturn(true);

		assertTrue(writer.canWrite(forClass(String.class), TEXT_HTML));
		assertFalse(writer.canWrite(forClass(String.class), TEXT_XML));
	}

	@Test
	public void useNegotiatedMediaType() throws Exception {
		HttpMessageWriter<String> writer = getWriter(MimeTypeUtils.ALL);
		writer.write(Mono.just("body"), forClass(String.class), TEXT_PLAIN, this.response, NO_HINTS);

		assertEquals(TEXT_PLAIN, response.getHeaders().getContentType());
		assertEquals(TEXT_PLAIN, this.mediaTypeCaptor.getValue());
	}

	@Test
	public void useDefaultMediaType() throws Exception {
		testDefaultMediaType(null);
		testDefaultMediaType(new MediaType("text", "*"));
		testDefaultMediaType(new MediaType("*", "*"));
		testDefaultMediaType(MediaType.APPLICATION_OCTET_STREAM);
	}

	private void testDefaultMediaType(MediaType negotiatedMediaType) {

		this.response = new MockServerHttpResponse();
		this.mediaTypeCaptor = ArgumentCaptor.forClass(MediaType.class);

		MimeType defaultContentType = MimeTypeUtils.TEXT_XML;
		HttpMessageWriter<String> writer = getWriter(defaultContentType);
		writer.write(Mono.just("body"), forClass(String.class), negotiatedMediaType, this.response, NO_HINTS);

		assertEquals(defaultContentType, this.response.getHeaders().getContentType());
		assertEquals(defaultContentType, this.mediaTypeCaptor.getValue());
	}

	@Test
	public void useDefaultMediaTypeCharset() throws Exception {
		HttpMessageWriter<String> writer = getWriter(TEXT_PLAIN_UTF_8, TEXT_HTML);
		writer.write(Mono.just("body"), forClass(String.class), TEXT_HTML, response, NO_HINTS);

		assertEquals(new MediaType("text", "html", UTF_8), this.response.getHeaders().getContentType());
		assertEquals(new MediaType("text", "html", UTF_8), this.mediaTypeCaptor.getValue());
	}

	@Test
	public void useNegotiatedMediaTypeCharset() throws Exception {

		MediaType negotiatedMediaType = new MediaType("text", "html", ISO_8859_1);

		HttpMessageWriter<String> writer = getWriter(TEXT_PLAIN_UTF_8, TEXT_HTML);
		writer.write(Mono.just("body"), forClass(String.class), negotiatedMediaType, this.response, NO_HINTS);

		assertEquals(negotiatedMediaType, this.response.getHeaders().getContentType());
		assertEquals(negotiatedMediaType, this.mediaTypeCaptor.getValue());
	}

	@Test
	public void useHttpOutputMessageMediaType() throws Exception {

		MediaType outputMessageMediaType = MediaType.TEXT_HTML;
		this.response.getHeaders().setContentType(outputMessageMediaType);

		HttpMessageWriter<String> writer = getWriter(TEXT_PLAIN_UTF_8, TEXT_HTML);
		writer.write(Mono.just("body"), forClass(String.class), TEXT_PLAIN, this.response, NO_HINTS);

		assertEquals(outputMessageMediaType, this.response.getHeaders().getContentType());
		assertEquals(outputMessageMediaType, this.mediaTypeCaptor.getValue());
	}


	private HttpMessageWriter<String> getWriter(MimeType... mimeTypes) {
		List<MimeType> typeList = Arrays.asList(mimeTypes);
		when(this.encoder.getEncodableMimeTypes()).thenReturn(typeList);
		when(this.encoder.encode(any(), any(), any(), this.mediaTypeCaptor.capture(), any())).thenReturn(Flux.empty());
		return new EncoderHttpMessageWriter<>(this.encoder);
	}

}
