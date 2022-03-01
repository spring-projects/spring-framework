package springtest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "prototype")
//@Scope(value = "singleton")
public class Cat implements Animal{

    private int i = 0;
    @Value("${conf_name}")
    private String name;
    @Override
    public void printName() {
        System.out.println("I am a " + name + "  miao! miao!! reqCount:"+(i++));
    }
}
