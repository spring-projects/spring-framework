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

package org.springframework.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Sam Brannen
 */
class StringUtilsTests {

	@Test
	void hasTextBlank() {
		String blank = "          ";
		assertThat(StringUtils.hasText(blank)).isFalse();
	}

	@Test
	void hasTextNullEmpty() {
		assertThat(StringUtils.hasText(null)).isFalse();
		assertThat(StringUtils.hasText("")).isFalse();
	}

	@Test
	void hasTextValid() {
		assertThat(StringUtils.hasText("t")).isTrue();
	}

	@Test
	void containsWhitespace() {
		assertThat(StringUtils.containsWhitespace(null)).isFalse();
		assertThat(StringUtils.containsWhitespace("")).isFalse();
		assertThat(StringUtils.containsWhitespace("a")).isFalse();
		assertThat(StringUtils.containsWhitespace("abc")).isFalse();
		assertThat(StringUtils.containsWhitespace(" ")).isTrue();
		assertThat(StringUtils.containsWhitespace("\t")).isTrue();
		assertThat(StringUtils.containsWhitespace("\n")).isTrue();
		assertThat(StringUtils.containsWhitespace(" a")).isTrue();
		assertThat(StringUtils.containsWhitespace("abc ")).isTrue();
		assertThat(StringUtils.containsWhitespace("a b")).isTrue();
		assertThat(StringUtils.containsWhitespace("a  b")).isTrue();
	}

	@Test
	@Deprecated
	void trimWhitespace() {
		assertThat(StringUtils.trimWhitespace(null)).isNull();
		assertThat(StringUtils.trimWhitespace("")).isEmpty();
		assertThat(StringUtils.trimWhitespace(" ")).isEmpty();
		assertThat(StringUtils.trimWhitespace("\t")).isEmpty();
		assertThat(StringUtils.trimWhitespace("\n")).isEmpty();
		assertThat(StringUtils.trimWhitespace(" \t\n")).isEmpty();
		assertThat(StringUtils.trimWhitespace(" a")).isEqualTo("a");
		assertThat(StringUtils.trimWhitespace("a ")).isEqualTo("a");
		assertThat(StringUtils.trimWhitespace(" a ")).isEqualTo("a");
		assertThat(StringUtils.trimWhitespace(" a b ")).isEqualTo("a b");
		assertThat(StringUtils.trimWhitespace(" a b  c ")).isEqualTo("a b  c");
	}

	@Test
	void trimAllWhitespace() {
		assertThat(StringUtils.trimAllWhitespace(null)).isNull();
		assertThat(StringUtils.trimAllWhitespace("")).isEmpty();
		assertThat(StringUtils.trimAllWhitespace(" ")).isEmpty();
		assertThat(StringUtils.trimAllWhitespace("\t")).isEmpty();
		assertThat(StringUtils.trimAllWhitespace("\n")).isEmpty();
		assertThat(StringUtils.trimAllWhitespace(" \t\n")).isEmpty();
		assertThat(StringUtils.trimAllWhitespace(" a")).isEqualTo("a");
		assertThat(StringUtils.trimAllWhitespace("a ")).isEqualTo("a");
		assertThat(StringUtils.trimAllWhitespace(" a ")).isEqualTo("a");
		assertThat(StringUtils.trimAllWhitespace(" a b ")).isEqualTo("ab");
		assertThat(StringUtils.trimAllWhitespace(" a b  c ")).isEqualTo("abc");
	}

	@Test
	@SuppressWarnings("deprecation")
	void trimLeadingWhitespace() {
		assertThat(StringUtils.trimLeadingWhitespace(null)).isNull();
		assertThat(StringUtils.trimLeadingWhitespace("")).isEmpty();
		assertThat(StringUtils.trimLeadingWhitespace(" ")).isEmpty();
		assertThat(StringUtils.trimLeadingWhitespace("\t")).isEmpty();
		assertThat(StringUtils.trimLeadingWhitespace("\n")).isEmpty();
		assertThat(StringUtils.trimLeadingWhitespace(" \t\n")).isEmpty();
		assertThat(StringUtils.trimLeadingWhitespace(" a")).isEqualTo("a");
		assertThat(StringUtils.trimLeadingWhitespace("a ")).isEqualTo("a ");
		assertThat(StringUtils.trimLeadingWhitespace(" a ")).isEqualTo("a ");
		assertThat(StringUtils.trimLeadingWhitespace(" a b ")).isEqualTo("a b ");
		assertThat(StringUtils.trimLeadingWhitespace(" a b  c ")).isEqualTo("a b  c ");
	}

	@Test
	@SuppressWarnings("deprecation")
	void trimTrailingWhitespace() {
		assertThat(StringUtils.trimTrailingWhitespace(null)).isNull();
		assertThat(StringUtils.trimTrailingWhitespace("")).isEmpty();
		assertThat(StringUtils.trimTrailingWhitespace(" ")).isEmpty();
		assertThat(StringUtils.trimTrailingWhitespace("\t")).isEmpty();
		assertThat(StringUtils.trimTrailingWhitespace("\n")).isEmpty();
		assertThat(StringUtils.trimTrailingWhitespace(" \t\n")).isEmpty();
		assertThat(StringUtils.trimTrailingWhitespace("a ")).isEqualTo("a");
		assertThat(StringUtils.trimTrailingWhitespace(" a")).isEqualTo(" a");
		assertThat(StringUtils.trimTrailingWhitespace(" a ")).isEqualTo(" a");
		assertThat(StringUtils.trimTrailingWhitespace(" a b ")).isEqualTo(" a b");
		assertThat(StringUtils.trimTrailingWhitespace(" a b  c ")).isEqualTo(" a b  c");
	}

