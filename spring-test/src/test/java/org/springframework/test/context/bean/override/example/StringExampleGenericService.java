/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.example;

/**
 * {@link ExampleGenericService} implementation for tests.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 6.2
 */
public record StringExampleGenericService(String greeting) implements ExampleGenericService<String> {
}
