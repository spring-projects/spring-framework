import org.junit.Test;

/**
 * TODO(描述这个类的作用) <br/>
 *
 * @author qinhd
 * @version 1.0
 * @date 2018/9/10 11:43
 * @since JDK 1.8+
 */
public class GradleTest {
	@Test
	public void test1() {

		print("test1");
	}

	@Test
	public void test2() {

		print("test2");
	}

	private void print(String methodName) {
		System.out.println("================" + methodName + "===============");
	}
}
