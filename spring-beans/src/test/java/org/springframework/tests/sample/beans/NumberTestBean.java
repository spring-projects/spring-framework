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

package org.springframework.tests.sample.beans;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @author Juergen Hoeller
 * @since 10.06.2003
 */
public class NumberTestBean {

	private short short1;
	private Short short2;

	private int int1;
	private Integer int2;

	private long long1;
	private Long long2;

	private BigInteger bigInteger;

	private float float1;
	private Float float2;

	private double double1;
	private Double double2;

	private BigDecimal bigDecimal;

	public short getShort1() {
		return short1;
	}

	public void setShort1(short short1) {
		this.short1 = short1;
	}

	public Short getShort2() {
		return short2;
	}

	public void setShort2(Short short2) {
		this.short2 = short2;
	}

	public int getInt1() {
		return int1;
	}

	public void setInt1(int int1) {
		this.int1 = int1;
	}

	public Integer getInt2() {
		return int2;
	}

	public void setInt2(Integer int2) {
		this.int2 = int2;
	}

	public long getLong1() {
		return long1;
	}

	public void setLong1(long long1) {
		this.long1 = long1;
	}

	public Long getLong2() {
		return long2;
	}

	public void setLong2(Long long2) {
		this.long2 = long2;
	}

	public BigInteger getBigInteger() {
		return bigInteger;
	}

	public void setBigInteger(BigInteger bigInteger) {
		this.bigInteger = bigInteger;
	}

	public float getFloat1() {
		return float1;
	}

	public void setFloat1(float float1) {
		this.float1 = float1;
	}

	public Float getFloat2() {
		return float2;
	}

	public void setFloat2(Float float2) {
		this.float2 = float2;
	}

	public double getDouble1() {
		return double1;
	}

	public void setDouble1(double double1) {
		this.double1 = double1;
	}

	public Double getDouble2() {
		return double2;
	}

	public void setDouble2(Double double2) {
		this.double2 = double2;
	}

	public BigDecimal getBigDecimal() {
		return bigDecimal;
	}

	public void setBigDecimal(BigDecimal bigDecimal) {
		this.bigDecimal = bigDecimal;
	}

}