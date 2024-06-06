package org.springframework.context.annotation;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import java.util.Map;

public class OnPropertyCondition implements Condition {

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnProperty.class.getName());
		if (attributes == null) {
			return false;
		}
		String propertyName = (String) attributes.get("name");
		String expectedValue = (String) attributes.get("havingValue");

		if (propertyName == null || expectedValue == null) {
			return false;
		}

		String actualValue = context.getEnvironment().getProperty(propertyName);
		return expectedValue.equals(actualValue);
	}
}
