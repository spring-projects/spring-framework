/*
 * Copyright 2002-2020 the original author or authors.
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
import java.util.Locale;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rick Evans
 */
class StringUtilsTests {

	@Test
	void hasTextBlank() {
		String blank = "          ";
		assertThat(StringUtils.hasText(blank)).isEqualTo(false);
	}

	@Test
	void hasTextNullEmpty() {
		assertThat(StringUtils.hasText(null)).isEqualTo(false);
		assertThat(StringUtils.hasText("")).isEqualTo(false);
	}

	@Test
	void hasTextValid() {
		assertThat(StringUtils.hasText("t")).isEqualTo(true);
	}

	@Test
	void containsWhitespace() {
		assertThat(StringUtils.containsWhitespace(null)).isFalse();
		assertThat(StringUtils.containsWhitespace("")).isFalse();
		assertThat(StringUtils.containsWhitespace("a")).isFalse();
		assertThat(StringUtils.containsWhitespace("abc")).isFalse();
		assertThat(StringUtils.containsWhitespace(" ")).isTrue();
		assertThat(StringUtils.containsWhitespace(" a")).isTrue();
		assertThat(StringUtils.containsWhitespace("abc ")).isTrue();
		assertThat(StringUtils.containsWhitespace("a b")).isTrue();
		assertThat(StringUtils.containsWhitespace("a  b")).isTrue();
	}

	@Test
	void trimWhitespace() {
		assertThat(StringUtils.trimWhitespace(null)).isEqualTo(null);
		assertThat(StringUtils.trimWhitespace("")).isEqualTo("");
		assertThat(StringUtils.trimWhitespace(" ")).isEqualTo("");
		assertThat(StringUtils.trimWhitespace("\t")).isEqualTo("");
		assertThat(StringUtils.trimWhitespace(" a")).isEqualTo("a");
		assertThat(StringUtils.trimWhitespace("a ")).isEqualTo("a");
		assertThat(StringUtils.trimWhitespace(" a ")).isEqualTo("a");
		assertThat(StringUtils.trimWhitespace(" a b ")).isEqualTo("a b");
		assertThat(StringUtils.trimWhitespace(" a b  c ")).isEqualTo("a b  c");
	}

	@Test
	void trimAllWhitespace() {
		assertThat(StringUtils.trimAllWhitespace("")).isEqualTo("");
		assertThat(StringUtils.trimAllWhitespace(" ")).isEqualTo("");
		assertThat(StringUtils.trimAllWhitespace("\t")).isEqualTo("");
		assertThat(StringUtils.trimAllWhitespace(" a")).isEqualTo("a");
		assertThat(StringUtils.trimAllWhitespace("a ")).isEqualTo("a");
		assertThat(StringUtils.trimAllWhitespace(" a ")).isEqualTo("a");
		assertThat(StringUtils.trimAllWhitespace(" a b ")).isEqualTo("ab");
		assertThat(StringUtils.trimAllWhitespace(" a b  c ")).isEqualTo("abc");
	}

	@Test
	void trimLeadingWhitespace() {
		assertThat(StringUtils.trimLeadingWhitespace(null)).isEqualTo(null);
		assertThat(StringUtils.trimLeadingWhitespace("")).isEqualTo("");
		assertThat(StringUtils.trimLeadingWhitespace(" ")).isEqualTo("");
		assertThat(StringUtils.trimLeadingWhitespace("\t")).isEqualTo("");
		assertThat(StringUtils.trimLeadingWhitespace(" a")).isEqualTo("a");
		assertThat(StringUtils.trimLeadingWhitespace("a ")).isEqualTo("a ");
		assertThat(StringUtils.trimLeadingWhitespace(" a ")).isEqualTo("a ");
		assertThat(StringUtils.trimLeadingWhitespace(" a b ")).isEqualTo("a b ");
		assertThat(StringUtils.trimLeadingWhitespace(" a b  c ")).isEqualTo("a b  c ");
	}

