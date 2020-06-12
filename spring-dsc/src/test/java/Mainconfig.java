import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author duanshichao
 * @date 2020/5/7
 * @Desc
 */
@Configuration
public class Mainconfig {

	@Bean
	public Student student(){
		return new Student();
	}


	@Bean
	public Teacher teacher(@Autowired Student student){
		Teacher teacher=new Teacher();
		teacher.setStudent(student);
		return teacher;
	}

}
