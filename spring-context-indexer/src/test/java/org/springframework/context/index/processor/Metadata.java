/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
			return (itemMetadata != null && itemMetadata.getStereotypes().size() == stereotypes.size() &&
					itemMetadata.getStereotypes().containsAll(stereotypes));
		}, "Candidates with type %s and stereotypes %s", type, stereotypes);
	}

}
