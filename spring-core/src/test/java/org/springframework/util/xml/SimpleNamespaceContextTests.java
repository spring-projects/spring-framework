/*
 * Copyright 2002-2015 the original author or authors.
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.xml.XMLConstants;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 * @author Leo Arnold
 */
public class SimpleNamespaceContextTests {

	private final String unboundPrefix = "unbound";
	private final String prefix = "prefix";
	private final String namespaceUri = "http://Namespace-name-URI";
	private final String additionalNamespaceUri = "http://Additional-namespace-name-URI";
	private final String unboundNamespaceUri = "http://Unbound-namespace-name-URI";
	private final String defaultNamespaceUri = "http://Default-namespace-name-URI";

	private final SimpleNamespaceContext context = new SimpleNamespaceContext();


	@Test(expected = IllegalArgumentException.class)
	public void getNamespaceURI_withNull() throws Exception {
		context.getNamespaceURI(null);
	}

	@Test
	public void getNamespaceURI() {
		context.bindNamespaceUri(XMLConstants.XMLNS_ATTRIBUTE, additionalNamespaceUri);
		assertThat("Always returns \"http://www.w3.org/2000/xmlns/\" for \"xmlns\"",
				context.getNamespaceURI(XMLConstants.XMLNS_ATTRIBUTE), is(XMLConstants.XMLNS_ATTRIBUTE_NS_URI));
		context.bindNamespaceUri(XMLConstants.XML_NS_PREFIX, additionalNamespaceUri);
		assertThat("Always returns \"http://www.w3.org/XML/1998/namespace\" for \"xml\"",
				context.getNamespaceURI(XMLConstants.XML_NS_PREFIX), is(XMLConstants.XML_NS_URI));

		assertThat("Returns \"\" for an unbound prefix", context.getNamespaceURI(unboundPrefix),
				is(XMLConstants.NULL_NS_URI));
		context.bindNamespaceUri(prefix, namespaceUri);
		assertThat("Returns the bound namespace URI for a bound prefix", context.getNamespaceURI(prefix),
				is(namespaceUri));

		assertThat("By default returns URI \"\" for the default namespace prefix",
				context.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX), is(XMLConstants.NULL_NS_URI));
		context.bindDefaultNamespaceUri(defaultNamespaceUri);
		assertThat("Returns the set URI for the default namespace prefix",
				context.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX), is(defaultNamespaceUri));
	}

	@Test(expected = IllegalArgumentException.class)
	public void getPrefix_withNull() throws Exception {
		context.getPrefix(null);
	}

	@Test
	public void getPrefix() {
		assertThat("Always returns \"xmlns\" for \"http://www.w3.org/2000/xmlns/\"",
				context.getPrefix(XMLConstants.XMLNS_ATTRIBUTE_NS_URI), is(XMLConstants.XMLNS_ATTRIBUTE));
		assertThat("Always returns \"xml\" for \"http://www.w3.org/XML/1998/namespace\"",
				context.getPrefix(XMLConstants.XML_NS_URI), is(XMLConstants.XML_NS_PREFIX));

		assertThat("Returns null for an unbound namespace URI", context.getPrefix(unboundNamespaceUri),
				is(nullValue()));
		context.bindNamespaceUri("prefix1", namespaceUri);
		context.bindNamespaceUri("prefix2", namespaceUri);
		assertThat("Returns a prefix for a bound namespace URI", context.getPrefix(namespaceUri),
				anyOf(is("prefix1"), is("prefix2")));
	}

	@Test(expected = IllegalArgumentException.class)
	public void getPrefixes_withNull() throws Exception {
		context.getPrefixes(null);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void getPrefixes_IteratorIsNotModifiable() throws Exception {
		context.bindNamespaceUri(prefix, namespaceUri);
		Iterator<String> iterator = context.getPrefixes(namespaceUri);
		iterator.remove();
	}

	@Test
	public void getPrefixes() {
		assertThat("Returns only \"xmlns\" for \"http://www.w3.org/2000/xmlns/\"",
				getItemSet(context.getPrefixes(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)),
				is(makeSet(XMLConstants.XMLNS_ATTRIBUTE)));
		assertThat("Returns only \"xml\" for \"http://www.w3.org/XML/1998/namespace\"",
				getItemSet(context.getPrefixes(XMLConstants.XML_NS_URI)), is(makeSet(XMLConstants.XML_NS_PREFIX)));

		assertThat("Returns empty iterator for unbound prefix", context.getPrefixes("unbound Namespace URI").hasNext(),
				is(false));
		context.bindNamespaceUri("prefix1", namespaceUri);
		context.bindNamespaceUri("prefix2", namespaceUri);
		assertThat("Returns all prefixes (and only those) bound to the namespace URI",
				getItemSet(context.getPrefixes(namespaceUri)), is(makeSet("prefix1", "prefix2")));
	}

	@Test(expected = IllegalArgumentException.class)
	public void bindNamespaceUri_withNullNamespaceUri() {
		context.bindNamespaceUri("prefix", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void bindNamespaceUri_withNullPrefix() {
		context.bindNamespaceUri(null, namespaceUri);
	}

	@Test
	public void bindNamespaceUri() {
		context.bindNamespaceUri(prefix, namespaceUri);
		assertThat("The Namespace URI was bound to the prefix", context.getNamespaceURI(prefix), is(namespaceUri));
		assertThat("The prefix was bound to the namespace URI", getItemSet(context.getPrefixes(namespaceUri)),
				hasItem(prefix));
	}

	@Test
	public void getBoundPrefixes() {
		context.bindNamespaceUri("prefix1", namespaceUri);
		context.bindNamespaceUri("prefix2", namespaceUri);
		context.bindNamespaceUri("prefix3", additionalNamespaceUri);
		assertThat("Returns all bound prefixes", getItemSet(context.getBoundPrefixes()),
				is(makeSet("prefix1", "prefix2", "prefix3")));
	}

	@Test
	public void clear() {
		context.bindNamespaceUri("prefix1", namespaceUri);
		context.bindNamespaceUri("prefix2", namespaceUri);
		context.bindNamespaceUri("prefix3", additionalNamespaceUri);
		context.clear();
		assertThat("All bound prefixes were removed", context.getBoundPrefixes().hasNext(), is(false));
		assertThat("All bound namespace URIs were removed", context.getPrefixes(namespaceUri).hasNext(), is(false));
	}

	@Test
	public void removeBinding() {
		context.removeBinding(unboundPrefix);

		context.bindNamespaceUri(prefix, namespaceUri);
		context.removeBinding(prefix);
		assertThat("Returns default namespace URI for removed prefix", context.getNamespaceURI(prefix),
				is(XMLConstants.NULL_NS_URI));
		assertThat("#getPrefix returns null when all prefixes for a namespace URI were removed",
				context.getPrefix(namespaceUri), is(nullValue()));
		assertThat("#getPrefixes returns an empty iterator when all prefixes for a namespace URI were removed",
				context.getPrefixes(namespaceUri).hasNext(), is(false));

		context.bindNamespaceUri("prefix1", additionalNamespaceUri);
		context.bindNamespaceUri("prefix2", additionalNamespaceUri);
		context.removeBinding("prefix1");
		assertThat("Prefix was unbound", context.getNamespaceURI("prefix1"), is(XMLConstants.NULL_NS_URI));
		assertThat("#getPrefix returns a bound prefix after removal of another prefix for the same namespace URI",
				context.getPrefix(additionalNamespaceUri), is("prefix2"));
		assertThat("Prefix was removed from namespace URI", getItemSet(context.getPrefixes(additionalNamespaceUri)),
				is(makeSet("prefix2")));
	}


	private Set<String> getItemSet(Iterator<String> iterator) {
		Set<String> itemSet = new HashSet<String>();
		while (iterator.hasNext()) {
			itemSet.add(iterator.next());
		}
		return itemSet;
	}

	private Set<String> makeSet(String... items) {
		Set<String> itemSet = new HashSet<String>();
		for (String item : items) {
			itemSet.add(item);
		}
		return itemSet;
	}

}
