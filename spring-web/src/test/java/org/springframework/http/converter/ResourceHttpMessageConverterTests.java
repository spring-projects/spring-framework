/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.http.converter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.testfixture.http.MockHttpInputMessage;
import org.springframework.web.testfixture.http.MockHttpOutputMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

/**
 * @author Arjen Poutsma
 * @author Kazuki Shimizu
 * @author Brian Clozel
 */
class ResourceHttpMessageConverterTests {

	private final ResourceHttpMessageConverter converter = new ResourceHttpMessageConverter();


	@Test
	void canReadResource() {
		assertThat(converter.canRead(Resource.class, new MediaType("application", "octet-stream"))).isTrue();
	}

	@Test
	void canWriteResource() {
		assertThat(converter.canWrite(Resource.class, new MediaType("application", "octet-stream"))).isTrue();
		assertThat(converter.canWrite(Resource.class, MediaType.ALL)).isTrue();
	}

	@Test
	void shouldReadImageResource() throws IOException {
		byte[] body = FileCopyUtils.copyToByteArray(getClass().getResourceAsStream("logo.jpg"));
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
		inputMessage.getHeaders().setContentType(MediaType.IMAGE_JPEG);
		inputMessage.getHeaders().setContentDisposition(
				ContentDisposition.attachment().filename("yourlogo.jpg").build());
		Resource actualResource = converter.read(Resource.class, inputMessage);
		assertThat(FileCopyUtils.copyToByteArray(actualResource.getInputStream())).isEqualTo(body);
		assertThat(actualResource.getFilename()).isEqualTo("yourlogo.jpg");
	}

	@Test  // SPR-13443
	public void shouldReadInputStreamResource() throws IOException {
		try (InputStream body = getClass().getResourceAsStream("logo.jpg") ) {
			MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
			inputMessage.getHeaders().setContentType(MediaType.IMAGE_JPEG);
			inputMessage.getHeaders().setContentDisposition(
					ContentDisposition.attachment().filename("yourlogo.jpg").build());
			inputMessage.getHeaders().setContentLength(123);
			Resource actualResource = converter.read(InputStreamResource.class, inputMessage);
			assertThat(actualResource).isInstanceOf(InputStreamResource.class);
			assertThat(actualResource.getInputStream()).isEqualTo(body);
			assertThat(actualResource.getFilename()).isEqualTo("yourlogo.jpg");
			assertThat(actualResource.contentLength()).isEqualTo(123);
		}
	}

	@Test  // SPR-14882
	public void shouldNotReadInputStreamResource() throws IOException {
		ResourceHttpMessageConverter noStreamConverter = new ResourceHttpMessageConverter(false);
		try (InputStream body = getClass().getResourceAsStream("logo.jpg") ) {
			MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
			inputMessage.getHeaders().setContentType(MediaType.IMAGE_JPEG);
			assertThatExceptionOfType(HttpMessageNotReadableException.class).isThrownBy(() ->
					noStreamConverter.read(InputStreamResource.class, inputMessage));
		}
	}

	@Test
	void shouldWriteImageResource() throws IOException {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Resource body = new ClassPathResource("logo.jpg", getClass());
		converter.write(body, null, outputMessage);

		assertThat(outputMessage.getHeaders().getContentType())
				.as("Invalid content-type").isEqualTo(MediaType.IMAGE_JPEG);
		assertThat(outputMessage.getHeaders().getContentLength())
				.as("Invalid content-length").isEqualTo(body.getFile().length());
	}

	@Test  // SPR-10848
	public void writeByteArrayNullMediaType() throws IOException {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		byte[] byteArray = {1, 2, 3};
		Resource body = new ByteArrayResource(byteArray);
		converter.write(body, null, outputMessage);

		assertThat(Arrays.equals(byteArray, outputMessage.getBodyAsBytes())).isTrue();
	}

	@Test  // SPR-12999
	public void writeContentNotGettingInputStream() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Resource resource = mock();
		given(resource.getInputStream()).willThrow(FileNotFoundException.class);
		converter.write(resource, MediaType.APPLICATION_OCTET_STREAM, outputMessage);

