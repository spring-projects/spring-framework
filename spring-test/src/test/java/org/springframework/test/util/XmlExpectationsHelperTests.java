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

package org.springframework.test.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for {@link XmlExpectationsHelper}.
 *
 * @author Matthew Depue
 */
class XmlExpectationsHelperTests {

	@Test
	void assertXmlEqualForEqual() throws Exception {
		String control = "<root><field1>f1</field1><field2>f2</field2></root>";
		String test = "<root><field1>f1</field1><field2>f2</field2></root>";
		XmlExpectationsHelper xmlHelper = new XmlExpectationsHelper();
		xmlHelper.assertXmlEqual(control, test);
	}

	@Test
	void assertXmlEqualExceptionForIncorrectValue() throws Exception {
		String control = "<root><field1>f1</field1><field2>f2</field2></root>";
		String test = "<root><field1>notf1</field1><field2>f2</field2></root>";
		XmlExpectationsHelper xmlHelper = new XmlExpectationsHelper();
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				xmlHelper.assertXmlEqual(control, test))
			.withMessageStartingWith("Body content Expected child 'field1'");
	}

	@Test
	void assertXmlEqualForOutOfOrder() throws Exception {
		String control = "<root><field1>f1</field1><field2>f2</field2></root>";
		String test = "<root><field2>f2</field2><field1>f1</field1></root>";
		XmlExpectationsHelper xmlHelper = new XmlExpectationsHelper();
		xmlHelper.assertXmlEqual(control, test);
	}

	@Test
	void assertXmlEqualExceptionForMoreEntries() throws Exception {
		String control = "<root><field1>f1</field1><field2>f2</field2></root>";
		String test = "<root><field1>f1</field1><field2>f2</field2><field3>f3</field3></root>";
		XmlExpectationsHelper xmlHelper = new XmlExpectationsHelper();
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				xmlHelper.assertXmlEqual(control, test))
			.withMessageContaining("Expected child nodelist length '2' but was '3'");

	}

	@Test
	void assertXmlEqualExceptionForLessEntries() throws Exception {
		String control = "<root><field1>f1</field1><field2>f2</field2><field3>f3</field3></root>";
		String test = "<root><field1>f1</field1><field2>f2</field2></root>";
		XmlExpectationsHelper xmlHelper = new XmlExpectationsHelper();
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				xmlHelper.assertXmlEqual(control, test))
			.withMessageContaining("Expected child nodelist length '3' but was '2'");
	}

}
