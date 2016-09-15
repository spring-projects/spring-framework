/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rick Evans
 */
public class StringUtilsTests {

	@Test
	public void testHasTextBlank() throws Exception {
		String blank = "          ";
		assertEquals(false, StringUtils.hasText(blank));
	}

	@Test
	public void testHasTextNullEmpty() throws Exception {
		assertEquals(false, StringUtils.hasText(null));
		assertEquals(false, StringUtils.hasText(""));
	}

	@Test
	public void testHasTextValid() throws Exception {
		assertEquals(true, StringUtils.hasText("t"));
	}

	@Test
	public void testContainsWhitespace() throws Exception {
		assertFalse(StringUtils.containsWhitespace(null));
		assertFalse(StringUtils.containsWhitespace(""));
		assertFalse(StringUtils.containsWhitespace("a"));
		assertFalse(StringUtils.containsWhitespace("abc"));
		assertTrue(StringUtils.containsWhitespace(" "));
		assertTrue(StringUtils.containsWhitespace(" a"));
		assertTrue(StringUtils.containsWhitespace("abc "));
		assertTrue(StringUtils.containsWhitespace("a b"));
		assertTrue(StringUtils.containsWhitespace("a  b"));
	}

	@Test
	public void testTrimWhitespace() throws Exception {
		assertEquals(null, StringUtils.trimWhitespace(null));
		assertEquals("", StringUtils.trimWhitespace(""));
		assertEquals("", StringUtils.trimWhitespace(" "));
		assertEquals("", StringUtils.trimWhitespace("\t"));
		assertEquals("a", StringUtils.trimWhitespace(" a"));
		assertEquals("a", StringUtils.trimWhitespace("a "));
		assertEquals("a", StringUtils.trimWhitespace(" a "));
		assertEquals("a b", StringUtils.trimWhitespace(" a b "));
		assertEquals("a b  c", StringUtils.trimWhitespace(" a b  c "));
	}

	@Test
	public void testTrimAllWhitespace() throws Exception {
		assertEquals("", StringUtils.trimAllWhitespace(""));
		assertEquals("", StringUtils.trimAllWhitespace(" "));
		assertEquals("", StringUtils.trimAllWhitespace("\t"));
		assertEquals("a", StringUtils.trimAllWhitespace(" a"));
		assertEquals("a", StringUtils.trimAllWhitespace("a "));
		assertEquals("a", StringUtils.trimAllWhitespace(" a "));
		assertEquals("ab", StringUtils.trimAllWhitespace(" a b "));
		assertEquals("abc", StringUtils.trimAllWhitespace(" a b  c "));
	}

	@Test
	public void testTrimLeadingWhitespace() throws Exception {
		assertEquals(null, StringUtils.trimLeadingWhitespace(null));
		assertEquals("", StringUtils.trimLeadingWhitespace(""));
		assertEquals("", StringUtils.trimLeadingWhitespace(" "));
		assertEquals("", StringUtils.trimLeadingWhitespace("\t"));
		assertEquals("a", StringUtils.trimLeadingWhitespace(" a"));
		assertEquals("a ", StringUtils.trimLeadingWhitespace("a "));
		assertEquals("a ", StringUtils.trimLeadingWhitespace(" a "));
		assertEquals("a b ", StringUtils.trimLeadingWhitespace(" a b "));
		assertEquals("a b  c ", StringUtils.trimLeadingWhitespace(" a b  c "));
	}

	@Test
	public void testTrimTrailingWhitespace() throws Exception {
		assertEquals(null, StringUtils.trimTrailingWhitespace(null));
		assertEquals("", StringUtils.trimTrailingWhitespace(""));
		assertEquals("", StringUtils.trimTrailingWhitespace(" "));
		assertEquals("", StringUtils.trimTrailingWhitespace("\t"));
		assertEquals("a", StringUtils.trimTrailingWhitespace("a "));
		assertEquals(" a", StringUtils.trimTrailingWhitespace(" a"));
		assertEquals(" a", StringUtils.trimTrailingWhitespace(" a "));
		assertEquals(" a b", StringUtils.trimTrailingWhitespace(" a b "));
		assertEquals(" a b  c", StringUtils.trimTrailingWhitespace(" a b  c "));
	}

