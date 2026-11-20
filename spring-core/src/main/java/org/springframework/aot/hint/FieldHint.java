/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.hint;

import java.lang.reflect.Field;

/**
 * A hint that describes the need for reflection on a {@link Field}.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public final class FieldHint extends MemberHint {

	FieldHint(String name) {
		super(name);
	}

}
