package com.example.codeeval.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 *
 * 作用：为 LLM 智能评价提供专用线程池，使评价请求"秒级受理、后台执行"，
 * 前端通过轮询获取进度，从根本上规避 HTTP 请求超时问题。
 *
 * 注意：@EnableAsync 必须配合线程池 Bean 使用，否则 Spring 默认使用
 * SimpleAsyncTaskExecutor（每次新建线程，无复用、无上限），存在资源风险。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 评价专用线程池
     *
     * 参数设计理由：
     * - corePoolSize=2 / maxPoolSize=4：DeepSeek 是外部付费 API，并发过高容易触发
     *   平台限流（429），且单个评价任务耗时 10~60 秒，少量线程足够保证吞吐
     * - queueCapacity=100：任务先排队而非无限扩容线程，保护后端内存
     * - CallerRunsPolicy：线程和队列都满时，由提交任务的 HTTP 请求线程自己执行，
     *   实现"背压降级"——请求变慢但任务绝不丢失（比 AbortPolicy 丢任务、
     *   DiscardPolicy 静默丢弃更符合评价场景的可靠性要求）
     * - waitForTasksToCompleteOnShutdown + awaitTerminationSeconds=60：
     *   应用停机时等待进行中的评价任务最多 60 秒，避免 LLM 调用半途而废导致
     *   记录永远卡在 PROCESSING 状态
     */
    @Bean("evaluationExecutor")
    public Executor evaluationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("eval-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