	@Test
	void trimTrailingWhitespace() {
		assertThat(StringUtils.trimTrailingWhitespace(null)).isEqualTo(null);
		assertThat(StringUtils.trimTrailingWhitespace("")).isEqualTo("");
		assertThat(StringUtils.trimTrailingWhitespace(" ")).isEqualTo("");
		assertThat(StringUtils.trimTrailingWhitespace("\t")).isEqualTo("");
		assertThat(StringUtils.trimTrailingWhitespace("a ")).isEqualTo("a");
		assertThat(StringUtils.trimTrailingWhitespace(" a")).isEqualTo(" a");
		assertThat(StringUtils.trimTrailingWhitespace(" a ")).isEqualTo(" a");
		assertThat(StringUtils.trimTrailingWhitespace(" a b ")).isEqualTo(" a b");
		assertThat(StringUtils.trimTrailingWhitespace(" a b  c ")).isEqualTo(" a b  c");
	}

	@Test
	void trimLeadingCharacter() {
		assertThat(StringUtils.trimLeadingCharacter(null, ' ')).isEqualTo(null);
		assertThat(StringUtils.trimLeadingCharacter("", ' ')).isEqualTo("");
		assertThat(StringUtils.trimLeadingCharacter(" ", ' ')).isEqualTo("");
		assertThat(StringUtils.trimLeadingCharacter("\t", ' ')).isEqualTo("\t");
		assertThat(StringUtils.trimLeadingCharacter(" a", ' ')).isEqualTo("a");
		assertThat(StringUtils.trimLeadingCharacter("a ", ' ')).isEqualTo("a ");
		assertThat(StringUtils.trimLeadingCharacter(" a ", ' ')).isEqualTo("a ");
		assertThat(StringUtils.trimLeadingCharacter(" a b ", ' ')).isEqualTo("a b ");
		assertThat(StringUtils.trimLeadingCharacter(" a b  c ", ' ')).isEqualTo("a b  c ");
	}

	@Test
	void trimTrailingCharacter() {
		assertThat(StringUtils.trimTrailingCharacter(null, ' ')).isEqualTo(null);
		assertThat(StringUtils.trimTrailingCharacter("", ' ')).isEqualTo("");
		assertThat(StringUtils.trimTrailingCharacter(" ", ' ')).isEqualTo("");
		assertThat(StringUtils.trimTrailingCharacter("\t", ' ')).isEqualTo("\t");
		assertThat(StringUtils.trimTrailingCharacter("a ", ' ')).isEqualTo("a");
		assertThat(StringUtils.trimTrailingCharacter(" a", ' ')).isEqualTo(" a");
		assertThat(StringUtils.trimTrailingCharacter(" a ", ' ')).isEqualTo(" a");
		assertThat(StringUtils.trimTrailingCharacter(" a b ", ' ')).isEqualTo(" a b");
		assertThat(StringUtils.trimTrailingCharacter(" a b  c ", ' ')).isEqualTo(" a b  c");
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
		assertThat(StringUtils.countOccurrencesOf(null, null) == 0).as("nullx2 = 0").isTrue();
		assertThat(StringUtils.countOccurrencesOf("s", null) == 0).as("null string = 0").isTrue();
		assertThat(StringUtils.countOccurrencesOf(null, "s") == 0).as("null substring = 0").isTrue();
		String s = "erowoiueoiur";
		assertThat(StringUtils.countOccurrencesOf(s, "WERWER") == 0).as("not found = 0").isTrue();
		assertThat(StringUtils.countOccurrencesOf(s, "x") == 0).as("not found char = 0").isTrue();
		assertThat(StringUtils.countOccurrencesOf(s, " ") == 0).as("not found ws = 0").isTrue();
		assertThat(StringUtils.countOccurrencesOf(s, "") == 0).as("not found empty string = 0").isTrue();
		assertThat(StringUtils.countOccurrencesOf(s, "e") == 2).as("found char=2").isTrue();
		assertThat(StringUtils.countOccurrencesOf(s, "oi") == 2).as("found substring=2").isTrue();
		assertThat(StringUtils.countOccurrencesOf(s, "oiu") == 2).as("found substring=2").isTrue();
		assertThat(StringUtils.countOccurrencesOf(s, "oiur") == 1).as("found substring=3").isTrue();
		assertThat(StringUtils.countOccurrencesOf(s, "r") == 2).as("test last").isTrue();
	}

