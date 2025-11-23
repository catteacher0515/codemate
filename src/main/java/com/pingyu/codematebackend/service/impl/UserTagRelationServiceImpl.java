package com.pingyu.codematebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pingyu.codematebackend.mapper.UserTagRelationMapper;
import com.pingyu.codematebackend.model.UserTagRelation;
import com.pingyu.codematebackend.service.UserTagRelationService;
import org.springframework.stereotype.Service;

/**
 * 用户标签关系服务实现类
 * [关键修复] 必须加上 @Service 注解，Spring 才能扫描到它！
 */
@Service
public class UserTagRelationServiceImpl
        extends ServiceImpl<UserTagRelationMapper, UserTagRelation>
        implements UserTagRelationService {

}