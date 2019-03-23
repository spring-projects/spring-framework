/*
 * Copyright 2002-2013 the original author or authors.
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
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link JOptCommandLinePropertySource}.
 *
 * @author Chris Beams
 * @since 3.1
 */
public class JOptCommandLinePropertySourceTests {

	@Test
	public void withRequiredArg_andArgIsPresent() {
		OptionParser parser = new OptionParser();
		parser.accepts("foo").withRequiredArg();
		OptionSet options = parser.parse("--foo=bar");

		PropertySource<?> ps = new JOptCommandLinePropertySource(options);
		assertThat((String)ps.getProperty("foo"), equalTo("bar"));
	}

	@Test
	public void withOptionalArg_andArgIsMissing() {
		OptionParser parser = new OptionParser();
		parser.accepts("foo").withOptionalArg();
		OptionSet options = parser.parse("--foo");

		PropertySource<?> ps = new JOptCommandLinePropertySource(options);
		assertThat(ps.containsProperty("foo"), is(true));
		assertThat((String)ps.getProperty("foo"), equalTo(""));
	}

	@Test
	public void withNoArg() {
		OptionParser parser = new OptionParser();
		parser.accepts("o1");
		parser.accepts("o2");
		OptionSet options = parser.parse("--o1");

		PropertySource<?> ps = new JOptCommandLinePropertySource(options);
		assertThat(ps.containsProperty("o1"), is(true));
		assertThat(ps.containsProperty("o2"), is(false));
		assertThat((String)ps.getProperty("o1"), equalTo(""));
		assertThat(ps.getProperty("o2"), nullValue());
	}

	@Test
	public void withRequiredArg_andMultipleArgsPresent_usingDelimiter() {
		OptionParser parser = new OptionParser();
		parser.accepts("foo").withRequiredArg().withValuesSeparatedBy(',');
		OptionSet options = parser.parse("--foo=bar,baz,biz");

		CommandLinePropertySource<?> ps = new JOptCommandLinePropertySource(options);
		assertEquals(Arrays.asList("bar","baz","biz"), ps.getOptionValues("foo"));
		assertThat(ps.getProperty("foo"), equalTo("bar,baz,biz"));
	}

	@Test
	public void withRequiredArg_andMultipleArgsPresent_usingRepeatedOption() {
		OptionParser parser = new OptionParser();
		parser.accepts("foo").withRequiredArg().withValuesSeparatedBy(',');
		OptionSet options = parser.parse("--foo=bar", "--foo=baz", "--foo=biz");

		CommandLinePropertySource<?> ps = new JOptCommandLinePropertySource(options);
		assertEquals(Arrays.asList("bar","baz","biz"), ps.getOptionValues("foo"));
		assertThat(ps.getProperty("foo"), equalTo("bar,baz,biz"));
	}

	@Test
	public void withMissingOption() {
		OptionParser parser = new OptionParser();
		parser.accepts("foo").withRequiredArg().withValuesSeparatedBy(',');
		OptionSet options = parser.parse(); // <-- no options whatsoever

		PropertySource<?> ps = new JOptCommandLinePropertySource(options);
		assertThat(ps.getProperty("foo"), nullValue());
	}

	@Test
	public void withDottedOptionName() {
		OptionParser parser = new OptionParser();
		parser.accepts("spring.profiles.active").withRequiredArg();
		OptionSet options = parser.parse("--spring.profiles.active=p1");

		CommandLinePropertySource<?> ps = new JOptCommandLinePropertySource(options);
		assertThat(ps.getProperty("spring.profiles.active"), equalTo("p1"));
	}

	@Test
	public void withDefaultNonOptionArgsNameAndNoNonOptionArgsPresent() {
		OptionParser parser = new OptionParser();
		parser.acceptsAll(Arrays.asList("o1","option1")).withRequiredArg();
		parser.accepts("o2");
		OptionSet optionSet = parser.parse("--o1=v1", "--o2");
		EnumerablePropertySource<?> ps = new JOptCommandLinePropertySource(optionSet);

		assertThat(ps.containsProperty("nonOptionArgs"), is(false));
		assertThat(ps.containsProperty("o1"), is(true));
		assertThat(ps.containsProperty("o2"), is(true));

		assertThat(ps.containsProperty("nonOptionArgs"), is(false));
		assertThat(ps.getProperty("nonOptionArgs"), nullValue());
		assertThat(ps.getPropertyNames().length, is(2));
	}

	@Test
	public void withDefaultNonOptionArgsNameAndNonOptionArgsPresent() {
		OptionParser parser = new OptionParser();
		parser.accepts("o1").withRequiredArg();
		parser.accepts("o2");
		OptionSet optionSet = parser.parse("--o1=v1", "noa1", "--o2", "noa2");
		PropertySource<?> ps = new JOptCommandLinePropertySource(optionSet);

		assertThat(ps.containsProperty("nonOptionArgs"), is(true));
		assertThat(ps.containsProperty("o1"), is(true));
		assertThat(ps.containsProperty("o2"), is(true));

		String nonOptionArgs = (String)ps.getProperty("nonOptionArgs");
		assertThat(nonOptionArgs, equalTo("noa1,noa2"));
	}

	@Test
	public void withCustomNonOptionArgsNameAndNoNonOptionArgsPresent() {
		OptionParser parser = new OptionParser();
		parser.accepts("o1").withRequiredArg();
		parser.accepts("o2");
		OptionSet optionSet = parser.parse("--o1=v1", "noa1", "--o2", "noa2");
		CommandLinePropertySource<?> ps = new JOptCommandLinePropertySource(optionSet);
		ps.setNonOptionArgsPropertyName("NOA");

		assertThat(ps.containsProperty("nonOptionArgs"), is(false));
		assertThat(ps.containsProperty("NOA"), is(true));
		assertThat(ps.containsProperty("o1"), is(true));
		assertThat(ps.containsProperty("o2"), is(true));
		String nonOptionArgs = ps.getProperty("NOA");
		assertThat(nonOptionArgs, equalTo("noa1,noa2"));
	}

	@Test
	public void withRequiredArg_ofTypeEnum() {
		OptionParser parser = new OptionParser();
		parser.accepts("o1").withRequiredArg().ofType(OptionEnum.class);
		OptionSet options = parser.parse("--o1=VAL_1");

		PropertySource<?> ps = new JOptCommandLinePropertySource(options);
		assertThat(ps.getProperty("o1"), equalTo("VAL_1"));
	}

	public static enum OptionEnum {
		VAL_1;
	}
}
