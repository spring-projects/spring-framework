/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Example class used to test {@link AnnotationsScanner} with enclosing classes.
 *
 * @author Phillip Webb
 * @since 5.2
 */
@AnnotationEnclosingClassSample.EnclosedOne
public class AnnotationEnclosingClassSample {

	@EnclosedTwo
	public static class EnclosedStatic {

		@EnclosedThree
		public static class EnclosedStaticStatic {
		}

	}

	@EnclosedTwo
	public class EnclosedInner {

		@EnclosedThree
		public class EnclosedInnerInner {
		}

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface EnclosedOne {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface EnclosedTwo {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface EnclosedThree {
	}

}
