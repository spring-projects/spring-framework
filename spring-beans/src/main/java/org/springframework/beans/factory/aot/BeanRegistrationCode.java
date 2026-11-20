/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
