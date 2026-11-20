/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.index.processor;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

/**
 * A {@link StereotypesProvider} that extracts a stereotype for each
 * {@code jakarta.*} annotation <i>present</i> on a class or interface.
 *
 * @author Stephane Nicoll
 * @since 5.0
 */
class StandardStereotypesProvider implements StereotypesProvider {

	private final TypeHelper typeHelper;


	StandardStereotypesProvider(TypeHelper typeHelper) {
		this.typeHelper = typeHelper;
	}


	@Override
	public Set<String> getStereotypes(Element element) {
		Set<String> stereotypes = new LinkedHashSet<>();
		ElementKind kind = element.getKind();
		if (kind != ElementKind.CLASS && kind != ElementKind.INTERFACE) {
			return stereotypes;
		}
		for (AnnotationMirror annotation : this.typeHelper.getAllAnnotationMirrors(element)) {
			String type = this.typeHelper.getType(annotation);
			if (type.startsWith("jakarta.")) {
				stereotypes.add(type);
			}
		}
		return stereotypes;
	}

}
