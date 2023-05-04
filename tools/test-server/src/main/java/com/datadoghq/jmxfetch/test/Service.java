package com.datadoghq.jmxfetch.test;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.Hashtable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.SECONDS;

class MetricTick implements Runnable {
    private final MBeanServer mbs;
    private final MetricsDAO metricsDAO;
    private long nextBeanIdx;

    MetricTick(final MBeanServer mbs, final MetricsDAO dao, final long nextBeanIdx) {
        this.mbs = mbs;
        this.metricsDAO = dao;
        this.nextBeanIdx = nextBeanIdx;
    }

    @Override
    public void run() {
        if (nextBeanIdx != -1) {
            try {
                final String metricName = String.format("Example_%010d", this.nextBeanIdx++);
                final MetricsMBean metrics = new Metrics(metricName, metricsDAO);
                mbs.registerMBean(metrics, null);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }

        metricsDAO.Do();
        System.out.println("Beans Incremented at: " + new Date() + ", " +
                "on thread: " + Thread.currentThread().getName());
    }
}

public class Service {
    private final static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void main(final String[] args) throws Exception {

        long totalMetrics = 1;
        long generateNewBeans = -1;
        if (args.length > 0) {
            totalMetrics = Long.parseLong(args[0]);
            if (args.length == 2) {
                generateNewBeans = totalMetrics;
            }
        }
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        final MetricsDAO metricsDAO = new MetricsDAO();
        for (int i = 0; i < totalMetrics; i++) {
            final String metricName = String.format("Example_%010d", i);
            final MetricsMBean metrics = new Metrics(metricName, metricsDAO);
            mbs.registerMBean(metrics, null);
        }

        // register a bean a single bean with different ObjectName
        final Hashtable<String, String> scopeTestTable = new Hashtable<>();
        scopeTestTable.put("type", "scoped");
        scopeTestTable.put("name", "scopedTest");
        scopeTestTable.put("request", "special");
        mbs.registerMBean(new Metrics("scopeTest", metricsDAO), new ObjectName("scope.test", scopeTestTable));

        final MetricTick ticker = new MetricTick(mbs, metricsDAO, generateNewBeans);
        Service.scheduler.scheduleAtFixedRate(ticker, 1, 1, SECONDS);
        Thread.currentThread().join();
    }
}