	@Test
	void trimLeadingCharacter() {
		assertThat(StringUtils.trimLeadingCharacter(null, ' ')).isNull();
		assertThat(StringUtils.trimLeadingCharacter("", ' ')).isEmpty();
		assertThat(StringUtils.trimLeadingCharacter(" ", ' ')).isEmpty();
		assertThat(StringUtils.trimLeadingCharacter("\t", ' ')).isEqualTo("\t");
		assertThat(StringUtils.trimLeadingCharacter(" a", ' ')).isEqualTo("a");
		assertThat(StringUtils.trimLeadingCharacter("a ", ' ')).isEqualTo("a ");
		assertThat(StringUtils.trimLeadingCharacter(" a ", ' ')).isEqualTo("a ");
		assertThat(StringUtils.trimLeadingCharacter(" a b ", ' ')).isEqualTo("a b ");
		assertThat(StringUtils.trimLeadingCharacter(" a b  c ", ' ')).isEqualTo("a b  c ");
	}

	@Test
	void trimTrailingCharacter() {
		assertThat(StringUtils.trimTrailingCharacter(null, ' ')).isNull();
		assertThat(StringUtils.trimTrailingCharacter("", ' ')).isEmpty();
		assertThat(StringUtils.trimTrailingCharacter(" ", ' ')).isEmpty();
		assertThat(StringUtils.trimTrailingCharacter("\t", ' ')).isEqualTo("\t");
		assertThat(StringUtils.trimTrailingCharacter("a ", ' ')).isEqualTo("a");
		assertThat(StringUtils.trimTrailingCharacter(" a", ' ')).isEqualTo(" a");
		assertThat(StringUtils.trimTrailingCharacter(" a ", ' ')).isEqualTo(" a");
		assertThat(StringUtils.trimTrailingCharacter(" a b ", ' ')).isEqualTo(" a b");
		assertThat(StringUtils.trimTrailingCharacter(" a b  c ", ' ')).isEqualTo(" a b  c");
	}

	@Test
	void matchesCharacter() {
		assertThat(StringUtils.matchesCharacter(null, '/')).isFalse();
		assertThat(StringUtils.matchesCharacter("/a", '/')).isFalse();
		assertThat(StringUtils.matchesCharacter("a", '/')).isFalse();
		assertThat(StringUtils.matchesCharacter("/", '/')).isTrue();
	}

	@Test
	void startsWithIgnoreCase() {
		String prefix = "fOo";
		assertThat(StringUtils.startsWithIgnoreCase("foo", prefix)).isTrue();
		assertThat(StringUtils.startsWithIgnoreCase("Foo", prefix)).isTrue();
		assertThat(StringUtils.startsWithIgnoreCase("foobar", prefix)).isTrue();
		assertThat(StringUtils.startsWithIgnoreCase("foobarbar", prefix)).isTrue();
		assertThat(StringUtils.startsWithIgnoreCase("Foobar", prefix)).isTrue();
		assertThat(StringUtils.startsWithIgnoreCase("FoobarBar", prefix)).isTrue();
		assertThat(StringUtils.startsWithIgnoreCase("foObar", prefix)).isTrue();
		assertThat(StringUtils.startsWithIgnoreCase("FOObar", prefix)).isTrue();
		assertThat(StringUtils.startsWithIgnoreCase("fOobar", prefix)).isTrue();
		assertThat(StringUtils.startsWithIgnoreCase(null, prefix)).isFalse();
		assertThat(StringUtils.startsWithIgnoreCase("fOobar", null)).isFalse();
		assertThat(StringUtils.startsWithIgnoreCase("b", prefix)).isFalse();
		assertThat(StringUtils.startsWithIgnoreCase("barfoo", prefix)).isFalse();
		assertThat(StringUtils.startsWithIgnoreCase("barfoobar", prefix)).isFalse();
	}

	@Test
	void endsWithIgnoreCase() {
		String suffix = "fOo";
		assertThat(StringUtils.endsWithIgnoreCase("foo", suffix)).isTrue();
		assertThat(StringUtils.endsWithIgnoreCase("Foo", suffix)).isTrue();
		assertThat(StringUtils.endsWithIgnoreCase("barfoo", suffix)).isTrue();
		assertThat(StringUtils.endsWithIgnoreCase("barbarfoo", suffix)).isTrue();
		assertThat(StringUtils.endsWithIgnoreCase("barFoo", suffix)).isTrue();
		assertThat(StringUtils.endsWithIgnoreCase("barBarFoo", suffix)).isTrue();
		assertThat(StringUtils.endsWithIgnoreCase("barfoO", suffix)).isTrue();
		assertThat(StringUtils.endsWithIgnoreCase("barFOO", suffix)).isTrue();
		assertThat(StringUtils.endsWithIgnoreCase("barfOo", suffix)).isTrue();
		assertThat(StringUtils.endsWithIgnoreCase(null, suffix)).isFalse();
		assertThat(StringUtils.endsWithIgnoreCase("barfOo", null)).isFalse();
		assertThat(StringUtils.endsWithIgnoreCase("b", suffix)).isFalse();
		assertThat(StringUtils.endsWithIgnoreCase("foobar", suffix)).isFalse();
		assertThat(StringUtils.endsWithIgnoreCase("barfoobar", suffix)).isFalse();
	}

