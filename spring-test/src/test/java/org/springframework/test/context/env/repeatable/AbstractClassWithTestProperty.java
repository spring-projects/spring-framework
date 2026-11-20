/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.env.repeatable;

import org.springframework.test.context.TestPropertySource;

/**
 * Abstract base class which declares an inlined property via
 * {@link TestPropertySource @TestPropertySource}.
 *
 * @author Anatoliy Korovin
 * @author Sam Brannen
 * @since 5.2
 */
@TestPropertySource(properties = "key1 = parent")
abstract class AbstractClassWithTestProperty extends AbstractRepeatableTestPropertySourceTests {
}
