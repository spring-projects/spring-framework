/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.expression.spel;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.support.StandardTypeLocator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for type comparison.
 *
 * @author Andy Clement
 */
class StandardTypeLocatorTests {

	@Test
	void testImports() throws EvaluationException {
		StandardTypeLocator locator = new StandardTypeLocator();
		assertThat(locator.findType("java.lang.Integer")).isEqualTo(Integer.class);
		assertThat(locator.findType("java.lang.String")).isEqualTo(String.class);

		List<String> prefixes = locator.getImportPrefixes();
		assertThat(prefixes).hasSize(1);
		assertThat(prefixes.contains("java.lang")).isTrue();
		assertThat(prefixes.contains("java.util")).isFalse();

		assertThat(locator.findType("Boolean")).isEqualTo(Boolean.class);
		// currently does not know about java.util by default
		// assertEquals(java.util.List.class,locator.findType("List"));

		assertThatExceptionOfType(SpelEvaluationException.class).isThrownBy(() ->
				locator.findType("URL"))
			.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.TYPE_NOT_FOUND));
		locator.registerImport("java.net");
		assertThat(locator.findType("URL")).isEqualTo(java.net.URL.class);
	}

}
