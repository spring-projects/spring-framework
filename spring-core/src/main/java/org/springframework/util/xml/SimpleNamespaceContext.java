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

package org.springframework.util.xml;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Simple {@code javax.xml.namespace.NamespaceContext} implementation.
 * Follows the standard {@code NamespaceContext} contract, and is loadable
 * via a {@code java.util.Map} or {@code java.util.Properties} object
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 */
public class SimpleNamespaceContext implements NamespaceContext {

	private final Map<String, String> prefixToNamespaceUri = new HashMap<>();

	private final Map<String, Set<String>> namespaceUriToPrefixes = new HashMap<>();

	private String defaultNamespaceUri = "";


	@Override
	public String getNamespaceURI(String prefix) {
		Assert.notNull(prefix, "No prefix given");
		if (XMLConstants.XML_NS_PREFIX.equals(prefix)) {
			return XMLConstants.XML_NS_URI;
		}
		else if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) {
			return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
		}
		else if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
			return this.defaultNamespaceUri;
		}
		else if (this.prefixToNamespaceUri.containsKey(prefix)) {
			return this.prefixToNamespaceUri.get(prefix);
		}
		return "";
	}

	@Override
	@Nullable
	public String getPrefix(String namespaceUri) {
		Set<String> prefixes = getPrefixesSet(namespaceUri);
		return (!prefixes.isEmpty() ? prefixes.iterator().next() : null);
	}

	@Override
	public Iterator<String> getPrefixes(String namespaceUri) {
		return getPrefixesSet(namespaceUri).iterator();
	}

	private Set<String> getPrefixesSet(String namespaceUri) {
		Assert.notNull(namespaceUri, "No namespaceUri given");
		if (this.defaultNamespaceUri.equals(namespaceUri)) {
			return Collections.singleton(XMLConstants.DEFAULT_NS_PREFIX);
		}
		else if (XMLConstants.XML_NS_URI.equals(namespaceUri)) {
			return Collections.singleton(XMLConstants.XML_NS_PREFIX);
		}
		else if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceUri)) {
			return Collections.singleton(XMLConstants.XMLNS_ATTRIBUTE);
		}
		else {
			Set<String> prefixes = this.namespaceUriToPrefixes.get(namespaceUri);
			return (prefixes != null ?  Collections.unmodifiableSet(prefixes) : Collections.emptySet());
		}
	}


	/**
	 * Set the bindings for this namespace context.
	 * The supplied map must consist of string key value pairs.
	 */
	public void setBindings(Map<String, String> bindings) {
		bindings.forEach(this::bindNamespaceUri);
	}

	/**
	 * Bind the given namespace as default namespace.
	 * @param namespaceUri the namespace uri
	 */
	public void bindDefaultNamespaceUri(String namespaceUri) {
		bindNamespaceUri(XMLConstants.DEFAULT_NS_PREFIX, namespaceUri);
	}

	/**
	 * Bind the given prefix to the given namespace.
	 * @param prefix the namespace prefix
	 * @param namespaceUri the namespace uri
	 */
	public void bindNamespaceUri(String prefix, String namespaceUri) {
		Assert.notNull(prefix, "No prefix given");
		Assert.notNull(namespaceUri, "No namespaceUri given");
		if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
			this.defaultNamespaceUri = namespaceUri;
		}
		else {
			this.prefixToNamespaceUri.put(prefix, namespaceUri);
			Set<String> prefixes = this.namespaceUriToPrefixes.get(namespaceUri);
			if (prefixes == null) {
				prefixes = new LinkedHashSet<>();
				this.namespaceUriToPrefixes.put(namespaceUri, prefixes);
			}
			prefixes.add(prefix);
		}
	}

	/**
	 * Remove the given prefix from this context.
	 * @param prefix the prefix to be removed
	 */
	public void removeBinding(@Nullable String prefix) {
		if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
			this.defaultNamespaceUri = "";
		}
		else if (prefix != null) {
			String namespaceUri = this.prefixToNamespaceUri.remove(prefix);
			if (namespaceUri != null) {
				Set<String> prefixes = this.namespaceUriToPrefixes.get(namespaceUri);
				if (prefixes != null) {
					prefixes.remove(prefix);
					if (prefixes.isEmpty()) {
						this.namespaceUriToPrefixes.remove(namespaceUri);
					}
				}
			}
		}
	}

	/**
	 * Remove all declared prefixes.
	 */
	public void clear() {
		this.prefixToNamespaceUri.clear();
		this.namespaceUriToPrefixes.clear();
	}

	/**
	 * Return all declared prefixes.
	 */
	public Iterator<String> getBoundPrefixes() {
		return this.prefixToNamespaceUri.keySet().iterator();
	}

}
