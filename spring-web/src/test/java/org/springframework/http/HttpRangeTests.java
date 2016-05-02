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
package org.springframework.http;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.ResourceRegion;

/**
 * Unit tests for {@link HttpRange}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
public class HttpRangeTests {

	@Test(expected = IllegalArgumentException.class)
	public void invalidFirstPosition() throws Exception {
		HttpRange.createByteRange(-1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidLastLessThanFirst() throws Exception {
		HttpRange.createByteRange(10, 9);
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidSuffixLength() throws Exception {
		HttpRange.createSuffixRange(-1);
	}

	@Test
	public void byteRange() throws Exception {
		HttpRange range = HttpRange.createByteRange(0, 499);
		assertEquals(0, range.getRangeStart(1000));
		assertEquals(499, range.getRangeEnd(1000));
	}

	@Test
	public void byteRangeWithoutLastPosition() throws Exception {
		HttpRange range = HttpRange.createByteRange(9500);
		assertEquals(9500, range.getRangeStart(10000));
		assertEquals(9999, range.getRangeEnd(10000));
	}

	@Test
	public void byteRangeOfZeroLength() throws Exception {
		HttpRange range = HttpRange.createByteRange(9500, 9500);
		assertEquals(9500, range.getRangeStart(10000));
		assertEquals(9500, range.getRangeEnd(10000));
	}

	@Test
	public void suffixRange() throws Exception {
		HttpRange range = HttpRange.createSuffixRange(500);
		assertEquals(500, range.getRangeStart(1000));
		assertEquals(999, range.getRangeEnd(1000));
	}

	@Test
	public void suffixRangeShorterThanRepresentation() throws Exception {
		HttpRange range = HttpRange.createSuffixRange(500);
		assertEquals(0, range.getRangeStart(350));
		assertEquals(349, range.getRangeEnd(350));
	}

	@Test
	public void parseRanges() throws Exception {
		List<HttpRange> ranges = HttpRange.parseRanges("bytes=0-0,500-,-1");
		assertEquals(3, ranges.size());
		assertEquals(0, ranges.get(0).getRangeStart(1000));
		assertEquals(0, ranges.get(0).getRangeEnd(1000));
		assertEquals(500, ranges.get(1).getRangeStart(1000));
		assertEquals(999, ranges.get(1).getRangeEnd(1000));
		assertEquals(999, ranges.get(2).getRangeStart(1000));
		assertEquals(999, ranges.get(2).getRangeEnd(1000));
	}

	@Test
	public void rangeToString() {
		List<HttpRange> ranges = new ArrayList<>();
		ranges.add(HttpRange.createByteRange(0, 499));
		ranges.add(HttpRange.createByteRange(9500));
		ranges.add(HttpRange.createSuffixRange(500));
		assertEquals("Invalid Range header", "bytes=0-499, 9500-, -500", HttpRange.toString(ranges));
	}

	@Test
	public void toResourceRegion() {
		byte[] bytes = "Spring Framework".getBytes(Charset.forName("UTF-8"));
		ByteArrayResource resource = new ByteArrayResource(bytes);
		HttpRange range = HttpRange.createByteRange(0, 5);
		ResourceRegion region = range.toResourceRegion(resource);
		assertEquals(resource, region.getResource());
		assertEquals(0L, region.getPosition());
		assertEquals(6L, region.getCount());
	}

	@Test(expected = IllegalArgumentException.class)
	public void toResourceRegionInputStreamResource() {
		InputStreamResource resource = mock(InputStreamResource.class);
		HttpRange range = HttpRange.createByteRange(0, 9);
		range.toResourceRegion(resource);
	}

	@Test(expected = IllegalArgumentException.class)
	public void toResourceRegionIllegalLength() {
		ByteArrayResource resource = mock(ByteArrayResource.class);
		given(resource.contentLength()).willReturn(-1L);
		HttpRange range = HttpRange.createByteRange(0, 9);
		range.toResourceRegion(resource);
	}

	@Test(expected = IllegalArgumentException.class)
	public void toResourceRegionExceptionLength() {
		ByteArrayResource resource = mock(ByteArrayResource.class);
		given(resource.contentLength()).willThrow(IOException.class);
		HttpRange range = HttpRange.createByteRange(0, 9);
		range.toResourceRegion(resource);
	}

}
