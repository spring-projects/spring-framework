/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.access;

import java.util.List;

/**
 * Scrap bean for use in tests.
 *
 * @author Colin Sampaleanu
 */
public class TestBean {

	private String name;

	private List<?> list;

	private Object objRef;

	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return Returns the list.
	 */
	public List<?> getList() {
		return list;
	}

	/**
	 * @param list The list to set.
	 */
	public void setList(List<?> list) {
		this.list = list;
	}

	/**
	 * @return Returns the object.
	 */
	public Object getObjRef() {
		return objRef;
	}

	/**
	 * @param object The object to set.
	 */
	public void setObjRef(Object object) {
		this.objRef = object;
	}

}
