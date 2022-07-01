/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.transaction.annotation;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.support.RuntimeHintsUtils;

import static java.util.Arrays.asList;

/**
 * {@link RuntimeHintsRegistrar} implementation that registers runtime hints for
 * transaction management.
 *
 * @author Sebastien Deleuze
 * @since 6.0
 */
public class TransactionRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		RuntimeHintsUtils.registerAnnotation(hints, org.springframework.transaction.annotation.Transactional.class);

		hints.reflection()
				.registerTypes(asList(
								TypeReference.of(org.springframework.transaction.annotation.Isolation.class),
								TypeReference.of(org.springframework.transaction.annotation.Propagation.class),
								TypeReference.of(org.springframework.transaction.TransactionDefinition.class)),
						builder -> builder.withMembers(MemberCategory.DECLARED_FIELDS));
	}
}
