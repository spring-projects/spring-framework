/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.format.number;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Keith Donald
 */
class CurrencyStyleFormatterTests {

	private final CurrencyStyleFormatter formatter = new CurrencyStyleFormatter();


	@Test
	void formatValue() {
		assertThat(formatter.print(new BigDecimal("23"), Locale.US)).isEqualTo("$23.00");
	}

	@Test
	void parseValue() throws ParseException {
		assertThat(formatter.parse("$23.56", Locale.US)).isEqualTo(new BigDecimal("23.56"));
	}

	@Test
	void parseBogusValue() {
		assertThatExceptionOfType(ParseException.class).isThrownBy(() ->
				formatter.parse("bogus", Locale.US));
	}

	@Test
	void parseValueDefaultRoundDown() throws ParseException {
		this.formatter.setRoundingMode(RoundingMode.DOWN);
		assertThat(formatter.parse("$23.567", Locale.US)).isEqualTo(new BigDecimal("23.56"));
	}

	@Test
	void parseWholeValue() throws ParseException {
		assertThat(formatter.parse("$23", Locale.US)).isEqualTo(new BigDecimal("23.00"));
	}

	@Test
	void parseValueNotLenientFailure() {
		assertThatExceptionOfType(ParseException.class).isThrownBy(() ->
				formatter.parse("$23.56bogus", Locale.US));
	}

}
