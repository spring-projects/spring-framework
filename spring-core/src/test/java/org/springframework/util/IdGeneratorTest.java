package org.springframework.util;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class IdGeneratorTest {

	private JdkIdGenerator jdkIdGenerator;

	@BeforeClass
	public void init() {
		jdkIdGenerator = new JdkIdGenerator();
	}
	@Test
	public void testJdkIdGeneratro() {
		List<Thread> threads = new ArrayList();
		List<String> ids = new ArrayList<>();
		for(int i = 0;i<100;i++) {
			Thread thread = new Thread(new Runnable() {
				public void run() {
					ids.add(jdkIdGenerator.generateId().toString());
				}
			});
		}
		for(int i = 0;i<100;i++) {
			threads.get(i).start();
		}
		System.out.println(ids.size());
		HashSet set = new HashSet(ids);
		System.out.println(set.size());
	}
}
