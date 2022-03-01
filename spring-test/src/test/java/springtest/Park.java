package springtest;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
//默认是单例模式
@Scope(value = "prototype")
//@Scope(value = "singleton")
public class Park {
    @Resource(name = "cat")
    private Animal cat;
    @Resource(name = "frog")
    private Animal aFrog;

    public void whoIsThere() {
        cat.printName();
        aFrog.printName();
    }
}