		assertThat(outputMessage.getHeaders().getContentLength()).isEqualTo(0);
	}

	@Test  // SPR-12999
	public void writeContentNotClosingInputStream() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Resource resource = mock();
		InputStream inputStream = mock();
		given(resource.getInputStream()).willReturn(inputStream);
		given(inputStream.read(any())).willReturn(-1);
		willThrow(new NullPointerException()).given(inputStream).close();
		converter.write(resource, MediaType.APPLICATION_OCTET_STREAM, outputMessage);

		assertThat(outputMessage.getHeaders().getContentLength()).isEqualTo(0);
	}

	@Test  // SPR-13620
	public void writeContentInputStreamThrowingNullPointerException() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Resource resource = mock();
		InputStream in = mock();
		given(resource.getInputStream()).willReturn(in);
		given(in.read(any())).willThrow(NullPointerException.class);
		converter.write(resource, MediaType.APPLICATION_OCTET_STREAM, outputMessage);

		assertThat(outputMessage.getHeaders().getContentLength()).isEqualTo(0);
	}
	@Test
	void canReadWithNullMediaType() {
		assertThat(converter.canRead(Resource.class, null)).isTrue();
	}

	@Test
	void canWriteWithNullMediaType() {
		assertThat(converter.canWrite(Resource.class, null)).isTrue();
	}

	@Test
	void shouldReadWithNullContentDisposition() throws IOException {
		byte[] body = FileCopyUtils.copyToByteArray(getClass().getResourceAsStream("logo.jpg"));
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
		inputMessage.getHeaders().setContentType(MediaType.IMAGE_JPEG);
		Resource actualResource = converter.read(Resource.class, inputMessage);
		assertThat(FileCopyUtils.copyToByteArray(actualResource.getInputStream())).isEqualTo(body);
		assertThat(actualResource.getFilename()).isNull();
	}

	@Test
	void getContentLengthWithInputStreamResource() throws IOException {
		try (InputStream body = getClass().getResourceAsStream("logo.jpg")) {
			InputStreamResource resource = new InputStreamResource(body);
			Long contentLength = converter.getContentLength(resource, MediaType.IMAGE_JPEG);
			assertThat(contentLength).isNull();
		}
	}

	@Test
	void getContentLengthWithByteArrayResource() throws IOException {
		byte[] bytes = new byte[100];
		ByteArrayResource resource = new ByteArrayResource(bytes);
		Long contentLength = converter.getContentLength(resource, MediaType.APPLICATION_OCTET_STREAM);
		assertThat(contentLength).isEqualTo(100);
	}

	@Test
	void supportsRepeatableWritesWithInputStreamResource() {
		InputStreamResource resource = new InputStreamResource(mock(InputStream.class));
		assertThat(converter.supportsRepeatableWrites(resource)).isFalse();
	}

	@Test
	void supportsRepeatableWritesWithByteArrayResource() {
		ByteArrayResource resource = new ByteArrayResource(new byte[0]);
		assertThat(converter.supportsRepeatableWrites(resource)).isTrue();
	}

	@Test
	void getDefaultContentTypeWithKnownExtension() throws IOException {
		Resource resource = new ClassPathResource("logo.jpg", getClass());
		MediaType mediaType = converter.getDefaultContentType(resource);
		assertThat(mediaType).isEqualTo(MediaType.IMAGE_JPEG);
	}

	@Test
	void getDefaultContentTypeWithUnknownExtension() throws IOException {
		Resource resource = new ByteArrayResource(new byte[0]) {
			@Override
			public String getFilename() {
				return "test.unknown";
			}
		};
		MediaType mediaType = converter.getDefaultContentType(resource);
		assertThat(mediaType).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
	}

	@Test
	void getContentLengthWithUnknownLengthShouldReturnNull() throws IOException {
		Resource resource = new ByteArrayResource(new byte[0]) {
			@Override
			public long contentLength() {
				return -1;
			}
		};
		assertThat(converter.getContentLength(resource, MediaType.APPLICATION_OCTET_STREAM)).isNull();
	}

	@Test
	void getDefaultContentTypeWithNoFilenameShouldFallbackToOctetStream() {
		Resource resource = new ByteArrayResource(new byte[0]) {
			@Override
			public String getFilename() {
				return null;
			}
		};
		assertThat(converter.getDefaultContentType(resource)).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
	}

	@Test
	void supportsWithNonResourceClassShouldReturnFalse() {
		assertThat(converter.canRead(String.class, MediaType.APPLICATION_OCTET_STREAM)).isFalse();
		assertThat(converter.canWrite(String.class, MediaType.APPLICATION_OCTET_STREAM)).isFalse();
	}

	@Test
	void shouldWriteWithCustomContentDisposition() throws IOException {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Resource body = new ClassPathResource("logo.jpg", getClass());
		ContentDisposition contentDisposition = ContentDisposition.inline()
				.filename("custom_logo.jpg")
				.build();
		outputMessage.getHeaders().setContentDisposition(contentDisposition);

		converter.write(body, null, outputMessage);

		assertThat(outputMessage.getHeaders().getContentDisposition().getFilename())
				.isEqualTo("custom_logo.jpg");
	}

	@Test
	void writeContentWhenInputStreamIsEmpty() throws IOException {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Resource resource = new ByteArrayResource(new byte[0]);
		converter.write(resource, MediaType.APPLICATION_OCTET_STREAM, outputMessage);
		assertThat(outputMessage.getBodyAsBytes()).isEmpty();
	}

}
