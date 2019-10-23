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

package org.springframework.http;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.support.ResourceRegion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link HttpRange}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
public class HttpRangeTests {

	@Test
	public void invalidFirstPosition() {
		assertThatIllegalArgumentException().isThrownBy(() ->
			HttpRange.createByteRange(-1));
	}

	@Test
	public void invalidLastLessThanFirst() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				HttpRange.createByteRange(10, 9));
	}

	@Test
	public void invalidSuffixLength() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				HttpRange.createSuffixRange(-1));
	}

	@Test
	public void byteRange() {
		HttpRange range = HttpRange.createByteRange(0, 499);
		assertThat(range.getRangeStart(1000)).isEqualTo(0);
		assertThat(range.getRangeEnd(1000)).isEqualTo(499);
	}

	@Test
	public void byteRangeWithoutLastPosition() {
		HttpRange range = HttpRange.createByteRange(9500);
		assertThat(range.getRangeStart(10000)).isEqualTo(9500);
		assertThat(range.getRangeEnd(10000)).isEqualTo(9999);
	}

	@Test
	public void byteRangeOfZeroLength() {
		HttpRange range = HttpRange.createByteRange(9500, 9500);
		assertThat(range.getRangeStart(10000)).isEqualTo(9500);
		assertThat(range.getRangeEnd(10000)).isEqualTo(9500);
	}

	@Test
	public void suffixRange() {
		HttpRange range = HttpRange.createSuffixRange(500);
		assertThat(range.getRangeStart(1000)).isEqualTo(500);
		assertThat(range.getRangeEnd(1000)).isEqualTo(999);
	}

	@Test
	public void suffixRangeShorterThanRepresentation() {
		HttpRange range = HttpRange.createSuffixRange(500);
		assertThat(range.getRangeStart(350)).isEqualTo(0);
		assertThat(range.getRangeEnd(350)).isEqualTo(349);
	}

	@Test
	public void parseRanges() {
		List<HttpRange> ranges = HttpRange.parseRanges("bytes=0-0,500-,-1");
		assertThat(ranges.size()).isEqualTo(3);
		assertThat(ranges.get(0).getRangeStart(1000)).isEqualTo(0);
		assertThat(ranges.get(0).getRangeEnd(1000)).isEqualTo(0);
		assertThat(ranges.get(1).getRangeStart(1000)).isEqualTo(500);
		assertThat(ranges.get(1).getRangeEnd(1000)).isEqualTo(999);
		assertThat(ranges.get(2).getRangeStart(1000)).isEqualTo(999);
		assertThat(ranges.get(2).getRangeEnd(1000)).isEqualTo(999);
	}

	@Test
	public void parseRangesValidations() {

		// 1. At limit..
		StringBuilder atLimit = new StringBuilder("bytes=0-0");
		for (int i=0; i < 99; i++) {
			atLimit.append(",").append(i).append("-").append(i + 1);
		}
		List<HttpRange> ranges = HttpRange.parseRanges(atLimit.toString());
		assertThat(ranges.size()).isEqualTo(100);

		// 2. Above limit..
		StringBuilder aboveLimit = new StringBuilder("bytes=0-0");
		for (int i=0; i < 100; i++) {
			aboveLimit.append(",").append(i).append("-").append(i + 1);
		}
		assertThatIllegalArgumentException().isThrownBy(() ->
				HttpRange.parseRanges(aboveLimit.toString()));
	}

	@Test
	public void rangeToString() {
		List<HttpRange> ranges = new ArrayList<>();
		ranges.add(HttpRange.createByteRange(0, 499));
		ranges.add(HttpRange.createByteRange(9500));
		ranges.add(HttpRange.createSuffixRange(500));
		assertThat(HttpRange.toString(ranges)).as("Invalid Range header").isEqualTo("bytes=0-499, 9500-, -500");
	}

	@Test
	public void toResourceRegion() {
		byte[] bytes = "Spring Framework".getBytes(StandardCharsets.UTF_8);
		ByteArrayResource resource = new ByteArrayResource(bytes);
		HttpRange range = HttpRange.createByteRange(0, 5);
		ResourceRegion region = range.toResourceRegion(resource);
		assertThat(region.getResource()).isEqualTo(resource);
		assertThat(region.getPosition()).isEqualTo(0L);
		assertThat(region.getCount()).isEqualTo(6L);
	}

	@Test
	public void toResourceRegionInputStreamResource() {
		InputStreamResource resource = mock(InputStreamResource.class);
		HttpRange range = HttpRange.createByteRange(0, 9);
		assertThatIllegalArgumentException().isThrownBy(() ->
				range.toResourceRegion(resource));
	}

	@Test
	public void toResourceRegionIllegalLength() {
		ByteArrayResource resource = mock(ByteArrayResource.class);
		given(resource.contentLength()).willReturn(-1L);
		HttpRange range = HttpRange.createByteRange(0, 9);
		assertThatIllegalArgumentException().isThrownBy(() -> range.toResourceRegion(resource));
	}

	@Test
	public void toResourceRegionExceptionLength() throws IOException {
		InputStreamResource resource = mock(InputStreamResource.class);
		given(resource.contentLength()).willThrow(IOException.class);
		HttpRange range = HttpRange.createByteRange(0, 9);
		assertThatIllegalArgumentException().isThrownBy(() -> range.toResourceRegion(resource));
	}

	@Test // gh-23576
	public void toResourceRegionStartingAtResourceByteCount() {
		byte[] bytes = "Spring Framework".getBytes(StandardCharsets.UTF_8);
		ByteArrayResource resource = new ByteArrayResource(bytes);
		HttpRange range = HttpRange.createByteRange(resource.contentLength());
		assertThatIllegalArgumentException().isThrownBy(() -> range.toResourceRegion(resource));
	}

	@Test
	public void toResourceRegionsValidations() {
		byte[] bytes = "12345".getBytes(StandardCharsets.UTF_8);
		ByteArrayResource resource = new ByteArrayResource(bytes);

		// 1. Below length
		List<HttpRange> belowLengthRanges = HttpRange.parseRanges("bytes=0-1,2-3");
		List<ResourceRegion> regions = HttpRange.toResourceRegions(belowLengthRanges, resource);
		assertThat(regions.size()).isEqualTo(2);

		// 2. At length
		List<HttpRange> atLengthRanges = HttpRange.parseRanges("bytes=0-1,2-4");
		assertThatIllegalArgumentException().isThrownBy(() ->
				HttpRange.toResourceRegions(atLengthRanges, resource));
	}

}
