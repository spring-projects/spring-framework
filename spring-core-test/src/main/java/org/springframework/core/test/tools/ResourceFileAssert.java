/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.test.tools;

/**
 * Assertion methods for {@code ResourceFile} instances.
 *
 * @author Phillip Webb
 * @since 6.0
 */
public class ResourceFileAssert extends DynamicFileAssert<ResourceFileAssert, ResourceFile> {

	ResourceFileAssert(ResourceFile actual) {
		super(actual, ResourceFileAssert.class);
	}

}
