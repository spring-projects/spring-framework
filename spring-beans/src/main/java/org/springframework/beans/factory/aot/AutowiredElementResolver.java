/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.aot;

import java.util.Set;

import javax.lang.model.element.Element;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.core.log.LogMessage;

/**
 * Base class for resolvers that support autowiring related to an
 * {@link Element}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 */
abstract class AutowiredElementResolver {

	private final Log logger = LogFactory.getLog(getClass());

	protected final void registerDependentBeans(ConfigurableBeanFactory beanFactory,
			String beanName, Set<String> autowiredBeanNames) {

		for (String autowiredBeanName : autowiredBeanNames) {
			if (beanFactory.containsBean(autowiredBeanName)) {
				beanFactory.registerDependentBean(autowiredBeanName, beanName);
			}
			logger.trace(LogMessage.format(
					"Autowiring by type from bean name %s' to bean named '%s'", beanName,
					autowiredBeanName));
		}
	}


	/**
	 * {@link DependencyDescriptor} that supports shortcut bean resolution.
	 */
	@SuppressWarnings("serial")
	static class ShortcutDependencyDescriptor extends DependencyDescriptor {

		private final String shortcut;

		public ShortcutDependencyDescriptor(DependencyDescriptor original, String shortcut) {
			super(original);
			this.shortcut = shortcut;
		}

		@Override
		public Object resolveShortcut(BeanFactory beanFactory) {
			return beanFactory.getBean(this.shortcut, getDependencyType());
		}
	}

}
