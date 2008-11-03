/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.beans;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author Juergen Hoeller
 */
public class BeanWrapperEnumTests extends TestCase {

	public void testCustomEnum() {
		GenericBean gb = new GenericBean();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnum", "VALUE_1");
		Assert.assertEquals(CustomEnum.VALUE_1, gb.getCustomEnum());
	}

	public void testCustomEnumWithNull() {
		GenericBean gb = new GenericBean();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnum", null);
		Assert.assertEquals(null, gb.getCustomEnum());
	}

	public void testCustomEnumWithEmptyString() {
		GenericBean gb = new GenericBean();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnum", "");
		Assert.assertEquals(null, gb.getCustomEnum());
	}

}