	@Test
	void substringMatch() {
		assertThat(StringUtils.substringMatch("foo", 0, "foo")).isTrue();
		assertThat(StringUtils.substringMatch("foo", 1, "oo")).isTrue();
		assertThat(StringUtils.substringMatch("foo", 2, "o")).isTrue();
		assertThat(StringUtils.substringMatch("foo", 0, "fOo")).isFalse();
		assertThat(StringUtils.substringMatch("foo", 1, "fOo")).isFalse();
		assertThat(StringUtils.substringMatch("foo", 2, "fOo")).isFalse();
		assertThat(StringUtils.substringMatch("foo", 3, "fOo")).isFalse();
		assertThat(StringUtils.substringMatch("foo", 1, "Oo")).isFalse();
		assertThat(StringUtils.substringMatch("foo", 2, "Oo")).isFalse();
		assertThat(StringUtils.substringMatch("foo", 3, "Oo")).isFalse();
		assertThat(StringUtils.substringMatch("foo", 2, "O")).isFalse();
		assertThat(StringUtils.substringMatch("foo", 3, "O")).isFalse();
	}

	@Test
	void countOccurrencesOf() {
		assertThat(StringUtils.countOccurrencesOf(null, null)).as("nullx2 = 0").isEqualTo(0);
		assertThat(StringUtils.countOccurrencesOf("s", null)).as("null string = 0").isEqualTo(0);
		assertThat(StringUtils.countOccurrencesOf(null, "s")).as("null substring = 0").isEqualTo(0);
		String s = "erowoiueoiur";
		assertThat(StringUtils.countOccurrencesOf(s, "WERWER")).as("not found = 0").isEqualTo(0);
		assertThat(StringUtils.countOccurrencesOf(s, "x")).as("not found char = 0").isEqualTo(0);
		assertThat(StringUtils.countOccurrencesOf(s, " ")).as("not found ws = 0").isEqualTo(0);
		assertThat(StringUtils.countOccurrencesOf(s, "")).as("not found empty string = 0").isEqualTo(0);
		assertThat(StringUtils.countOccurrencesOf(s, "e")).as("found char=2").isEqualTo(2);
		assertThat(StringUtils.countOccurrencesOf(s, "oi")).as("found substring=2").isEqualTo(2);
		assertThat(StringUtils.countOccurrencesOf(s, "oiu")).as("found substring=2").isEqualTo(2);
		assertThat(StringUtils.countOccurrencesOf(s, "oiur")).as("found substring=3").isEqualTo(1);
		assertThat(StringUtils.countOccurrencesOf(s, "r")).as("test last").isEqualTo(2);
	}

	@Test
	void replace() {
		String inString = "a6AazAaa77abaa";
		String oldPattern = "aa";
		String newPattern = "foo";

		// Simple replace
		String s = StringUtils.replace(inString, oldPattern, newPattern);
		assertThat(s).as("Replace 1 worked").isEqualTo("a6AazAfoo77abfoo");

		// Non match: no change
		s = StringUtils.replace(inString, "qwoeiruqopwieurpoqwieur", newPattern);
		assertThat(s).as("Replace non-matched is returned as-is").isSameAs(inString);

		// Null new pattern: should ignore
		s = StringUtils.replace(inString, oldPattern, null);
		assertThat(s).as("Replace non-matched is returned as-is").isSameAs(inString);

		// Null old pattern: should ignore
		s = StringUtils.replace(inString, null, newPattern);
		assertThat(s).as("Replace non-matched is returned as-is").isSameAs(inString);
	}

	@Test
	void delete() {
		String inString = "The quick brown fox jumped over the lazy dog";

		String noThe = StringUtils.delete(inString, "the");
		assertThat(noThe).as("Result has no the [" + noThe + "]")
				.isEqualTo("The quick brown fox jumped over  lazy dog");

		String nohe = StringUtils.delete(inString, "he");
		assertThat(nohe).as("Result has no he [" + nohe + "]").isEqualTo("T quick brown fox jumped over t lazy dog");

		String nosp = StringUtils.delete(inString, " ");
		assertThat(nosp).as("Result has no spaces").isEqualTo("Thequickbrownfoxjumpedoverthelazydog");

		String killEnd = StringUtils.delete(inString, "dog");
		assertThat(killEnd).as("Result has no dog").isEqualTo("The quick brown fox jumped over the lazy ");

		String mismatch = StringUtils.delete(inString, "dxxcxcxog");
		assertThat(mismatch).as("Result is unchanged").isEqualTo(inString);

		String nochange = StringUtils.delete(inString, "");
		assertThat(nochange).as("Result is unchanged").isEqualTo(inString);
	}

	@Test
	void deleteAny() {
		String inString = "Able was I ere I saw Elba";

		String res = StringUtils.deleteAny(inString, "I");
		assertThat(res).as("Result has no 'I'").isEqualTo("Able was  ere  saw Elba");

		res = StringUtils.deleteAny(inString, "AeEba!");
		assertThat(res).as("Result has no 'AeEba!'").isEqualTo("l ws I r I sw l");

		res = StringUtils.deleteAny(inString, "#@$#$^");
		assertThat(res).as("Result is unchanged").isEqualTo(inString);
	}

