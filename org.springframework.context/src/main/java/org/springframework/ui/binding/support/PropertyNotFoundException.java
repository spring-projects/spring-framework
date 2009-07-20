package org.springframework.ui.binding.support;

public class PropertyNotFoundException extends RuntimeException {

	private String property;

	private Class<?> modelClass;

	public PropertyNotFoundException(String property, Class<?> modelClass) {
		super("No property '" + property + "' found on model [" + modelClass.getName() + "]");
		this.property = property;
		this.modelClass = modelClass;
	}

	public String getProperty() {
		return property;
	}

	public Class<?> getModelClass() {
		return modelClass;
	}

}
