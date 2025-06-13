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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.testfixture.http.MockHttpOutputMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test cases for {@link ResourceRegionHttpMessageConverter} class.
 *
 * @author Brian Clozel
 */
class ResourceRegionHttpMessageConverterTests {

	private final ResourceRegionHttpMessageConverter converter = new ResourceRegionHttpMessageConverter();

	@Test
	void canReadResource() {
		assertThat(converter.canRead(Resource.class, MediaType.APPLICATION_OCTET_STREAM)).isFalse();
		assertThat(converter.canRead(Resource.class, MediaType.ALL)).isFalse();
		assertThat(converter.canRead(List.class, MediaType.APPLICATION_OCTET_STREAM)).isFalse();
		assertThat(converter.canRead(List.class, MediaType.ALL)).isFalse();
	}

	@Test
	void canWriteResource() {
		assertThat(converter.canWrite(ResourceRegion.class, null, MediaType.APPLICATION_OCTET_STREAM)).isTrue();
		assertThat(converter.canWrite(ResourceRegion.class, null, MediaType.ALL)).isTrue();
		assertThat(converter.canWrite(Object.class, null, MediaType.ALL)).isFalse();
	}

	@Test
	void canWriteResourceCollection() {
		Type resourceRegionList = new ParameterizedTypeReference<List<ResourceRegion>>() {}.getType();
		assertThat(converter.canWrite(resourceRegionList, null, MediaType.APPLICATION_OCTET_STREAM)).isTrue();
		assertThat(converter.canWrite(resourceRegionList, null, MediaType.ALL)).isTrue();

		assertThat(converter.canWrite(List.class, MediaType.APPLICATION_OCTET_STREAM)).isFalse();
		assertThat(converter.canWrite(List.class, MediaType.ALL)).isFalse();
		Type resourceObjectList = new ParameterizedTypeReference<List<Object>>() {}.getType();
		assertThat(converter.canWrite(resourceObjectList, null, MediaType.ALL)).isFalse();
	}

	@Test
	void shouldWritePartialContentByteRange() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Resource body = new ClassPathResource("byterangeresource.txt", getClass());
		ResourceRegion region = HttpRange.createByteRange(0, 5).toResourceRegion(body);
		converter.write(region, MediaType.TEXT_PLAIN, outputMessage);

		HttpHeaders headers = outputMessage.getHeaders();
		assertThat(headers.getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
		assertThat(headers.getContentLength()).isEqualTo(6L);
		assertThat(headers.get(HttpHeaders.CONTENT_RANGE)).containsExactly("bytes 0-5/39");
		assertThat(outputMessage.getBodyAsString(StandardCharsets.UTF_8)).isEqualTo("Spring");
	}

	@Test
	void shouldWritePartialContentByteRangeNoEnd() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Resource body = new ClassPathResource("byterangeresource.txt", getClass());
		ResourceRegion region = HttpRange.createByteRange(7).toResourceRegion(body);
		converter.write(region, MediaType.TEXT_PLAIN, outputMessage);

		HttpHeaders headers = outputMessage.getHeaders();
		assertThat(headers.getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
		assertThat(headers.getContentLength()).isEqualTo(32L);
		assertThat(headers.get(HttpHeaders.CONTENT_RANGE)).containsExactly("bytes 7-38/39");
		assertThat(outputMessage.getBodyAsString(StandardCharsets.UTF_8)).isEqualTo("Framework test resource content.");
	}

	@Test
	void partialContentMultipleByteRanges() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Resource body = new ClassPathResource("byterangeresource.txt", getClass());
		List<HttpRange> rangeList = HttpRange.parseRanges("bytes=0-5,7-15,17-20,22-38");
		List<ResourceRegion> regions = new ArrayList<>();
		for(HttpRange range : rangeList) {
			regions.add(range.toResourceRegion(body));
		}

		converter.write(regions, MediaType.TEXT_PLAIN, outputMessage);

		HttpHeaders headers = outputMessage.getHeaders();
		assertThat(headers.getContentType().toString()).startsWith("multipart/byteranges;boundary=");
		String boundary = "--" + headers.getContentType().toString().substring(30);
		String content = outputMessage.getBodyAsString(StandardCharsets.UTF_8);
		String[] ranges = StringUtils.tokenizeToStringArray(content, "\r\n", false, true);

		assertThat(ranges[0]).isEqualTo(boundary);
		assertThat(ranges[1]).isEqualTo("Content-Type: text/plain");
		assertThat(ranges[2]).isEqualTo("Content-Range: bytes 0-5/39");
		assertThat(ranges[3]).isEqualTo("Spring");

		assertThat(ranges[4]).isEqualTo(boundary);
		assertThat(ranges[5]).isEqualTo("Content-Type: text/plain");
		assertThat(ranges[6]).isEqualTo("Content-Range: bytes 7-15/39");
		assertThat(ranges[7]).isEqualTo("Framework");