	@Test
	void deleteAnyWhitespace() {
		String whitespace = "This is\n\n\n    \t   a messagy string with whitespace\n";
		assertThat(whitespace).as("Has CR").contains("\n");
		assertThat(whitespace).as("Has tab").contains("\t");
		assertThat(whitespace).as("Has space").contains(" ");

		String cleaned = StringUtils.deleteAny(whitespace, "\n\t ");
		assertThat(cleaned).as("Has no CR").doesNotContain("\n");
		assertThat(cleaned).as("Has no tab").doesNotContain("\t");
		assertThat(cleaned).as("Has no space").doesNotContain(" ");
		assertThat(cleaned.length()).as("Still has chars").isGreaterThan(10);
	}

	@Test
	void quote() {
		assertThat(StringUtils.quote("myString")).isEqualTo("'myString'");
		assertThat(StringUtils.quote("")).isEqualTo("''");
		assertThat(StringUtils.quote(null)).isNull();
	}

	@Test
	void quoteIfString() {
		assertThat(StringUtils.quoteIfString("myString")).isEqualTo("'myString'");
		assertThat(StringUtils.quoteIfString("")).isEqualTo("''");
		assertThat(StringUtils.quoteIfString(5)).isEqualTo(5);
		assertThat(StringUtils.quoteIfString(null)).isNull();
	}

	@Test
	void unqualify() {
		String qualified = "i.am.not.unqualified";
		assertThat(StringUtils.unqualify(qualified)).isEqualTo("unqualified");
	}

	@Test
	void capitalize() {
		String capitalized = "i am not capitalized";
		assertThat(StringUtils.capitalize(capitalized)).isEqualTo("I am not capitalized");
	}

	@Test
	void uncapitalize() {
		String capitalized = "I am capitalized";
		assertThat(StringUtils.uncapitalize(capitalized)).isEqualTo("i am capitalized");
	}

	@Test
	void getFilename() {
		assertThat(StringUtils.getFilename(null)).isNull();
		assertThat(StringUtils.getFilename("")).isEmpty();
		assertThat(StringUtils.getFilename("myfile")).isEqualTo("myfile");
		assertThat(StringUtils.getFilename("mypath/myfile")).isEqualTo("myfile");
		assertThat(StringUtils.getFilename("myfile.")).isEqualTo("myfile.");
		assertThat(StringUtils.getFilename("mypath/myfile.")).isEqualTo("myfile.");
		assertThat(StringUtils.getFilename("myfile.txt")).isEqualTo("myfile.txt");
		assertThat(StringUtils.getFilename("mypath/myfile.txt")).isEqualTo("myfile.txt");
	}

	@Test
	void getFilenameExtension() {
		assertThat(StringUtils.getFilenameExtension(null)).isNull();
		assertThat(StringUtils.getFilenameExtension("")).isNull();
		assertThat(StringUtils.getFilenameExtension("myfile")).isNull();
		assertThat(StringUtils.getFilenameExtension("myPath/myfile")).isNull();
		assertThat(StringUtils.getFilenameExtension("/home/user/.m2/settings/myfile")).isNull();
		assertThat(StringUtils.getFilenameExtension("myfile.")).isEmpty();
		assertThat(StringUtils.getFilenameExtension("myPath/myfile.")).isEmpty();
		assertThat(StringUtils.getFilenameExtension("myfile.txt")).isEqualTo("txt");
		assertThat(StringUtils.getFilenameExtension("mypath/myfile.txt")).isEqualTo("txt");
		assertThat(StringUtils.getFilenameExtension("/home/user/.m2/settings/myfile.txt")).isEqualTo("txt");
	}

	@Test
	void stripFilenameExtension() {
		assertThat(StringUtils.stripFilenameExtension("")).isEmpty();
		assertThat(StringUtils.stripFilenameExtension("myfile")).isEqualTo("myfile");
		assertThat(StringUtils.stripFilenameExtension("myfile.")).isEqualTo("myfile");
		assertThat(StringUtils.stripFilenameExtension("myfile.txt")).isEqualTo("myfile");
		assertThat(StringUtils.stripFilenameExtension("mypath/myfile")).isEqualTo("mypath/myfile");
		assertThat(StringUtils.stripFilenameExtension("mypath/myfile.")).isEqualTo("mypath/myfile");
		assertThat(StringUtils.stripFilenameExtension("mypath/myfile.txt")).isEqualTo("mypath/myfile");
		assertThat(StringUtils.stripFilenameExtension("/home/user/.m2/settings/myfile")).isEqualTo("/home/user/.m2/settings/myfile");
		assertThat(StringUtils.stripFilenameExtension("/home/user/.m2/settings/myfile.")).isEqualTo("/home/user/.m2/settings/myfile");
		assertThat(StringUtils.stripFilenameExtension("/home/user/.m2/settings/myfile.txt")).isEqualTo("/home/user/.m2/settings/myfile");
	}