	@Test
	public void testTrimLeadingCharacter() throws Exception {
		assertEquals(null, StringUtils.trimLeadingCharacter(null, ' '));
		assertEquals("", StringUtils.trimLeadingCharacter("", ' '));
		assertEquals("", StringUtils.trimLeadingCharacter(" ", ' '));
		assertEquals("\t", StringUtils.trimLeadingCharacter("\t", ' '));
		assertEquals("a", StringUtils.trimLeadingCharacter(" a", ' '));
		assertEquals("a ", StringUtils.trimLeadingCharacter("a ", ' '));
		assertEquals("a ", StringUtils.trimLeadingCharacter(" a ", ' '));
		assertEquals("a b ", StringUtils.trimLeadingCharacter(" a b ", ' '));
		assertEquals("a b  c ", StringUtils.trimLeadingCharacter(" a b  c ", ' '));
	}

	@Test
	public void testTrimTrailingCharacter() throws Exception {
		assertEquals(null, StringUtils.trimTrailingCharacter(null, ' '));
		assertEquals("", StringUtils.trimTrailingCharacter("", ' '));
		assertEquals("", StringUtils.trimTrailingCharacter(" ", ' '));
		assertEquals("\t", StringUtils.trimTrailingCharacter("\t", ' '));
		assertEquals("a", StringUtils.trimTrailingCharacter("a ", ' '));
		assertEquals(" a", StringUtils.trimTrailingCharacter(" a", ' '));
		assertEquals(" a", StringUtils.trimTrailingCharacter(" a ", ' '));
		assertEquals(" a b", StringUtils.trimTrailingCharacter(" a b ", ' '));
		assertEquals(" a b  c", StringUtils.trimTrailingCharacter(" a b  c ", ' '));
	}

	@Test
	public void testCountOccurrencesOf() {
		assertTrue("nullx2 = 0",
				StringUtils.countOccurrencesOf(null, null) == 0);
		assertTrue("null string = 0",
				StringUtils.countOccurrencesOf("s", null) == 0);
		assertTrue("null substring = 0",
				StringUtils.countOccurrencesOf(null, "s") == 0);
		String s = "erowoiueoiur";
		assertTrue("not found = 0",
				StringUtils.countOccurrencesOf(s, "WERWER") == 0);
		assertTrue("not found char = 0",
				StringUtils.countOccurrencesOf(s, "x") == 0);
		assertTrue("not found ws = 0",
				StringUtils.countOccurrencesOf(s, " ") == 0);
		assertTrue("not found empty string = 0",
				StringUtils.countOccurrencesOf(s, "") == 0);
		assertTrue("found char=2", StringUtils.countOccurrencesOf(s, "e") == 2);
		assertTrue("found substring=2",
				StringUtils.countOccurrencesOf(s, "oi") == 2);
		assertTrue("found substring=2",
				StringUtils.countOccurrencesOf(s, "oiu") == 2);
		assertTrue("found substring=3",
				StringUtils.countOccurrencesOf(s, "oiur") == 1);
		assertTrue("test last", StringUtils.countOccurrencesOf(s, "r") == 2);
	}

