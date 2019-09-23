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

package org.springframework.util.unit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link DataSize}.
 *
 * @author Stephane Nicoll
 * @author Evgeniy Zubakhin
 */
class DataSizeTests {

	@Test
	void ofBytesToBytes() {
		assertThat(DataSize.ofBytes(1000).toBytes()).isEqualTo(1000);
	}

	@Test
	void ofBytesToKilobytes() {
		assertThat(DataSize.ofBytes(1000).toKilobytes()).isEqualTo(1);
	}

	@Test
	void ofKilobytesToKilobytes() {
		assertThat(DataSize.ofKilobytes(1000).toKilobytes()).isEqualTo(1000);
	}

	@Test
	void ofKilobytesToMegabytes() {
		assertThat(DataSize.ofKilobytes(1000).toMegabytes()).isEqualTo(1);
	}

	@Test
	void ofMegabytesToMegabytes() {
		assertThat(DataSize.ofMegabytes(1000).toMegabytes()).isEqualTo(1000);
	}

	@Test
	void ofMegabytesToGigabytes() {
		assertThat(DataSize.ofMegabytes(2000).toGigabytes()).isEqualTo(2);
	}

	@Test
	void ofGigabytesToGigabytes() {
		assertThat(DataSize.ofGigabytes(4000).toGigabytes()).isEqualTo(4000);
	}

	@Test
	void ofGigabytesToTerabytes() {
		assertThat(DataSize.ofGigabytes(4000).toTerabytes()).isEqualTo(4);
	}

	@Test
	void ofTerabytesToGigabytes() {
		assertThat(DataSize.ofTerabytes(1).toGigabytes()).isEqualTo(1000);
	}

	@Test
	void ofWithBytesUnit() {
		assertThat(DataSize.of(10, DataUnit.BYTES)).isEqualTo(DataSize.ofBytes(10));
	}

	@Test
	void ofWithKilobytesUnit() {
		assertThat(DataSize.of(20, DataUnit.KILOBYTES)).isEqualTo(DataSize.ofKilobytes(20));
	}

	@Test
	void ofWithMegabytesUnit() {
		assertThat(DataSize.of(30, DataUnit.MEGABYTES)).isEqualTo(DataSize.ofMegabytes(30));
	}

	@Test
	void ofWithGigabytesUnit() {
		assertThat(DataSize.of(40, DataUnit.GIGABYTES)).isEqualTo(DataSize.ofGigabytes(40));
	}

	@Test
	void ofWithTerabytesUnit() {
		assertThat(DataSize.of(50, DataUnit.TERABYTES)).isEqualTo(DataSize.ofTerabytes(50));
	}

	@Test
	void parseWithDefaultUnitUsesBytesDecimal() {
		assertThat(DataSize.parse("1000")).isEqualTo(DataSize.ofKilobytes(1));
	}

	@Test
	void parseNegativeNumberWithDefaultUnitUsesBytesDecimal() {
		assertThat(DataSize.parse("-1")).isEqualTo(DataSize.ofBytes(-1));
	}

	@Test
	void parseWithNullDefaultUnitUsesBytesDecimal() {
		assertThat(DataSize.parse("1000", null)).isEqualTo(DataSize.ofKilobytes(1));
	}

	@Test
	void parseNegativeNumberWithNullDefaultUnitUsesBytesDecimal() {
		assertThat(DataSize.parse("-1000", null)).isEqualTo(DataSize.ofKilobytes(-1));
	}

	@Test
	void parseWithCustomDefaultUnitDecimal() {
		assertThat(DataSize.parse("1", DataUnit.KILOBYTES)).isEqualTo(DataSize.ofKilobytes(1));
	}

	@Test
	void parseNegativeNumberWithCustomDefaultUnitDecimal() {
		assertThat(DataSize.parse("-1", DataUnit.KILOBYTES)).isEqualTo(DataSize.ofKilobytes(-1));
	}

