/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.wiring;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Simple default implementation of the {@link BeanWiringInfoResolver} interface,
 * looking for a bean with the same name as the fully-qualified class name.
 * This matches the default name of the bean in a Spring XML file if the
 * bean tag's "id" attribute is not used.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 */
public class ClassNameBeanWiringInfoResolver implements BeanWiringInfoResolver {

	@Override
	public BeanWiringInfo resolveWiringInfo(Object beanInstance) {
		Assert.notNull(beanInstance, "Bean instance must not be null");
		return new BeanWiringInfo(ClassUtils.getUserClass(beanInstance).getName(), true);
	}

}
