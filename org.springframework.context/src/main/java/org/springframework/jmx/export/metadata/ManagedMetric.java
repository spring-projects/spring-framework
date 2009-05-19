package org.springframework.jmx.export.metadata;

import org.springframework.jmx.support.MetricType;

/**
 * Metadata that indicates to expose a given bean property as a JMX attribute,
 * with additional descriptor properties that indicate that the attribute is a
 * metric. Only valid when used on a JavaBean getter.
 * 
 * @author Jennifer Hickey
 * @since 3.0
 * @see org.springframework.jmx.export.assembler.MetadataMBeanInfoAssembler
 */
public class ManagedMetric extends AbstractJmxAttribute {

	private String category = "";

	private String displayName = "";

	private MetricType metricType = MetricType.GAUGE;

	private int persistPeriod = -1;

	private String persistPolicy = "";

	private String unit = "";

	/**
	 * 
	 *@return The category of this metric (ex. throughput, performance, utilization)
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * 
	 * @return A display name for this metric
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * 
	 * @return A description of how this metric's values change over time
	 */
	public MetricType getMetricType() {
		return metricType;
	}

	/**
	 * 
	 * @return The persist period for this metric
	 */
	public int getPersistPeriod() {
		return persistPeriod;
	}

	/**
	 * 
	 * @return The persist policy for this metric
	 */
	public String getPersistPolicy() {
		return persistPolicy;
	}

	/**
	 * 
	 * @return The expected unit of measurement values
	 */
	public String getUnit() {
		return unit;
	}

	/**
	 * 
	 * @param category The category of this metric (ex. throughput, performance, utilization)
	 */
	public void setCategory(String category) {
		this.category = category;
	}

	/**
	 * 
	 * @param displayName A display name for this metric
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * 
	 * @param metricType A description of how this metric's values change over time
	 */
	public void setMetricType(MetricType metricType) {
		this.metricType = metricType;
	}

	/**
	 * 
	 * @param persistPeriod The persist period for this metric
	 */
	public void setPersistPeriod(int persistPeriod) {
		this.persistPeriod = persistPeriod;
	}

	/**
	 * 
	 * @param persistPolicy The persist policy for this metric
	 */
	public void setPersistPolicy(String persistPolicy) {
		this.persistPolicy = persistPolicy;
	}

	/**
	 * 
	 * @param unit The expected unit of measurement values
	 */
	public void setUnit(String unit) {
		this.unit = unit;
	}

}
