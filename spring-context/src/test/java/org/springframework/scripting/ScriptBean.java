/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scripting;

/**
 * Simple interface used in testing the scripted beans support.
 *
 * @author Rick Evans
 */
public interface ScriptBean {

	String getName();

	void setName(String name);

	int getAge();

	void setAge(int age);

}
