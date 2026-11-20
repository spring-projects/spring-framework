/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.aspectj;

import java.io.Serializable;

import org.springframework.beans.factory.annotation.Configurable;

@Configurable("configuredBean")
@SuppressWarnings("serial")
public class ShouldBeConfiguredBySpring implements Serializable {

	private String name;

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

}