	@Test
	void replace() {
		String inString = "a6AazAaa77abaa";
		String oldPattern = "aa";
		String newPattern = "foo";

		// Simple replace
		String s = StringUtils.replace(inString, oldPattern, newPattern);
		assertThat(s.equals("a6AazAfoo77abfoo")).as("Replace 1 worked").isTrue();

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
		assertThat(noThe.equals("The quick brown fox jumped over  lazy dog")).as("Result has no the [" + noThe + "]").isTrue();

		String nohe = StringUtils.delete(inString, "he");
		assertThat(nohe.equals("T quick brown fox jumped over t lazy dog")).as("Result has no he [" + nohe + "]").isTrue();

		String nosp = StringUtils.delete(inString, " ");
		assertThat(nosp.equals("Thequickbrownfoxjumpedoverthelazydog")).as("Result has no spaces").isTrue();

		String killEnd = StringUtils.delete(inString, "dog");
		assertThat(killEnd.equals("The quick brown fox jumped over the lazy ")).as("Result has no dog").isTrue();

		String mismatch = StringUtils.delete(inString, "dxxcxcxog");
		assertThat(mismatch.equals(inString)).as("Result is unchanged").isTrue();

		String nochange = StringUtils.delete(inString, "");
		assertThat(nochange.equals(inString)).as("Result is unchanged").isTrue();
	}

	@Test
	void deleteAny() {
		String inString = "Able was I ere I saw Elba";

		String res = StringUtils.deleteAny(inString, "I");
		assertThat(res.equals("Able was  ere  saw Elba")).as("Result has no Is [" + res + "]").isTrue();

		res = StringUtils.deleteAny(inString, "AeEba!");
		assertThat(res.equals("l ws I r I sw l")).as("Result has no Is [" + res + "]").isTrue();

		String mismatch = StringUtils.deleteAny(inString, "#@$#$^");
		assertThat(mismatch.equals(inString)).as("Result is unchanged").isTrue();

		String whitespace = "This is\n\n\n    \t   a messagy string with whitespace\n";
		assertThat(whitespace.contains("\n")).as("Has CR").isTrue();
		assertThat(whitespace.contains("\t")).as("Has tab").isTrue();
		assertThat(whitespace.contains(" ")).as("Has  sp").isTrue();
		String cleaned = StringUtils.deleteAny(whitespace, "\n\t ");
		boolean condition2 = !cleaned.contains("\n");
		assertThat(condition2).as("Has no CR").isTrue();
		boolean condition1 = !cleaned.contains("\t");
		assertThat(condition1).as("Has no tab").isTrue();
		boolean condition = !cleaned.contains(" ");
		assertThat(condition).as("Has no sp").isTrue();
		assertThat(cleaned.length() > 10).as("Still has chars").isTrue();
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
		assertThat(StringUtils.quoteIfString(5)).isEqualTo(Integer.valueOf(5));
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
		assertThat(StringUtils.getFilename(null)).isEqualTo(null);
		assertThat(StringUtils.getFilename("")).isEqualTo("");
		assertThat(StringUtils.getFilename("myfile")).isEqualTo("myfile");
		assertThat(StringUtils.getFilename("mypath/myfile")).isEqualTo("myfile");
		assertThat(StringUtils.getFilename("myfile.")).isEqualTo("myfile.");
		assertThat(StringUtils.getFilename("mypath/myfile.")).isEqualTo("myfile.");
		assertThat(StringUtils.getFilename("myfile.txt")).isEqualTo("myfile.txt");
		assertThat(StringUtils.getFilename("mypath/myfile.txt")).isEqualTo("myfile.txt");
	}

