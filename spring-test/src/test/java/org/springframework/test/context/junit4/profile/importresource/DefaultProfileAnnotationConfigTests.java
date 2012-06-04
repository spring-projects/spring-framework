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

package org.springframework.test.context.junit4.profile.importresource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.Employee;
import org.springframework.beans.Pet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Juergen Hoeller
 * @since 3.1
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DefaultProfileConfig.class)
public class DefaultProfileAnnotationConfigTests {

	@Autowired
	protected Pet pet;

	@Autowired(required = false)
	protected Employee employee;


	@Test
	public void pet() {
		assertNotNull(pet);
		assertEquals("Fido", pet.getName());
	}

	@Test
	public void employee() {
		assertNull("employee bean should not be created for the default profile", employee);
	}

}
