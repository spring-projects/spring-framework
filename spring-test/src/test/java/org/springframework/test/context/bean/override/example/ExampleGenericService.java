/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.example;

/**
 * Example generic service interface for tests.
 *
 * @param <T> the generic type
 * @author Phillip Webb
 * @since 6.2
 */
public interface ExampleGenericService<T> {

	T greeting();

}
