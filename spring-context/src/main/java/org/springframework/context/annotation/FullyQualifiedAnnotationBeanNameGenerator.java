/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.util.Assert;

/**
 * An extension of {@code AnnotationBeanNameGenerator} that uses the fully qualified
 * class name as the default bean name if an explicit bean name is not supplied via
 * a supported type-level annotation such as {@code @Component} (see
 * {@link AnnotationBeanNameGenerator} for details on supported annotations).
 *
 * <p>Favor this bean naming strategy over {@code AnnotationBeanNameGenerator} if
 * you run into naming conflicts due to multiple autodetected components having the
 * same non-qualified class name (i.e., classes with identical names but residing in
 * different packages). If you need such conflict avoidance for {@link Bean @Bean}
 * methods as well, consider {@link FullyQualifiedConfigurationBeanNameGenerator}.
 *
 * <p>Note that an instance of this class is used by default for configuration-level
 * import purposes; whereas, the default for component scanning purposes is a plain
 * {@code AnnotationBeanNameGenerator}.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 5.2.3
 * @see org.springframework.beans.factory.support.DefaultBeanNameGenerator
 * @see AnnotationBeanNameGenerator
 * @see FullyQualifiedConfigurationBeanNameGenerator
 * @see ConfigurationClassPostProcessor#IMPORT_BEAN_NAME_GENERATOR
 */
public class FullyQualifiedAnnotationBeanNameGenerator extends AnnotationBeanNameGenerator {

	/**
	 * A convenient constant for a default {@code FullyQualifiedAnnotationBeanNameGenerator}
	 * instance, as used for configuration-level import purposes.
	 * @since 5.2.11
	 */
	public static final FullyQualifiedAnnotationBeanNameGenerator INSTANCE =
			new FullyQualifiedAnnotationBeanNameGenerator();


	@Override
	protected String buildDefaultBeanName(BeanDefinition definition) {
		String beanClassName = definition.getBeanClassName();
		Assert.state(beanClassName != null, "No bean class name set");
		return beanClassName;
	}

}
