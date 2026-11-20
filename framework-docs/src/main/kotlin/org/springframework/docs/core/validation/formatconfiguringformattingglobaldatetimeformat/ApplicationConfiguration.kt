/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.core.validation.formatconfiguringformattingglobaldatetimeformat

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.format.datetime.DateFormatter
import org.springframework.format.datetime.DateFormatterRegistrar
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar
import org.springframework.format.number.NumberFormatAnnotationFormatterFactory
import org.springframework.format.support.DefaultFormattingConversionService
import org.springframework.format.support.FormattingConversionService
import java.time.format.DateTimeFormatter

// tag::snippet[]
@Configuration
class ApplicationConfiguration {

	@Bean
	fun conversionService(): FormattingConversionService {
		// Use the DefaultFormattingConversionService but do not register defaults
		return DefaultFormattingConversionService(false).apply {

			// Ensure @NumberFormat is still supported
			addFormatterForFieldAnnotation(NumberFormatAnnotationFormatterFactory())

			// Register JSR-310 date conversion with a specific global format
			val dateTimeRegistrar = DateTimeFormatterRegistrar()
			dateTimeRegistrar.setDateFormatter(DateTimeFormatter.ofPattern("yyyyMMdd"))
			dateTimeRegistrar.registerFormatters(this)

			// Register date conversion with a specific global format
			val dateRegistrar = DateFormatterRegistrar()
			dateRegistrar.setFormatter(DateFormatter("yyyyMMdd"))
			dateRegistrar.registerFormatters(this)
		}
	}
}
// end::snippet[]