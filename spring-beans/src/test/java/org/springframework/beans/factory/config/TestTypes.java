/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.config;

import org.springframework.beans.factory.ObjectFactory;

/**
 * Shared test types for this package.
 *
 * @author Chris Beams
 */
final class TestTypes {}

/**
 * @author Juergen Hoeller
 */
class NoOpScope implements Scope {

	@Override
	public Object get(String name, ObjectFactory<?> objectFactory) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object remove(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void registerDestructionCallback(String name, Runnable callback) {
	}

}
