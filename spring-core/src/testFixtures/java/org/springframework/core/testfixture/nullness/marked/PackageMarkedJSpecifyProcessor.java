/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.core.testfixture.nullness.marked;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface PackageMarkedJSpecifyProcessor {

	String process(String unspecified, @Nullable String nullable, @NonNull String nonNull);

	@Nullable String nullableProcess();

	@NonNull String nonNullProcess();
}
