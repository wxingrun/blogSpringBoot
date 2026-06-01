package com.peng.controller;


import com.peng.aspect.MyLog;
import com.peng.entity.Blog;
import com.peng.entity.Comment;
import com.peng.service.IBlogService;
import com.peng.service.ICacheService;
import com.peng.service.ICommentService;
import com.peng.util.IpUtil;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;


@Controller
public class CommentControllers {

    @Autowired
    private ICommentService iCommentService;

    @Autowired
    private IBlogService iBlogService;

    @Autowired
    private ICacheService iCacheService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @MyLog
    @GetMapping("/comments/{blId}")
    public String comments(@PathVariable Long blId, Model model) {
        Blog blog = iBlogService.findFullById(blId);
        if (!blog.getPublished()) {
            throw new RuntimeException("无效资源！");
        }
        model.addAttribute("blog", blog);
        return "blog :: commentList";
    }

    @MyLog
    @PostMapping("/comments")
    public String postComments(Comment comment, HttpServletRequest request) {
        if (comment.getParentId() != null && comment.getParentId() <= -1) {
            comment.setParentId(null);
        }
        String ipAddress = IpUtil.getIpAddress(request);
        comment.setIpAddress(ipAddress);
        boolean saved = iCommentService.saveOrUpdate(comment);
        if (!saved) {
            throw new RuntimeException("评论提交失败！");
        }
        rabbitTemplate.convertAndSend("msg-event-exchange", "msg.wx-pn", formatWxMsg(comment));
        iCacheService.clearCommentRelatedCache(comment.getBlId());
        return "redirect:/comments/" + comment.getBlId() + "?t=" + System.currentTimeMillis();
    }

    private String formatWxMsg(Comment comment) {
        StringBuilder sb = new StringBuilder();
        sb.append("发送人:").append(comment.getName()).append(";");
        if (comment.getParentId() != null) {
            Comment parent = iCommentService.getById(comment.getParentId());
            if (parent != null) {
                sb.append("接收人:").append(parent.getName()).append(";");
            }
        }
        sb.append("内容:").append(comment.getContent()).append(";");
        return sb.toString();
    }

}
