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

package org.springframework.context.annotation.spr16756;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author Juergen Hoeller
 */
class Spr16756Tests {

	@Test
	void shouldNotFailOnNestedScopedComponent() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(ScanningConfiguration.class);
		context.refresh();
		context.getBean(ScannedComponent.class);
		context.getBean(ScannedComponent.State.class);
		context.close();
	}

}
