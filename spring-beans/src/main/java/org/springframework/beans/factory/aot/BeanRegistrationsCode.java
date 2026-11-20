/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.aot;

import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.javapoet.ClassName;

/**
 * Interface that can be used to configure the code that will be generated to
 * register beans.
 *
 * @author Phillip Webb
 * @since 6.0
 */
public interface BeanRegistrationsCode {

	/**
	 * Return the name of the class being used for registrations.
	 * @return the generated class name.
	 */
	ClassName getClassName();

	/**
	 * Return a {@link GeneratedMethods} being used by the registrations code.
	 * @return the method generator
	 */
	GeneratedMethods getMethods();

}
