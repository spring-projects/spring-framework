/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.context.annotation4;

import org.springframework.beans.TestBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.FactoryMethod;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.ScopedProxy;
import org.springframework.context.annotation.BeanAge;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Class used to test the functionality of @FactoryMethod bean definitions declared inside
 * a Spring @Component class.
 *  
 * @author Mark Pollack
 */
@Component
public class FactoryMethodComponent {

	private static TestBean staticTestBean = new TestBean("staticInstance",1);
	
	@Autowired @Qualifier("public")
	public TestBean autowiredTestBean;
	
	private static int i;

	@FactoryMethod @Qualifier("static")
	public static TestBean staticInstance()
	{
		return staticTestBean;
	}
	
	public static TestBean nullInstance() 
	{
		return null;
	}
	
	@FactoryMethod @Qualifier("public")
	public TestBean getPublicInstance() {
		return new TestBean("publicInstance");
	}

	@FactoryMethod @BeanAge(1)
	protected TestBean getProtectedInstance() {
		return new TestBean("protectedInstance", 1);
	}
	
	@FactoryMethod @Scope("prototype")
	private TestBean getPrivateInstance() {
		return new TestBean("privateInstance", i++);
	}
	
	@FactoryMethod @Scope("request") @ScopedProxy
	public TestBean requestScopedInstance()
	{
		TestBean testBean = new TestBean("requestScopedInstance", 3); 
		return testBean;
	}
	
	//TODO method for test that fails if use @ScopedProxy with singleton scope.
	
}
