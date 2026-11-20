/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.conversionservice;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;

/**
 * @author Keith Donald
 * @author Juergen Hoeller
 */
public class TestClient {

	private List<Bar> bars;

	private boolean bool;

	private List<String> stringList;

	private Resource[] resourceArray;

	private List<Resource> resourceList;

	private Map<String, Resource> resourceMap;


	public List<Bar> getBars() {
		return bars;
	}

	@Autowired
	public void setBars(List<Bar> bars) {
		this.bars = bars;
	}

	public boolean isBool() {
		return bool;
	}

	public void setBool(boolean bool) {
		this.bool = bool;
	}

	public List<String> getStringList() {
		return stringList;
	}

	public void setStringList(List<String> stringList) {
		this.stringList = stringList;
	}

	public Resource[] getResourceArray() {
		return resourceArray;
	}

	public void setResourceArray(Resource[] resourceArray) {
		this.resourceArray = resourceArray;
	}

	public List<Resource> getResourceList() {
		return resourceList;
	}

	public void setResourceList(List<Resource> resourceList) {
		this.resourceList = resourceList;
	}

	public Map<String, Resource> getResourceMap() {
		return resourceMap;
	}

	public void setResourceMap(Map<String, Resource> resourceMap) {
		this.resourceMap = resourceMap;
	}

}
