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

package org.springframework.context.index.processor;

import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Condition;

/**
 * AssertJ {@link Condition} to help test {@link CandidateComponentsMetadata}.
 *
 * @author Stephane Nicoll
 */
class Metadata {

	public static Condition<CandidateComponentsMetadata> of(Class<?> type, Class<?>... stereotypes) {
		return of(type.getName(), Arrays.stream(stereotypes).map(Class::getName).toList());
	}

	public static Condition<CandidateComponentsMetadata> of(String type, String... stereotypes) {
		return of(type, Arrays.asList(stereotypes));
	}

	public static Condition<CandidateComponentsMetadata> of(String type,
			List<String> stereotypes) {
		return new Condition<>(metadata -> {
			ItemMetadata itemMetadata = metadata.getItems().stream()
					.filter(item -> item.getType().equals(type))
					.findFirst().orElse(null);
			return itemMetadata != null && itemMetadata.getStereotypes().size() == stereotypes.size()
					&& itemMetadata.getStereotypes().containsAll(stereotypes);
		}, "Candidates with type %s and stereotypes %s", type, stereotypes);
	}

}
