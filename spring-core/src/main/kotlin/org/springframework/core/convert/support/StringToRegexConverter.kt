package org.springframework.core.convert.support

import org.springframework.core.convert.converter.Converter

/**
 * Converts from a String to a [Regex].
 *
 * @author Stephane Nicoll
 */
class StringToRegexConverter : Converter<String, Regex> {

	override fun convert(source: String): Regex? {
		if (source.isEmpty()) {
			return null
		}
		return source.toRegex()
	}

}