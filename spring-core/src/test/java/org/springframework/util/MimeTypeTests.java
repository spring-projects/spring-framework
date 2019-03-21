/*
 * Copyright 2002-2018 the original author or authors.
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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

import static java.util.Collections.*;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link MimeType}.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Dimitrios Liapis
 */
public class MimeTypeTests {

	@Test(expected = IllegalArgumentException.class)
	public void slashInSubtype() {
		new MimeType("text", "/");
	}

	@Test(expected = InvalidMimeTypeException.class)
	public void valueOfNoSubtype() {
		MimeType.valueOf("audio");
	}

	@Test(expected = InvalidMimeTypeException.class)
	public void valueOfNoSubtypeSlash() {
		MimeType.valueOf("audio/");
	}

	@Test(expected = InvalidMimeTypeException.class)
	public void valueOfIllegalType() {
		MimeType.valueOf("audio(/basic");
	}

	@Test(expected = InvalidMimeTypeException.class)
	public void valueOfIllegalSubtype() {
		MimeType.valueOf("audio/basic)");
	}

	@Test(expected = InvalidMimeTypeException.class)
	public void valueOfIllegalCharset() {
		MimeType.valueOf("text/html; charset=foo-bar");
	}

	@Test
	public void parseCharset() {
		String s = "text/html; charset=iso-8859-1";
		MimeType mimeType = MimeType.valueOf(s);
		assertEquals("Invalid type", "text", mimeType.getType());
		assertEquals("Invalid subtype", "html", mimeType.getSubtype());
		assertEquals("Invalid charset", StandardCharsets.ISO_8859_1, mimeType.getCharset());
	}

	@Test
	public void parseQuotedCharset() {
		String s = "application/xml;charset=\"utf-8\"";
		MimeType mimeType = MimeType.valueOf(s);
		assertEquals("Invalid type", "application", mimeType.getType());
		assertEquals("Invalid subtype", "xml", mimeType.getSubtype());
		assertEquals("Invalid charset", StandardCharsets.UTF_8, mimeType.getCharset());
	}

	@Test
	public void parseQuotedSeparator() {
		String s = "application/xop+xml;charset=utf-8;type=\"application/soap+xml;action=\\\"https://x.y.z\\\"\"";
		MimeType mimeType = MimeType.valueOf(s);
		assertEquals("Invalid type", "application", mimeType.getType());
		assertEquals("Invalid subtype", "xop+xml", mimeType.getSubtype());
		assertEquals("Invalid charset", StandardCharsets.UTF_8, mimeType.getCharset());
		assertEquals("\"application/soap+xml;action=\\\"https://x.y.z\\\"\"", mimeType.getParameter("type"));
	}

	@Test
	public void withConversionService() {
		ConversionService conversionService = new DefaultConversionService();
		assertTrue(conversionService.canConvert(String.class, MimeType.class));
		MimeType mimeType = MimeType.valueOf("application/xml");
		assertEquals(mimeType, conversionService.convert("application/xml", MimeType.class));
	}

	@Test
	public void includes() {
		MimeType textPlain = MimeTypeUtils.TEXT_PLAIN;
		assertTrue("Equal types is not inclusive", textPlain.includes(textPlain));
		MimeType allText = new MimeType("text");

		assertTrue("All subtypes is not inclusive", allText.includes(textPlain));
		assertFalse("All subtypes is inclusive", textPlain.includes(allText));

		assertTrue("All types is not inclusive", MimeTypeUtils.ALL.includes(textPlain));
		assertFalse("All types is inclusive", textPlain.includes(MimeTypeUtils.ALL));

		assertTrue("All types is not inclusive", MimeTypeUtils.ALL.includes(textPlain));
		assertFalse("All types is inclusive", textPlain.includes(MimeTypeUtils.ALL));

		MimeType applicationSoapXml = new MimeType("application", "soap+xml");
		MimeType applicationWildcardXml = new MimeType("application", "*+xml");
		MimeType suffixXml = new MimeType("application", "x.y+z+xml"); // SPR-15795

		assertTrue(applicationSoapXml.includes(applicationSoapXml));
		assertTrue(applicationWildcardXml.includes(applicationWildcardXml));
		assertTrue(applicationWildcardXml.includes(suffixXml));

		assertTrue(applicationWildcardXml.includes(applicationSoapXml));
		assertFalse(applicationSoapXml.includes(applicationWildcardXml));
		assertFalse(suffixXml.includes(applicationWildcardXml));

		assertFalse(applicationWildcardXml.includes(MimeTypeUtils.APPLICATION_JSON));
	}

