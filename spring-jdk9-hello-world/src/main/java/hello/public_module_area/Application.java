package hello.public_module_area;

import hello.private_module_area.MessagePrinter;
import hello.private_module_area.MessageServices;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;

@Configuration
@ComponentScan(basePackages = {"hello"})
public class Application {

    @Bean
	public MessageServices mockMessageService() {
        return () -> "Hello World!";
    }

    public static void main(String[] args) {
        ApplicationContext context
                = new AnnotationConfigApplicationContext(Application.class);

        MessagePrinter printer
                = context.getBean(MessagePrinter.class);
        printer.printMessage();
    }
}