/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * Simple implementation of the {@link AliasRegistry} interface.
 * Serves as base class for
 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}
 * implementations.
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 */
public class SimpleAliasRegistry implements AliasRegistry {

	/** Map from alias to canonical name */
	private final Map aliasMap = CollectionFactory.createConcurrentMapIfPossible(16);


	public void registerAlias(String name, String alias) {
		Assert.hasText(name, "'name' must not be empty");
		Assert.hasText(alias, "'alias' must not be empty");
		if (alias.equals(name)) {
			this.aliasMap.remove(alias);
		}
		else {
			if (!allowAliasOverriding()) {
				String registeredName = (String) this.aliasMap.get(alias);
				if (registeredName != null && !registeredName.equals(name)) {
					throw new IllegalStateException("Cannot register alias '" + alias + "' for name '" +
							name + "': It is already registered for name '" + registeredName + "'.");
				}
			}
			this.aliasMap.put(alias, name);
		}
	}

	/**
	 * Return whether alias overriding is allowed.
	 * Default is <code>true</code>.
	 */
	protected boolean allowAliasOverriding() {
		return true;
	}

	public void removeAlias(String alias) {
		String name = (String) this.aliasMap.remove(alias);
		if (name == null) {
			throw new IllegalStateException("No alias '" + alias + "' registered");
		}
	}

	public boolean isAlias(String name) {
		return this.aliasMap.containsKey(name);
	}

	public String[] getAliases(String name) {
		List aliases = new ArrayList();
		synchronized (this.aliasMap) {
			for (Iterator it = this.aliasMap.entrySet().iterator(); it.hasNext();) {
				Map.Entry entry = (Map.Entry) it.next();
				String registeredName = (String) entry.getValue();
				if (registeredName.equals(name)) {
					aliases.add(entry.getKey());
				}
			}
		}
		return StringUtils.toStringArray(aliases);
	}

	/**
	 * Resolve all alias target names and aliases registered in this
	 * factory, applying the given StringValueResolver to them.
	 * <p>The value resolver may for example resolve placeholders
	 * in target bean names and even in alias names.
	 * @param valueResolver the StringValueResolver to apply
	 */
	public void resolveAliases(StringValueResolver valueResolver) {
		Assert.notNull(valueResolver, "StringValueResolver must not be null");
		synchronized (this.aliasMap) {
			Map aliasCopy = new HashMap(this.aliasMap);
			for (Iterator it = aliasCopy.keySet().iterator(); it.hasNext();) {
				String alias = (String) it.next();
				String registeredName = (String) aliasCopy.get(alias);
				String resolvedAlias = valueResolver.resolveStringValue(alias);
				String resolvedName = valueResolver.resolveStringValue(registeredName);
				if (!resolvedAlias.equals(alias)) {
					String existingName = (String) this.aliasMap.get(resolvedAlias);
					if (existingName != null && !existingName.equals(resolvedName)) {
						throw new IllegalStateException("Cannot register resolved alias '" +
								resolvedAlias + "' (original: '" + alias + "') for name '" + resolvedName +
								"': It is already registered for name '" + registeredName + "'.");
					}
					this.aliasMap.put(resolvedAlias, resolvedName);
					this.aliasMap.remove(alias);
				}
				else if (!registeredName.equals(resolvedName)) {
					this.aliasMap.put(alias, resolvedName);
				}
			}
		}
	}

	/**
	 * Determine the raw name, resolving aliases to canonical names.
	 * @param name the user-specified name
	 * @return the transformed name
	 */
	public String canonicalName(String name) {
		String canonicalName = name;
		// Handle aliasing.
		String resolvedName = null;
		do {
			resolvedName = (String) this.aliasMap.get(canonicalName);
			if (resolvedName != null) {
				canonicalName = resolvedName;
			}
		}
		while (resolvedName != null);
		return canonicalName;
	}

}
