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

package org.springframework.context.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Locale;
import java.util.Properties;

import org.junit.Test;

/**
 * Test cases for {@link PropertiesMessageSource} class.
 * 
 * @author <a href="mailto:dmitry.katsubo@gmail.com">Dmitry Katsubo</a>
 */
public class PropertiesMessageSourceTests {

	@Test
	public void testResolveCode() {
		Properties properties = new Properties();
		properties.put("mykey1", "myvalue");
		properties.put("mykey2", "mypattern{0}");

		PropertiesMessageSource messageSource = new PropertiesMessageSource();
		messageSource.setProperties(properties);

		assertEquals("myvalue",
				messageSource.resolveCode("mykey1", Locale.getDefault()).format(new Object[0]));
		assertEquals(
				"mypattern94",
				messageSource.resolveCode("mykey2", Locale.ENGLISH).format(
						new Object[] { Integer.valueOf(94) }));
		assertNull("mypattern94", messageSource.resolveCode("mykey3", Locale.getDefault()));
	}
}
