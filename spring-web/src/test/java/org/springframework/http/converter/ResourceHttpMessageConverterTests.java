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

import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsInstanceOf.*;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpRangeResource;
import org.springframework.http.MediaType;
import org.springframework.http.MockHttpInputMessage;
import org.springframework.http.MockHttpOutputMessage;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * @author Arjen Poutsma
 * @author Kazuki Shimizu
 * @author Brian Clozel
 */
public class ResourceHttpMessageConverterTests {

	private final ResourceHttpMessageConverter converter = new ResourceHttpMessageConverter();


	@Test
	public void canReadResource() {
		assertTrue(converter.canRead(Resource.class, new MediaType("application", "octet-stream")));
	}

	@Test
	public void canWriteResource() {
		assertTrue(converter.canWrite(Resource.class, new MediaType("application", "octet-stream")));
		assertTrue(converter.canWrite(Resource.class, MediaType.ALL));
	}

	@Test
	public void shouldReadImageResource() throws IOException {
		byte[] body = FileCopyUtils.copyToByteArray(getClass().getResourceAsStream("logo.jpg"));
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
		inputMessage.getHeaders().setContentType(MediaType.IMAGE_JPEG);
		Resource actualResource = converter.read(Resource.class, inputMessage);
		assertThat(FileCopyUtils.copyToByteArray(actualResource.getInputStream()), is(body));
	}

	@Test  // SPR-13443
	public void shouldReadInputStreamResource() throws IOException {
		try (InputStream body = getClass().getResourceAsStream("logo.jpg") ) {
			MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
			inputMessage.getHeaders().setContentType(MediaType.IMAGE_JPEG);
			Resource actualResource = converter.read(InputStreamResource.class, inputMessage);
			assertThat(actualResource, instanceOf(InputStreamResource.class));
			assertThat(actualResource.getInputStream(), is(body));
		}
	}

	@Test
	public void shouldWriteImageResource() throws IOException {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Resource body = new ClassPathResource("logo.jpg", getClass());
		converter.write(body, null, outputMessage);
		assertEquals("Invalid content-type", MediaType.IMAGE_JPEG,
				outputMessage.getHeaders().getContentType());
		assertEquals("Invalid content-length", body.getFile().length(), outputMessage.getHeaders().getContentLength());
	}

	@Test
	public void shouldWritePartialContentByteRange() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Resource body = new ClassPathResource("byterangeresource.txt", getClass());
		List<HttpRange> httpRangeList = HttpRange.parseRanges("bytes=0-5");

		converter.write(new HttpRangeResource(httpRangeList, body), MediaType.TEXT_PLAIN, outputMessage);

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
		List<HttpRange> httpRangeList = HttpRange.parseRanges("bytes=7-");

		converter.write(new HttpRangeResource(httpRangeList, body), MediaType.TEXT_PLAIN, outputMessage);

