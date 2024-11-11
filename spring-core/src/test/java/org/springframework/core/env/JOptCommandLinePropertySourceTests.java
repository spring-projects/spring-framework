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

import java.util.Arrays;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JOptCommandLinePropertySource}.
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @since 3.1
 */
@SuppressWarnings("deprecation")
class JOptCommandLinePropertySourceTests {

	@Test
	void withRequiredArg_andArgIsPresent() {
		OptionParser parser = new OptionParser();
		parser.accepts("foo").withRequiredArg();
		OptionSet options = parser.parse("--foo=bar");

		PropertySource<?> ps = new JOptCommandLinePropertySource(options);
		assertThat(ps.getProperty("foo")).isEqualTo("bar");
	}

	@Test
	void withOptionalArg_andArgIsMissing() {
		OptionParser parser = new OptionParser();
		parser.accepts("foo").withOptionalArg();
		OptionSet options = parser.parse("--foo");

		PropertySource<?> ps = new JOptCommandLinePropertySource(options);
		assertThat(ps.containsProperty("foo")).isTrue();
		assertThat(ps.getProperty("foo")).isEqualTo("");
	}

	@Test // gh-24464
	void withOptionalArg_andArgIsEmpty() {
		OptionParser parser = new OptionParser();
		parser.accepts("foo").withOptionalArg();
		OptionSet options = parser.parse("--foo=");

		PropertySource<?> ps = new JOptCommandLinePropertySource(options);
		assertThat(ps.containsProperty("foo")).isTrue();
		assertThat(ps.getProperty("foo")).isEqualTo("");
	}

	@Test
	void withNoArg() {
		OptionParser parser = new OptionParser();
		parser.accepts("o1");
		parser.accepts("o2");
		OptionSet options = parser.parse("--o1");

		PropertySource<?> ps = new JOptCommandLinePropertySource(options);
		assertThat(ps.containsProperty("o1")).isTrue();
		assertThat(ps.containsProperty("o2")).isFalse();
		assertThat(ps.getProperty("o1")).isEqualTo("");
		assertThat(ps.getProperty("o2")).isNull();
	}

	@Test
	void withRequiredArg_andMultipleArgsPresent_usingDelimiter() {
		OptionParser parser = new OptionParser();
		parser.accepts("foo").withRequiredArg().withValuesSeparatedBy(',');
		OptionSet options = parser.parse("--foo=bar,baz,biz");

		CommandLinePropertySource<?> ps = new JOptCommandLinePropertySource(options);
		assertThat(ps.getOptionValues("foo")).containsExactly("bar", "baz", "biz");
		assertThat(ps.getProperty("foo")).isEqualTo("bar,baz,biz");
	}

	@Test
	void withRequiredArg_andMultipleArgsPresent_usingRepeatedOption() {
		OptionParser parser = new OptionParser();
		parser.accepts("foo").withRequiredArg().withValuesSeparatedBy(',');
		OptionSet options = parser.parse("--foo=bar", "--foo=baz", "--foo=biz");

		CommandLinePropertySource<?> ps = new JOptCommandLinePropertySource(options);
		assertThat(ps.getOptionValues("foo")).containsExactly("bar", "baz", "biz");
		assertThat(ps.getProperty("foo")).isEqualTo("bar,baz,biz");
	}

	@Test
	void withMissingOption() {
		OptionParser parser = new OptionParser();
		parser.accepts("foo").withRequiredArg().withValuesSeparatedBy(',');
		OptionSet options = parser.parse(); // <-- no options whatsoever

		PropertySource<?> ps = new JOptCommandLinePropertySource(options);
		assertThat(ps.getProperty("foo")).isNull();
	}

	@Test
	void withDottedOptionName() {
		OptionParser parser = new OptionParser();
		parser.accepts("spring.profiles.active").withRequiredArg();
		OptionSet options = parser.parse("--spring.profiles.active=p1");

		CommandLinePropertySource<?> ps = new JOptCommandLinePropertySource(options);
		assertThat(ps.getProperty("spring.profiles.active")).isEqualTo("p1");
	}

	@Test
	void withDefaultNonOptionArgsNameAndNoNonOptionArgsPresent() {
		OptionParser parser = new OptionParser();
		parser.acceptsAll(Arrays.asList("o1","option1")).withRequiredArg();
		parser.accepts("o2");
		OptionSet optionSet = parser.parse("--o1=v1", "--o2");
		EnumerablePropertySource<?> ps = new JOptCommandLinePropertySource(optionSet);

		assertThat(ps.containsProperty("nonOptionArgs")).isFalse();
		assertThat(ps.containsProperty("o1")).isTrue();
		assertThat(ps.containsProperty("o2")).isTrue();

		assertThat(ps.containsProperty("nonOptionArgs")).isFalse();
		assertThat(ps.getProperty("nonOptionArgs")).isNull();
		assertThat(ps.getPropertyNames()).hasSize(2);
	}

	@Test
	void withDefaultNonOptionArgsNameAndNonOptionArgsPresent() {
		OptionParser parser = new OptionParser();
		parser.accepts("o1").withRequiredArg();
		parser.accepts("o2");
		OptionSet optionSet = parser.parse("--o1=v1", "noa1", "--o2", "noa2");
		PropertySource<?> ps = new JOptCommandLinePropertySource(optionSet);

		assertThat(ps.containsProperty("nonOptionArgs")).isTrue();
		assertThat(ps.containsProperty("o1")).isTrue();
		assertThat(ps.containsProperty("o2")).isTrue();

		assertThat(ps.getProperty("nonOptionArgs")).isEqualTo("noa1,noa2");
	}

	@Test
	void withCustomNonOptionArgsNameAndNoNonOptionArgsPresent() {
		OptionParser parser = new OptionParser();
		parser.accepts("o1").withRequiredArg();
		parser.accepts("o2");
		OptionSet optionSet = parser.parse("--o1=v1", "noa1", "--o2", "noa2");
		CommandLinePropertySource<?> ps = new JOptCommandLinePropertySource(optionSet);
		ps.setNonOptionArgsPropertyName("NOA");

		assertThat(ps.containsProperty("nonOptionArgs")).isFalse();
		assertThat(ps.containsProperty("NOA")).isTrue();
		assertThat(ps.containsProperty("o1")).isTrue();
		assertThat(ps.containsProperty("o2")).isTrue();
		String nonOptionArgs = ps.getProperty("NOA");
		assertThat(nonOptionArgs).isEqualTo("noa1,noa2");
	}

	@Test
	void withRequiredArg_ofTypeEnum() {
		OptionParser parser = new OptionParser();
		parser.accepts("o1").withRequiredArg().ofType(OptionEnum.class);
		OptionSet options = parser.parse("--o1=VAL_1");

		PropertySource<?> ps = new JOptCommandLinePropertySource(options);
		assertThat(ps.getProperty("o1")).isEqualTo("VAL_1");
	}

	public enum OptionEnum {
		VAL_1
	}

}