	@Test
	public void testReplace() throws Exception {
		String inString = "a6AazAaa77abaa";
		String oldPattern = "aa";
		String newPattern = "foo";

		// Simple replace
		String s = StringUtils.replace(inString, oldPattern, newPattern);
		assertTrue("Replace 1 worked", s.equals("a6AazAfoo77abfoo"));

		// Non match: no change
		s = StringUtils.replace(inString, "qwoeiruqopwieurpoqwieur", newPattern);
		assertTrue("Replace non matched is equal", s.equals(inString));

		// Null new pattern: should ignore
		s = StringUtils.replace(inString, oldPattern, null);
		assertTrue("Replace non matched is equal", s.equals(inString));

		// Null old pattern: should ignore
		s = StringUtils.replace(inString, null, newPattern);
		assertTrue("Replace non matched is equal", s.equals(inString));
	}

	@Test
	public void testDelete() throws Exception {
		String inString = "The quick brown fox jumped over the lazy dog";

		String noThe = StringUtils.delete(inString, "the");
		assertTrue("Result has no the [" + noThe + "]",
				noThe.equals("The quick brown fox jumped over  lazy dog"));

		String nohe = StringUtils.delete(inString, "he");
		assertTrue("Result has no he [" + nohe + "]",
				nohe.equals("T quick brown fox jumped over t lazy dog"));

		String nosp = StringUtils.delete(inString, " ");
		assertTrue("Result has no spaces",
				nosp.equals("Thequickbrownfoxjumpedoverthelazydog"));

		String killEnd = StringUtils.delete(inString, "dog");
		assertTrue("Result has no dog",
				killEnd.equals("The quick brown fox jumped over the lazy "));

		String mismatch = StringUtils.delete(inString, "dxxcxcxog");
		assertTrue("Result is unchanged", mismatch.equals(inString));

		String nochange = StringUtils.delete(inString, "");
		assertTrue("Result is unchanged", nochange.equals(inString));
	}

	@Test
	public void testDeleteAny() throws Exception {
		String inString = "Able was I ere I saw Elba";

		String res = StringUtils.deleteAny(inString, "I");
		assertTrue("Result has no Is [" + res + "]", res.equals("Able was  ere  saw Elba"));

		res = StringUtils.deleteAny(inString, "AeEba!");
		assertTrue("Result has no Is [" + res + "]", res.equals("l ws I r I sw l"));

		String mismatch = StringUtils.deleteAny(inString, "#@$#$^");
		assertTrue("Result is unchanged", mismatch.equals(inString));

		String whitespace = "This is\n\n\n    \t   a messagy string with whitespace\n";
		assertTrue("Has CR", whitespace.contains("\n"));
		assertTrue("Has tab", whitespace.contains("\t"));
		assertTrue("Has  sp", whitespace.contains(" "));
		String cleaned = StringUtils.deleteAny(whitespace, "\n\t ");
		assertTrue("Has no CR", !cleaned.contains("\n"));
		assertTrue("Has no tab", !cleaned.contains("\t"));
		assertTrue("Has no sp", !cleaned.contains(" "));
		assertTrue("Still has chars", cleaned.length() > 10);
	}


	@Test
	public void testQuote() {
		assertEquals("'myString'", StringUtils.quote("myString"));
		assertEquals("''", StringUtils.quote(""));
		assertNull(StringUtils.quote(null));
	}

	@Test
	public void testQuoteIfString() {
		assertEquals("'myString'", StringUtils.quoteIfString("myString"));
		assertEquals("''", StringUtils.quoteIfString(""));
		assertEquals(new Integer(5), StringUtils.quoteIfString(5));
		assertNull(StringUtils.quoteIfString(null));
	}

	@Test
	public void testUnqualify() {
		String qualified = "i.am.not.unqualified";
		assertEquals("unqualified", StringUtils.unqualify(qualified));
	}

	@Test
	public void testCapitalize() {
		String capitalized = "i am not capitalized";
		assertEquals("I am not capitalized", StringUtils.capitalize(capitalized));
	}

	@Test
	public void testUncapitalize() {
		String capitalized = "I am capitalized";
		assertEquals("i am capitalized", StringUtils.uncapitalize(capitalized));
	}

