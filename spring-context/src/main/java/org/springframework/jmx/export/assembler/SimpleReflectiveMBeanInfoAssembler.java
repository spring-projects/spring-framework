/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export.assembler;

import java.lang.reflect.Method;

/**
 * Simple subclass of {@code AbstractReflectiveMBeanInfoAssembler}
 * that always votes yes for method and property inclusion, effectively exposing
 * all public methods and properties as operations and attributes.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 */
public class SimpleReflectiveMBeanInfoAssembler extends AbstractConfigurableMBeanInfoAssembler {

	/**
	 * Always returns {@code true}.
	 */
	@Override
	protected boolean includeReadAttribute(Method method, String beanKey) {
		return true;
	}

	/**
	 * Always returns {@code true}.
	 */
	@Override
	protected boolean includeWriteAttribute(Method method, String beanKey) {
		return true;
	}

	/**
	 * Always returns {@code true}.
	 */
	@Override
	protected boolean includeOperation(Method method, String beanKey) {
		return true;
	}

}
