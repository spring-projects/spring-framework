/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.xml;

import org.springframework.beans.factory.BeanNameAware;

/**
 * @author Rob Harrop
 */
public class GeneratedNameBean implements BeanNameAware {

	private String beanName;

	private GeneratedNameBean child;

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public String getBeanName() {
		return beanName;
	}

	public void setChild(GeneratedNameBean child) {
		this.child = child;
	}

	public GeneratedNameBean getChild() {
		return child;
	}

}