	@Test
	public void testGetFilename() {
		assertEquals(null, StringUtils.getFilename(null));
		assertEquals("", StringUtils.getFilename(""));
		assertEquals("myfile", StringUtils.getFilename("myfile"));
		assertEquals("myfile", StringUtils.getFilename("mypath/myfile"));
		assertEquals("myfile.", StringUtils.getFilename("myfile."));
		assertEquals("myfile.", StringUtils.getFilename("mypath/myfile."));
		assertEquals("myfile.txt", StringUtils.getFilename("myfile.txt"));
		assertEquals("myfile.txt", StringUtils.getFilename("mypath/myfile.txt"));
	}

	@Test
	public void testGetFilenameExtension() {
		assertEquals(null, StringUtils.getFilenameExtension(null));
		assertEquals(null, StringUtils.getFilenameExtension(""));
		assertEquals(null, StringUtils.getFilenameExtension("myfile"));
		assertEquals(null, StringUtils.getFilenameExtension("myPath/myfile"));
		assertEquals(null, StringUtils.getFilenameExtension("/home/user/.m2/settings/myfile"));
		assertEquals("", StringUtils.getFilenameExtension("myfile."));
		assertEquals("", StringUtils.getFilenameExtension("myPath/myfile."));
		assertEquals("txt", StringUtils.getFilenameExtension("myfile.txt"));
		assertEquals("txt", StringUtils.getFilenameExtension("mypath/myfile.txt"));
		assertEquals("txt", StringUtils.getFilenameExtension("/home/user/.m2/settings/myfile.txt"));
	}

	@Test
	public void testStripFilenameExtension() {
		assertEquals(null, StringUtils.stripFilenameExtension(null));
		assertEquals("", StringUtils.stripFilenameExtension(""));
		assertEquals("myfile", StringUtils.stripFilenameExtension("myfile"));
		assertEquals("myfile", StringUtils.stripFilenameExtension("myfile."));
		assertEquals("myfile", StringUtils.stripFilenameExtension("myfile.txt"));
		assertEquals("mypath/myfile", StringUtils.stripFilenameExtension("mypath/myfile"));
		assertEquals("mypath/myfile", StringUtils.stripFilenameExtension("mypath/myfile."));
		assertEquals("mypath/myfile", StringUtils.stripFilenameExtension("mypath/myfile.txt"));
		assertEquals("/home/user/.m2/settings/myfile", StringUtils.stripFilenameExtension("/home/user/.m2/settings/myfile"));
		assertEquals("/home/user/.m2/settings/myfile", StringUtils.stripFilenameExtension("/home/user/.m2/settings/myfile."));
		assertEquals("/home/user/.m2/settings/myfile", StringUtils.stripFilenameExtension("/home/user/.m2/settings/myfile.txt"));
	}

	@Test
	public void testCleanPath() {
		assertEquals("mypath/myfile", StringUtils.cleanPath("mypath/myfile"));
		assertEquals("mypath/myfile", StringUtils.cleanPath("mypath\\myfile"));
		assertEquals("mypath/myfile", StringUtils.cleanPath("mypath/../mypath/myfile"));
		assertEquals("mypath/myfile", StringUtils.cleanPath("mypath/myfile/../../mypath/myfile"));
		assertEquals("../mypath/myfile", StringUtils.cleanPath("../mypath/myfile"));
		assertEquals("../mypath/myfile", StringUtils.cleanPath("../mypath/../mypath/myfile"));
		assertEquals("../mypath/myfile", StringUtils.cleanPath("mypath/../../mypath/myfile"));
		assertEquals("/../mypath/myfile", StringUtils.cleanPath("/../mypath/myfile"));
		assertEquals("/mypath/myfile", StringUtils.cleanPath("/a/:b/../../mypath/myfile"));
		assertEquals("file:///c:/path/to/the%20file.txt", StringUtils.cleanPath("file:///c:/some/../path/to/the%20file.txt"));
	}