		assertThat(ranges[8]).isEqualTo(boundary);
		assertThat(ranges[9]).isEqualTo("Content-Type: text/plain");
		assertThat(ranges[10]).isEqualTo("Content-Range: bytes 17-20/39");
		assertThat(ranges[11]).isEqualTo("test");

		assertThat(ranges[12]).isEqualTo(boundary);
		assertThat(ranges[13]).isEqualTo("Content-Type: text/plain");
		assertThat(ranges[14]).isEqualTo("Content-Range: bytes 22-38/39");
		assertThat(ranges[15]).isEqualTo("resource content.");
	}

	@Test
	void partialContentMultipleByteRangesInRandomOrderAndOverlapping() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Resource body = new ClassPathResource("byterangeresource.txt", getClass());
		List<HttpRange> rangeList = HttpRange.parseRanges("bytes=7-15,0-5,17-20,20-29");
		List<ResourceRegion> regions = new ArrayList<>();
		for(HttpRange range : rangeList) {
			regions.add(range.toResourceRegion(body));
		}

		converter.write(regions, MediaType.TEXT_PLAIN, outputMessage);

		HttpHeaders headers = outputMessage.getHeaders();
		assertThat(headers.getContentType().toString()).startsWith("multipart/byteranges;boundary=");
		String boundary = "--" + headers.getContentType().toString().substring(30);
		String content = outputMessage.getBodyAsString(StandardCharsets.UTF_8);
		String[] ranges = StringUtils.tokenizeToStringArray(content, "\r\n", false, true);

		assertThat(ranges[0]).isEqualTo(boundary);
		assertThat(ranges[1]).isEqualTo("Content-Type: text/plain");
		assertThat(ranges[2]).isEqualTo("Content-Range: bytes 7-15/39");
		assertThat(ranges[3]).isEqualTo("Framework");

		assertThat(ranges[4]).isEqualTo(boundary);
		assertThat(ranges[5]).isEqualTo("Content-Type: text/plain");
		assertThat(ranges[6]).isEqualTo("Content-Range: bytes 0-5/39");
		assertThat(ranges[7]).isEqualTo("Spring");

		assertThat(ranges[8]).isEqualTo(boundary);
		assertThat(ranges[9]).isEqualTo("Content-Type: text/plain");
		assertThat(ranges[10]).isEqualTo("Content-Range: bytes 17-20/39");
		assertThat(ranges[11]).isEqualTo("test");

		assertThat(ranges[12]).isEqualTo(boundary);
		assertThat(ranges[13]).isEqualTo("Content-Type: text/plain");
		assertThat(ranges[14]).isEqualTo("Content-Range: bytes 20-29/39");
		assertThat(ranges[15]).isEqualTo("t resource");
	}

	@Test // SPR-15041
	public void applicationOctetStreamDefaultContentType() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		ClassPathResource body = mock();
		given(body.getFilename()).willReturn("spring.dat");
		given(body.contentLength()).willReturn(12L);
		given(body.getInputStream()).willReturn(new ByteArrayInputStream("Spring Framework".getBytes()));
		HttpRange range = HttpRange.createByteRange(0, 5);
		ResourceRegion resourceRegion = range.toResourceRegion(body);

		converter.write(Collections.singletonList(resourceRegion), null, outputMessage);

		assertThat(outputMessage.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
		assertThat(outputMessage.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE)).isEqualTo("bytes 0-5/12");
		assertThat(outputMessage.getBodyAsString(StandardCharsets.UTF_8)).isEqualTo("Spring");
	}

	@Test
	void shouldNotWriteForUnsupportedType() {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Object unsupportedBody = new Object();

		assertThatThrownBy(() -> converter.write(unsupportedBody, null, outputMessage))
				.isInstanceOfAny(ClassCastException.class, HttpMessageNotWritableException.class);
	}

	@Test
	void shouldGetDefaultContentTypeForResourceRegion() {
		Resource resource = new ClassPathResource("byterangeresource.txt", getClass());
		ResourceRegion region = new ResourceRegion(resource, 0, 10);

		MediaType contentType = converter.getDefaultContentType(region);
		assertThat(contentType).isEqualTo(MediaType.TEXT_PLAIN);
	}

	@Test
	void shouldGetDefaultOctetStreamContentTypeForUnknownResource() {
		Resource resource = mock(Resource.class);
		given(resource.getFilename()).willReturn("unknown.dat");
		ResourceRegion region = new ResourceRegion(resource, 0, 10);

		MediaType contentType = converter.getDefaultContentType(region);
		assertThat(contentType).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
	}

	@Test
	void shouldSupportRepeatableWritesForNonInputStreamResource() {
		Resource resource = new ClassPathResource("byterangeresource.txt", getClass());
		ResourceRegion region = new ResourceRegion(resource, 0, 10);

		assertThat(converter.supportsRepeatableWrites(region)).isTrue();
	}

	@Test
	void shouldNotSupportRepeatableWritesForInputStreamResource() {
		Resource resource = mock(InputStreamResource.class);
		ResourceRegion region = new ResourceRegion(resource, 0, 10);

		assertThat(converter.supportsRepeatableWrites(region)).isFalse();
	}

	@Test
	void shouldHandleIOExceptionWhenWritingRegion() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Resource resource = mock(Resource.class);
		given(resource.contentLength()).willReturn(10L);
		given(resource.getInputStream()).willThrow(new IOException("Simulated error"));
		ResourceRegion region = new ResourceRegion(resource, 0, 5);

		// Should not throw exception
		converter.write(region, MediaType.TEXT_PLAIN, outputMessage);

		// Verify Content-Range header is set correctly
		assertThat(outputMessage.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE))
				.isEqualTo("bytes 0-4/10");

		// Verify no content was written due to the IOException
		assertThat(outputMessage.getBodyAsString(StandardCharsets.UTF_8)).isEmpty();
	}
	@Test
	void shouldHandleIOExceptionWhenWritingRegionCollection() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Resource resource = mock(Resource.class);
		given(resource.contentLength()).willReturn(10L);
		given(resource.getInputStream()).willThrow(new IOException("Simulated error"));
		ResourceRegion region = new ResourceRegion(resource, 0, 5);
		List<ResourceRegion> regions = Collections.singletonList(region);

		// Should not throw exception
		converter.write(regions, MediaType.TEXT_PLAIN, outputMessage);

		assertThat(outputMessage.getHeaders().getContentType().toString())
				.isEqualTo("text/plain");
	}

	@Test
	void shouldHandleNullResourceRegion() {
		assertThatThrownBy(() -> converter.write(null, null, new MockHttpOutputMessage()))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	void shouldHandleInvalidRangeBeyondResourceLength() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Resource resource = new ClassPathResource("byterangeresource.txt", getClass());
		ResourceRegion region = new ResourceRegion(resource, 35, 10); // Goes beyond resource length

		converter.write(region, MediaType.TEXT_PLAIN, outputMessage);

		assertThat(outputMessage.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE))
				.isEqualTo("bytes 35-38/39");
		assertThat(outputMessage.getBodyAsString(StandardCharsets.UTF_8)).hasSize(4);
	}

	@Test
	void shouldHandleZeroLengthResourceRegion() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Resource resource = new ClassPathResource("byterangeresource.txt", getClass());
		ResourceRegion region = new ResourceRegion(resource, 5, 0);

		converter.write(region, MediaType.TEXT_PLAIN, outputMessage);

		assertThat(outputMessage.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE))
				.isEqualTo("bytes 5-4/39");
		assertThat(outputMessage.getBodyAsString(StandardCharsets.UTF_8)).isEmpty();
	}

	@Test
	void shouldHandleMultipleResourcesInCollection() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Resource resource1 = new ClassPathResource("byterangeresource.txt", getClass());
		Resource resource2 = new ClassPathResource("byterangeresource.txt", getClass());
		List<ResourceRegion> regions = List.of(
				new ResourceRegion(resource1, 0, 5),  // "Spring" is 6 bytes (0-5)
				new ResourceRegion(resource2, 7, 8)  // "Framework" is 8 bytes (7-14)
		);

		converter.write(regions, MediaType.TEXT_PLAIN, outputMessage);

		String content = outputMessage.getBodyAsString(StandardCharsets.UTF_8);

		// Verify multipart structure
		assertThat(content).contains("Content-Type: text/plain");
		assertThat(content).contains("Content-Range: bytes 7-14/39");

		// Verify partial content (note the ranges only include parts of the words)
		assertThat(content).contains("Sprin");  // First 5 bytes of "Spring" (0-4)
		assertThat(content).contains("Framewor"); // First 7 bytes of "Framework" (7-13)
	}
	@Test
	void shouldHandleNullContentType() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Resource resource = new ClassPathResource("byterangeresource.txt", getClass());
		ResourceRegion region = new ResourceRegion(resource, 0, 5);

		converter.write(region, null, outputMessage);

		assertThat(outputMessage.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
	}

	@Test
	void shouldHandleUnreadableResource() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Resource resource = mock(Resource.class);
		given(resource.contentLength()).willReturn(10L);
		given(resource.getInputStream()).willThrow(new IOException("Cannot read resource"));
		ResourceRegion region = new ResourceRegion(resource, 0, 5);

		converter.write(region, MediaType.TEXT_PLAIN, outputMessage);

		assertThat(outputMessage.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE))
				.isEqualTo("bytes 0-4/10");
		assertThat(outputMessage.getBodyAsString(StandardCharsets.UTF_8)).isEmpty();
	}

	@Test
	void shouldHandleCanWriteWithNullType() {
		assertThat(converter.canWrite(null, null, null)).isFalse();
	}

	@Test
	void shouldHandleCanWriteWithNonParameterizedType() {
		assertThat(converter.canWrite(ResourceRegion.class, null, null)).isTrue();
		assertThat(converter.canWrite(String.class, null, null)).isFalse();
	}


}
