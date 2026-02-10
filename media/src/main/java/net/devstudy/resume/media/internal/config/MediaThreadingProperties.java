package net.devstudy.resume.media.internal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "media.threading")
public class MediaThreadingProperties {

    private boolean parallelEnabled = true;
    private int poolSize = 0;
    private int queueCapacity = 100;
    private int shutdownTimeoutSeconds = 30;

    public boolean isParallelEnabled() {
        return parallelEnabled;
    }

    public void setParallelEnabled(boolean parallelEnabled) {
        this.parallelEnabled = parallelEnabled;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public int getShutdownTimeoutSeconds() {
        return shutdownTimeoutSeconds;
    }

    public void setShutdownTimeoutSeconds(int shutdownTimeoutSeconds) {
        this.shutdownTimeoutSeconds = shutdownTimeoutSeconds;
    }
}