	@Test
	public void testPathEquals() {
		assertTrue("Must be true for the same strings",
				StringUtils.pathEquals("/dummy1/dummy2/dummy3",
						"/dummy1/dummy2/dummy3"));
		assertTrue("Must be true for the same win strings",
				StringUtils.pathEquals("C:\\dummy1\\dummy2\\dummy3",
						"C:\\dummy1\\dummy2\\dummy3"));
		assertTrue("Must be true for one top path on 1",
				StringUtils.pathEquals("/dummy1/bin/../dummy2/dummy3",
						"/dummy1/dummy2/dummy3"));
		assertTrue("Must be true for one win top path on 2",
				StringUtils.pathEquals("C:\\dummy1\\dummy2\\dummy3",
						"C:\\dummy1\\bin\\..\\dummy2\\dummy3"));
		assertTrue("Must be true for two top paths on 1",
				StringUtils.pathEquals("/dummy1/bin/../dummy2/bin/../dummy3",
						"/dummy1/dummy2/dummy3"));
		assertTrue("Must be true for two win top paths on 2",
				StringUtils.pathEquals("C:\\dummy1\\dummy2\\dummy3",
						"C:\\dummy1\\bin\\..\\dummy2\\bin\\..\\dummy3"));
		assertTrue("Must be true for double top paths on 1",
				StringUtils.pathEquals("/dummy1/bin/tmp/../../dummy2/dummy3",
						"/dummy1/dummy2/dummy3"));
		assertTrue("Must be true for double top paths on 2 with similarity",
				StringUtils.pathEquals("/dummy1/dummy2/dummy3",
						"/dummy1/dum/dum/../../dummy2/dummy3"));
		assertTrue("Must be true for current paths",
				StringUtils.pathEquals("./dummy1/dummy2/dummy3",
						"dummy1/dum/./dum/../../dummy2/dummy3"));
		assertFalse("Must be false for relative/absolute paths",
				StringUtils.pathEquals("./dummy1/dummy2/dummy3",
						"/dummy1/dum/./dum/../../dummy2/dummy3"));
		assertFalse("Must be false for different strings",
				StringUtils.pathEquals("/dummy1/dummy2/dummy3",
						"/dummy1/dummy4/dummy3"));
		assertFalse("Must be false for one false path on 1",
				StringUtils.pathEquals("/dummy1/bin/tmp/../dummy2/dummy3",
						"/dummy1/dummy2/dummy3"));
		assertFalse("Must be false for one false win top path on 2",
				StringUtils.pathEquals("C:\\dummy1\\dummy2\\dummy3",
						"C:\\dummy1\\bin\\tmp\\..\\dummy2\\dummy3"));
		assertFalse("Must be false for top path on 1 + difference",
				StringUtils.pathEquals("/dummy1/bin/../dummy2/dummy3",
						"/dummy1/dummy2/dummy4"));
	}

	@Test
	public void testConcatenateStringArrays() {
		String[] input1 = new String[] {"myString2"};
		String[] input2 = new String[] {"myString1", "myString2"};
		String[] result = StringUtils.concatenateStringArrays(input1, input2);
		assertEquals(3, result.length);
		assertEquals("myString2", result[0]);
		assertEquals("myString1", result[1]);
		assertEquals("myString2", result[2]);

		assertArrayEquals(input1, StringUtils.concatenateStringArrays(input1, null));
		assertArrayEquals(input2, StringUtils.concatenateStringArrays(null, input2));
		assertNull(StringUtils.concatenateStringArrays(null, null));
	}

	@Test
	public void testMergeStringArrays() {
		String[] input1 = new String[] {"myString2"};
		String[] input2 = new String[] {"myString1", "myString2"};
		String[] result = StringUtils.mergeStringArrays(input1, input2);
		assertEquals(2, result.length);
		assertEquals("myString2", result[0]);
		assertEquals("myString1", result[1]);

		assertArrayEquals(input1, StringUtils.mergeStringArrays(input1, null));
		assertArrayEquals(input2, StringUtils.mergeStringArrays(null, input2));
		assertNull(StringUtils.mergeStringArrays(null, null));
	}

