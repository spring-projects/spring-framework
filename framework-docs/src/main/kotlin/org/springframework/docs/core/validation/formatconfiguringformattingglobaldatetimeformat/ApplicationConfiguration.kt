/*
 * Copyright 2002-2023 the original author or authors.
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