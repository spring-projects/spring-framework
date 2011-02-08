/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.context.annotation;

import org.springframework.context.config.FeatureSpecification;
import org.springframework.context.config.FeatureSpecificationExecutor;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Interface for parsing {@link AnnotationMetadata} from a {@link FeatureAnnotation}
 * into a {@link FeatureSpecification} object. Used in conjunction with a
 * {@link FeatureSpecificationExecutor} to provide a source-agnostic approach to
 * handling configuration metadata.
 *
 * <p>For example, Spring's component-scanning can be configured via XML using
 * the {@code context:component-scan} element or via the {@link ComponentScan}
 * annotation. In either case, the metadata is the same -- only the source
 * format differs.  {@link ComponentScanBeanDefinitionParser} is used to create
 * a specification from the {@code <context:component-scan>} XML element, while
 * {@link ComponentScanAnnotationParser} creates a specification from the
 * the annotation style. They both produce a {@link ComponentScanSpec}
 * object that is ultimately delegated to a {@link ComponentScanExecutor}
 * which understands how to configure a {@link ClassPathBeanDefinitionScanner},
 * perform actual scanning, and register actual bean definitions against the
 * container.
 *
 * <p>Implementations must be instantiable via a no-arg constructor.
 *
 * TODO SPR-7420: documentation (clean up)
 * TODO SPR-7420: rework so annotations declare their creator.
 *
 *
 * @author Chris Beams
 * @since 3.1
 * @see FeatureAnnotation#parser()
 * @see FeatureSpecification
 * @see FeatureSpecificationExecutor
 */
public interface FeatureAnnotationParser {

	/**
	 * Parse the given annotation metadata and populate a {@link FeatureSpecification}
	 * object suitable for execution by a {@link FeatureSpecificationExecutor}.
	 */
	FeatureSpecification process(AnnotationMetadata metadata);

}
