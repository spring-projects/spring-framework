/*
 * Copyright 2002-2009 the original author or authors.
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
package org.springframework.util.xml;

import java.util.Collections;
import java.util.Iterator;
import javax.xml.XMLConstants;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class SimpleNamespaceContextTests {

	private SimpleNamespaceContext context;

	@Before
	public void createContext() throws Exception {
		context = new SimpleNamespaceContext();
		context.bindNamespaceUri("prefix", "namespaceURI");
	}

	@Test
	public void getNamespaceURI() {
		assertEquals("Invalid namespaceURI for default namespace", "",
				context.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX));
		String defaultNamespaceUri = "defaultNamespace";
		context.bindNamespaceUri(XMLConstants.DEFAULT_NS_PREFIX, defaultNamespaceUri);
		assertEquals("Invalid namespaceURI for default namespace", defaultNamespaceUri,
				context.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX));
		assertEquals("Invalid namespaceURI for bound prefix", "namespaceURI", context.getNamespaceURI("prefix"));
		assertEquals("Invalid namespaceURI for unbound prefix", "", context.getNamespaceURI("unbound"));
		assertEquals("Invalid namespaceURI for namespace prefix", XMLConstants.XML_NS_URI,
				context.getNamespaceURI(XMLConstants.XML_NS_PREFIX));
		assertEquals("Invalid namespaceURI for attribute prefix", XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
				context.getNamespaceURI(XMLConstants.XMLNS_ATTRIBUTE));

	}

	@Test
	public void getPrefix() {
		assertEquals("Invalid prefix for default namespace", XMLConstants.DEFAULT_NS_PREFIX, context.getPrefix(""));
		assertEquals("Invalid prefix for bound namespace", "prefix", context.getPrefix("namespaceURI"));
		assertNull("Invalid prefix for unbound namespace", context.getPrefix("unbound"));
		assertEquals("Invalid prefix for namespace", XMLConstants.XML_NS_PREFIX,
				context.getPrefix(XMLConstants.XML_NS_URI));
		assertEquals("Invalid prefix for attribute namespace", XMLConstants.XMLNS_ATTRIBUTE,
				context.getPrefix(XMLConstants.XMLNS_ATTRIBUTE_NS_URI));
	}

	@Test
	public void getPrefixes() {
		assertPrefixes("", XMLConstants.DEFAULT_NS_PREFIX);
		assertPrefixes("namespaceURI", "prefix");
		assertFalse("Invalid prefix for unbound namespace", context.getPrefixes("unbound").hasNext());
		assertPrefixes(XMLConstants.XML_NS_URI, XMLConstants.XML_NS_PREFIX);
		assertPrefixes(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, XMLConstants.XMLNS_ATTRIBUTE);
	}

	@Test
	public void multiplePrefixes() {
		context.bindNamespaceUri("prefix1", "namespace");
		context.bindNamespaceUri("prefix2", "namespace");
		Iterator iterator = context.getPrefixes("namespace");
		assertNotNull("getPrefixes returns null", iterator);
		assertTrue("iterator is empty", iterator.hasNext());
		String result = (String) iterator.next();
		assertTrue("Invalid prefix", result.equals("prefix1") || result.equals("prefix2"));
		assertTrue("iterator is empty", iterator.hasNext());
		result = (String) iterator.next();
		assertTrue("Invalid prefix", result.equals("prefix1") || result.equals("prefix2"));
		assertFalse("iterator contains more than two values", iterator.hasNext());
	}

	private void assertPrefixes(String namespaceUri, String prefix) {
		Iterator iterator = context.getPrefixes(namespaceUri);
		assertNotNull("getPrefixes returns null", iterator);
		assertTrue("iterator is empty", iterator.hasNext());
		String result = (String) iterator.next();
		assertEquals("Invalid prefix", prefix, result);
		assertFalse("iterator contains multiple values", iterator.hasNext());
	}

	@Test
	public void getBoundPrefixes() throws Exception {
		Iterator iterator = context.getBoundPrefixes();
		assertNotNull("getPrefixes returns null", iterator);
		assertTrue("iterator is empty", iterator.hasNext());
		String result = (String) iterator.next();
		assertEquals("Invalid prefix", "prefix", result);
		assertFalse("iterator contains multiple values", iterator.hasNext());
	}

	@Test
	public void setBindings() throws Exception {
		context.setBindings(Collections.singletonMap("prefix", "namespace"));
		assertEquals("Invalid namespace uri", "namespace", context.getNamespaceURI("prefix"));
	}

	@Test
	public void removeBinding() throws Exception {
		context.removeBinding("prefix");
		assertNull("Invalid prefix for unbound namespace", context.getPrefix("prefix"));


	}

}
