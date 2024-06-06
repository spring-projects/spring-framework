package org.springframework.context.annotation;

public class MyFeatureBean {

	private String featureName;
	private int featureValue;

	public MyFeatureBean() {
		// Initialize your bean with default values
		this.featureName = "DefaultFeature";
		this.featureValue = 42;  // Arbitrary default value
	}

	public MyFeatureBean(String featureName, int featureValue) {
		// Initialize your bean with provided values
		this.featureName = featureName;
		this.featureValue = featureValue;
	}

	public String getFeatureName() {
		return featureName;
	}

	public void setFeatureName(String featureName) {
		this.featureName = featureName;
	}

	public int getFeatureValue() {
		return featureValue;
	}

	public void setFeatureValue(int featureValue) {
		this.featureValue = featureValue;
	}

	public void executeFeature() {
		System.out.println("Feature executed: " + featureName + " with value " + featureValue);
	}

	@Override
	public String toString() {
		return "MyFeatureBean{" +
				"featureName='" + featureName + '\'' +
				", featureValue=" + featureValue +
				'}';
	}
}