	@Test
	void parseWithBytes() {
		assertThat(DataSize.parse("1000B")).isEqualTo(DataSize.ofKilobytes(1));
	}

	@Test
	void parseWithNegativeBytes() {
		assertThat(DataSize.parse("-1000B")).isEqualTo(DataSize.ofKilobytes(-1));
	}

	@Test
	void parseWithPositiveBytes() {
		assertThat(DataSize.parse("+1000B")).isEqualTo(DataSize.ofKilobytes(1));
	}

	@Test
	void parseWithKilobytes() {
		assertThat(DataSize.parse("1KB")).isEqualTo(DataSize.ofBytes(1000));
	}

	@Test
	void parseWithNegativeKilobytes() {
		assertThat(DataSize.parse("-1KB")).isEqualTo(DataSize.ofBytes(-1000));
	}

	@Test
	void parseWithMegabytes() {
		assertThat(DataSize.parse("4MB")).isEqualTo(DataSize.ofMegabytes(4));
	}

	@Test
	void parseWithNegativeMegabytes() {
		assertThat(DataSize.parse("-4MB")).isEqualTo(DataSize.ofMegabytes(-4));
	}

	@Test
	void parseWithGigabytes() {
		assertThat(DataSize.parse("1GB")).isEqualTo(DataSize.ofMegabytes(1000));
	}

	@Test
	void parseWithNegativeGigabytes() {
		assertThat(DataSize.parse("-1GB")).isEqualTo(DataSize.ofMegabytes(-1000));
	}

	@Test
	void parseWithTerabytes() {
		assertThat(DataSize.parse("1TB")).isEqualTo(DataSize.ofTerabytes(1));
	}

	@Test
	void parseWithNegativeTerabytes() {
		assertThat(DataSize.parse("-1TB")).isEqualTo(DataSize.ofTerabytes(-1));
	}

	@Test
	void isNegativeWithPositive() {
		assertThat(DataSize.ofBytes(50).isNegative()).isFalse();
	}

	@Test
	void isNegativeWithZero() {
		assertThat(DataSize.ofBytes(0).isNegative()).isFalse();
	}

	@Test
	void isNegativeWithNegative() {
		assertThat(DataSize.ofBytes(-1).isNegative()).isTrue();
	}

	@Test
	void ofBytesToKibibytes() {
		assertThat(DataSize.ofBytes(1024).toKibibytes()).isEqualTo(1);
	}

	@Test
	void ofKibibytesToKibibytes() {
		assertThat(DataSize.ofKibibytes(1024).toKibibytes()).isEqualTo(1024);
	}

	@Test
	void ofKibibytesToMebibytes() {
		assertThat(DataSize.ofKibibytes(1024).toMebibytes()).isEqualTo(1);
	}

	@Test
	void ofMebibytesToMebibytes() {
		assertThat(DataSize.ofMebibytes(1024).toMebibytes()).isEqualTo(1024);
	}

	@Test
	void ofMebibytesToGibibytes() {
		assertThat(DataSize.ofMebibytes(2048).toGibibytes()).isEqualTo(2);
	}

	@Test
	void ofGibibytesToGibibytes() {
		assertThat(DataSize.ofGibibytes(4096).toGibibytes()).isEqualTo(4096);
	}

	@Test
	void ofGibibytesToTebibytes() {
		assertThat(DataSize.ofGibibytes(4096).toTebibytes()).isEqualTo(4);
	}

	@Test
	void ofTebibytesToGibibytes() {
		assertThat(DataSize.ofTebibytes(1).toGibibytes()).isEqualTo(1024);
	}

	@Test
	void ofWithKibibytesUnit() {
		assertThat(DataSize.of(20, DataUnit.KIBIBYTES)).isEqualTo(DataSize.ofKibibytes(20));
	}

	@Test
	void ofWithMebibytesUnit() {
		assertThat(DataSize.of(30, DataUnit.MEBIBYTES)).isEqualTo(DataSize.ofMebibytes(30));
	}

