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

package org.springframework.test.context.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Integration tests which verify the behavior of a class whose configurations is
 * divided in:
 * <ul>
 * <li>a @{@link ContextConfiguration} as direct annotation</li>
 * <li>a @{@link FirstAnnotation} annotation (which declares a @{@link ContextConfiguration})</li>
 * <li>a @{@link SecondAnnotation} annotation (which declares a @{@link ContextConfiguration})</li>
 * <li>a @{@link ThirdAnnotation} annotation (which is annotated in the @{@link SecondAnnotation} and declares a @{@link ContextConfiguration})</li>
 * </ul>
 * The ApplicationContext is divided in that files, and the declared beans depends on each others.
 * 
 * @author Giovanni Dall'Oglio Risso
 * @since 3.2.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@FirstAnnotation
@SecondAnnotation
public class MultiAnnotationTest  {


	@Autowired
	private ApplicationContext applicationContext;

	@Test
	public void verifyExtendedAnnotationAutowiredFields() {

		assertNotNull("The applicationContext should have been autowired.", applicationContext);
		
		Map<String, Person> personBeans = applicationContext.getBeansOfType(Person.class);

		assertNotNull("The applicationContext should have beans of type Person.", personBeans);
		assertEquals("The applicationContext should have 4 beans of type Person.", 4, personBeans.size());

		Person person1 = personBeans.get("p1");
		Person person2 = personBeans.get("p2");
		Person person3 = personBeans.get("p3");
		Person personM = personBeans.get("pM");

		assertNotNull(person1);
		assertNotNull(person2);
		assertNotNull(person3);
		assertNotNull(personM);

		assertEquals("Person 1", person1.getName());
		assertEquals("Person 2", person2.getName());
		assertEquals("Person 3", person3.getName());
		assertEquals("Person Multi", personM.getName());
	}

}
