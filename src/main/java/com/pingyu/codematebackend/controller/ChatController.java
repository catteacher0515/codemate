package com.pingyu.codematebackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pingyu.codematebackend.common.BaseResponse;
import com.pingyu.codematebackend.common.ErrorCode;
import com.pingyu.codematebackend.dto.ChatRequest;
import com.pingyu.codematebackend.dto.ChatVO;
import com.pingyu.codematebackend.exception.BusinessException;
import com.pingyu.codematebackend.model.PrivateChat;
import com.pingyu.codematebackend.model.TeamChat;
import com.pingyu.codematebackend.model.User;
import com.pingyu.codematebackend.model.UserTeamRelation;
import com.pingyu.codematebackend.service.PrivateChatService;
import com.pingyu.codematebackend.service.TeamChatService;
import com.pingyu.codematebackend.service.UserService;
import com.pingyu.codematebackend.service.UserTeamRelationService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 【案卷 #011】队伍聊天室控制器 (WebSocket + HTTP)
 */
@RestController
public class ChatController {

    @Resource
    private TeamChatService teamChatService;

    @Resource
    private UserService userService;

    @Resource
    private UserTeamRelationService userTeamRelationService;

    // 【核心装备】消息广播器
    @Resource
    private SimpMessagingTemplate messagingTemplate;

    @Resource
    private PrivateChatService privateChatService; // 【新增】