	@Test
	public void testSortStringArray() {
		String[] input = new String[] {"myString2"};
		input = StringUtils.addStringToArray(input, "myString1");
		assertEquals("myString2", input[0]);
		assertEquals("myString1", input[1]);

		StringUtils.sortStringArray(input);
		assertEquals("myString1", input[0]);
		assertEquals("myString2", input[1]);
	}

	@Test
	public void testRemoveDuplicateStrings() {
		String[] input = new String[] {"myString2", "myString1", "myString2"};
		input = StringUtils.removeDuplicateStrings(input);
		assertEquals("myString1", input[0]);
		assertEquals("myString2", input[1]);
	}

	@Test
	public void testSplitArrayElementsIntoProperties() {
		String[] input = new String[] {"key1=value1 ", "key2 =\"value2\""};
		Properties result = StringUtils.splitArrayElementsIntoProperties(input, "=");
		assertEquals("value1", result.getProperty("key1"));
		assertEquals("\"value2\"", result.getProperty("key2"));
	}

	@Test
	public void testSplitArrayElementsIntoPropertiesAndDeletedChars() {
		String[] input = new String[] {"key1=value1 ", "key2 =\"value2\""};
		Properties result = StringUtils.splitArrayElementsIntoProperties(input, "=", "\"");
		assertEquals("value1", result.getProperty("key1"));
		assertEquals("value2", result.getProperty("key2"));
	}

	@Test
	public void testTokenizeToStringArray() {
		String[] sa = StringUtils.tokenizeToStringArray("a,b , ,c", ",");
		assertEquals(3, sa.length);
		assertTrue("components are correct",
				sa[0].equals("a") && sa[1].equals("b") && sa[2].equals("c"));
	}

	@Test
	public void testTokenizeToStringArrayWithNotIgnoreEmptyTokens() {
		String[] sa = StringUtils.tokenizeToStringArray("a,b , ,c", ",", true, false);
		assertEquals(4, sa.length);
		assertTrue("components are correct",
				sa[0].equals("a") && sa[1].equals("b") && sa[2].equals("") && sa[3].equals("c"));
	}

	@Test
	public void testTokenizeToStringArrayWithNotTrimTokens() {
		String[] sa = StringUtils.tokenizeToStringArray("a,b ,c", ",", false, true);
		assertEquals(3, sa.length);
		assertTrue("components are correct",
				sa[0].equals("a") && sa[1].equals("b ") && sa[2].equals("c"));
	}

	@Test
	public void testCommaDelimitedListToStringArrayWithNullProducesEmptyArray() {
		String[] sa = StringUtils.commaDelimitedListToStringArray(null);
		assertTrue("String array isn't null with null input", sa != null);
		assertTrue("String array length == 0 with null input", sa.length == 0);
	}

	@Test
	public void testCommaDelimitedListToStringArrayWithEmptyStringProducesEmptyArray() {
		String[] sa = StringUtils.commaDelimitedListToStringArray("");
		assertTrue("String array isn't null with null input", sa != null);
		assertTrue("String array length == 0 with null input", sa.length == 0);
	}

	@Test
	public void testDelimitedListToStringArrayWithComma() {
		String[] sa = StringUtils.delimitedListToStringArray("a,b", ",");
		assertEquals(2, sa.length);
		assertEquals("a", sa[0]);
		assertEquals("b", sa[1]);
	}

	@Test
	public void testDelimitedListToStringArrayWithSemicolon() {
		String[] sa = StringUtils.delimitedListToStringArray("a;b", ";");
		assertEquals(2, sa.length);
		assertEquals("a", sa[0]);
		assertEquals("b", sa[1]);
	}

