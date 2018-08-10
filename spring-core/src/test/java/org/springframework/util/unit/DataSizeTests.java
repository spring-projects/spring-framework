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
	public void ofBytesToKiloBytes() {
		assertEquals(1, DataSize.ofBytes(1024).toKiloBytes());
	}

	@Test
	public void ofKiloBytesToKiloBytes() {
		assertEquals(1024, DataSize.ofKiloBytes(1024).toKiloBytes());
	}

	@Test
	public void ofKiloBytesToMegaBytes() {
		assertEquals(1, DataSize.ofKiloBytes(1024).toMegaBytes());
	}

	@Test
	public void ofMegaBytesToMegaBytes() {
		assertEquals(1024, DataSize.ofMegaBytes(1024).toMegaBytes());
	}

	@Test
	public void ofMegaBytesToGigaBytes() {
		assertEquals(2, DataSize.ofMegaBytes(2048).toGigaBytes());
	}

	@Test
	public void ofGigaBytesToGigaBytes() {
		assertEquals(4096, DataSize.ofGigaBytes(4096).toGigaBytes());
	}

	@Test
	public void ofGigaBytesToTeraBytes() {
		assertEquals(4, DataSize.ofGigaBytes(4096).toTeraBytes());
	}

	@Test
	public void ofTeraBytesToGigaBytes() {
		assertEquals(1024, DataSize.ofTeraBytes(1).toGigaBytes());
	}

	@Test
	public void ofWithBytesUnit() {
		assertEquals(DataSize.ofBytes(10), DataSize.of(10, DataUnit.BYTES));
	}

	@Test
	public void ofWithKiloBytesUnit() {
		assertEquals(DataSize.ofKiloBytes(20), DataSize.of(20, DataUnit.KILOBYTES));
	}

	@Test
	public void ofWithMegaBytesUnit() {
		assertEquals(DataSize.ofMegaBytes(30), DataSize.of(30, DataUnit.MEGABYTES));
	}

	@Test
	public void ofWithGigaBytesUnit() {
		assertEquals(DataSize.ofGigaBytes(40), DataSize.of(40, DataUnit.GIGABYTES));
	}

	@Test
	public void ofWithTeraBytesUnit() {
		assertEquals(DataSize.ofTeraBytes(50), DataSize.of(50, DataUnit.TERABYTES));
	}

	@Test
	public void parseWithDefaultUnitUsesBytes() {
		assertEquals(DataSize.ofKiloBytes(1), DataSize.parse("1024"));
	}

	@Test
	public void parseNegativeNumberWithDefaultUnitUsesBytes() {
		assertEquals(DataSize.ofBytes(-1), DataSize.parse("-1"));
	}

	@Test
	public void parseWithNullDefaultUnitUsesBytes() {
		assertEquals(DataSize.ofKiloBytes(1), DataSize.parse("1024", null));
	}

	@Test
	public void parseNegativeNumberWithNullDefaultUnitUsesBytes() {
		assertEquals(DataSize.ofKiloBytes(-1), DataSize.parse("-1024", null));
	}

	@Test
	public void parseWithCustomDefaultUnit() {
		assertEquals(DataSize.ofKiloBytes(1), DataSize.parse("1", DataUnit.KILOBYTES));
	}

	@Test
	public void parseNegativeNumberWithCustomDefaultUnit() {
		assertEquals(DataSize.ofKiloBytes(-1), DataSize.parse("-1", DataUnit.KILOBYTES));
	}

	@Test
	public void parseWithBytes() {
		assertEquals(DataSize.ofKiloBytes(1), DataSize.parse("1024B"));
	}

	@Test
	public void parseWithNegativeBytes() {
		assertEquals(DataSize.ofKiloBytes(-1), DataSize.parse("-1024B"));
	}

	@Test
	public void parseWithPostivieBytes() {
		assertEquals(DataSize.ofKiloBytes(1), DataSize.parse("+1024B"));
	}

	@Test
	public void parseWithKiloBytes() {
		assertEquals(DataSize.ofBytes(1024), DataSize.parse("1KB"));
	}

	@Test
	public void parseWithNegativeKiloBytes() {
		assertEquals(DataSize.ofBytes(-1024), DataSize.parse("-1KB"));
	}

	@Test
	public void parseWithMegaBytes() {
		assertEquals(DataSize.ofMegaBytes(4), DataSize.parse("4MB"));
	}

	@Test
	public void parseWithNegativeMegaBytes() {
		assertEquals(DataSize.ofMegaBytes(-4), DataSize.parse("-4MB"));
	}

	@Test
	public void parseWithGigaBytes() {
		assertEquals(DataSize.ofMegaBytes(1024), DataSize.parse("1GB"));
	}

	@Test
	public void parseWithNegativeGigaBytes() {
		assertEquals(DataSize.ofMegaBytes(-1024), DataSize.parse("-1GB"));
	}

	@Test
	public void parseWithTeraBytes() {
		assertEquals(DataSize.ofTeraBytes(1), DataSize.parse("1TB"));
	}

	@Test
	public void parseWithNegativeTeraBytes() {
		assertEquals(DataSize.ofTeraBytes(-1), DataSize.parse("-1TB"));
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
		assertEquals("1024B", DataSize.ofKiloBytes(1).toString());
	}

	@Test
	public void toStringWithNegativeBytes() {
		assertEquals("-1024B", DataSize.ofKiloBytes(-1).toString());
	}

	@Test
	public void parseWithUnsupportedUnit() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("3WB");
		this.thrown.expectMessage("is not a valid data size");
		DataSize.parse("3WB");
	}

}
