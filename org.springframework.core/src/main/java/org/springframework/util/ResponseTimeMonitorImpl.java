/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.util;

import java.util.Date;

/**
 * Default implementation of {@link ResponseTimeMonitor}.
 *
 * @author Rod Johnson
 * @since November 21, 2000
 * @deprecated as of Spring 2.5, to be removed in Spring 3.0
 */
public class ResponseTimeMonitorImpl implements ResponseTimeMonitor {

	/** The system time at which this object was initialized */
	private final long initedMillis = System.currentTimeMillis();

	/** The number of operations recorded by this object */
	private volatile int accessCount;

	/** The sum of the response times for all operations */
	private volatile int totalResponseTimeMillis = 0;

	/** The best response time this object has recorded */
	private volatile int bestResponseTimeMillis = Integer.MAX_VALUE;

	/** The worst response time this object has recorded */
	private volatile int worstResponseTimeMillis = Integer.MIN_VALUE;


	/**
	 * Return the date when this object was loaded.
	 */
	public Date getLoadDate() {
		return new Date(this.initedMillis);
	}

	/**
	 * Return the number of hits this object has handled.
	 */
	public int getAccessCount() {
		return this.accessCount;
	}

	/**
	 * Return the number of milliseconds since this object was loaded.
	 */
	public long getUptimeMillis() {
		return System.currentTimeMillis() - this.initedMillis;
	}

	/**
	 * Return the average response time achieved by this object.
	 */
	public int getAverageResponseTimeMillis() {
		int count = getAccessCount();
		// avoid division by 0
		return (count != 0 ? this.totalResponseTimeMillis / count : 0);
	}

	/**
	 * Return the best (lowest) response time achieved by this object.
	 */
	public int getBestResponseTimeMillis() {
		return this.bestResponseTimeMillis;
	}

	/**
	 * Return the worst (slowest) response time achieved by this object.
	 */
	public int getWorstResponseTimeMillis() {
		return this.worstResponseTimeMillis;
	}


	/**
	 * Utility method to record this response time, updating
	 * the best and worst response times if necessary.
	 * @param responseTimeMillis the response time of this request
	 */
	public synchronized void recordResponseTime(long responseTimeMillis) {
		++this.accessCount;
		int iResponseTime = (int) responseTimeMillis;
		this.totalResponseTimeMillis += iResponseTime;
		if (iResponseTime < this.bestResponseTimeMillis) {
			this.bestResponseTimeMillis = iResponseTime;
		}
		if (iResponseTime > this.worstResponseTimeMillis) {
			this.worstResponseTimeMillis = iResponseTime;
		}
	}

	/**
	 * Return a human-readable string showing the performance
	 * data recorded by this object.
	 */
	public synchronized String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("hits=[").append(getAccessCount()).append("]; ");
		sb.append("average=[").append(getAverageResponseTimeMillis()).append("ms]; ");
		sb.append("best=[").append(getBestResponseTimeMillis()).append("ms]; ");
		sb.append("worst=[").append(getWorstResponseTimeMillis()).append("ms]");
		return sb.toString();
	}

}
