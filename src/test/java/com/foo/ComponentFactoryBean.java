/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

	@Override
	public Component getObject() throws Exception {
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
