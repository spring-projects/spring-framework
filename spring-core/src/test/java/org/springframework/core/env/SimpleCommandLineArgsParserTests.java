/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.core.env;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link SimpleCommandLineArgsParser}.
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @author Brian Clozel
 */
class SimpleCommandLineArgsParserTests {

	private final SimpleCommandLineArgsParser parser = new SimpleCommandLineArgsParser();


	@Test
	void withNoOptions() {
		assertThat(parser.parse().getOptionValues("foo")).isNull();
	}

	@Test
	void withSingleOptionAndNoValue() {
		CommandLineArgs args = parser.parse("--o1");
		assertThat(args.containsOption("o1")).isTrue();
		assertThat(args.getOptionValues("o1")).isEqualTo(Collections.EMPTY_LIST);
	}

	@Test
	void withSingleOptionAndValue() {
		CommandLineArgs args = parser.parse("--o1=v1");
		assertThat(args.containsOption("o1")).isTrue();
		assertThat(args.getOptionValues("o1")).containsExactly("v1");
	}

	@Test
	void withMixOfOptionsHavingValueAndOptionsHavingNoValue() {
		CommandLineArgs args = parser.parse("--o1=v1", "--o2");
		assertThat(args.containsOption("o1")).isTrue();
		assertThat(args.containsOption("o2")).isTrue();
		assertThat(args.containsOption("o3")).isFalse();
		assertThat(args.getOptionValues("o1")).containsExactly("v1");
		assertThat(args.getOptionValues("o2")).isEqualTo(Collections.EMPTY_LIST);
		assertThat(args.getOptionValues("o3")).isNull();
	}

	@Test
	void withEmptyOptionName() {
		assertThatIllegalArgumentException().isThrownBy(() -> parser.parse("--=v1"));
	}

	@Test
	void withEmptyOptionValue() {
		CommandLineArgs args = parser.parse("--o1=");
		assertThat(args.containsOption("o1")).isTrue();
		assertThat(args.getOptionValues("o1")).containsExactly("");
	}

	@Test
	void withEmptyOptionNameAndEmptyOptionValue() {
		assertThatIllegalArgumentException().isThrownBy(() -> parser.parse("--="));
	}

	@Test
	void withNonOptionArguments() {
		CommandLineArgs args = parser.parse("--o1=v1", "noa1", "--o2=v2", "noa2");
		assertThat(args.getOptionValues("o1")).containsExactly("v1");
		assertThat(args.getOptionValues("o2")).containsExactly("v2");

		List<String> nonOptions = args.getNonOptionArgs();
		assertThat(nonOptions).containsExactly("noa1", "noa2");
	}

	@Test
	void assertOptionNamesIsUnmodifiable() {
		CommandLineArgs args = new SimpleCommandLineArgsParser().parse();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				args.getOptionNames().add("bogus"));
	}

	@Test
	void assertNonOptionArgsIsUnmodifiable() {
		CommandLineArgs args = new SimpleCommandLineArgsParser().parse();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				args.getNonOptionArgs().add("foo"));
	}

	@Test
	void supportsEndOfOptionsDelimiter() {
		CommandLineArgs args = parser.parse("--o1=v1", "--", "--o2=v2");
		assertThat(args.containsOption("o1")).isTrue();
		assertThat(args.containsOption("o2")).isFalse();
		assertThat(args.getOptionValues("o1")).containsExactly("v1");
		assertThat(args.getNonOptionArgs()).contains("--o2=v2");
	}

}
