package com.peng.service.Impl;


import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.peng.entity.Comment;
import com.peng.mapper.CommentMapper;
import com.peng.service.ICommentService;
import com.peng.service.ICacheService;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements ICommentService {

    @Autowired
    private ICacheService iCacheService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "comment.cache.queue", durable = "true"),
            exchange = @Exchange(value = "msg-event-exchange", type = "topic", ignoreDeclarationExceptions = "true"),
            key = "msg.wx-pn"
    ))
    public void clearCommentCache(Comment comment) {
        if (comment != null && comment.getBlId() != null) {
            iCacheService.clearCommentCache(comment.getBlId());
        }
    }

    @Override
    public PageInfo<Comment> getListByPage(Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum,pageSize);
        List<Comment> list = this.list();
        PageInfo<Comment> result = new PageInfo<>(list);
        return result;
    }

    @Override
    public boolean setDeleted(Long coId, boolean flag) {
        return this.update(new LambdaUpdateWrapper<Comment>().eq(Comment::getCoId, coId).set(Comment::getIsDelete, flag));
    }

}
