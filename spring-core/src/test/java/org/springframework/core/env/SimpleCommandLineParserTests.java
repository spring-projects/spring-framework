/*
 * Copyright 2002-2019 the original author or authors.
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

class SimpleCommandLineParserTests {

	@Test
	void withNoOptions() {
		SimpleCommandLineArgsParser parser = new SimpleCommandLineArgsParser();
		assertThat(parser.parse().getOptionValues("foo")).isNull();
	}

	@Test
	void withSingleOptionAndNoValue() {
		SimpleCommandLineArgsParser parser = new SimpleCommandLineArgsParser();
		CommandLineArgs args = parser.parse("--o1");
		assertThat(args.containsOption("o1")).isTrue();
		assertThat(args.getOptionValues("o1")).isEqualTo(Collections.EMPTY_LIST);
	}

	@Test
	void withSingleOptionAndValue() {
		SimpleCommandLineArgsParser parser = new SimpleCommandLineArgsParser();
		CommandLineArgs args = parser.parse("--o1=v1");
		assertThat(args.containsOption("o1")).isTrue();
		assertThat(args.getOptionValues("o1").get(0)).isEqualTo("v1");
	}

	@Test
	void withMixOfOptionsHavingValueAndOptionsHavingNoValue() {
		SimpleCommandLineArgsParser parser = new SimpleCommandLineArgsParser();
		CommandLineArgs args = parser.parse("--o1=v1", "--o2");
		assertThat(args.containsOption("o1")).isTrue();
		assertThat(args.containsOption("o2")).isTrue();
		assertThat(args.containsOption("o3")).isFalse();
		assertThat(args.getOptionValues("o1").get(0)).isEqualTo("v1");
		assertThat(args.getOptionValues("o2")).isEqualTo(Collections.EMPTY_LIST);
		assertThat(args.getOptionValues("o3")).isNull();
	}

	@Test
	void withEmptyOptionText() {
		SimpleCommandLineArgsParser parser = new SimpleCommandLineArgsParser();
		assertThatIllegalArgumentException().isThrownBy(() ->
				parser.parse("--"));
	}

	@Test
	void withEmptyOptionName() {
		SimpleCommandLineArgsParser parser = new SimpleCommandLineArgsParser();
		assertThatIllegalArgumentException().isThrownBy(() ->
				parser.parse("--=v1"));
	}

	@Test
	void withEmptyOptionValue() {
		SimpleCommandLineArgsParser parser = new SimpleCommandLineArgsParser();
		assertThatIllegalArgumentException().isThrownBy(() ->
				parser.parse("--o1="));
	}

	@Test
	void withEmptyOptionNameAndEmptyOptionValue() {
		SimpleCommandLineArgsParser parser = new SimpleCommandLineArgsParser();
		assertThatIllegalArgumentException().isThrownBy(() ->
				parser.parse("--="));
	}

	@Test
	void withNonOptionArguments() {
		SimpleCommandLineArgsParser parser = new SimpleCommandLineArgsParser();
		CommandLineArgs args = parser.parse("--o1=v1", "noa1", "--o2=v2", "noa2");
		assertThat(args.getOptionValues("o1").get(0)).isEqualTo("v1");
		assertThat(args.getOptionValues("o2").get(0)).isEqualTo("v2");

		List<String> nonOptions = args.getNonOptionArgs();
		assertThat(nonOptions.get(0)).isEqualTo("noa1");
		assertThat(nonOptions.get(1)).isEqualTo("noa2");
		assertThat(nonOptions.size()).isEqualTo(2);
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

}
