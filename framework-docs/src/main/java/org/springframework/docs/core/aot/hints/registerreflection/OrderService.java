/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.core.aot.hints.registerreflection;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.stereotype.Component;

// tag::snippet[]
@Component
class OrderService {

	@RegisterReflectionForBinding(Order.class)
	public void process(Order order) {
		// ...
	}

}
// end::snippet[]

record Order() {}
