package com.pingyu.codematebackend.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pingyu.codematebackend.mapper.PrivateChatMapper;
import com.pingyu.codematebackend.model.PrivateChat;
import com.pingyu.codematebackend.service.PrivateChatService;
import org.springframework.stereotype.Service;

@Service
public class PrivateChatServiceImpl extends ServiceImpl<PrivateChatMapper, PrivateChat>
        implements PrivateChatService {}