	@Test
	void ofWithGibibytesUnit() {
		assertThat(DataSize.of(40, DataUnit.GIBIBYTES)).isEqualTo(DataSize.ofGibibytes(40));
	}

	@Test
	void ofWithTebibytesUnit() {
		assertThat(DataSize.of(50, DataUnit.TEBIBYTES)).isEqualTo(DataSize.ofTebibytes(50));
	}

	@Test
	void parseWithDefaultUnitUsesBytesBinary() {
		assertThat(DataSize.parse("1024")).isEqualTo(DataSize.ofKibibytes(1));
	}

	@Test
	void parseWithNullDefaultUnitUsesBytesBinary() {
		assertThat(DataSize.parse("1024", null)).isEqualTo(DataSize.ofKibibytes(1));
	}

	@Test
	void parseNegativeNumberWithNullDefaultUnitUsesBytesBinary() {
		assertThat(DataSize.parse("-1024", null)).isEqualTo(DataSize.ofKibibytes(-1));
	}

	@Test
	void parseWithCustomDefaultUnitBinary() {
		assertThat(DataSize.parse("1", DataUnit.KIBIBYTES)).isEqualTo(DataSize.ofKibibytes(1));
	}

	@Test
	void parseNegativeNumberWithCustomDefaultUnitBinary() {
		assertThat(DataSize.parse("-1", DataUnit.KIBIBYTES)).isEqualTo(DataSize.ofKibibytes(-1));
	}

	@Test
	void parseWithBytesBinary() {
		assertThat(DataSize.parse("1024B")).isEqualTo(DataSize.ofKibibytes(1));
	}

	@Test
	void parseWithNegativeBytesBinary() {
		assertThat(DataSize.parse("-1024B")).isEqualTo(DataSize.ofKibibytes(-1));
	}

	@Test
	void parseWithPositiveBytesBinary() {
		assertThat(DataSize.parse("+1024B")).isEqualTo(DataSize.ofKibibytes(1));
	}

	@Test
	void parseWithKibibytes() {
		assertThat(DataSize.parse("1KiB")).isEqualTo(DataSize.ofBytes(1024));
	}

	@Test
	void parseWithNegativeKibibytes() {
		assertThat(DataSize.parse("-1KiB")).isEqualTo(DataSize.ofBytes(-1024));
	}

	@Test
	void parseWithMebibytes() {
		assertThat(DataSize.parse("4MiB")).isEqualTo(DataSize.ofMebibytes(4));
	}

	@Test
	void parseWithNegativeMebibytes() {
		assertThat(DataSize.parse("-4MiB")).isEqualTo(DataSize.ofMebibytes(-4));
	}

	@Test
	void parseWithGibibytes() {
		assertThat(DataSize.parse("1GiB")).isEqualTo(DataSize.ofMebibytes(1024));
	}

	@Test
	void parseWithNegativeGibibytes() {
		assertThat(DataSize.parse("-1GiB")).isEqualTo(DataSize.ofMebibytes(-1024));
	}

	@Test
	void parseWithTebibytes() {
		assertThat(DataSize.parse("1TiB")).isEqualTo(DataSize.ofTebibytes(1));
	}

	@Test
	void parseWithNegativeTebibytes() {
		assertThat(DataSize.parse("-1TiB")).isEqualTo(DataSize.ofTebibytes(-1));
	}

	@Test
	void toStringUsesBytes() {
		assertThat(DataSize.ofKilobytes(1).toString()).isEqualTo("1000B");
	}

	@Test
	void toStringWithNegativeBytes() {
		assertThat(DataSize.ofKilobytes(-1).toString()).isEqualTo("-1000B");
	}

	@Test
	void parseWithUnsupportedUnit() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				DataSize.parse("3WB"))
			.withMessage("'3WB' is not a valid data size");
	}

}
