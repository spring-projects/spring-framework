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

package org.springframework.test.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link XmlExpectationsHelper}.
 *
 * @author Matthew Depue
 */
class XmlExpectationsHelperTests {

	private static final String CONTROL = "<root><field1>f1</field1><field2>f2</field2></root>";

	private final XmlExpectationsHelper xmlHelper = new XmlExpectationsHelper();


	@Test
	void assertXmlEqualForEqual() throws Exception {
		String test = "<root><field1>f1</field1><field2>f2</field2></root>";
		xmlHelper.assertXmlEqual(CONTROL, test);
	}

	@Test
	void assertXmlEqualExceptionForIncorrectValue() {
		String test = "<root><field1>notf1</field1><field2>f2</field2></root>";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> xmlHelper.assertXmlEqual(CONTROL, test))
				.withMessageStartingWith("Body content Expected child 'field1'");
	}

	@Test
	void assertXmlEqualForOutOfOrder() throws Exception {
		String test = "<root><field2>f2</field2><field1>f1</field1></root>";
		xmlHelper.assertXmlEqual(CONTROL, test);
	}

	@Test
	void assertXmlEqualExceptionForMoreEntries() {
		String test = "<root><field1>f1</field1><field2>f2</field2><field3>f3</field3></root>";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> xmlHelper.assertXmlEqual(CONTROL, test))
				.withMessageContaining("Expected child nodelist length '2' but was '3'");

	}

	@Test
	void assertXmlEqualExceptionForFewerEntries() {
		String control = "<root><field1>f1</field1><field2>f2</field2><field3>f3</field3></root>";
		String test = "<root><field1>f1</field1><field2>f2</field2></root>";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> xmlHelper.assertXmlEqual(control, test))
				.withMessageContaining("Expected child nodelist length '3' but was '2'");
	}

	@Test
	void assertXmlEqualExceptionWithFullDescription() {
		String test = "<root><field2>f2</field2><field3>f3</field3></root>";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> xmlHelper.assertXmlEqual(CONTROL, test))
				.withMessageContaining("Expected child 'field1' but was 'null'")
				.withMessageContaining("Expected child 'null' but was 'field3'");
	}

}
