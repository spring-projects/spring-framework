/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation4;

import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.annotation.Bean;

/**
 * Class to test that @FactoryMethods are detected only when inside a class with an @Component
 * class annotation.
 *
 * @author Mark Pollack
 */
public class SimpleBean {

	// This should *not* recognized as a bean since it does not reside inside an @Component
	@Bean
	public TestBean getPublicInstance() {
		return new TestBean("publicInstance");
	}

}
