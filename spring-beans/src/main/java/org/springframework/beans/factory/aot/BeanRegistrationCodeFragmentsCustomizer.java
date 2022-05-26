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

package org.springframework.beans.factory.aot;

import org.springframework.beans.factory.support.RegisteredBean;

/**
 * Strategy factory interface that can be used to customize the
 * {@link BeanRegistrationCodeFragments} that us used for a given
 * {@link RegisteredBean}. This interface can be used if default code generation
 * isn't suitable for specific types of {@link RegisteredBean}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
@FunctionalInterface
public interface BeanRegistrationCodeFragmentsCustomizer {

	/**
	 * Apply this {@link BeanRegistrationCodeFragmentsCustomizer} to the given
	 * {@link BeanRegistrationCodeFragments code fragments generator}. The
	 * returned code generator my be a
	 * {@link BeanRegistrationCodeFragmentsWrapper wrapper} around the original.
	 * @param registeredBean the registered bean
	 * @param codeFragments the existing code fragments
	 * @return the code generator to use, either the original or a wrapped one;
	 */
	BeanRegistrationCodeFragments customizeBeanRegistrationCodeFragments(
			RegisteredBean registeredBean, BeanRegistrationCodeFragments codeFragments);

}
