package org.ulinda.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Custom thread pool for async operations
     */
    @Bean(name = "errorLogExecutor")
    public Executor errorLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);        // Minimum threads
        executor.setMaxPoolSize(5);         // Maximum threads
        executor.setQueueCapacity(100);     // Queue size before creating new threads
        executor.setThreadNamePrefix("error-log-");
        executor.setKeepAliveSeconds(60);   // How long excess threads stay alive
        executor.setRejectedExecutionHandler((r, executor1) -> {
            log.warn("Error log task rejected, queue is full. Executing synchronously.");
            r.run(); // Execute synchronously as fallback
        });
        executor.initialize();
        return executor;
    }

    /**
     * Default async executor
     */
    @Override
    public Executor getAsyncExecutor() {
        return errorLogExecutor();
    }

    /**
     * Handle uncaught exceptions in async methods
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            log.error("Async method '{}' threw an uncaught exception: {}",
                    method.getName(), ex.getMessage(), ex);
        };
    }
}