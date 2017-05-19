package myOwnTest;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * Created by lxj on 2016/6/25.
 */
public class Test {
    @org.junit.Test
    public void testFileSystemXmlApplicationContext() {
        FileSystemXmlApplicationContext fsxac = new FileSystemXmlApplicationContext("org/springframework/core/env/EnvironmentSystemIntegrationTests-context.xml");
        fsxac.refresh();
    }

    @org.junit.Test
    public void testApplicationContext() {
        ApplicationContext ac = new FileSystemXmlApplicationContext("build/resources/test/myOwnTest/test.xml");
        People people = (People) ac.getBean("testBean");
        System.out.println(people.getName());
    }
}