	@Test
	void getFilenameExtension() {
		assertThat(StringUtils.getFilenameExtension(null)).isEqualTo(null);
		assertThat(StringUtils.getFilenameExtension("")).isEqualTo(null);
		assertThat(StringUtils.getFilenameExtension("myfile")).isEqualTo(null);
		assertThat(StringUtils.getFilenameExtension("myPath/myfile")).isEqualTo(null);
		assertThat(StringUtils.getFilenameExtension("/home/user/.m2/settings/myfile")).isEqualTo(null);
		assertThat(StringUtils.getFilenameExtension("myfile.")).isEqualTo("");
		assertThat(StringUtils.getFilenameExtension("myPath/myfile.")).isEqualTo("");
		assertThat(StringUtils.getFilenameExtension("myfile.txt")).isEqualTo("txt");
		assertThat(StringUtils.getFilenameExtension("mypath/myfile.txt")).isEqualTo("txt");
		assertThat(StringUtils.getFilenameExtension("/home/user/.m2/settings/myfile.txt")).isEqualTo("txt");
	}

	@Test
	void stripFilenameExtension() {
		assertThat(StringUtils.stripFilenameExtension("")).isEqualTo("");
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
		assertThat(StringUtils.cleanPath("mypath/..")).isEqualTo("");
		assertThat(StringUtils.cleanPath("mypath/../.")).isEqualTo("");
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
		assertThat(StringUtils.cleanPath("file:///c:/some/../path/the%20file.txt")).isEqualTo("file:///c:/path/the%20file.txt");
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
		assertThat(result.length).isEqualTo(3);
		assertThat(result[0]).isEqualTo("myString2");
		assertThat(result[1]).isEqualTo("myString1");
		assertThat(result[2]).isEqualTo("myString2");

		assertThat(StringUtils.concatenateStringArrays(input1, null)).isEqualTo(input1);
		assertThat(StringUtils.concatenateStringArrays(null, input2)).isEqualTo(input2);
		assertThat(StringUtils.concatenateStringArrays(null, null)).isNull();
	}

	@Test
	@Deprecated
	void mergeStringArrays() {
		String[] input1 = new String[] {"myString2"};
		String[] input2 = new String[] {"myString1", "myString2"};
		String[] result = StringUtils.mergeStringArrays(input1, input2);
		assertThat(result.length).isEqualTo(2);
		assertThat(result[0]).isEqualTo("myString2");
		assertThat(result[1]).isEqualTo("myString1");

		assertThat(StringUtils.mergeStringArrays(input1, null)).isEqualTo(input1);
		assertThat(StringUtils.mergeStringArrays(null, input2)).isEqualTo(input2);
		assertThat(StringUtils.mergeStringArrays(null, null)).isNull();
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
		assertThat(sa.length).isEqualTo(3);
		assertThat(sa[0].equals("a") && sa[1].equals("b") && sa[2].equals("c")).as("components are correct").isTrue();
	}

	@Test
	void tokenizeToStringArrayWithNotIgnoreEmptyTokens() {
		String[] sa = StringUtils.tokenizeToStringArray("a,b , ,c", ",", true, false);
		assertThat(sa.length).isEqualTo(4);
		assertThat(sa[0].equals("a") && sa[1].equals("b") && sa[2].equals("") && sa[3].equals("c")).as("components are correct").isTrue();
	}

	@Test
	void tokenizeToStringArrayWithNotTrimTokens() {
		String[] sa = StringUtils.tokenizeToStringArray("a,b ,c", ",", false, true);
		assertThat(sa.length).isEqualTo(3);
		assertThat(sa[0].equals("a") && sa[1].equals("b ") && sa[2].equals("c")).as("components are correct").isTrue();
	}

