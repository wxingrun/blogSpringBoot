package com.peng.rabbitmq;

import com.peng.service.ICacheService;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 评论消息消费者
 * 用于处理评论提交后的缓存清除
 */
@Component
public class CommentMessageConsumer {

    @Autowired
    private ICacheService cacheService;

    /**
     * 处理评论消息
     * 当收到评论消息时，清除相关的缓存
     */
    @RabbitHandler
    @RabbitListener(queues = "msg.queue")
    public void processCommentMessage(String message) {
        // 清除评论总数缓存
        cacheService.clearCommentCountCache();
        // 清除首页缓存
        cacheService.clearIndexPageCache();
    }
}
