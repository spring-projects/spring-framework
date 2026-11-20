/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.target;

/**
 * Simple {@link org.springframework.aop.TargetSource} implementation,
 * freshly obtaining the specified target bean from its containing
 * Spring {@link org.springframework.beans.factory.BeanFactory}.
 *
 * <p>Can obtain any kind of target bean: singleton, scoped, or prototype.
 * Typically used for scoped beans.
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 */
@SuppressWarnings("serial")
public class SimpleBeanTargetSource extends AbstractBeanFactoryBasedTargetSource {

	@Override
	public Object getTarget() throws Exception {
		return getBeanFactory().getBean(getTargetBeanName());
	}

}
