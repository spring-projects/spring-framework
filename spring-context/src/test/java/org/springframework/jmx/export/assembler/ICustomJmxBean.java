/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export.assembler;

/**
 * @author Rob Harrop
 */
public interface ICustomJmxBean extends ICustomBase {

	String getName();

	void setName(String name);

	int getAge();

}
