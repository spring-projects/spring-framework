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

package org.springframework.aot.graalvm;

import com.oracle.svm.core.annotate.AutomaticFeature;
import com.oracle.svm.hosted.FeatureImpl.DuringSetupAccessImpl;
import org.graalvm.compiler.debug.DebugContext;
import org.graalvm.nativeimage.hosted.Feature;

/**
 * GraalVM {@link Feature} that substitutes field values that match a certain pattern
 * with constants without causing build-time initialization.
 *
 * @author Phillip Webb
 * @author Sebastien Deleuze
 * @since 6.0
 */
@AutomaticFeature
class ConstantFieldFeature implements Feature {

	@Override
	public void duringSetup(DuringSetupAccess access) {
		duringSetup((DuringSetupAccessImpl) access);
	}

	private void duringSetup(DuringSetupAccessImpl access) {
		DebugContext debug = access.getDebugContext();
		try (DebugContext.Scope scope = debug.scope("ConstantFieldFeature.duringSetup")) {
			debug.log("Installing constant field substitution processor : " + scope);
			ClassLoader classLoader = ConstantFieldFeature.class.getClassLoader();
			ConstantFieldSubstitutionProcessor substitutionProcessor =
					new ConstantFieldSubstitutionProcessor(debug, classLoader);
			access.registerSubstitutionProcessor(substitutionProcessor);
		}
	}

}
