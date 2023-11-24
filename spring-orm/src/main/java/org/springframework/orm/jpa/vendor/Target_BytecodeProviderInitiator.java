/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.orm.jpa.vendor;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.hibernate.bytecode.spi.BytecodeProvider;

import static com.oracle.svm.core.annotate.RecomputeFieldValue.Kind;

/**
 * Hibernate substitution designed to prevent ByteBuddy reachability on native, and to enforce the
 * usage of {@code org.hibernate.bytecode.internal.none.BytecodeProviderImpl} with Hibernate 6.3+.
 * TODO Collaborate with Hibernate team on a substitution-less alternative that does not require a custom list of StandardServiceInitiator
 *
 * @author Sebastien Deleuze
 * @since 6.1
 */
@TargetClass(className = "org.hibernate.bytecode.internal.BytecodeProviderInitiator", onlyWith = SubstituteOnlyIfPresent.class)
final class Target_BytecodeProviderInitiator {

	@Alias
	public static String BYTECODE_PROVIDER_NAME_NONE;

	@Alias
	@RecomputeFieldValue(kind = Kind.FromAlias)
	public static String BYTECODE_PROVIDER_NAME_DEFAULT = BYTECODE_PROVIDER_NAME_NONE;

	@Substitute
	public static BytecodeProvider buildBytecodeProvider(String providerName) {
		return new org.hibernate.bytecode.internal.none.BytecodeProviderImpl();
	}
}