    /**
     * 【HTTP】获取历史消息
     * GET /api/chat/history?teamId=1
     */
    @GetMapping("/chat/history")
    public BaseResponse<List<ChatVO>> getHistoryMessage(@RequestParam Long teamId, HttpServletRequest request) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 1. HTTP 鉴权
        User loginUser = (User) request.getSession().getAttribute("loginUser");
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGGED_IN);
        }
        // 2. 队伍鉴权
        checkIsMember(teamId, loginUser.getId());

        // 3. 查询数据库 (只查最近 30 条)
        QueryWrapper<TeamChat> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("team_id", teamId);
        queryWrapper.orderByAsc("create_time");
        // (实际生产中通常是分页查，这里为了 MVP 简化)
        // queryWrapper.last("limit 30");

        List<TeamChat> chatList = teamChatService.list(queryWrapper);

        // 4. 转换为 VO
        List<ChatVO> voList = chatList.stream().map(chat -> {
            User sender = userService.getById(chat.getUserId());
            return ChatVO.objToVo(chat, sender, loginUser.getId());
        }).collect(Collectors.toList());

        return BaseResponse.success(voList);
    }

    /**
     * 【WebSocket】发送消息
     * 前端发送地址: /app/chat/{teamId}
     * @param teamId 路径参数
     * @param chatRequest 消息体
     * @param headerAccessor 用于获取 WebSocket Session (里面的 loginUser)  <-- 这里加个空格
     */
    @MessageMapping("/chat/{teamId}")
    public void sendMessage(@DestinationVariable Long teamId,
                            @Payload ChatRequest chatRequest,
                            SimpMessageHeaderAccessor headerAccessor) {

        // 1. 从 WebSocket Session 中提取“偷渡”过来的 loginUser
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        User loginUser = (User) sessionAttributes.get("loginUser");

        if (loginUser == null) {
            // (Socket 异常处理比较特殊，这里暂时只能打日志，无法直接返回 HTTP 401)
            System.err.println("【WebSocket】未登录用户尝试发送消息，已拦截");
            return;
        }

        // 2. 校验：是否是该队伍成员
        // (防止有人知道了队伍ID就随便发消息)
        checkIsMember(teamId, loginUser.getId());

        // 3. 【持久化】存入 MySQL
        TeamChat teamChat = new TeamChat();
        teamChat.setTeamId(teamId);
        teamChat.setUserId(loginUser.getId());
        teamChat.setContent(chatRequest.getContent());
        teamChat.setCreateTime(LocalDateTime.now());
        teamChatService.save(teamChat);

        // 4. 【广播】推送到所有订阅者
        // 构造 VO
        ChatVO chatVO = ChatVO.objToVo(teamChat, loginUser, loginUser.getId());
        // 这里有一个小技巧：广播出去的消息，isMine 字段对接收者来说是不确定的
        // (接收者前端会根据自己的 ID 重新判断 isMine，所以这里 VO 的 isMine 设什么都可以)

        // 目标频道: /topic/team/{teamId}
        String destination = "/topic/team/" + teamId;
        messagingTemplate.convertAndSend(destination, chatVO);
    }
    /**
     * 获取私聊历史
     */
    /**
     * 获取私聊历史
     */
    @GetMapping("/chat/private/history")
    public BaseResponse<List<ChatVO>> getPrivateHistory(@RequestParam Long targetUserId, HttpServletRequest request) {
        User loginUser = (User) request.getSession().getAttribute("loginUser");
        if (loginUser == null) throw new BusinessException(ErrorCode.NOT_LOGGED_IN);

        Long myId = loginUser.getId();
        Long targetId = targetUserId;

        // 【重构：LambdaQueryWrapper】
        // 翻译：(sender_id = 我 AND receiver_id = 他) OR (sender_id = 他 AND receiver_id = 我)
        LambdaQueryWrapper<PrivateChat> qw = new LambdaQueryWrapper<>();
        qw.and(wrapper -> wrapper
                .eq(PrivateChat::getSenderId, myId).eq(PrivateChat::getReceiverId, targetId)
                .or()
                .eq(PrivateChat::getSenderId, targetId).eq(PrivateChat::getReceiverId, myId)
        );

        // 按时间升序（旧 -> 新）
        qw.orderByAsc(PrivateChat::getCreateTime);

        List<PrivateChat> chatList = privateChatService.list(qw);

        // 转换 VO (保持原有逻辑)
        List<ChatVO> voList = chatList.stream().map(chat -> {
            User sender = userService.getById(chat.getSenderId());
            ChatVO vo = new ChatVO();
            BeanUtils.copyProperties(chat, vo);
            if (sender != null) {
                vo.setUsername(sender.getUsername());
                vo.setUserAvatar(sender.getAvatarUrl());
            }
            vo.setUserId(chat.getSenderId());
            // 判断是否是“我”发的消息
            vo.setIsMine(chat.getSenderId().equals(myId));
            // 安全地格式化时间
            if (chat.getCreateTime() != null) {
                vo.setCreateTime(chat.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
            return vo;
        }).collect(Collectors.toList());

        return BaseResponse.success(voList);
    }

    /**
     * 发送私聊消息
     * 目标地址: /app/chat/private
     */
    @MessageMapping("/chat/private")
    public void sendPrivateMessage(@Payload ChatRequest chatRequest, SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        User loginUser = (User) sessionAttributes.get("loginUser");
        if (loginUser == null) return;

        Long targetUserId = chatRequest.getTargetUserId(); // 需在 DTO 中添加此字段
        String content = chatRequest.getContent();

        // 1. 入库
        PrivateChat privateChat = new PrivateChat();
        privateChat.setSenderId(loginUser.getId());
        privateChat.setReceiverId(targetUserId);
        privateChat.setContent(content);
        privateChat.setCreateTime(LocalDateTime.now());
        privateChatService.save(privateChat);

        // 2. 广播 (双向通知)
        // 我们生成一个唯一的“会话ID”：minId_maxId
        // 例如 1 和 2 聊天，频道就是 /topic/private/1_2
        long minId = Math.min(loginUser.getId(), targetUserId);
        long maxId = Math.max(loginUser.getId(), targetUserId);
        String destination = "/topic/private/" + minId + "_" + maxId;

        ChatVO chatVO = new ChatVO();
        BeanUtils.copyProperties(privateChat, chatVO);
        chatVO.setUserId(loginUser.getId());
        chatVO.setUsername(loginUser.getUsername());
        chatVO.setUserAvatar(loginUser.getAvatarUrl());
        chatVO.setIsMine(true); // 这里的 isMine 仅对发送者有效，接收者在前端会重新判断
        chatVO.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        messagingTemplate.convertAndSend(destination, chatVO);
    }

    // --- 辅助方法 ---
    private void checkIsMember(Long teamId, Long userId) {
        QueryWrapper<UserTeamRelation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId", teamId);
        queryWrapper.eq("userId", userId);
        long count = userTeamRelationService.count(queryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.NO_AUTH, "非队伍成员无法查看/发送消息");
        }
    }
}