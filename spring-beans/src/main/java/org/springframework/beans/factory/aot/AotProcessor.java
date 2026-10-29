/*
 * Copyright 2002-present the original author or authors.
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

/**
 * Common contract for bean factory AOT processors.
 *
 * @author Arnab Nandy
 * @since 7.1
 * @see BeanFactoryInitializationAotProcessor
 * @see BeanRegistrationAotProcessor
 */
public interface AotProcessor {


	/**
	 * Return whether the bean associated with this processor should be excluded
	 * from registration in the AOT-optimized application context.
	 * <p>By default, return {@code true} since the processor's runtime behavior
	 * is replaced by its AOT contributions. Override this method to return
	 * {@code false} if the bean also provides behavior required at runtime.
	 * This does not prevent an explicit exclusion by a
	 * {@link BeanRegistrationExcludeFilter} or the
	 * {@link BeanRegistrationAotProcessor#IGNORE_REGISTRATION_ATTRIBUTE}.
	 * <p>Excluding the bean's registration does not prevent this processor
	 * from making AOT contributions for the bean factory or other beans.
	 * @return whether the processor bean should be excluded from registration
	 * @since 7.1
	 */
	default boolean isBeanExcludedFromAotRegistration() {
		return true;
	}

}
