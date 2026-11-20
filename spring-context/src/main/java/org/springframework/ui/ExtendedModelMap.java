/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.ui;

import java.util.Collection;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Subclass of {@link ModelMap} that implements the {@link Model} interface.
 *
 * <p>This is an implementation class exposed to handler methods by Spring MVC, typically via
 * a declaration of the {@link org.springframework.ui.Model} interface. There is no need to
 * build it within user code; a plain {@link org.springframework.ui.ModelMap} or even a just
 * a regular {@link Map} with String keys will be good enough to return a user model.
 *
 * @author Juergen Hoeller
 * @since 2.5.1
 */
@SuppressWarnings("serial")
public class ExtendedModelMap extends ModelMap implements Model {

	@Override
	public ExtendedModelMap addAttribute(String attributeName, @Nullable Object attributeValue) {
		super.addAttribute(attributeName, attributeValue);
		return this;
	}

	@Override
	public ExtendedModelMap addAttribute(Object attributeValue) {
		super.addAttribute(attributeValue);
		return this;
	}

	@Override
	public ExtendedModelMap addAllAttributes(@Nullable Collection<?> attributeValues) {
		super.addAllAttributes(attributeValues);
		return this;
	}

	@Override
	public ExtendedModelMap addAllAttributes(@Nullable Map<String, ?> attributes) {
		super.addAllAttributes(attributes);
		return this;
	}

	@Override
	public ExtendedModelMap mergeAttributes(@Nullable Map<String, ?> attributes) {
		super.mergeAttributes(attributes);
		return this;
	}

	@Override
	public Map<String, Object> asMap() {
		return this;
	}

}
