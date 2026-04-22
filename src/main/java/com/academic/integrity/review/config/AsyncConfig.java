package com.academic.integrity.review.config;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

	private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

	@Bean(name = "analysisTaskExecutor")
	public Executor analysisTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(25);
		executor.setThreadNamePrefix("analysis-");
		executor.initialize();
		return executor;
	}

	@Override
	public Executor getAsyncExecutor() {
		return analysisTaskExecutor();
	}

	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return new LoggingAsyncUncaughtExceptionHandler();
	}

	private static final class LoggingAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {
		@Override
		public void handleUncaughtException(Throwable ex, Method method, Object... params) {
			log.error("Unhandled async error in {}", method.getName(), ex);
		}
	}
}