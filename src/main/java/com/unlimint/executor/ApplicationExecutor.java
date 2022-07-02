package com.unlimint.executor;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


@Component
public class ApplicationExecutor {
    private static final List<Future<?>> futures = new ArrayList<>();
    public static final ExecutorService executorService = Executors.newFixedThreadPool(4);

    public ExecutorService getExecutor() {
        return executorService;
    }

    public void addFuture(Future<?> future) {
        futures.add(future);
    }

    public List<Future<?>> getFutures() {
        return new ArrayList<>(futures);
    }

    public void shutdownExecutor() {
        executorService.shutdown();
    }

}
