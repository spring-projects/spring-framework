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

package org.springframework.format.number;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Keith Donald
 */
class PercentStyleFormatterTests {

	private final PercentStyleFormatter formatter = new PercentStyleFormatter();


	@Test
	void formatValue() {
		assertThat(formatter.print(new BigDecimal(".23"), Locale.US)).isEqualTo("23%");
	}

	@Test
	void parseValue() throws ParseException {
		assertThat(formatter.parse("23.56%", Locale.US)).isEqualTo(new BigDecimal(".2356"));
	}

	@Test
	void parseBogusValue() {
		assertThatExceptionOfType(ParseException.class).isThrownBy(() ->
				formatter.parse("bogus", Locale.US));
	}

	@Test
	void parsePercentValueNotLenientFailure() {
		assertThatExceptionOfType(ParseException.class).isThrownBy(() ->
				formatter.parse("23.56%bogus", Locale.US));
	}

}