	@Test
	void cleanPath() {
		assertThat(StringUtils.cleanPath("mypath/myfile")).isEqualTo("mypath/myfile");
		assertThat(StringUtils.cleanPath("mypath\\myfile")).isEqualTo("mypath/myfile");
		assertThat(StringUtils.cleanPath("mypath/../mypath/myfile")).isEqualTo("mypath/myfile");
		assertThat(StringUtils.cleanPath("mypath/myfile/../../mypath/myfile")).isEqualTo("mypath/myfile");
		assertThat(StringUtils.cleanPath("../mypath/myfile")).isEqualTo("../mypath/myfile");
		assertThat(StringUtils.cleanPath("../mypath/../mypath/myfile")).isEqualTo("../mypath/myfile");
		assertThat(StringUtils.cleanPath("mypath/../../mypath/myfile")).isEqualTo("../mypath/myfile");
		assertThat(StringUtils.cleanPath("/../mypath/myfile")).isEqualTo("/../mypath/myfile");
		assertThat(StringUtils.cleanPath("/a/:b/../../mypath/myfile")).isEqualTo("/mypath/myfile");
		assertThat(StringUtils.cleanPath("/")).isEqualTo("/");
		assertThat(StringUtils.cleanPath("/mypath/../")).isEqualTo("/");
		assertThat(StringUtils.cleanPath("mypath/..")).isEmpty();
		assertThat(StringUtils.cleanPath("mypath/../.")).isEmpty();
		assertThat(StringUtils.cleanPath("mypath/../")).isEqualTo("./");
		assertThat(StringUtils.cleanPath("././")).isEqualTo("./");
		assertThat(StringUtils.cleanPath("./")).isEqualTo("./");
		assertThat(StringUtils.cleanPath("../")).isEqualTo("../");
		assertThat(StringUtils.cleanPath("./../")).isEqualTo("../");
		assertThat(StringUtils.cleanPath(".././")).isEqualTo("../");
		assertThat(StringUtils.cleanPath("file:/")).isEqualTo("file:/");
		assertThat(StringUtils.cleanPath("file:/mypath/../")).isEqualTo("file:/");
		assertThat(StringUtils.cleanPath("file:mypath/..")).isEqualTo("file:");
		assertThat(StringUtils.cleanPath("file:mypath/../.")).isEqualTo("file:");
		assertThat(StringUtils.cleanPath("file:mypath/../")).isEqualTo("file:./");
		assertThat(StringUtils.cleanPath("file:././")).isEqualTo("file:./");
		assertThat(StringUtils.cleanPath("file:./")).isEqualTo("file:./");
		assertThat(StringUtils.cleanPath("file:../")).isEqualTo("file:../");
		assertThat(StringUtils.cleanPath("file:./../")).isEqualTo("file:../");
		assertThat(StringUtils.cleanPath("file:.././")).isEqualTo("file:../");
		assertThat(StringUtils.cleanPath("file:/mypath/spring.factories")).isEqualTo("file:/mypath/spring.factories");
		assertThat(StringUtils.cleanPath("file:///c:/some/../path/the%20file.txt")).isEqualTo("file:///c:/path/the%20file.txt");
		assertThat(StringUtils.cleanPath("jar:file:///c:\\some\\..\\path\\.\\the%20file.txt")).isEqualTo("jar:file:///c:/path/the%20file.txt");
		assertThat(StringUtils.cleanPath("jar:file:///c:/some/../path/./the%20file.txt")).isEqualTo("jar:file:///c:/path/the%20file.txt");
		assertThat(StringUtils.cleanPath("jar:file:///c:\\\\some\\\\..\\\\path\\\\.\\\\the%20file.txt")).isEqualTo("jar:file:///c:/path/the%20file.txt");
	}

	@Test
	void pathEquals() {
		assertThat(StringUtils.pathEquals("/dummy1/dummy2/dummy3", "/dummy1/dummy2/dummy3")).as("Must be true for the same strings").isTrue();
		assertThat(StringUtils.pathEquals("C:\\dummy1\\dummy2\\dummy3", "C:\\dummy1\\dummy2\\dummy3")).as("Must be true for the same win strings").isTrue();
		assertThat(StringUtils.pathEquals("/dummy1/bin/../dummy2/dummy3", "/dummy1/dummy2/dummy3")).as("Must be true for one top path on 1").isTrue();
		assertThat(StringUtils.pathEquals("C:\\dummy1\\dummy2\\dummy3", "C:\\dummy1\\bin\\..\\dummy2\\dummy3")).as("Must be true for one win top path on 2").isTrue();
		assertThat(StringUtils.pathEquals("/dummy1/bin/../dummy2/bin/../dummy3", "/dummy1/dummy2/dummy3")).as("Must be true for two top paths on 1").isTrue();
		assertThat(StringUtils.pathEquals("C:\\dummy1\\dummy2\\dummy3", "C:\\dummy1\\bin\\..\\dummy2\\bin\\..\\dummy3")).as("Must be true for two win top paths on 2").isTrue();
		assertThat(StringUtils.pathEquals("/dummy1/bin/tmp/../../dummy2/dummy3", "/dummy1/dummy2/dummy3")).as("Must be true for double top paths on 1").isTrue();
		assertThat(StringUtils.pathEquals("/dummy1/dummy2/dummy3", "/dummy1/dum/dum/../../dummy2/dummy3")).as("Must be true for double top paths on 2 with similarity").isTrue();
		assertThat(StringUtils.pathEquals("./dummy1/dummy2/dummy3", "dummy1/dum/./dum/../../dummy2/dummy3")).as("Must be true for current paths").isTrue();
		assertThat(StringUtils.pathEquals("./dummy1/dummy2/dummy3", "/dummy1/dum/./dum/../../dummy2/dummy3")).as("Must be false for relative/absolute paths").isFalse();
		assertThat(StringUtils.pathEquals("/dummy1/dummy2/dummy3", "/dummy1/dummy4/dummy3")).as("Must be false for different strings").isFalse();
		assertThat(StringUtils.pathEquals("/dummy1/bin/tmp/../dummy2/dummy3", "/dummy1/dummy2/dummy3")).as("Must be false for one false path on 1").isFalse();
		assertThat(StringUtils.pathEquals("C:\\dummy1\\dummy2\\dummy3", "C:\\dummy1\\bin\\tmp\\..\\dummy2\\dummy3")).as("Must be false for one false win top path on 2").isFalse();
		assertThat(StringUtils.pathEquals("/dummy1/bin/../dummy2/dummy3", "/dummy1/dummy2/dummy4")).as("Must be false for top path on 1 + difference").isFalse();
	}