	@Test
	void commaDelimitedListToStringArrayWithNullProducesEmptyArray() {
		String[] sa = StringUtils.commaDelimitedListToStringArray(null);
		assertThat(sa != null).as("String array isn't null with null input").isTrue();
		assertThat(sa.length == 0).as("String array length == 0 with null input").isTrue();
	}

	@Test
	void commaDelimitedListToStringArrayWithEmptyStringProducesEmptyArray() {
		String[] sa = StringUtils.commaDelimitedListToStringArray("");
		assertThat(sa != null).as("String array isn't null with null input").isTrue();
		assertThat(sa.length == 0).as("String array length == 0 with null input").isTrue();
	}

	@Test
	void delimitedListToStringArrayWithComma() {
		String[] sa = StringUtils.delimitedListToStringArray("a,b", ",");
		assertThat(sa.length).isEqualTo(2);
		assertThat(sa[0]).isEqualTo("a");
		assertThat(sa[1]).isEqualTo("b");
	}

	@Test
	void delimitedListToStringArrayWithSemicolon() {
		String[] sa = StringUtils.delimitedListToStringArray("a;b", ";");
		assertThat(sa.length).isEqualTo(2);
		assertThat(sa[0]).isEqualTo("a");
		assertThat(sa[1]).isEqualTo("b");
	}

	@Test
	void delimitedListToStringArrayWithEmptyString() {
		String[] sa = StringUtils.delimitedListToStringArray("a,b", "");
		assertThat(sa.length).isEqualTo(3);
		assertThat(sa[0]).isEqualTo("a");
		assertThat(sa[1]).isEqualTo(",");
		assertThat(sa[2]).isEqualTo("b");
	}

	@Test
	void delimitedListToStringArrayWithNullDelimiter() {
		String[] sa = StringUtils.delimitedListToStringArray("a,b", null);
		assertThat(sa.length).isEqualTo(1);
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
		assertThat(sa.length == 1).as("Found one String with no delimiters").isTrue();
		assertThat(sa[0].equals(s)).as("Single array entry matches input String with no delimiters").isTrue();
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
		assertThat(sa[0].equals("a") && sa[1].equals("") && sa[2].equals("b")).as("components are correct").isTrue();

		sa = new String[] {"", "", "a", ""};
		doTestCommaDelimitedListToStringArrayLegalMatch(sa);
	}

	private void doTestCommaDelimitedListToStringArrayLegalMatch(String[] components) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < components.length; i++) {
			if (i != 0) {
				sb.append(",");
			}
			sb.append(components[i]);
		}
		String[] sa = StringUtils.commaDelimitedListToStringArray(sb.toString());
		assertThat(sa != null).as("String array isn't null with legal match").isTrue();
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
	void parseLocaleStringWithMalformedLocaleString() {
		Locale locale = StringUtils.parseLocaleString("_banjo_on_my_knee");
		assertThat(locale).as("When given a malformed Locale string, must not return null.").isNotNull();
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

	@Test  // SPR-3671
	void parseLocaleWithMultiValuedVariantUsingSpacesAsSeparatorsWithLotsOfLeadingWhitespace() {
		String variant = "proper northern";
		String localeString = "en GB            " + variant;  // lots of whitespace
		Locale locale = StringUtils.parseLocaleString(localeString);
		assertThat(locale.getVariant()).as("Multi-valued variant portion of the Locale not extracted correctly.").isEqualTo(variant);
	}

	@Test  // SPR-3671
	void parseLocaleWithMultiValuedVariantUsingUnderscoresAsSeparatorsWithLotsOfLeadingWhitespace() {
		String variant = "proper_northern";
		String localeString = "en_GB_____" + variant;  // lots of underscores
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
				assertThat(locale.getLanguage()).isEqualTo("");
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
				assertThat(locale.getLanguage()).isEqualTo("");
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

}
