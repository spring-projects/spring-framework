/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.core.enums;

/**
 * Base class for static type-safe labeled enum instances.
 *
 * Usage example:
 *
 * <pre>
 * public class FlowSessionStatus extends StaticLabeledEnum {
 *
 *     // public static final instances!
 *     public static FlowSessionStatus CREATED = new FlowSessionStatus(0, &quot;Created&quot;);
 *     public static FlowSessionStatus ACTIVE = new FlowSessionStatus(1, &quot;Active&quot;);
 *     public static FlowSessionStatus PAUSED = new FlowSessionStatus(2, &quot;Paused&quot;);
 *     public static FlowSessionStatus SUSPENDED = new FlowSessionStatus(3, &quot;Suspended&quot;);
 *     public static FlowSessionStatus ENDED = new FlowSessionStatus(4, &quot;Ended&quot;);
 *
 *     // private constructor!
 *     private FlowSessionStatus(int code, String label) {
 *         super(code, label);
 *     }
 *
 *     // custom behavior
 * }</pre>
 *
 * @author Keith Donald
 * @since 1.2.6
 * @deprecated as of Spring 3.0, in favor of Java 5 enums.
 */
@Deprecated
@SuppressWarnings("serial")
public abstract class StaticLabeledEnum extends AbstractLabeledEnum {

	/**
	 * The unique code of the enum.
	 */
	private final Short code;

	/**
	 * A descriptive label for the enum.
	 */
	private final transient String label;


	/**
	 * Create a new StaticLabeledEnum instance.
	 * @param code the short code
	 * @param label the label (can be {@code null})
	 */
	protected StaticLabeledEnum(int code, String label) {
		this.code = new Short((short) code);
		if (label != null) {
			this.label = label;
		}
		else {
			this.label = this.code.toString();
		}
	}

	public Comparable getCode() {
		return code;
	}

	public String getLabel() {
		return label;
	}

	/**
	 * Return the code of this LabeledEnum instance as a short.
	 */
	public short shortValue() {
		return ((Number) getCode()).shortValue();
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	/**
	 * Return the resolved type safe static enum instance.
	 */
	protected Object readResolve() {
		return StaticLabeledEnumResolver.instance().getLabeledEnumByCode(getType(), getCode());
	}

}
