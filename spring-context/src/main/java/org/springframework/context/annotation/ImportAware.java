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

import org.springframework.beans.factory.Aware;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Interface to be implemented by any @{@link Configuration} class that wishes
 * to be injected with the {@link AnnotationMetadata} of the @{@code Configuration}
 * class that imported it. Useful in conjunction with annotations that
 * use @{@link Import} as a meta-annotation.
 *
 * @author Chris Beams
 * @since 3.1
 */
public interface ImportAware extends Aware {

	/**
	 * Set the annotation metadata of the importing @{@code Configuration} class.
	 */
	void setImportMetadata(AnnotationMetadata importMetadata);

}
