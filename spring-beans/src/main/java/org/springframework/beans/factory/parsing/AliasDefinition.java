/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans.factory.parsing;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.util.Assert;

/**
 * Representation of an alias that has been registered during the parsing process.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see ReaderEventListener#aliasRegistered(AliasDefinition)
 */
public class AliasDefinition implements BeanMetadataElement {

	private final String beanName;

	private final String alias;

	private final Object source;


	/**
	 * Create a new AliasDefinition.
	 * @param beanName the canonical name of the bean
	 * @param alias the alias registered for the bean
	 */
	public AliasDefinition(String beanName, String alias) {
		this(beanName, alias, null);
	}

	/**
	 * Create a new AliasDefinition.
	 * @param beanName the canonical name of the bean
	 * @param alias the alias registered for the bean
	 * @param source the source object (may be {@code null})
	 */
	public AliasDefinition(String beanName, String alias, Object source) {
		Assert.notNull(beanName, "Bean name must not be null");
		Assert.notNull(alias, "Alias must not be null");
		this.beanName = beanName;
		this.alias = alias;
		this.source = source;
	}


	/**
	 * Return the canonical name of the bean.
	 */
	public final String getBeanName() {
		return this.beanName;
	}

	/**
	 * Return the alias registered for the bean.
	 */
	public final String getAlias() {
		return this.alias;
	}

	@Override
	public final Object getSource() {
		return this.source;
	}

}
