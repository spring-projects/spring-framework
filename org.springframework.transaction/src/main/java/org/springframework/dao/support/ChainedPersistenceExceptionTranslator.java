/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.dao.support;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.util.Assert;

/**
 * Implementation of PersistenceExceptionTranslator that supports chaining,
 * allowing the addition of PersistenceExceptionTranslator instances in order.
 * Returns <code>non-null</code> on the first (if any) match.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 */
public class ChainedPersistenceExceptionTranslator implements PersistenceExceptionTranslator {
	
	/** List of PersistenceExceptionTranslators */
	private final List delegates = new ArrayList(4);


	/**
	 * Add a PersistenceExceptionTranslator to the chained delegate list.
	 */
	public final void addDelegate(PersistenceExceptionTranslator pet) {
		Assert.notNull(pet, "PersistenceExceptionTranslator must not be null");
		this.delegates.add(pet);
	}

	/**
	 * Return all registered PersistenceExceptionTranslator delegates (as array).
	 */
	public final PersistenceExceptionTranslator[] getDelegates() {
		return (PersistenceExceptionTranslator[])
				this.delegates.toArray(new PersistenceExceptionTranslator[this.delegates.size()]);
	}


	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
		DataAccessException translatedDex = null;
		for (Iterator it = this.delegates.iterator(); translatedDex == null && it.hasNext(); ) {
			PersistenceExceptionTranslator pet = (PersistenceExceptionTranslator) it.next();
			translatedDex = pet.translateExceptionIfPossible(ex);
		}
		return translatedDex;
	}

}