	@Test
	public void isCompatible() {
		MimeType textPlain = MimeTypeUtils.TEXT_PLAIN;
		assertTrue("Equal types is not compatible", textPlain.isCompatibleWith(textPlain));
		MimeType allText = new MimeType("text");

		assertTrue("All subtypes is not compatible", allText.isCompatibleWith(textPlain));
		assertTrue("All subtypes is not compatible", textPlain.isCompatibleWith(allText));

		assertTrue("All types is not compatible", MimeTypeUtils.ALL.isCompatibleWith(textPlain));
		assertTrue("All types is not compatible", textPlain.isCompatibleWith(MimeTypeUtils.ALL));

		assertTrue("All types is not compatible", MimeTypeUtils.ALL.isCompatibleWith(textPlain));
		assertTrue("All types is compatible", textPlain.isCompatibleWith(MimeTypeUtils.ALL));

		MimeType applicationSoapXml = new MimeType("application", "soap+xml");
		MimeType applicationWildcardXml = new MimeType("application", "*+xml");
		MimeType suffixXml = new MimeType("application", "x.y+z+xml"); // SPR-15795

		assertTrue(applicationSoapXml.isCompatibleWith(applicationSoapXml));
		assertTrue(applicationWildcardXml.isCompatibleWith(applicationWildcardXml));
		assertTrue(applicationWildcardXml.isCompatibleWith(suffixXml));

		assertTrue(applicationWildcardXml.isCompatibleWith(applicationSoapXml));
		assertTrue(applicationSoapXml.isCompatibleWith(applicationWildcardXml));
		assertTrue(suffixXml.isCompatibleWith(applicationWildcardXml));

		assertFalse(applicationWildcardXml.isCompatibleWith(MimeTypeUtils.APPLICATION_JSON));
	}

	@Test
	public void testToString() {
		MimeType mimeType = new MimeType("text", "plain");
		String result = mimeType.toString();
		assertEquals("Invalid toString() returned", "text/plain", result);
	}

	@Test
	public void parseMimeType() {
		String s = "audio/*";
		MimeType mimeType = MimeTypeUtils.parseMimeType(s);
		assertEquals("Invalid type", "audio", mimeType.getType());
		assertEquals("Invalid subtype", "*", mimeType.getSubtype());
	}

	@Test(expected = InvalidMimeTypeException.class)
	public void parseMimeTypeNoSubtype() {
		MimeTypeUtils.parseMimeType("audio");
	}

	@Test(expected = InvalidMimeTypeException.class)
	public void parseMimeTypeNoSubtypeSlash() {
		MimeTypeUtils.parseMimeType("audio/");
	}

	@Test(expected = InvalidMimeTypeException.class)
	public void parseMimeTypeTypeRange() {
		MimeTypeUtils.parseMimeType("*/json");
	}

	@Test(expected = InvalidMimeTypeException.class)
	public void parseMimeTypeIllegalType() {
		MimeTypeUtils.parseMimeType("audio(/basic");
	}

	@Test(expected = InvalidMimeTypeException.class)
	public void parseMimeTypeIllegalSubtype() {
		MimeTypeUtils.parseMimeType("audio/basic)");
	}

	@Test(expected = InvalidMimeTypeException.class)
	public void parseMimeTypeMissingTypeAndSubtype() {
		MimeTypeUtils.parseMimeType("     ;a=b");
	}

	@Test(expected = InvalidMimeTypeException.class)
	public void parseMimeTypeEmptyParameterAttribute() {
		MimeTypeUtils.parseMimeType("audio/*;=value");
	}

	@Test(expected = InvalidMimeTypeException.class)
	public void parseMimeTypeEmptyParameterValue() {
		MimeTypeUtils.parseMimeType("audio/*;attr=");
	}

	@Test(expected = InvalidMimeTypeException.class)
	public void parseMimeTypeIllegalParameterAttribute() {
		MimeTypeUtils.parseMimeType("audio/*;attr<=value");
	}

	@Test(expected = InvalidMimeTypeException.class)
	public void parseMimeTypeIllegalParameterValue() {
		MimeTypeUtils.parseMimeType("audio/*;attr=v>alue");
	}

	@Test(expected = InvalidMimeTypeException.class)
	public void parseMimeTypeIllegalCharset() {
		MimeTypeUtils.parseMimeType("text/html; charset=foo-bar");
	}

	@Test  // SPR-8917
	public void parseMimeTypeQuotedParameterValue() {
		MimeType mimeType = MimeTypeUtils.parseMimeType("audio/*;attr=\"v>alue\"");
		assertEquals("\"v>alue\"", mimeType.getParameter("attr"));
	}

	@Test  // SPR-8917
	public void parseMimeTypeSingleQuotedParameterValue() {
		MimeType mimeType = MimeTypeUtils.parseMimeType("audio/*;attr='v>alue'");
		assertEquals("'v>alue'", mimeType.getParameter("attr"));
	}

	@Test // SPR-16630
	public void parseMimeTypeWithSpacesAroundEquals() {
		MimeType mimeType = MimeTypeUtils.parseMimeType("multipart/x-mixed-replace;boundary = --myboundary");
		assertEquals("--myboundary", mimeType.getParameter("boundary"));
	}

