/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scripting.groovy;

import org.codehaus.groovy.control.customizers.ImportCustomizer;

/**
 * @author Juergen Hoeller
 */
public class MyImportCustomizer extends ImportCustomizer {

	public MyImportCustomizer() {
		addStarImports("org.springframework.scripting.groovy");
	}

}
