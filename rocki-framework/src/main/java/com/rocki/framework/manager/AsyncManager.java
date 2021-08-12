package com.rocki.framework.manager;

import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.rocki.common.utils.Threads;
import com.rocki.common.utils.spring.SpringUtils;


public class AsyncManager
{

    private final int OPERATE_DELAY_TIME = 10;


    private ScheduledExecutorService executor = SpringUtils.getBean("scheduledExecutorService");


    private AsyncManager(){}

    private static AsyncManager me = new AsyncManager();

    public static AsyncManager me()
    {
        return me;
    }

    public void execute(TimerTask task)
    {
        executor.schedule(task, OPERATE_DELAY_TIME, TimeUnit.MILLISECONDS);
    }

    public void shutdown()
    {
        Threads.shutdownAndAwaitTermination(executor);
    }
}
