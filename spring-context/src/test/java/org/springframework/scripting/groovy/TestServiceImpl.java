/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scripting.groovy;

@Log
public class TestServiceImpl implements TestService{

	@Override
	public String sayHello() {
		throw new TestException("TestServiceImpl");
	}
}
