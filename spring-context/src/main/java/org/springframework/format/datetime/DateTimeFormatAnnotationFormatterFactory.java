/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.format.datetime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.context.support.EmbeddedValueResolutionSupport;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Formatter;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;

/**
 * Formats fields annotated with the {@link DateTimeFormat} annotation using a {@link DateFormatter}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 3.2
 */
public class DateTimeFormatAnnotationFormatterFactory extends EmbeddedValueResolutionSupport
		implements AnnotationFormatterFactory<DateTimeFormat> {

	private static final Set<Class<?>> FIELD_TYPES = Set.of(Date.class, Calendar.class, Long.class);

	@Override
	public Set<Class<?>> getFieldTypes() {
		return FIELD_TYPES;
	}

	@Override
	public Printer<?> getPrinter(DateTimeFormat annotation, Class<?> fieldType) {
		return getFormatter(annotation, fieldType);
	}

	@Override
	public Parser<?> getParser(DateTimeFormat annotation, Class<?> fieldType) {
		return getFormatter(annotation, fieldType);
	}

	protected Formatter<Date> getFormatter(DateTimeFormat annotation, Class<?> fieldType) {
		DateFormatter formatter = new DateFormatter();
		formatter.setSource(annotation);
		formatter.setIso(annotation.iso());

		String style = resolveEmbeddedValue(annotation.style());
		if (StringUtils.hasLength(style)) {
			formatter.setStylePattern(style);
		}

		String pattern = resolveEmbeddedValue(annotation.pattern());
		if (StringUtils.hasLength(pattern)) {
			formatter.setPattern(pattern);
		}

		List<String> resolvedFallbackPatterns = new ArrayList<>();
		for (String fallbackPattern : annotation.fallbackPatterns()) {
			String resolvedFallbackPattern = resolveEmbeddedValue(fallbackPattern);
			if (StringUtils.hasLength(resolvedFallbackPattern)) {
				resolvedFallbackPatterns.add(resolvedFallbackPattern);
			}
		}
		if (!resolvedFallbackPatterns.isEmpty()) {
			formatter.setFallbackPatterns(resolvedFallbackPatterns.toArray(new String[0]));
		}

		return formatter;
	}

}
