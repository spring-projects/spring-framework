/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.json;

import org.jspecify.annotations.Nullable;

/**
 * Default {@link AbstractJsonContentAssert} implementation.
 *
 * @author Stephane Nicoll
 * @since 6.2
 */
public class JsonContentAssert extends AbstractJsonContentAssert<JsonContentAssert> {

	/**
	 * Create an assert for the given JSON document.
	 * @param json the JSON document to assert
	 */
	public JsonContentAssert(@Nullable JsonContent json) {
		super(json, JsonContentAssert.class);
	}

}
