import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author duanshichao
 * @date 2020/5/7
 * @Desc
 */
public class Test {

@org.junit.Test
	public void testA(){
		AnnotationConfigApplicationContext ac=new AnnotationConfigApplicationContext(Mainconfig.class);

		Teacher teacher =(Teacher) ac.getBean("teacher");

		ac.refresh();
		System.out.println(teacher);


	}
}