	@Test // SPR-16630
	public void parseMimeTypeWithSpacesAroundEqualsAndQuotedValue() {
		MimeType mimeType = MimeTypeUtils.parseMimeType("text/plain; foo = \" bar \" ");
		assertEquals("\" bar \"", mimeType.getParameter("foo"));
	}

	@Test(expected = InvalidMimeTypeException.class)
	public void parseMimeTypeIllegalQuotedParameterValue() {
		MimeTypeUtils.parseMimeType("audio/*;attr=\"");
	}

	@Test
	public void parseMimeTypes() {
		String s = "text/plain, text/html, text/x-dvi, text/x-c";
		List<MimeType> mimeTypes = MimeTypeUtils.parseMimeTypes(s);
		assertNotNull("No mime types returned", mimeTypes);
		assertEquals("Invalid amount of mime types", 4, mimeTypes.size());

		mimeTypes = MimeTypeUtils.parseMimeTypes(null);
		assertNotNull("No mime types returned", mimeTypes);
		assertEquals("Invalid amount of mime types", 0, mimeTypes.size());
	}

	@Test // SPR-17459
	public void parseMimeTypesWithQuotedParameters() {
		testWithQuotedParameters("foo/bar;param=\",\"");
		testWithQuotedParameters("foo/bar;param=\"s,a,\"");
		testWithQuotedParameters("foo/bar;param=\"s,\"", "text/x-c");
		testWithQuotedParameters("foo/bar;param=\"a\\\"b,c\"");
		testWithQuotedParameters("foo/bar;param=\"\\\\\"");
		testWithQuotedParameters("foo/bar;param=\"\\,\\\"");
	}

	private void testWithQuotedParameters(String... mimeTypes) {
		String s = String.join(",", mimeTypes);
		List<MimeType> actual = MimeTypeUtils.parseMimeTypes(s);
		assertEquals(mimeTypes.length, actual.size());
		for (int i=0; i < mimeTypes.length; i++) {
			assertEquals(mimeTypes[i], actual.get(i).toString());
		}
	}

	@Test
	public void compareTo() {
		MimeType audioBasic = new MimeType("audio", "basic");
		MimeType audio = new MimeType("audio");
		MimeType audioWave = new MimeType("audio", "wave");
		MimeType audioBasicLevel = new MimeType("audio", "basic", singletonMap("level", "1"));

		// equal
		assertEquals("Invalid comparison result", 0, audioBasic.compareTo(audioBasic));
		assertEquals("Invalid comparison result", 0, audio.compareTo(audio));
		assertEquals("Invalid comparison result", 0, audioBasicLevel.compareTo(audioBasicLevel));

		assertTrue("Invalid comparison result", audioBasicLevel.compareTo(audio) > 0);

		List<MimeType> expected = new ArrayList<>();
		expected.add(audio);
		expected.add(audioBasic);
		expected.add(audioBasicLevel);
		expected.add(audioWave);

		List<MimeType> result = new ArrayList<>(expected);
		Random rnd = new Random();
		// shuffle & sort 10 times
		for (int i = 0; i < 10; i++) {
			Collections.shuffle(result, rnd);
			Collections.sort(result);

			for (int j = 0; j < result.size(); j++) {
				assertSame("Invalid media type at " + j + ", run " + i, expected.get(j), result.get(j));
			}
		}
	}

	@Test
	public void compareToCaseSensitivity() {
		MimeType m1 = new MimeType("audio", "basic");
		MimeType m2 = new MimeType("Audio", "Basic");
		assertEquals("Invalid comparison result", 0, m1.compareTo(m2));
		assertEquals("Invalid comparison result", 0, m2.compareTo(m1));

		m1 = new MimeType("audio", "basic", singletonMap("foo", "bar"));
		m2 = new MimeType("audio", "basic", singletonMap("Foo", "bar"));
		assertEquals("Invalid comparison result", 0, m1.compareTo(m2));
		assertEquals("Invalid comparison result", 0, m2.compareTo(m1));

		m1 = new MimeType("audio", "basic", singletonMap("foo", "bar"));
		m2 = new MimeType("audio", "basic", singletonMap("foo", "Bar"));
		assertTrue("Invalid comparison result", m1.compareTo(m2) != 0);
		assertTrue("Invalid comparison result", m2.compareTo(m1) != 0);
	}

	/**
	 * SPR-13157
	 * @since 4.2
	 */
	@Test
	public void equalsIsCaseInsensitiveForCharsets() {
		MimeType m1 = new MimeType("text", "plain", singletonMap("charset", "UTF-8"));
		MimeType m2 = new MimeType("text", "plain", singletonMap("charset", "utf-8"));
		assertEquals(m1, m2);
		assertEquals(m2, m1);
		assertEquals(0, m1.compareTo(m2));
		assertEquals(0, m2.compareTo(m1));
	}

}
