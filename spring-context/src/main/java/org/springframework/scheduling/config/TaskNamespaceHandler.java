/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * {@code NamespaceHandler} for the 'task' namespace.
 *
 * @author Mark Fisher
 * @since 3.0
 */
public class TaskNamespaceHandler extends NamespaceHandlerSupport {

	@Override
	public void init() {
		this.registerBeanDefinitionParser("annotation-driven", new AnnotationDrivenBeanDefinitionParser());
		this.registerBeanDefinitionParser("executor", new ExecutorBeanDefinitionParser());
		this.registerBeanDefinitionParser("scheduled-tasks", new ScheduledTasksBeanDefinitionParser());
		this.registerBeanDefinitionParser("scheduler", new SchedulerBeanDefinitionParser());
	}

}
