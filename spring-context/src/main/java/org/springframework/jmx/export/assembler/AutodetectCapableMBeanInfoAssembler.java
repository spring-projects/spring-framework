/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export.assembler;

/**
 * Extends the {@code MBeanInfoAssembler} to add auto-detection logic.
 * Implementations of this interface are given the opportunity by the
 * {@code MBeanExporter} to include additional beans in the registration process.
 *
 * <p>The exact mechanism for deciding which beans to include is left to
 * implementing classes.
 *
 * @author Rob Harrop
 * @since 1.2
 * @see org.springframework.jmx.export.MBeanExporter
 */
public interface AutodetectCapableMBeanInfoAssembler extends MBeanInfoAssembler {

	/**
	 * Indicate whether a particular bean should be included in the registration
	 * process, if it is not specified in the {@code beans} map of the
	 * {@code MBeanExporter}.
	 * @param beanClass the class of the bean (might be a proxy class)
	 * @param beanName the name of the bean in the bean factory
	 */
	boolean includeBean(Class<?> beanClass, String beanName);

}
