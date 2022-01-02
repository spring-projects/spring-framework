import cn.cxd.beans.Car;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

/**
 * TODO: 一句话简介
 *
 * @author ChenXiaoDong
 * @since 2022/1/2
 */
@ComponentScan("cn.cxd")
public class MainStart {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MainStart.class);
		Car bean = context.getBean(Car.class);
		System.out.println(bean);
		System.out.println("good");
	}
}
