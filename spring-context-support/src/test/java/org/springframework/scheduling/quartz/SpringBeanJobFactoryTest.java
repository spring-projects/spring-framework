package org.springframework.scheduling.quartz;

import java.util.Date;

import org.junit.Test;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

/**
 * Tests for the {@link SpringBeanJobFactory}
 *
 * @author Marten Deinum
 */
public class SpringBeanJobFactoryTest {

    @Test
    public void shouldSetParameterFromContextAndAutowireDependenciesInAJob() throws Exception {
        ApplicationContext context = loadContext();
        TriggerFiredBundle tfb = createTrigger(context, "jobDetailWithJob");

        SpringBeanJobFactory factory = context.getBean(SpringBeanJobFactory.class);

        DummyJob job = (DummyJob) factory.createJobInstance(tfb);

        assertThat(job.getDummyBean(), sameInstance(context.getBean(DummyBean.class)));
        assertThat(job.getParam(), is(42));
    }

    private ApplicationContext loadContext() {
        return new ClassPathXmlApplicationContext("scheduler-with-job-factory.xml", SpringBeanJobFactoryTest.class);
    }


    private TriggerFiredBundle createTrigger(ApplicationContext context, String name) {
        final Date now = new Date();
        final JobDetail jobDetail = context.getBean(name, JobDetail.class);
        return new TriggerFiredBundle(jobDetail, new SimpleTriggerImpl(), null, false, now, now, now, now);
    }


    public static class DummyBean {
    }

    public static class DummyJob implements Job {

        private int param;

        @Autowired
        private DummyBean dummyBean;


        @Override
        public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        }


        public void setParam(int value) {
            param = value;
        }

        public int getParam() {
            return param;
        }

        public DummyBean getDummyBean() {
            return dummyBean;
        }
    }
}