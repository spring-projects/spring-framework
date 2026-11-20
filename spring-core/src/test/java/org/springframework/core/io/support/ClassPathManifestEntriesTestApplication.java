/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.io.support;

import java.io.IOException;
import java.util.List;

/**
 * Class packaged into a temporary jar to test
 * {@link PathMatchingResourcePatternResolver} detection of classpath manifest
 * entries.
 *
 * @author Phillip Webb
 */
public class ClassPathManifestEntriesTestApplication {

	public static void main(String[] args) throws IOException {
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		System.out.println("!!!!" + List.of(resolver.getResources("classpath*:/**/*.txt")));
	}

}
