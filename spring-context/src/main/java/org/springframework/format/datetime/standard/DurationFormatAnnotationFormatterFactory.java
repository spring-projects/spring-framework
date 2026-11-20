/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.format.datetime.standard;

import java.time.Duration;
import java.util.Set;

import org.springframework.context.support.EmbeddedValueResolutionSupport;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.annotation.DurationFormat;

/**
 * Formats fields annotated with the {@link DurationFormat} annotation using the
 * selected style for parsing and printing JSR-310 {@code Duration}.
 *
 * @author Simon Baslé
 * @since 6.2
 * @see DurationFormat
 * @see DurationFormatter
 */
public class DurationFormatAnnotationFormatterFactory extends EmbeddedValueResolutionSupport
		implements AnnotationFormatterFactory<DurationFormat> {

	// Create the set of field types that may be annotated with @DurationFormat.
	private static final Set<Class<?>> FIELD_TYPES = Set.of(Duration.class);

	@Override
	public final Set<Class<?>> getFieldTypes() {
		return FIELD_TYPES;
	}

	@Override
	public Printer<?> getPrinter(DurationFormat annotation, Class<?> fieldType) {
		return new DurationFormatter(annotation.style(), annotation.defaultUnit());
	}

	@Override
	public Parser<?> getParser(DurationFormat annotation, Class<?> fieldType) {
		return new DurationFormatter(annotation.style(), annotation.defaultUnit());
	}
}
