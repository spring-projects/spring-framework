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

package org.springframework.util.xml;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.xml.XMLConstants;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Arjen Poutsma
 * @author Leo Arnold
 */
class SimpleNamespaceContextTests {

	private final String unboundPrefix = "unbound";
	private final String prefix = "prefix";
	private final String namespaceUri = "https://Namespace-name-URI";
	private final String additionalNamespaceUri = "https://Additional-namespace-name-URI";
	private final String unboundNamespaceUri = "https://Unbound-namespace-name-URI";
	private final String defaultNamespaceUri = "https://Default-namespace-name-URI";

	private final SimpleNamespaceContext context = new SimpleNamespaceContext();


	@Test
	void getNamespaceURI_withNull() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				context.getNamespaceURI(null));
	}

	@Test
	void getNamespaceURI() {
		context.bindNamespaceUri(XMLConstants.XMLNS_ATTRIBUTE, additionalNamespaceUri);
		assertThat(context.getNamespaceURI(XMLConstants.XMLNS_ATTRIBUTE))
				.as("Always returns \"http://www.w3.org/2000/xmlns/\" for \"xmlns\"")
				.isEqualTo(XMLConstants.XMLNS_ATTRIBUTE_NS_URI);
		context.bindNamespaceUri(XMLConstants.XML_NS_PREFIX, additionalNamespaceUri);
		assertThat(context.getNamespaceURI(XMLConstants.XML_NS_PREFIX))
				.as("Always returns \"http://www.w3.org/XML/1998/namespace\" for \"xml\"")
				.isEqualTo(XMLConstants.XML_NS_URI);

		assertThat(context.getNamespaceURI(unboundPrefix))
				.as("Returns \"\" for an unbound prefix").isEmpty();
		context.bindNamespaceUri(prefix, namespaceUri);
		assertThat(context.getNamespaceURI(prefix))
				.as("Returns the bound namespace URI for a bound prefix")
				.isEqualTo(namespaceUri);

		assertThat(context.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX))
				.as("By default returns URI \"\" for the default namespace prefix").isEmpty();
		context.bindDefaultNamespaceUri(defaultNamespaceUri);
		assertThat(context.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX))
				.as("Returns the set URI for the default namespace prefix")
				.isEqualTo(defaultNamespaceUri);
	}

	@Test
	void getPrefix_withNull() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				context.getPrefix(null));
	}

	@Test
	void getPrefix() {
		assertThat(context.getPrefix(XMLConstants.XMLNS_ATTRIBUTE_NS_URI))
				.as("Always returns \"xmlns\" for \"http://www.w3.org/2000/xmlns/\"")
				.isEqualTo(XMLConstants.XMLNS_ATTRIBUTE);
		assertThat(context.getPrefix(XMLConstants.XML_NS_URI))
				.as("Always returns \"xml\" for \"http://www.w3.org/XML/1998/namespace\"")
				.isEqualTo(XMLConstants.XML_NS_PREFIX);

		assertThat(context.getPrefix(unboundNamespaceUri)).as("Returns null for an unbound namespace URI").isNull();
		context.bindNamespaceUri("prefix1", namespaceUri);
		context.bindNamespaceUri("prefix2", namespaceUri);
		assertThat(context.getPrefix(namespaceUri))
				.as("Returns a prefix for a bound namespace URI")
				.matches(prefix -> "prefix1".equals(prefix) || "prefix2".equals(prefix));
	}

	@Test
	void getPrefixes_withNull() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				context.getPrefixes(null));
	}

	@Test
	void getPrefixes_IteratorIsNotModifiable() {
		context.bindNamespaceUri(prefix, namespaceUri);
		Iterator<String> iterator = context.getPrefixes(namespaceUri);
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(
				iterator::remove);
	}

	@Test
	void getPrefixes() {
		assertThat(getItemSet(context.getPrefixes(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)))
				.as("Returns only \"xmlns\" for \"http://www.w3.org/2000/xmlns/\"")
				.containsExactly(XMLConstants.XMLNS_ATTRIBUTE);
		assertThat(getItemSet(context.getPrefixes(XMLConstants.XML_NS_URI)))
				.as("Returns only \"xml\" for \"http://www.w3.org/XML/1998/namespace\"")
				.containsExactly(XMLConstants.XML_NS_PREFIX);

		assertThat(context.getPrefixes("unbound Namespace URI").hasNext())
				.as("Returns empty iterator for unbound prefix")
				.isFalse();
		context.bindNamespaceUri("prefix1", namespaceUri);
		context.bindNamespaceUri("prefix2", namespaceUri);
		assertThat(getItemSet(context.getPrefixes(namespaceUri)))
				.as("Returns all prefixes (and only those) bound to the namespace URI")
				.containsExactlyInAnyOrder("prefix1", "prefix2");
	}

	@Test
	void bindNamespaceUri_withNullNamespaceUri() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				context.bindNamespaceUri("prefix", null));
	}

	@Test
	void bindNamespaceUri_withNullPrefix() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				context.bindNamespaceUri(null, namespaceUri));
	}

	@Test
	void bindNamespaceUri() {
		context.bindNamespaceUri(prefix, namespaceUri);
		assertThat(context.getNamespaceURI(prefix))
				.as("The Namespace URI was bound to the prefix")
				.isEqualTo(namespaceUri);
		assertThat(getItemSet(context.getPrefixes(namespaceUri)))
				.as("The prefix was bound to the namespace URI")
				.contains(prefix);
	}

	@Test
	void getBoundPrefixes() {
		context.bindNamespaceUri("prefix1", namespaceUri);
		context.bindNamespaceUri("prefix2", namespaceUri);
		context.bindNamespaceUri("prefix3", additionalNamespaceUri);
		assertThat(getItemSet(context.getBoundPrefixes()))
				.as("Returns all bound prefixes")
				.containsExactlyInAnyOrder("prefix1", "prefix2", "prefix3");
	}

	@Test
	void clear() {
		context.bindNamespaceUri("prefix1", namespaceUri);
		context.bindNamespaceUri("prefix2", namespaceUri);
		context.bindNamespaceUri("prefix3", additionalNamespaceUri);
		context.clear();
		assertThat(context.getBoundPrefixes().hasNext()).as("All bound prefixes were removed").isFalse();
		assertThat(context.getPrefixes(namespaceUri).hasNext()).as("All bound namespace URIs were removed").isFalse();
	}

	@Test
	void removeBinding() {
		context.removeBinding(unboundPrefix);

		context.bindNamespaceUri(prefix, namespaceUri);
		context.removeBinding(prefix);
		assertThat(context.getNamespaceURI(prefix)).as("Returns default namespace URI for removed prefix").isEmpty();
		assertThat(context.getPrefix(namespaceUri)).as("#getPrefix returns null when all prefixes for a namespace URI were removed").isNull();
		assertThat(context.getPrefixes(namespaceUri).hasNext()).as("#getPrefixes returns an empty iterator when all prefixes for a namespace URI were removed").isFalse();

		context.bindNamespaceUri("prefix1", additionalNamespaceUri);
		context.bindNamespaceUri("prefix2", additionalNamespaceUri);
		context.removeBinding("prefix1");
		assertThat(context.getNamespaceURI("prefix1")).as("Prefix was unbound").isEmpty();
		assertThat(context.getPrefix(additionalNamespaceUri)).as("#getPrefix returns a bound prefix after removal of another prefix for the same namespace URI").isEqualTo("prefix2");
		assertThat(getItemSet(context.getPrefixes(additionalNamespaceUri)))
				.as("Prefix was removed from namespace URI")
				.containsExactly("prefix2");
	}


	private Set<String> getItemSet(Iterator<String> iterator) {
		Set<String> itemSet = new LinkedHashSet<>();
		iterator.forEachRemaining(itemSet::add);
		return itemSet;
	}

}
