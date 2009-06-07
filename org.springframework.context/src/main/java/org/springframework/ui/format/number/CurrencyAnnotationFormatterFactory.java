package org.springframework.ui.format.number;

import java.math.BigDecimal;

import org.springframework.ui.format.AnnotationFormatterFactory;
import org.springframework.ui.format.Formatter;

public class CurrencyAnnotationFormatterFactory implements AnnotationFormatterFactory<CurrencyFormat, BigDecimal> {
	public Formatter<BigDecimal> getFormatter(CurrencyFormat annotation) {
		return new CurrencyFormatter();
	}
}
