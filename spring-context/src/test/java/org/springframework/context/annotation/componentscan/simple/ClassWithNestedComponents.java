/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation.componentscan.simple;

import org.springframework.stereotype.Component;

public class ClassWithNestedComponents {

	@Component
	public static class NestedComponent extends ClassWithNestedComponents {
	}

	@Component
	public static class OtherNestedComponent extends ClassWithNestedComponents {
	}

}