	@Test
	public void testDelimitedListToStringArrayWithEmptyString() {
		String[] sa = StringUtils.delimitedListToStringArray("a,b", "");
		assertEquals(3, sa.length);
		assertEquals("a", sa[0]);
		assertEquals(",", sa[1]);
		assertEquals("b", sa[2]);
	}

	@Test
	public void testDelimitedListToStringArrayWithNullDelimiter() {
		String[] sa = StringUtils.delimitedListToStringArray("a,b", null);
		assertEquals(1, sa.length);
		assertEquals("a,b", sa[0]);
	}

	@Test
	public void testCommaDelimitedListToStringArrayMatchWords() {
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
		assertEquals("Reverse transformation is equal",
				Arrays.asList(sa),
				Arrays.asList(reverse));
	}

	@Test
	public void testCommaDelimitedListToStringArraySingleString() {
		// Could read these from files
		String s = "woeirqupoiewuropqiewuorpqiwueopriquwopeiurqopwieur";
		String[] sa = StringUtils.commaDelimitedListToStringArray(s);
		assertTrue("Found one String with no delimiters", sa.length == 1);
		assertTrue("Single array entry matches input String with no delimiters",
				sa[0].equals(s));
	}

	@Test
	public void testCommaDelimitedListToStringArrayWithOtherPunctuation() {
		// Could read these from files
		String[] sa = new String[] {"xcvwert4456346&*.", "///", ".!", ".", ";"};
		doTestCommaDelimitedListToStringArrayLegalMatch(sa);
	}

	/**
	 * We expect to see the empty Strings in the output.
	 */
	@Test
	public void testCommaDelimitedListToStringArrayEmptyStrings() {
		// Could read these from files
		String[] sa = StringUtils.commaDelimitedListToStringArray("a,,b");
		assertEquals("a,,b produces array length 3", 3, sa.length);
		assertTrue("components are correct",
				sa[0].equals("a") && sa[1].equals("") && sa[2].equals("b"));

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
		assertTrue("String array isn't null with legal match", sa != null);
		assertEquals("String array length is correct with legal match", components.length, sa.length);
		assertTrue("Output equals input", Arrays.equals(sa, components));
	}

	@Test
	public void testEndsWithIgnoreCase() {
		String suffix = "fOo";
		assertTrue(StringUtils.endsWithIgnoreCase("foo", suffix));
		assertTrue(StringUtils.endsWithIgnoreCase("Foo", suffix));
		assertTrue(StringUtils.endsWithIgnoreCase("barfoo", suffix));
		assertTrue(StringUtils.endsWithIgnoreCase("barbarfoo", suffix));
		assertTrue(StringUtils.endsWithIgnoreCase("barFoo", suffix));
		assertTrue(StringUtils.endsWithIgnoreCase("barBarFoo", suffix));
		assertTrue(StringUtils.endsWithIgnoreCase("barfoO", suffix));
		assertTrue(StringUtils.endsWithIgnoreCase("barFOO", suffix));
		assertTrue(StringUtils.endsWithIgnoreCase("barfOo", suffix));
		assertFalse(StringUtils.endsWithIgnoreCase(null, suffix));
		assertFalse(StringUtils.endsWithIgnoreCase("barfOo", null));
		assertFalse(StringUtils.endsWithIgnoreCase("b", suffix));
	}


	@Test
	public void testParseLocaleStringSunnyDay() throws Exception {
		Locale expectedLocale = Locale.UK;
		Locale locale = StringUtils.parseLocaleString(expectedLocale.toString());
		assertNotNull("When given a bona-fide Locale string, must not return null.", locale);
		assertEquals(expectedLocale, locale);
	}

	@Test
	public void testParseLocaleStringWithMalformedLocaleString() throws Exception {
		Locale locale = StringUtils.parseLocaleString("_banjo_on_my_knee");
		assertNotNull("When given a malformed Locale string, must not return null.", locale);
	}