	@Test
	void concatenateStringArrays() {
		String[] input1 = new String[] {"myString2"};
		String[] input2 = new String[] {"myString1", "myString2"};
		String[] result = StringUtils.concatenateStringArrays(input1, input2);
		assertThat(result).hasSize(3);
		assertThat(result[0]).isEqualTo("myString2");
		assertThat(result[1]).isEqualTo("myString1");
		assertThat(result[2]).isEqualTo("myString2");

		assertThat(StringUtils.concatenateStringArrays(input1, null)).isEqualTo(input1);
		assertThat(StringUtils.concatenateStringArrays(null, input2)).isEqualTo(input2);
		assertThat(StringUtils.concatenateStringArrays(null, null)).isNull();
	}

	@Test
	void sortStringArray() {
		String[] input = new String[] {"myString2"};
		input = StringUtils.addStringToArray(input, "myString1");
		assertThat(input[0]).isEqualTo("myString2");
		assertThat(input[1]).isEqualTo("myString1");

		StringUtils.sortStringArray(input);
		assertThat(input[0]).isEqualTo("myString1");
		assertThat(input[1]).isEqualTo("myString2");
	}

	@Test
	void removeDuplicateStrings() {
		String[] input = new String[] {"myString2", "myString1", "myString2"};
		input = StringUtils.removeDuplicateStrings(input);
		assertThat(input[0]).isEqualTo("myString2");
		assertThat(input[1]).isEqualTo("myString1");
	}

	@Test
	void splitArrayElementsIntoProperties() {
		String[] input = new String[] {"key1=value1 ", "key2 =\"value2\""};
		Properties result = StringUtils.splitArrayElementsIntoProperties(input, "=");
		assertThat(result.getProperty("key1")).isEqualTo("value1");
		assertThat(result.getProperty("key2")).isEqualTo("\"value2\"");
	}

	@Test
	void splitArrayElementsIntoPropertiesAndDeletedChars() {
		String[] input = new String[] {"key1=value1 ", "key2 =\"value2\""};
		Properties result = StringUtils.splitArrayElementsIntoProperties(input, "=", "\"");
		assertThat(result.getProperty("key1")).isEqualTo("value1");
		assertThat(result.getProperty("key2")).isEqualTo("value2");
	}

	@Test
	void tokenizeToStringArray() {
		String[] sa = StringUtils.tokenizeToStringArray("a,b , ,c", ",");
		assertThat(sa).hasSize(3);
		assertThat(sa[0].equals("a") && sa[1].equals("b") && sa[2].equals("c")).as("components are correct").isTrue();
	}

	@Test
	void tokenizeToStringArrayWithNotIgnoreEmptyTokens() {
		String[] sa = StringUtils.tokenizeToStringArray("a,b , ,c", ",", true, false);
		assertThat(sa).hasSize(4);
		assertThat(sa[0].equals("a") && sa[1].equals("b") && sa[2].isEmpty() && sa[3].equals("c")).as("components are correct").isTrue();
	}

	@Test
	void tokenizeToStringArrayWithNotTrimTokens() {
		String[] sa = StringUtils.tokenizeToStringArray("a,b ,c", ",", false, true);
		assertThat(sa).hasSize(3);
		assertThat(sa[0].equals("a") && sa[1].equals("b ") && sa[2].equals("c")).as("components are correct").isTrue();
	}

	@Test
	void commaDelimitedListToStringArrayWithNullProducesEmptyArray() {
		String[] sa = StringUtils.commaDelimitedListToStringArray(null);
		assertThat(sa).as("String array isn't null with null input").isNotNull();
		assertThat(sa.length).as("String array length == 0 with null input").isEqualTo(0);
	}

	@Test
	void commaDelimitedListToStringArrayWithEmptyStringProducesEmptyArray() {
		String[] sa = StringUtils.commaDelimitedListToStringArray("");
		assertThat(sa).as("String array isn't null with null input").isNotNull();
		assertThat(sa.length).as("String array length == 0 with null input").isEqualTo(0);
	}

	@Test
	void delimitedListToStringArrayWithComma() {
		String[] sa = StringUtils.delimitedListToStringArray("a,b", ",");
		assertThat(sa).hasSize(2);
		assertThat(sa[0]).isEqualTo("a");
		assertThat(sa[1]).isEqualTo("b");
	}

	@Test
	void delimitedListToStringArrayWithSemicolon() {
		String[] sa = StringUtils.delimitedListToStringArray("a;b", ";");
		assertThat(sa).hasSize(2);
		assertThat(sa[0]).isEqualTo("a");
		assertThat(sa[1]).isEqualTo("b");
	}

