package com.foo;

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

	public Component getObject() throws Exception {
		if (this.children != null && this.children.size() > 0) {
			for (Component child : children) {
				this.parent.addComponent(child);
			}
		}
		return this.parent;
	}

	public Class<Component> getObjectType() {
		return Component.class;
	}

	public boolean isSingleton() {
		return true;
	}
}
