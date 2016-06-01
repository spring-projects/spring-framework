/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.http.converter;

import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.MockHttpOutputMessage;
import org.springframework.util.StringUtils;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Test cases for {@link ResourceRegionHttpMessageConverter} class.
 *
 * @author Brian Clozel
 */
public class ResourceRegionHttpMessageConverterTests {

	private final ResourceRegionHttpMessageConverter converter = new ResourceRegionHttpMessageConverter();

	@Test
	public void canReadResource() {
		assertFalse(converter.canRead(Resource.class, MediaType.APPLICATION_OCTET_STREAM));
		assertFalse(converter.canRead(Resource.class, MediaType.ALL));
		assertFalse(converter.canRead(List.class, MediaType.APPLICATION_OCTET_STREAM));
		assertFalse(converter.canRead(List.class, MediaType.ALL));
	}

	@Test
	public void canWriteResource() {
		assertTrue(converter.canWrite(ResourceRegion.class, null, MediaType.APPLICATION_OCTET_STREAM));
		assertTrue(converter.canWrite(ResourceRegion.class, null, MediaType.ALL));
	}

	@Test
	public void canWriteResourceCollection() {
		Type resourceRegionList = new ParameterizedTypeReference<List<ResourceRegion>>() {}.getType();
		assertTrue(converter.canWrite(resourceRegionList, null, MediaType.APPLICATION_OCTET_STREAM));
		assertTrue(converter.canWrite(resourceRegionList, null, MediaType.ALL));

		assertFalse(converter.canWrite(List.class, MediaType.APPLICATION_OCTET_STREAM));
		assertFalse(converter.canWrite(List.class, MediaType.ALL));
	}

	@Test
	public void shouldWritePartialContentByteRange() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Resource body = new ClassPathResource("byterangeresource.txt", getClass());
		ResourceRegion region = HttpRange.createByteRange(0, 5).toResourceRegion(body);
		converter.write(region, MediaType.TEXT_PLAIN, outputMessage);

		HttpHeaders headers = outputMessage.getHeaders();
		assertThat(headers.getContentType(), is(MediaType.TEXT_PLAIN));
		assertThat(headers.getContentLength(), is(6L));
		assertThat(headers.get(HttpHeaders.CONTENT_RANGE).size(), is(1));
		assertThat(headers.get(HttpHeaders.CONTENT_RANGE).get(0), is("bytes 0-5/39"));
		assertThat(outputMessage.getBodyAsString(Charset.forName("UTF-8")), is("Spring"));
	}

	@Test
	public void shouldWritePartialContentByteRangeNoEnd() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Resource body = new ClassPathResource("byterangeresource.txt", getClass());
		ResourceRegion region = HttpRange.createByteRange(7).toResourceRegion(body);
		converter.write(region, MediaType.TEXT_PLAIN, outputMessage);

		HttpHeaders headers = outputMessage.getHeaders();
		assertThat(headers.getContentType(), is(MediaType.TEXT_PLAIN));
		assertThat(headers.getContentLength(), is(32L));
		assertThat(headers.get(HttpHeaders.CONTENT_RANGE).size(), is(1));
		assertThat(headers.get(HttpHeaders.CONTENT_RANGE).get(0), is("bytes 7-38/39"));
		assertThat(outputMessage.getBodyAsString(Charset.forName("UTF-8")), is("Framework test resource content."));
	}

	@Test
	public void partialContentMultipleByteRanges() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Resource body = new ClassPathResource("byterangeresource.txt", getClass());
		List<HttpRange> rangeList = HttpRange.parseRanges("bytes=0-5,7-15,17-20,22-38");
		List<ResourceRegion> regions = new ArrayList<ResourceRegion>();
		for(HttpRange range : rangeList) {
			regions.add(range.toResourceRegion(body));
		}

		converter.write(regions, MediaType.TEXT_PLAIN, outputMessage);

		HttpHeaders headers = outputMessage.getHeaders();
		assertThat(headers.getContentType().toString(), Matchers.startsWith("multipart/byteranges;boundary="));
		String boundary = "--" + headers.getContentType().toString().substring(30);
		String content = outputMessage.getBodyAsString(Charset.forName("UTF-8"));
		String[] ranges = StringUtils.tokenizeToStringArray(content, "\r\n", false, true);

		assertThat(ranges[0], is(boundary));
		assertThat(ranges[1], is("Content-Type: text/plain"));
		assertThat(ranges[2], is("Content-Range: bytes 0-5/39"));
		assertThat(ranges[3], is("Spring"));

		assertThat(ranges[4], is(boundary));
		assertThat(ranges[5], is("Content-Type: text/plain"));
		assertThat(ranges[6], is("Content-Range: bytes 7-15/39"));
		assertThat(ranges[7], is("Framework"));

		assertThat(ranges[8], is(boundary));
		assertThat(ranges[9], is("Content-Type: text/plain"));
		assertThat(ranges[10], is("Content-Range: bytes 17-20/39"));
		assertThat(ranges[11], is("test"));

		assertThat(ranges[12], is(boundary));
		assertThat(ranges[13], is("Content-Type: text/plain"));
		assertThat(ranges[14], is("Content-Range: bytes 22-38/39"));
		assertThat(ranges[15], is("resource content."));
	}

}