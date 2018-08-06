/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.test.util;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit tests for {@link XmlExpectationsHelper}.
 *
 * @author Matthew Depue
 */
public class XmlExpectationsHelperTests {

	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Test
	public void assertXmlEqualForEqual() throws Exception {
		final String control = "<root><field1>f1</field1><field2>f2</field2></root>";
		final String test = "<root><field1>f1</field1><field2>f2</field2></root>";

		final XmlExpectationsHelper xmlHelper = new XmlExpectationsHelper();
		xmlHelper.assertXmlEqual(control, test);
	}

	@Test
	public void assertXmlEqualExceptionForIncorrectValue() throws Exception {
		final String control = "<root><field1>f1</field1><field2>f2</field2></root>";
		final String test = "<root><field1>notf1</field1><field2>f2</field2></root>";

		exception.expect(AssertionError.class);
		exception.expectMessage(Matchers.startsWith("Body content Expected child 'field1'"));

		final XmlExpectationsHelper xmlHelper = new XmlExpectationsHelper();
		xmlHelper.assertXmlEqual(control, test);
	}

	@Test
	public void assertXmlEqualForOutOfOrder() throws Exception {
		final String control = "<root><field1>f1</field1><field2>f2</field2></root>";
		final String test = "<root><field2>f2</field2><field1>f1</field1></root>";

		final XmlExpectationsHelper xmlHelper = new XmlExpectationsHelper();
		xmlHelper.assertXmlEqual(control, test);
	}

	@Test
	public void assertXmlEqualExceptionForMoreEntries() throws Exception {
		final String control = "<root><field1>f1</field1><field2>f2</field2></root>";
		final String test = "<root><field1>f1</field1><field2>f2</field2><field3>f3</field3></root>";

		exception.expect(AssertionError.class);
		exception.expectMessage(Matchers.containsString("Expected child nodelist length '2' but was '3'"));

		final XmlExpectationsHelper xmlHelper = new XmlExpectationsHelper();
		xmlHelper.assertXmlEqual(control, test);
	}

	@Test
	public void assertXmlEqualExceptionForLessEntries() throws Exception {
		final String control = "<root><field1>f1</field1><field2>f2</field2><field3>f3</field3></root>";
		final String test = "<root><field1>f1</field1><field2>f2</field2></root>";

		exception.expect(AssertionError.class);
		exception.expectMessage(Matchers.containsString("Expected child nodelist length '3' but was '2'"));

		final XmlExpectationsHelper xmlHelper = new XmlExpectationsHelper();
		xmlHelper.assertXmlEqual(control, test);
	}

}
