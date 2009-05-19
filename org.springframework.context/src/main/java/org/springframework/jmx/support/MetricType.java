package org.springframework.jmx.support;


/**
 * Represents how the measurement values of a <code>ManagedMetric</code> will change over time
 * @author Jennifer Hickey
 * @since 3.0
 * 
 */
public enum MetricType {
	/**
	 * The measurement values may go up or down over time
	 */
	GAUGE,
	
	/**
	 * The measurement values will always increase
	 */
	COUNTER
}
