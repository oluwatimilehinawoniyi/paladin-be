package com.paladin.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Custom thread pool for async operations.
     * Optimized for I/O-bound operations like email sending and API calls.
     */
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core pool size - threads that are always alive
        executor.setCorePoolSize(5);

        // Maximum pool size - scales up to this when queue is full
        executor.setMaxPoolSize(10);

        // Queue capacity - tasks wait here if all threads are busy
        executor.setQueueCapacity(50);

        executor.setThreadNamePrefix("async-executor-");

        // Graceful shutdown - wait for tasks to complete
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        log.info("Async executor initialized with corePoolSize=5, maxPoolSize=10, queueCapacity=50");
        return executor;
    }

    /**
     * Global exception handler for uncaught exceptions in async methods.
     * Logs errors that would otherwise be swallowed.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncUncaughtExceptionHandler() {
            @Override
            public void handleUncaughtException(Throwable ex, Method method, Object... params) {
                log.error("Uncaught async exception in method: {}.{}() - {}",
                        method.getDeclaringClass().getSimpleName(),
                        method.getName(),
                        ex.getMessage(),
                        ex);
            }
        };
    }
}