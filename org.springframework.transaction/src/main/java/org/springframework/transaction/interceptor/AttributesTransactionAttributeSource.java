/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.transaction.interceptor;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.metadata.Attributes;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Implementation of the {@link TransactionAttributeSource} interface that reads
 * metadata via Spring's {@link org.springframework.metadata.Attributes} abstraction.
 *
 * <p>Typically used for reading in source-level attributes via Commons Attributes.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.metadata.Attributes
 * @see org.springframework.metadata.commons.CommonsAttributes
 */
public class AttributesTransactionAttributeSource extends AbstractFallbackTransactionAttributeSource
		implements InitializingBean {
	
	/**
	 * Underlying Attributes implementation that we're using.
	 */
	private Attributes attributes;


	/**
	 * Create a new AttributesTransactionAttributeSource.
	 * @see #setAttributes
	 */
	public AttributesTransactionAttributeSource() {
	}

	/**
	 * Create a new AttributesTransactionAttributeSource.
	 * @param attributes the Attributes implementation to use
	 * @see org.springframework.metadata.commons.CommonsAttributes
	 */
	public AttributesTransactionAttributeSource(Attributes attributes) {
		Assert.notNull(attributes, "Attributes must not be null");
		this.attributes = attributes;
	}

	/**
	 * Set the Attributes implementation to use.
	 * @see org.springframework.metadata.commons.CommonsAttributes
	 */
	public void setAttributes(Attributes attributes) {
		this.attributes = attributes;
	}

	public void afterPropertiesSet() {
		Assert.notNull(this.attributes, "Property 'attributes' is required");
	}


	protected TransactionAttribute findTransactionAttribute(Method method) {
		Assert.notNull(this.attributes, "Property 'attributes' is required");
		return findTransactionAttribute(this.attributes.getAttributes(method));
	}

	protected TransactionAttribute findTransactionAttribute(Class clazz) {
		Assert.notNull(this.attributes, "Property 'attributes' is required");
		return findTransactionAttribute(this.attributes.getAttributes(clazz));
	}

	/**
	 * Return the transaction attribute, given this set of attributes
	 * attached to a method or class.
	 * <p>Protected rather than private as subclasses may want to customize
	 * how this is done: for example, returning a TransactionAttribute
	 * affected by the values of other attributes.
	 * <p>This implementation takes into account RollbackRuleAttributes,
	 * if the TransactionAttribute is a RuleBasedTransactionAttribute.
	 * @param atts attributes attached to a method or class (may be <code>null</code>)
	 * @return TransactionAttribute the corresponding transaction attribute,
	 * or <code>null</code> if none was found
	 */
	protected TransactionAttribute findTransactionAttribute(Collection atts) {
		if (atts == null) {
			return null;
		}

		TransactionAttribute txAttribute = null;

		// Check whether there is a transaction attribute.
		for (Iterator it = atts.iterator(); it.hasNext() && txAttribute == null; ) {
			Object att = it.next();
			if (att instanceof TransactionAttribute) {
				txAttribute = (TransactionAttribute) att;
			}
		}

		// Check if we have a RuleBasedTransactionAttribute.
		if (txAttribute instanceof RuleBasedTransactionAttribute) {
			RuleBasedTransactionAttribute rbta = (RuleBasedTransactionAttribute) txAttribute;
			// We really want value: bit of a hack.
			List rollbackRules = new LinkedList();
			for (Iterator it = atts.iterator(); it.hasNext(); ) {
				Object att = it.next();
				if (att instanceof RollbackRuleAttribute) {
					rollbackRules.add(att);
				}
			}
			// Repeatedly setting this isn't elegant, but it works.
			rbta.setRollbackRules(rollbackRules);
		}

		return txAttribute;
	}


	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AttributesTransactionAttributeSource)) {
			return false;
		}
		AttributesTransactionAttributeSource otherTas = (AttributesTransactionAttributeSource) other;
		return ObjectUtils.nullSafeEquals(this.attributes, otherTas.attributes);
	}

	public int hashCode() {
		return AttributesTransactionAttributeSource.class.hashCode();
	}

}
