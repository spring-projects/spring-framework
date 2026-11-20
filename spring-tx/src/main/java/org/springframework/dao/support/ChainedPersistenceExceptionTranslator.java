/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.dao.support;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.dao.DataAccessException;
import org.springframework.util.Assert;

/**
 * Implementation of {@link PersistenceExceptionTranslator} that supports chaining,
 * allowing the addition of PersistenceExceptionTranslator instances in order.
 * Returns {@code non-null} on the first (if any) match.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 */
public class ChainedPersistenceExceptionTranslator implements PersistenceExceptionTranslator {

	/** List of PersistenceExceptionTranslators. */
	private final List<PersistenceExceptionTranslator> delegates = new ArrayList<>(4);


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
		return this.delegates.toArray(new PersistenceExceptionTranslator[0]);
	}


	@Override
	public @Nullable DataAccessException translateExceptionIfPossible(RuntimeException ex) {
		for (PersistenceExceptionTranslator pet : this.delegates) {
			DataAccessException translatedDex = pet.translateExceptionIfPossible(ex);
			if (translatedDex != null) {
				return translatedDex;
			}
		}
		return null;
	}

}
