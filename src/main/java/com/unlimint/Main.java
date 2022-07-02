package com.unlimint;

import com.unlimint.executor.ApplicationExecutor;
import com.unlimint.parser.Parser;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.Thread.sleep;

@ComponentScan(basePackages = {"com.unlimint"})
public class Main {
    public static void main(String[] args) throws InterruptedException {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(Main.class);
        applicationContext.registerShutdownHook();

        Parser parser = applicationContext.getBean(Parser.class);
        ApplicationExecutor executor = applicationContext.getBean(ApplicationExecutor.class);

        // All input files run in different thread as they are independent of each other.
        for (String arg : args) {
            Path filePath = Paths.get(arg);
            if ("csv".equals(parser.getFileType(filePath))) {
                executor.addFuture(executor.getExecutor().submit(() -> parser.parseCsvFile(filePath)));
            } else if ("json".equals(parser.getFileType(filePath))) {
                executor.addFuture(executor.getExecutor().submit(() -> parser.parseJsonFile(filePath)));
            }
        }

        boolean allDone = false;
        // Waits until all tasks are completed
        while (!allDone) {
            if (executor.getFutures().stream().anyMatch(ele -> !ele.isDone())) {
                //To syn with executor.getFutures() and to avoid busy waiting
                sleep(100);
            } else {
                allDone = true;
            }
        }
        executor.shutdownExecutor();
        applicationContext.close();
    }
}
