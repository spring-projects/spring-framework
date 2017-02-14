/*
 * Copyright 2002-2017 the original author or authors.
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

import java.io.StringReader;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.hamcrest.Matcher;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.ElementSelectors;

import static org.hamcrest.MatcherAssert.*;

/**
 * A helper class for assertions on XML content.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class XmlExpectationsHelper {

	/**
	 * Parse the content as {@link Node} and apply a {@link Matcher}.
	 */
	public void assertNode(String content, Matcher<? super Node> matcher) throws Exception {
		Document document = parseXmlString(content);
		assertThat("Body content", document, matcher);
	}

	private Document parseXmlString(String xml) throws Exception  {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder documentBuilder = factory.newDocumentBuilder();
		InputSource inputSource = new InputSource(new StringReader(xml));
		return documentBuilder.parse(inputSource);
	}

	/**
	 * Parse the content as {@link DOMSource} and apply a {@link Matcher}.
	 * @see <a href="https://github.com/davidehringer/xml-matchers">xml-matchers</a>
	 */
	public void assertSource(String content, Matcher<? super Source> matcher) throws Exception {
		Document document = parseXmlString(content);
		assertThat("Body content", new DOMSource(document), matcher);
	}

	/**
	 * Parse the expected and actual content strings as XML and assert that the
	 * two are "similar" -- i.e. they contain the same elements and attributes
	 * regardless of order.
	 * <p>Use of this method assumes the
	 * <a href="https://github.com/xmlunit/xmlunit">XMLUnit<a/> library is available.
	 * @param expected the expected XML content
	 * @param actual the actual XML content
	 * @see org.springframework.test.web.servlet.result.MockMvcResultMatchers#xpath(String, Object...)
	 * @see org.springframework.test.web.servlet.result.MockMvcResultMatchers#xpath(String, Map, Object...)
	 */
	public void assertXmlEqual(String expected, String actual) throws Exception {
		XmlUnitDiff diff = new XmlUnitDiff(expected, actual);
		if (diff.hasDifferences()) {
			AssertionErrors.fail("Body content " + diff.toString());
		}
	}


	/**
	 * Inner class to prevent hard dependency on XML Unit.
	 */
	private static class XmlUnitDiff {

		private final Diff diff;


		XmlUnitDiff(String expected, String actual) {
			this.diff = DiffBuilder.compare(expected).withTest(actual)
					.withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText))
					.ignoreWhitespace().ignoreComments()
					.checkForSimilar()
					.build();
		}


		public boolean hasDifferences() {
			return diff.hasDifferences();
		}

		@Override
		public String toString() {
			return diff.toString();
		}

	}

}
