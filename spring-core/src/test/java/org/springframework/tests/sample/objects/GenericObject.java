/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.tests.sample.objects;

import java.util.List;

import org.springframework.core.io.Resource;

public class GenericObject<T> {

	private List<Resource> resourceList;

	public List<Resource> getResourceList() {
		return this.resourceList;
	}

	public void setResourceList(List<Resource> resourceList) {
		this.resourceList = resourceList;
	}

}
