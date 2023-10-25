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

import java.util.Map;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.property.access.spi.PropertyAccess;

/**
 * Hibernate 6.3+ substitution designed to leniently return {@code null}, as authorized by the API, to avoid throwing an
 * {@code HibernateException}.
 * TODO Ask Hibernate team to fix this as it looks like a bug
 *
 * @author Sebastien Deleuze
 * @since 6.1
 */
@TargetClass(className = "org.hibernate.bytecode.internal.none.BytecodeProviderImpl", onlyWith = SubstituteOnlyIfPresent.class)
final class Target_BytecodeProvider {

	@Substitute
	public ReflectionOptimizer getReflectionOptimizer(Class<?> clazz, Map<String, PropertyAccess> propertyAccessMap) {
		return null;
	}
}