	@Test
	public void testParseLocaleStringWithEmptyLocaleStringYieldsNullLocale() throws Exception {
		Locale locale = StringUtils.parseLocaleString("");
		assertNull("When given an empty Locale string, must return null.", locale);
	}

	@Test  // SPR-8637
	public void testParseLocaleWithMultiSpecialCharactersInVariant() throws Exception {
		String variant = "proper-northern";
		String localeString = "en_GB_" + variant;
		Locale locale = StringUtils.parseLocaleString(localeString);
		assertEquals("Multi-valued variant portion of the Locale not extracted correctly.", variant, locale.getVariant());
	}

	@Test  // SPR-3671
	public void testParseLocaleWithMultiValuedVariant() throws Exception {
		String variant = "proper_northern";
		String localeString = "en_GB_" + variant;
		Locale locale = StringUtils.parseLocaleString(localeString);
		assertEquals("Multi-valued variant portion of the Locale not extracted correctly.", variant, locale.getVariant());
	}

	@Test  // SPR-3671
	public void testParseLocaleWithMultiValuedVariantUsingSpacesAsSeparators() throws Exception {
		String variant = "proper northern";
		String localeString = "en GB " + variant;
		Locale locale = StringUtils.parseLocaleString(localeString);
		assertEquals("Multi-valued variant portion of the Locale not extracted correctly.", variant, locale.getVariant());
	}

	@Test  // SPR-3671
	public void testParseLocaleWithMultiValuedVariantUsingMixtureOfUnderscoresAndSpacesAsSeparators() throws Exception {
		String variant = "proper northern";
		String localeString = "en_GB_" + variant;
		Locale locale = StringUtils.parseLocaleString(localeString);
		assertEquals("Multi-valued variant portion of the Locale not extracted correctly.", variant, locale.getVariant());
	}

	@Test  // SPR-3671
	public void testParseLocaleWithMultiValuedVariantUsingSpacesAsSeparatorsWithLotsOfLeadingWhitespace() throws Exception {
		String variant = "proper northern";
		String localeString = "en GB            " + variant;  // lots of whitespace
		Locale locale = StringUtils.parseLocaleString(localeString);
		assertEquals("Multi-valued variant portion of the Locale not extracted correctly.", variant, locale.getVariant());
	}

	@Test  // SPR-3671
	public void testParseLocaleWithMultiValuedVariantUsingUnderscoresAsSeparatorsWithLotsOfLeadingWhitespace() throws Exception {
		String variant = "proper_northern";
		String localeString = "en_GB_____" + variant;  // lots of underscores
		Locale locale = StringUtils.parseLocaleString(localeString);
		assertEquals("Multi-valued variant portion of the Locale not extracted correctly.", variant, locale.getVariant());
	}

	@Test  // SPR-7779
	public void testParseLocaleWithInvalidCharacters() {
		try {
			StringUtils.parseLocaleString("%0D%0AContent-length:30%0D%0A%0D%0A%3Cscript%3Ealert%28123%29%3C/script%3E");
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	@Test  // SPR-9420
	public void testParseLocaleWithSameLowercaseTokenForLanguageAndCountry() {
		assertEquals("tr_TR", StringUtils.parseLocaleString("tr_tr").toString());
		assertEquals("bg_BG_vnt", StringUtils.parseLocaleString("bg_bg_vnt").toString());
	}

	@Test  // SPR-11806
	public void testParseLocaleWithVariantContainingCountryCode() {
		String variant = "GBtest";
		String localeString = "en_GB_" + variant;
		Locale locale = StringUtils.parseLocaleString(localeString);
		assertEquals("Variant containing country code not extracted correctly", variant, locale.getVariant());
	}

	@Test  // SPR-14718
	public void testParseJava7Variant() {
		assertEquals("sr_#LATN", StringUtils.parseLocaleString("sr_#LATN").toString());
	}

}
