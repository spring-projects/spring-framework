/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.format.number;

import java.util.Set;

import org.springframework.context.support.EmbeddedValueResolutionSupport;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Formatter;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.annotation.NumberFormat;
import org.springframework.format.annotation.NumberFormat.Style;
import org.springframework.util.NumberUtils;
import org.springframework.util.StringUtils;

/**
 * Formats fields annotated with the {@link NumberFormat} annotation.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 * @see NumberFormat
 */
public class NumberFormatAnnotationFormatterFactory extends EmbeddedValueResolutionSupport
		implements AnnotationFormatterFactory<NumberFormat> {

	@Override
	public Set<Class<?>> getFieldTypes() {
		return NumberUtils.STANDARD_NUMBER_TYPES;
	}

	@Override
	public Printer<Number> getPrinter(NumberFormat annotation, Class<?> fieldType) {
		return configureFormatterFrom(annotation);
	}

	@Override
	public Parser<Number> getParser(NumberFormat annotation, Class<?> fieldType) {
		return configureFormatterFrom(annotation);
	}


	private Formatter<Number> configureFormatterFrom(NumberFormat annotation) {
		String pattern = resolveEmbeddedValue(annotation.pattern());
		if (StringUtils.hasLength(pattern)) {
			return new NumberStyleFormatter(pattern);
		}
		else {
			Style style = annotation.style();
			if (style == Style.CURRENCY) {
				return new CurrencyStyleFormatter();
			}
			else if (style == Style.PERCENT) {
				return new PercentStyleFormatter();
			}
			else {
				return new NumberStyleFormatter();
			}
		}
	}

}
