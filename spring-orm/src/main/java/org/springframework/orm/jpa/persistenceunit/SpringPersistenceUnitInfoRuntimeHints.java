/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa.persistenceunit;


import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * {@link RuntimeHintsRegistrar} implementation that registers runtime hints for
 * the {@link SpringPersistenceUnitInfo}.
 *
 * @author Brian Clozel
 * @since 7.0
 */
public class SpringPersistenceUnitInfoRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		hints.reflection().registerType(SpringPersistenceUnitInfo.class, MemberCategory.INVOKE_PUBLIC_METHODS);
		hints.reflection().registerType(MutablePersistenceUnitInfo.class, MemberCategory.INVOKE_PUBLIC_METHODS);
	}

}