	@Test
	void delimitedListToStringArrayWithEmptyDelimiter() {
		String[] sa = StringUtils.delimitedListToStringArray("a,b", "");
		assertThat(sa).hasSize(3);
		assertThat(sa[0]).isEqualTo("a");
		assertThat(sa[1]).isEqualTo(",");
		assertThat(sa[2]).isEqualTo("b");
	}

	@Test
	void delimitedListToStringArrayWithNullDelimiter() {
		String[] sa = StringUtils.delimitedListToStringArray("a,b", null);
		assertThat(sa).hasSize(1);
		assertThat(sa[0]).isEqualTo("a,b");
	}

	@Test
	void commaDelimitedListToStringArrayMatchWords() {
		// Could read these from files
		String[] sa = new String[] {"foo", "bar", "big"};
		doTestCommaDelimitedListToStringArrayLegalMatch(sa);
		doTestStringArrayReverseTransformationMatches(sa);

		sa = new String[] {"a", "b", "c"};
		doTestCommaDelimitedListToStringArrayLegalMatch(sa);
		doTestStringArrayReverseTransformationMatches(sa);

		// Test same words
		sa = new String[] {"AA", "AA", "AA", "AA", "AA"};
		doTestCommaDelimitedListToStringArrayLegalMatch(sa);
		doTestStringArrayReverseTransformationMatches(sa);
	}

	private void doTestStringArrayReverseTransformationMatches(String[] sa) {
		String[] reverse =
				StringUtils.commaDelimitedListToStringArray(StringUtils.arrayToCommaDelimitedString(sa));
		assertThat(Arrays.asList(reverse)).as("Reverse transformation is equal").isEqualTo(Arrays.asList(sa));
	}

	@Test
	void commaDelimitedListToStringArraySingleString() {
		// Could read these from files
		String s = "woeirqupoiewuropqiewuorpqiwueopriquwopeiurqopwieur";
		String[] sa = StringUtils.commaDelimitedListToStringArray(s);
		assertThat(sa.length).as("Found one String with no delimiters").isEqualTo(1);
		assertThat(sa[0]).as("Single array entry matches input String with no delimiters").isEqualTo(s);
	}

	@Test
	void commaDelimitedListToStringArrayWithOtherPunctuation() {
		// Could read these from files
		String[] sa = new String[] {"xcvwert4456346&*.", "///", ".!", ".", ";"};
		doTestCommaDelimitedListToStringArrayLegalMatch(sa);
	}

	/**
	 * We expect to see the empty Strings in the output.
	 */
	@Test
	void commaDelimitedListToStringArrayEmptyStrings() {
		// Could read these from files
		String[] sa = StringUtils.commaDelimitedListToStringArray("a,,b");
		assertThat(sa.length).as("a,,b produces array length 3").isEqualTo(3);
		assertThat(sa[0].equals("a") && sa[1].isEmpty() && sa[2].equals("b")).as("components are correct").isTrue();

		sa = new String[] {"", "", "a", ""};
		doTestCommaDelimitedListToStringArrayLegalMatch(sa);
	}

	private void doTestCommaDelimitedListToStringArrayLegalMatch(String[] components) {
		String sb = String.join(String.valueOf(','), components);
		String[] sa = StringUtils.commaDelimitedListToStringArray(sb);
		assertThat(sa).as("String array isn't null with legal match").isNotNull();
		assertThat(sa.length).as("String array length is correct with legal match").isEqualTo(components.length);
		assertThat(Arrays.equals(sa, components)).as("Output equals input").isTrue();
	}


	@Test
	void parseLocaleStringSunnyDay() {
		Locale expectedLocale = Locale.UK;
		Locale locale = StringUtils.parseLocaleString(expectedLocale.toString());
		assertThat(locale).as("When given a bona-fide Locale string, must not return null.").isNotNull();
		assertThat(locale).isEqualTo(expectedLocale);
	}

	@Test
	void parseLocaleStringWithEmptyLocaleStringYieldsNullLocale() {
		Locale locale = StringUtils.parseLocaleString("");
		assertThat(locale).as("When given an empty Locale string, must return null.").isNull();
	}

	@Test  // SPR-8637
	void parseLocaleWithMultiSpecialCharactersInVariant() {
		String variant = "proper-northern";
		String localeString = "en_GB_" + variant;
		Locale locale = StringUtils.parseLocaleString(localeString);
		assertThat(locale.getVariant()).as("Multi-valued variant portion of the Locale not extracted correctly.").isEqualTo(variant);
	}

	@Test  // SPR-3671
	void parseLocaleWithMultiValuedVariant() {
		String variant = "proper_northern";
		String localeString = "en_GB_" + variant;
		Locale locale = StringUtils.parseLocaleString(localeString);
		assertThat(locale.getVariant()).as("Multi-valued variant portion of the Locale not extracted correctly.").isEqualTo(variant);
	}

	@Test  // SPR-3671
	void parseLocaleWithMultiValuedVariantUsingSpacesAsSeparators() {
		String variant = "proper northern";
		String localeString = "en GB " + variant;
		Locale locale = StringUtils.parseLocaleString(localeString);
		assertThat(locale.getVariant()).as("Multi-valued variant portion of the Locale not extracted correctly.").isEqualTo(variant);
	}

