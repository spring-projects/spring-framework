/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
class NumberStyleFormatterTests {

	private final NumberStyleFormatter formatter = new NumberStyleFormatter();


	@Test
	void formatValue() {
		assertThat(formatter.print(new BigDecimal("23.56"), Locale.US)).isEqualTo("23.56");
	}

	@Test
	void parseValue() throws ParseException {
		assertThat(formatter.parse("23.56", Locale.US)).isEqualTo(new BigDecimal("23.56"));
	}

	@Test
	void parseBogusValue() {
		assertThatExceptionOfType(ParseException.class).isThrownBy(() ->
				formatter.parse("bogus", Locale.US));
	}

	@Test
	void parsePercentValueNotLenientFailure() {
		assertThatExceptionOfType(ParseException.class).isThrownBy(() ->
				formatter.parse("23.56bogus", Locale.US));
	}

}
