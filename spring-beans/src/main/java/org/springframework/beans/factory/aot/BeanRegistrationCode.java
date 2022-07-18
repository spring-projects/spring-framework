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

import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.MethodReference;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.javapoet.ClassName;
import org.springframework.util.function.ThrowingBiFunction;

/**
 * Interface that can be used to configure the code that will be generated to
 * perform registration of a single bean.
 *
 * @author Phillip Webb
 * @since 6.0
 * @see BeanRegistrationCodeFragments
 */
public interface BeanRegistrationCode {

	/**
	 * Return the name of the class being used for registrations.
	 * @return the name of the class
	 */
	ClassName getClassName();

	/**
	 * Return a {@link GeneratedMethods} being used by the registrations code.
	 * @return the generated methods
	 */
	GeneratedMethods getMethods();

	/**
	 * Add an instance post processor method call to the registration code.
	 * @param methodReference a reference to the post-process method to call.
	 * The referenced method must have a functional signature compatible with
	 * {@link InstanceSupplier#andThen}.
	 * @see InstanceSupplier#andThen(ThrowingBiFunction)
	 */
	void addInstancePostProcessor(MethodReference methodReference);

}
