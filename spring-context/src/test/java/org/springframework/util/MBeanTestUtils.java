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

package org.springframework.util;

import org.junit.Test;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.Random;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

/**
 * Utilities for MBean tests.
 *
 * @author Phillip Webb
 */
public class MBeanTestUtils {

	/**
	 * Resets MBeanServerFactory and ManagementFactory to a known consistent state.
	 * This involves releasing all currently registered MBeanServers and resetting
	 * the platformMBeanServer to null.
	 */
	public static void resetMBeanServers() throws Exception {
		for (MBeanServer server : MBeanServerFactory.findMBeanServer(null)) {
			MBeanServerFactory.releaseMBeanServer(server);
		}
		Field field = ManagementFactory.class.getDeclaredField("platformMBeanServer");
		field.setAccessible(true);
		field.set(null, null);
	}

	/**
	 * 查找字符串
	 * 时间复杂度：（m-n）*n
	 */
	public void test1() {
		String source = "asdfasdfasdfasdfaewwrwerw";
		String target = "asf";
		//判断长度
		for (int sIndex = 0; sIndex < source.length() - target.length(); sIndex++) {
			int tempSindex = sIndex;
			boolean flag = true;
			for (int tIndex = 0; tIndex < target.length(); tIndex++) {
				if (target.indexOf(tIndex) != source.indexOf(tempSindex++)) {
					flag = false;
					break;
				}
			}
			if (flag) {
				//找到字符串所在位置
				System.out.println(sIndex);
			}
		}
	}

	/**
	 * n*m
	 */
	public void test2() {

	}

	public void test3() {
		//多线程
		Random random = new Random();
		random.nextInt();
	}

	/**
	 * 微信红包算法：
	 * 1、红包个数
	 * 2、随机包
	 * 3、请求并打开
	 */
	@Test
	public void redBag(){

	}
}
