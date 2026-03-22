package net.devstudy.resume.media.internal.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class MediaThreadingConfig {

    @Bean(name = "mediaOptimizationExecutor")
    public Executor mediaOptimizationExecutor(MediaThreadingProperties properties) {
        if (!properties.isParallelEnabled()) {
            return new SyncTaskExecutor();
        }
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int poolSize = resolvePoolSize(properties);
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(Math.max(0, properties.getQueueCapacity()));
        executor.setThreadNamePrefix("media-opt-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(Math.max(0, properties.getShutdownTimeoutSeconds()));
        executor.initialize();
        return executor;
    }

    private int resolvePoolSize(MediaThreadingProperties properties) {
        int configured = properties.getPoolSize();
        if (configured > 0) {
            return configured;
        }
        int cores = Runtime.getRuntime().availableProcessors();
        return Math.max(1, Math.min(4, cores));
    }
}
