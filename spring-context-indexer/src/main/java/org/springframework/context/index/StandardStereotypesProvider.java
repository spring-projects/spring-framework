/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.index;

import java.util.LinkedHashSet;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

/**
 * A {@link StereotypesProvider} that extract a stereotype for each
 * {@code javax.*} annotation placed on a class or interface.
 *
 * @author Stephane Nicoll
 */
class StandardStereotypesProvider implements StereotypesProvider {

	private final TypeUtils typeUtils;

	StandardStereotypesProvider(TypeUtils typeUtils) {
		this.typeUtils = typeUtils;
	}

	@Override
	public Set<String> getStereotypes(Element element) {
		Set<String> stereotypes = new LinkedHashSet<>();
		ElementKind kind = element.getKind();
		if (kind != ElementKind.CLASS && kind != ElementKind.INTERFACE) {
			return stereotypes;
		}
		for (AnnotationMirror annotation : this.typeUtils.getAllAnnotationMirrors(element)) {
			String type = this.typeUtils.getType(annotation);
			if (type.startsWith("javax.")) {
				stereotypes.add(type);
			}
		}
		return stereotypes;
	}

}
