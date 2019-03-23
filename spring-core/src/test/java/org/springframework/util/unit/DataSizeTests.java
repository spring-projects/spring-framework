/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.util.unit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

/**
 * Tests for {@link DataSize}.
 *
 * @author Stephane Nicoll
 */
public class DataSizeTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void ofBytesToBytes() {
		assertEquals(1024, DataSize.ofBytes(1024).toBytes());
	}

	@Test
	public void ofBytesToKilobytes() {
		assertEquals(1, DataSize.ofBytes(1024).toKilobytes());
	}

	@Test
	public void ofKilobytesToKilobytes() {
		assertEquals(1024, DataSize.ofKilobytes(1024).toKilobytes());
	}

	@Test
	public void ofKilobytesToMegabytes() {
		assertEquals(1, DataSize.ofKilobytes(1024).toMegabytes());
	}

	@Test
	public void ofMegabytesToMegabytes() {
		assertEquals(1024, DataSize.ofMegabytes(1024).toMegabytes());
	}

	@Test
	public void ofMegabytesToGigabytes() {
		assertEquals(2, DataSize.ofMegabytes(2048).toGigabytes());
	}

	@Test
	public void ofGigabytesToGigabytes() {
		assertEquals(4096, DataSize.ofGigabytes(4096).toGigabytes());
	}

	@Test
	public void ofGigabytesToTerabytes() {
		assertEquals(4, DataSize.ofGigabytes(4096).toTerabytes());
	}

	@Test
	public void ofTerabytesToGigabytes() {
		assertEquals(1024, DataSize.ofTerabytes(1).toGigabytes());
	}

	@Test
	public void ofWithBytesUnit() {
		assertEquals(DataSize.ofBytes(10), DataSize.of(10, DataUnit.BYTES));
	}

	@Test
	public void ofWithKilobytesUnit() {
		assertEquals(DataSize.ofKilobytes(20), DataSize.of(20, DataUnit.KILOBYTES));
	}

	@Test
	public void ofWithMegabytesUnit() {
		assertEquals(DataSize.ofMegabytes(30), DataSize.of(30, DataUnit.MEGABYTES));
	}

	@Test
	public void ofWithGigabytesUnit() {
		assertEquals(DataSize.ofGigabytes(40), DataSize.of(40, DataUnit.GIGABYTES));
	}

	@Test
	public void ofWithTerabytesUnit() {
		assertEquals(DataSize.ofTerabytes(50), DataSize.of(50, DataUnit.TERABYTES));
	}

	@Test
	public void parseWithDefaultUnitUsesBytes() {
		assertEquals(DataSize.ofKilobytes(1), DataSize.parse("1024"));
	}

	@Test
	public void parseNegativeNumberWithDefaultUnitUsesBytes() {
		assertEquals(DataSize.ofBytes(-1), DataSize.parse("-1"));
	}

	@Test
	public void parseWithNullDefaultUnitUsesBytes() {
		assertEquals(DataSize.ofKilobytes(1), DataSize.parse("1024", null));
	}

	@Test
	public void parseNegativeNumberWithNullDefaultUnitUsesBytes() {
		assertEquals(DataSize.ofKilobytes(-1), DataSize.parse("-1024", null));
	}

	@Test
	public void parseWithCustomDefaultUnit() {
		assertEquals(DataSize.ofKilobytes(1), DataSize.parse("1", DataUnit.KILOBYTES));
	}

	@Test
	public void parseNegativeNumberWithCustomDefaultUnit() {
		assertEquals(DataSize.ofKilobytes(-1), DataSize.parse("-1", DataUnit.KILOBYTES));
	}

	@Test
	public void parseWithBytes() {
		assertEquals(DataSize.ofKilobytes(1), DataSize.parse("1024B"));
	}

	@Test
	public void parseWithNegativeBytes() {
		assertEquals(DataSize.ofKilobytes(-1), DataSize.parse("-1024B"));
	}

	@Test
	public void parseWithPositiveBytes() {
		assertEquals(DataSize.ofKilobytes(1), DataSize.parse("+1024B"));
	}

	@Test
	public void parseWithKilobytes() {
		assertEquals(DataSize.ofBytes(1024), DataSize.parse("1KB"));
	}

	@Test
	public void parseWithNegativeKilobytes() {
		assertEquals(DataSize.ofBytes(-1024), DataSize.parse("-1KB"));
	}

	@Test
	public void parseWithMegabytes() {
		assertEquals(DataSize.ofMegabytes(4), DataSize.parse("4MB"));
	}

	@Test
	public void parseWithNegativeMegabytes() {
		assertEquals(DataSize.ofMegabytes(-4), DataSize.parse("-4MB"));
	}

	@Test
	public void parseWithGigabytes() {
		assertEquals(DataSize.ofMegabytes(1024), DataSize.parse("1GB"));
	}

	@Test
	public void parseWithNegativeGigabytes() {
		assertEquals(DataSize.ofMegabytes(-1024), DataSize.parse("-1GB"));
	}

	@Test
	public void parseWithTerabytes() {
		assertEquals(DataSize.ofTerabytes(1), DataSize.parse("1TB"));
	}

	@Test
	public void parseWithNegativeTerabytes() {
		assertEquals(DataSize.ofTerabytes(-1), DataSize.parse("-1TB"));
	}

	@Test
	public void isNegativeWithPositive() {
		assertFalse(DataSize.ofBytes(50).isNegative());
	}

	@Test
	public void isNegativeWithZero() {
		assertFalse(DataSize.ofBytes(0).isNegative());
	}

	@Test
	public void isNegativeWithNegative() {
		assertTrue(DataSize.ofBytes(-1).isNegative());
	}

	@Test
	public void toStringUsesBytes() {
		assertEquals("1024B", DataSize.ofKilobytes(1).toString());
	}

	@Test
	public void toStringWithNegativeBytes() {
		assertEquals("-1024B", DataSize.ofKilobytes(-1).toString());
	}

	@Test
	public void parseWithUnsupportedUnit() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("3WB");
		this.thrown.expectMessage("is not a valid data size");
		DataSize.parse("3WB");
	}

}
