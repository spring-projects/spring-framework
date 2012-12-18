/*
 * Copyright 2002-2012 the original author or authors.
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.Colour;
import org.springframework.beans.Employee;
import org.springframework.beans.Pet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@FirstAnnotation
@SecondAnnotation
public class MultiAnnotationTest  {

	@Autowired
	private Pet dog;

	@Autowired
	private String str;
	
	@Autowired
	private Employee employee; 

	@Autowired
	private Colour colour;
	
	@Test
	public void verifyExtendedAnnotationAutowiredFields() {

		assertNotNull("The string field should have been autowired.", this.str);
		assertEquals("string", this.str);
		
		assertNotNull("The dog field should have been autowired.", this.dog);
		assertEquals("Fido", this.dog.getName());


		assertNotNull("The colour field should have been autowired.", this.colour);
		assertEquals("BLUE", this.colour.getLabel());
		
		assertNotNull("The employee field should have been autowired.", this.employee);
		assertEquals("Giovanni", this.employee.getName());

		assertNotNull("The colour field should have been autowired in employee.", this.employee.getFavouriteColour());
		assertEquals(this.colour.getLabel(), this.employee.getFavouriteColour().getLabel());


	}

}
