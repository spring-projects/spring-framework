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

package test.parsing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.parsing.AliasDefinition;
import org.springframework.beans.factory.parsing.ComponentDefinition;
import org.springframework.beans.factory.parsing.DefaultsDefinition;
import org.springframework.beans.factory.parsing.ImportDefinition;
import org.springframework.beans.factory.parsing.ReaderEventListener;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class CollectingReaderEventListener implements ReaderEventListener {

	private final List<DefaultsDefinition> defaults = new LinkedList<DefaultsDefinition>();

	private final Map<String, Object> componentDefinitions = new LinkedHashMap<String, Object>(8);

	private final Map<String, List<Object>> aliasMap = new LinkedHashMap<String, List<Object>>(8);

	private final List<ImportDefinition> imports = new LinkedList<ImportDefinition>();


	public void defaultsRegistered(DefaultsDefinition defaultsDefinition) {
		this.defaults.add(defaultsDefinition);
	}

	public List<DefaultsDefinition> getDefaults() {
		return Collections.unmodifiableList(this.defaults);
	}

	public void componentRegistered(ComponentDefinition componentDefinition) {
		this.componentDefinitions.put(componentDefinition.getName(), componentDefinition);
	}

	public ComponentDefinition getComponentDefinition(String name) {
		return (ComponentDefinition) this.componentDefinitions.get(name);
	}

	public ComponentDefinition[] getComponentDefinitions() {
		Collection<Object> collection = this.componentDefinitions.values();
		return collection.toArray(new ComponentDefinition[collection.size()]);
	}

	public void aliasRegistered(AliasDefinition aliasDefinition) {
		List<Object> aliases = this.aliasMap.get(aliasDefinition.getBeanName());
		if(aliases == null) {
			aliases = new ArrayList<Object>();
			this.aliasMap.put(aliasDefinition.getBeanName(), aliases);
		}
		aliases.add(aliasDefinition);
	}

	public List<?> getAliases(String beanName) {
		List<?> aliases = this.aliasMap.get(beanName);
		return aliases == null ? null : Collections.unmodifiableList(aliases);
	}

	public void importProcessed(ImportDefinition importDefinition) {
		this.imports.add(importDefinition);
	}

	public List<ImportDefinition> getImports() {
		return Collections.unmodifiableList(this.imports);
	}

}
