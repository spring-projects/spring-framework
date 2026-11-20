/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.dao.support;

import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataAccessException;

public class MapPersistenceExceptionTranslator implements PersistenceExceptionTranslator {

	// in to out
	private final Map<RuntimeException, RuntimeException> translations = new HashMap<>();


	public void addTranslation(RuntimeException in, RuntimeException out) {
		this.translations.put(in, out);
	}


	@Override
	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
		return (DataAccessException) this.translations.get(ex);
	}

}
