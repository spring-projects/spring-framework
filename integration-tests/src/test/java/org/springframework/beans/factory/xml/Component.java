/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.xml;

import java.util.ArrayList;
import java.util.List;

public class Component {

	private String name;
	private List<Component> components = new ArrayList<>();

	// mmm, there is no setter method for the 'components'
	public void addComponent(Component component) {
		this.components.add(component);
	}

	public List<Component> getComponents() {
		return components;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
