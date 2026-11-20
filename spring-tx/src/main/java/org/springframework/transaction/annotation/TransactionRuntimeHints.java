/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.annotation;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeHint;
import org.springframework.aot.hint.TypeReference;

/**
 * {@link RuntimeHintsRegistrar} implementation that registers runtime hints
 * for transaction management.
 *
 * @author Sebastien Deleuze
 * @since 6.0
 * @see TransactionBeanRegistrationAotProcessor
 */
class TransactionRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		hints.reflection().registerTypes(
				TypeReference.listOf(Isolation.class, Propagation.class),
				TypeHint.builtWith(MemberCategory.ACCESS_DECLARED_FIELDS));
	}

}
