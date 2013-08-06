/*
 * Copyright 2002-2012 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

import org.springframework.util.Assert;

/**
 * Simple {@code javax.xml.namespace.NamespaceContext} implementation. Follows the standard
 * {@code NamespaceContext} contract, and is loadable via a {@code java.util.Map} or
 * {@code java.util.Properties} object
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public class SimpleNamespaceContext implements NamespaceContext {

	private Map<String, String> prefixToNamespaceUri = new HashMap<String, String>();

	private Map<String, List<String>> namespaceUriToPrefixes = new HashMap<String, List<String>>();

	private String defaultNamespaceUri = "";

	@Override
	public String getNamespaceURI(String prefix) {
		Assert.notNull(prefix, "prefix is null");
		if (XMLConstants.XML_NS_PREFIX.equals(prefix)) {
			return XMLConstants.XML_NS_URI;
		}
		else if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) {
			return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
		}
		else if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
			return defaultNamespaceUri;
		}
		else if (prefixToNamespaceUri.containsKey(prefix)) {
			return prefixToNamespaceUri.get(prefix);
		}
		return "";
	}

	@Override
	public String getPrefix(String namespaceUri) {
		List prefixes = getPrefixesInternal(namespaceUri);
		return prefixes.isEmpty() ? null : (String) prefixes.get(0);
	}

	@Override
	public Iterator getPrefixes(String namespaceUri) {
		return getPrefixesInternal(namespaceUri).iterator();
	}

	/**
	 * Sets the bindings for this namespace context. The supplied map must consist of string key value pairs.
	 *
	 * @param bindings the bindings
	 */
	public void setBindings(Map<String, String> bindings) {
		for (Map.Entry<String, String> entry : bindings.entrySet()) {
			bindNamespaceUri(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Binds the given namespace as default namespace.
	 *
	 * @param namespaceUri the namespace uri
	 */
	public void bindDefaultNamespaceUri(String namespaceUri) {
		bindNamespaceUri(XMLConstants.DEFAULT_NS_PREFIX, namespaceUri);
	}

	/**
	 * Binds the given prefix to the given namespace.
	 *
	 * @param prefix	   the namespace prefix
	 * @param namespaceUri the namespace uri
	 */
	public void bindNamespaceUri(String prefix, String namespaceUri) {
		Assert.notNull(prefix, "No prefix given");
		Assert.notNull(namespaceUri, "No namespaceUri given");
		if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
			defaultNamespaceUri = namespaceUri;
		}
		else {
			prefixToNamespaceUri.put(prefix, namespaceUri);
			getPrefixesInternal(namespaceUri).add(prefix);
		}
	}

	/** Removes all declared prefixes. */
	public void clear() {
		prefixToNamespaceUri.clear();
	}

	/**
	 * Returns all declared prefixes.
	 *
	 * @return the declared prefixes
	 */
	public Iterator<String> getBoundPrefixes() {
		return prefixToNamespaceUri.keySet().iterator();
	}

	private List<String> getPrefixesInternal(String namespaceUri) {
		if (defaultNamespaceUri.equals(namespaceUri)) {
			return Collections.singletonList(XMLConstants.DEFAULT_NS_PREFIX);
		}
		else if (XMLConstants.XML_NS_URI.equals(namespaceUri)) {
			return Collections.singletonList(XMLConstants.XML_NS_PREFIX);
		}
		else if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceUri)) {
			return Collections.singletonList(XMLConstants.XMLNS_ATTRIBUTE);
		}
		else {
			List<String> list = namespaceUriToPrefixes.get(namespaceUri);
			if (list == null) {
				list = new ArrayList<String>();
				namespaceUriToPrefixes.put(namespaceUri, list);
			}
			return list;
		}
	}

	/**
	 * Removes the given prefix from this context.
	 *
	 * @param prefix the prefix to be removed
	 */
	public void removeBinding(String prefix) {
		if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
			defaultNamespaceUri = "";
		}
		else {
			String namespaceUri = prefixToNamespaceUri.remove(prefix);
			List prefixes = getPrefixesInternal(namespaceUri);
			prefixes.remove(prefix);
		}
	}
}