	@Test  // SPR-3671
	void parseLocaleWithMultiValuedVariantUsingMixtureOfUnderscoresAndSpacesAsSeparators() {
		String variant = "proper northern";
		String localeString = "en_GB_" + variant;
		Locale locale = StringUtils.parseLocaleString(localeString);
		assertThat(locale.getVariant()).as("Multi-valued variant portion of the Locale not extracted correctly.").isEqualTo(variant);
	}

	@Test  // SPR-7779
	void parseLocaleWithInvalidCharacters() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				StringUtils.parseLocaleString("%0D%0AContent-length:30%0D%0A%0D%0A%3Cscript%3Ealert%28123%29%3C/script%3E"));
	}

	@Test  // SPR-9420
	void parseLocaleWithSameLowercaseTokenForLanguageAndCountry() {
		assertThat(StringUtils.parseLocaleString("tr_tr").toString()).isEqualTo("tr_TR");
		assertThat(StringUtils.parseLocaleString("bg_bg_vnt").toString()).isEqualTo("bg_BG_vnt");
	}

	@Test  // SPR-11806
	void parseLocaleWithVariantContainingCountryCode() {
		String variant = "GBtest";
		String localeString = "en_GB_" + variant;
		Locale locale = StringUtils.parseLocaleString(localeString);
		assertThat(locale.getVariant()).as("Variant containing country code not extracted correctly").isEqualTo(variant);
	}

	@Test  // SPR-14718, SPR-7598
	void parseJava7Variant() {
		assertThat(StringUtils.parseLocaleString("sr__#LATN").toString()).isEqualTo("sr__#LATN");
	}

	@Test  // SPR-16651
	void availableLocalesWithLocaleString() {
		for (Locale locale : Locale.getAvailableLocales()) {
			Locale parsedLocale = StringUtils.parseLocaleString(locale.toString());
			if (parsedLocale == null) {
				assertThat(locale.getLanguage()).isEmpty();
			}
			else {
				assertThat(locale.toString()).isEqualTo(parsedLocale.toString());
			}
		}
	}

	@Test  // SPR-16651
	void availableLocalesWithLanguageTag() {
		for (Locale locale : Locale.getAvailableLocales()) {
			Locale parsedLocale = StringUtils.parseLocale(locale.toLanguageTag());
			if (parsedLocale == null) {
				assertThat(locale.getLanguage()).isEmpty();
			}
			else {
				assertThat(locale.toLanguageTag()).isEqualTo(parsedLocale.toLanguageTag());
			}
		}
	}

	@Test
	void invalidLocaleWithLocaleString() {
		assertThat(StringUtils.parseLocaleString("invalid")).isEqualTo(new Locale("invalid"));
		assertThat(StringUtils.parseLocaleString("invalidvalue")).isEqualTo(new Locale("invalidvalue"));
		assertThat(StringUtils.parseLocaleString("invalidvalue_foo")).isEqualTo(new Locale("invalidvalue", "foo"));
		assertThat(StringUtils.parseLocaleString("")).isNull();
	}

	@Test
	void invalidLocaleWithLanguageTag() {
		assertThat(StringUtils.parseLocale("invalid")).isEqualTo(new Locale("invalid"));
		assertThat(StringUtils.parseLocale("invalidvalue")).isEqualTo(new Locale("invalidvalue"));
		assertThat(StringUtils.parseLocale("invalidvalue_foo")).isEqualTo(new Locale("invalidvalue", "foo"));
		assertThat(StringUtils.parseLocale("")).isNull();
	}

	@Test
	void parseLocaleStringWithEmptyCountryAndVariant() {
		assertThat(StringUtils.parseLocale("be__TARASK").toString()).isEqualTo("be__TARASK");
	}

	@Test
	void split() {
		assertThat(StringUtils.split("Hello, world", ",")).containsExactly("Hello", " world");
		assertThat(StringUtils.split(",Hello world", ",")).containsExactly("", "Hello world");
		assertThat(StringUtils.split("Hello world,", ",")).containsExactly("Hello world", "");
		assertThat(StringUtils.split("Hello, world,", ",")).containsExactly("Hello", " world,");
	}

	@Test
	void splitWithEmptyStringOrNull() {
		assertThat(StringUtils.split("Hello, world", "")).isNull();
		assertThat(StringUtils.split("", ",")).isNull();
		assertThat(StringUtils.split(null, ",")).isNull();
		assertThat(StringUtils.split("Hello, world", null)).isNull();
		assertThat(StringUtils.split(null, null)).isNull();
	}

	@Test
	void collectionToDelimitedStringWithNullValuesShouldNotFail() {
		assertThat(StringUtils.collectionToCommaDelimitedString(Collections.singletonList(null))).isEqualTo("null");
	}

	@Test
	void truncatePreconditions() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> StringUtils.truncate("foo", 0))
				.withMessage("Truncation threshold must be a positive number: 0");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> StringUtils.truncate("foo", -99))
				.withMessage("Truncation threshold must be a positive number: -99");
	}

	@ParameterizedTest
	@CsvSource(delimiterString = "-->", textBlock = """
			''                  --> ''
			aardvark            --> aardvark
			aardvark12          --> aardvark12
			aardvark123         --> aardvark12 (truncated)...
			aardvark, bird, cat --> aardvark,  (truncated)...
			"""
	)
	void truncate(String text, String truncated) {
		assertThat(StringUtils.truncate(text, 10)).isEqualTo(truncated);
	}

}
