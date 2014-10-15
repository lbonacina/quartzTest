package org.gamera.quartzTest;


import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.JobFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

@Startup
@Singleton
public class SchedulerBean {

    private Logger LOG = LoggerFactory.getLogger(SchedulerBean.class);

    private Scheduler scheduler;

    @Inject
    private JobFactory cdiJobFactory;

    @PostConstruct
    public void scheduleJobs() {
        try {
            scheduler = new StdSchedulerFactory().getScheduler();

            scheduler.setJobFactory(cdiJobFactory);

            scheduler.clear();

            JobKey job1Key = JobKey.jobKey("job1", "my-jobs");
            JobDetail job1 = JobBuilder
                    .newJob(SimpleJob.class)
                    .withIdentity(job1Key)
                    .build();

            TriggerKey tk1 = TriggerKey.triggerKey("trigger1", "my-jobs");
            Trigger trigger1 = TriggerBuilder
                    .newTrigger()
                    .withIdentity(tk1)
                    .startNow()
                    .withSchedule(SimpleScheduleBuilder.repeatSecondlyForever(10))
                    .build();

            scheduler.start(); // start before scheduling jobs
            scheduler.scheduleJob(job1, trigger1);

            printJobsAndTriggers(scheduler);

        } catch (SchedulerException e) {
            LOG.error("Error while creating scheduler", e);
        }
    }

    private void printJobsAndTriggers(Scheduler scheduler) throws SchedulerException {
        LOG.info("Quartz Scheduler: {}", scheduler.getSchedulerName());
        for (String group : scheduler.getJobGroupNames()) {
            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.<JobKey>groupEquals(group))) {
                LOG.info("Found job identified by {}", jobKey);
            }
        }
        for (String group : scheduler.getTriggerGroupNames()) {
            for (TriggerKey triggerKey : scheduler.getTriggerKeys(GroupMatcher.<TriggerKey>groupEquals(group))) {
                LOG.info("Found trigger identified by {}", triggerKey);
            }
        }
    }

    @PreDestroy
    public void stopJobs() {
        if (scheduler != null) {
            try {
                scheduler.shutdown(false);
            } catch (SchedulerException e) {
                LOG.error("Error while closing scheduler", e);
            }
        }
    }
}
