/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.support;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * @author Juergen Hoeller
 * @since 06.10.2004
 */
public class FactoryBeanAndApplicationListener implements FactoryBean<String>, ApplicationListener<ApplicationEvent> {

	@Override
	public String getObject() {
		return "";
	}

	@Override
	public Class<String> getObjectType() {
		return String.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
	}

}