		HttpHeaders headers = outputMessage.getHeaders();
		assertThat(headers.getContentType(), is(MediaType.TEXT_PLAIN));
		assertThat(headers.getContentLength(), is(32L));
		assertThat(headers.get(HttpHeaders.CONTENT_RANGE).size(), is(1));
		assertThat(headers.get(HttpHeaders.CONTENT_RANGE).get(0), is("bytes 7-38/39"));
		assertThat(outputMessage.getBodyAsString(Charset.forName("UTF-8")), is("Framework test resource content."));
	}

	@Test
	public void shouldWritePartialContentByteRangeLargeEnd() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Resource body = new ClassPathResource("byterangeresource.txt", getClass());
		List<HttpRange> httpRangeList = HttpRange.parseRanges("bytes=7-10000");

		converter.write(new HttpRangeResource(httpRangeList, body), MediaType.TEXT_PLAIN, outputMessage);

		HttpHeaders headers = outputMessage.getHeaders();
		assertThat(headers.getContentType(), is(MediaType.TEXT_PLAIN));
		assertThat(headers.getContentLength(), is(32L));
		assertThat(headers.get(HttpHeaders.CONTENT_RANGE).size(), is(1));
		assertThat(headers.get(HttpHeaders.CONTENT_RANGE).get(0), is("bytes 7-38/39"));
		assertThat(outputMessage.getBodyAsString(Charset.forName("UTF-8")), is("Framework test resource content."));
	}

	@Test
	public void shouldWritePartialContentSuffixRange() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Resource body = new ClassPathResource("byterangeresource.txt", getClass());
		List<HttpRange> httpRangeList = HttpRange.parseRanges("bytes=-8");

		converter.write(new HttpRangeResource(httpRangeList, body), MediaType.TEXT_PLAIN, outputMessage);

		HttpHeaders headers = outputMessage.getHeaders();
		assertThat(headers.getContentType(), is(MediaType.TEXT_PLAIN));
		assertThat(headers.getContentLength(), is(8L));
		assertThat(headers.get(HttpHeaders.CONTENT_RANGE).size(), is(1));
		assertThat(headers.get(HttpHeaders.CONTENT_RANGE).get(0), is("bytes 31-38/39"));
		assertThat(outputMessage.getBodyAsString(Charset.forName("UTF-8")), is("content."));
	}

	@Test
	public void shouldWritePartialContentSuffixRangeLargeSuffix() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Resource body = new ClassPathResource("byterangeresource.txt", getClass());
		List<HttpRange> httpRangeList = HttpRange.parseRanges("bytes=-50");

		converter.write(new HttpRangeResource(httpRangeList, body), MediaType.TEXT_PLAIN, outputMessage);

		HttpHeaders headers = outputMessage.getHeaders();
		assertThat(headers.getContentType(), is(MediaType.TEXT_PLAIN));
		assertThat(headers.getContentLength(), is(39L));
		assertThat(headers.get(HttpHeaders.CONTENT_RANGE).size(), is(1));
		assertThat(headers.get(HttpHeaders.CONTENT_RANGE).get(0), is("bytes 0-38/39"));
		assertThat(outputMessage.getBodyAsString(Charset.forName("UTF-8")), is("Spring Framework test resource content."));
	}

	@Test
	public void partialContentMultipleByteRanges() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Resource body = new ClassPathResource("byterangeresource.txt", getClass());
		List<HttpRange> httpRangeList = HttpRange.parseRanges("bytes=0-5, 7-15, 17-20");

		converter.write(new HttpRangeResource(httpRangeList, body), MediaType.TEXT_PLAIN, outputMessage);

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
	}

	@Test  // SPR-10848
	public void writeByteArrayNullMediaType() throws IOException {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		byte[] byteArray = {1, 2, 3};
		Resource body = new ByteArrayResource(byteArray);
		converter.write(body, null, outputMessage);
		assertTrue(Arrays.equals(byteArray, outputMessage.getBodyAsBytes()));
	}

	// SPR-12999
	@Test @SuppressWarnings("unchecked")
	public void writeContentNotGettingInputStream() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Resource resource = mock(Resource.class);
		given(resource.getInputStream()).willThrow(FileNotFoundException.class);

		converter.write(resource, MediaType.APPLICATION_OCTET_STREAM, outputMessage);

		assertEquals(0, outputMessage.getHeaders().getContentLength());
	}

	// SPR-12999
	@Test
	public void writeContentNotClosingInputStream() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Resource resource = mock(Resource.class);
		InputStream inputStream = mock(InputStream.class);
		given(resource.getInputStream()).willReturn(inputStream);
		given(inputStream.read(any())).willReturn(-1);
		doThrow(new NullPointerException()).when(inputStream).close();

		converter.write(resource, MediaType.APPLICATION_OCTET_STREAM, outputMessage);

		assertEquals(0, outputMessage.getHeaders().getContentLength());
	}

	// SPR-13620
	@Test @SuppressWarnings("unchecked")
	public void writeContentInputStreamThrowingNullPointerException() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Resource resource = mock(Resource.class);
		InputStream in = mock(InputStream.class);
		given(resource.getInputStream()).willReturn(in);
		given(in.read(any())).willThrow(NullPointerException.class);

		converter.write(resource, MediaType.APPLICATION_OCTET_STREAM, outputMessage);

		assertEquals(0, outputMessage.getHeaders().getContentLength());
	}

}
