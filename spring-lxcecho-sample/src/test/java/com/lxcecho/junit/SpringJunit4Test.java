package com.lxcecho.junit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:bean-junit.xml")
public class SpringJunit4Test {

	@Autowired
	private JUnitUser jUnitUser;

	@Test
	public void testUser4() {
		System.out.println(jUnitUser);
		jUnitUser.run();
	}

}
