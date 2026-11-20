/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export.annotation;

import org.springframework.beans.factory.FactoryBean;

/**
 * @author Juergen Hoeller
 */
public class AnnotationTestBeanFactory implements FactoryBean<FactoryCreatedAnnotationTestBean> {

	private final FactoryCreatedAnnotationTestBean instance = new FactoryCreatedAnnotationTestBean();

	public AnnotationTestBeanFactory() {
		this.instance.setName("FACTORY");
	}

	@Override
	public FactoryCreatedAnnotationTestBean getObject() {
		return this.instance;
	}

	@Override
	public Class<? extends AnnotationTestBean> getObjectType() {
		return FactoryCreatedAnnotationTestBean.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
