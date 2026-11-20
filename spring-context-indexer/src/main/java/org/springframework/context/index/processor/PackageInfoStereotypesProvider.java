/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.index.processor;

import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

/**
 * A {@link StereotypesProvider} implementation that provides the
 * {@value #STEREOTYPE} stereotype for each package-info.
 *
 * @author Stephane Nicoll
 * @since 5.0
 */
class PackageInfoStereotypesProvider implements StereotypesProvider {

	public static final String STEREOTYPE = "package-info";


	@Override
	public Set<String> getStereotypes(Element element) {
		Set<String> stereotypes = new HashSet<>();
		if (element.getKind() == ElementKind.PACKAGE) {
			stereotypes.add(STEREOTYPE);
		}
		return stereotypes;
	}

}
