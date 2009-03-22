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
package org.springframework.config.java.support;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Stack;

import org.springframework.config.java.Import;
import org.springframework.util.Assert;


/**
 * {@link Stack} used for detecting circular use of the {@link Import} annotation.
 * 
 * @author Chris Beams
 * @see Import
 * @see ImportStackHolder
 * @see CircularImportException
 */
@SuppressWarnings("serial")
class ImportStack extends Stack<ConfigurationClass> {

	/**
	 * Simplified contains() implementation that tests to see if any {@link ConfigurationClass}
	 * exists within this stack that has the same name as <var>elem</var>. Elem must be of
	 * type ConfigurationClass.
	 */
	@Override
	public boolean contains(Object elem) {
		Assert.isInstanceOf(ConfigurationClass.class, elem);

		ConfigurationClass configClass = (ConfigurationClass) elem;

		Comparator<ConfigurationClass> comparator = new Comparator<ConfigurationClass>() {
			public int compare(ConfigurationClass first, ConfigurationClass second) {
				return first.getName().equals(second.getName()) ? 0 : 1;
			}
		};

		int index = Collections.binarySearch(this, configClass, comparator);

		return index >= 0 ? true : false;
	}

	/**
	 * Given a stack containing (in order)
	 * <ol>
	 * <li>com.acme.Foo</li>
	 * <li>com.acme.Bar</li>
	 * <li>com.acme.Baz</li>
	 * </ol>
	 * Returns "Foo->Bar->Baz". In the case of an empty stack, returns empty string.
	 */
	@Override
	public synchronized String toString() {
		StringBuilder builder = new StringBuilder();

		Iterator<ConfigurationClass> iterator = this.iterator();

		while (iterator.hasNext()) {
			builder.append(iterator.next().getSimpleName());
			if (iterator.hasNext())
				builder.append("->");
		}

		return builder.toString();
	}

}
