/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.xml;

import java.util.List;

import org.springframework.beans.factory.FactoryBean;

public class ComponentFactoryBean implements FactoryBean<Component> {

	private Component parent;
	private List<Component> children;

	public void setParent(Component parent) {
		this.parent = parent;
	}

	public void setChildren(List<Component> children) {
		this.children = children;
	}

	@Override
	public Component getObject() {
		if (this.children != null && this.children.size() > 0) {
			for (Component child : children) {
				this.parent.addComponent(child);
			}
		}
		return this.parent;
	}

	@Override
	public Class<Component> getObjectType() {
		return Component.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
