/*
 * Copyright 2002-2019 the original author or authors.
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
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.MockHttpOutputMessage;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for {@link ResourceRegionHttpMessageConverter} class.
 *
 * @author Brian Clozel
 */
public class ResourceRegionHttpMessageConverterTests {

	private final ResourceRegionHttpMessageConverter converter = new ResourceRegionHttpMessageConverter();

	@Test
	public void canReadResource() {
		assertThat(converter.canRead(Resource.class, MediaType.APPLICATION_OCTET_STREAM)).isFalse();
		assertThat(converter.canRead(Resource.class, MediaType.ALL)).isFalse();
		assertThat(converter.canRead(List.class, MediaType.APPLICATION_OCTET_STREAM)).isFalse();
		assertThat(converter.canRead(List.class, MediaType.ALL)).isFalse();
	}

	@Test
	public void canWriteResource() {
		assertThat(converter.canWrite(ResourceRegion.class, null, MediaType.APPLICATION_OCTET_STREAM)).isTrue();
		assertThat(converter.canWrite(ResourceRegion.class, null, MediaType.ALL)).isTrue();
		assertThat(converter.canWrite(Object.class, null, MediaType.ALL)).isFalse();
	}

	@Test
	public void canWriteResourceCollection() {
		Type resourceRegionList = new ParameterizedTypeReference<List<ResourceRegion>>() {}.getType();
		assertThat(converter.canWrite(resourceRegionList, null, MediaType.APPLICATION_OCTET_STREAM)).isTrue();
		assertThat(converter.canWrite(resourceRegionList, null, MediaType.ALL)).isTrue();

		assertThat(converter.canWrite(List.class, MediaType.APPLICATION_OCTET_STREAM)).isFalse();
		assertThat(converter.canWrite(List.class, MediaType.ALL)).isFalse();
		Type resourceObjectList = new ParameterizedTypeReference<List<Object>>() {}.getType();
		assertThat(converter.canWrite(resourceObjectList, null, MediaType.ALL)).isFalse();
	}

	@Test
	public void shouldWritePartialContentByteRange() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Resource body = new ClassPathResource("byterangeresource.txt", getClass());
		ResourceRegion region = HttpRange.createByteRange(0, 5).toResourceRegion(body);
		converter.write(region, MediaType.TEXT_PLAIN, outputMessage);

		HttpHeaders headers = outputMessage.getHeaders();
		assertThat(headers.getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
		assertThat(headers.getContentLength()).isEqualTo(6L);
		assertThat(headers.get(HttpHeaders.CONTENT_RANGE)).hasSize(1);
		assertThat(headers.get(HttpHeaders.CONTENT_RANGE).get(0)).isEqualTo("bytes 0-5/39");
		assertThat(outputMessage.getBodyAsString(StandardCharsets.UTF_8)).isEqualTo("Spring");
	}

	@Test
	public void shouldWritePartialContentByteRangeNoEnd() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Resource body = new ClassPathResource("byterangeresource.txt", getClass());
		ResourceRegion region = HttpRange.createByteRange(7).toResourceRegion(body);
		converter.write(region, MediaType.TEXT_PLAIN, outputMessage);

		HttpHeaders headers = outputMessage.getHeaders();
		assertThat(headers.getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
		assertThat(headers.getContentLength()).isEqualTo(32L);
		assertThat(headers.get(HttpHeaders.CONTENT_RANGE)).hasSize(1);
		assertThat(headers.get(HttpHeaders.CONTENT_RANGE).get(0)).isEqualTo("bytes 7-38/39");
		assertThat(outputMessage.getBodyAsString(StandardCharsets.UTF_8)).isEqualTo("Framework test resource content.");
	}

	@Test
	public void partialContentMultipleByteRanges() throws Exception {
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
	public void partialContentMultipleByteRangesInRandomOrderAndOverlapping() throws Exception {
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
		ClassPathResource body = Mockito.mock(ClassPathResource.class);
		BDDMockito.given(body.getFilename()).willReturn("spring.dat");
		BDDMockito.given(body.contentLength()).willReturn(12L);
		BDDMockito.given(body.getInputStream()).willReturn(new ByteArrayInputStream("Spring Framework".getBytes()));
		HttpRange range = HttpRange.createByteRange(0, 5);
		ResourceRegion resourceRegion = range.toResourceRegion(body);

		converter.write(Collections.singletonList(resourceRegion), null, outputMessage);

		assertThat(outputMessage.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
		assertThat(outputMessage.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE)).isEqualTo("bytes 0-5/12");
		assertThat(outputMessage.getBodyAsString(StandardCharsets.UTF_8)).isEqualTo("Spring");
	}

}
