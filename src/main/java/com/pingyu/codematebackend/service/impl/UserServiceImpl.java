package com.pingyu.codematebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pingyu.codematebackend.common.ErrorCode;       // [SOP] 导入
import com.pingyu.codematebackend.exception.BusinessException; // [SOP] 导入
import com.pingyu.codematebackend.dto.UserUpdateDTO;
import com.pingyu.codematebackend.mapper.UserMapper;
import com.pingyu.codematebackend.model.Tag;
import com.pingyu.codematebackend.model.User;
import com.pingyu.codematebackend.model.UserTagRelation;
import com.pingyu.codematebackend.service.TagService;
import com.pingyu.codematebackend.service.UserService;
import com.pingyu.codematebackend.service.UserTagRelationService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 * [已重构为策略 B · SOP 最终版]
 * @author 花萍雨
 * @since 2025-10-26
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private TagService tagService;

    @Resource
    private UserTagRelationService userTagRelationService;

    // 存入缓存
//    String key = "pingyu:test:1";
//    String value = "is_a_detective";
//    redisTemplate.opsForValue().set(key, value,1,TimeUnit.HOURS);

    private static final String SALT = "pingyu_is_the_best_detective_!@#";
    private ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Override
    public boolean isAdmin(User user) {
        // 1. 判空
        if (user == null || user.getUserRole() == null) {
            return false;
        }
        // 2. 判定 (1 为管理员)
        return user.getUserRole() == 1;
    }

    /**
     * 【【【 案卷 #3：方案 C (并发 + 批量) 测试 】】】
     * (重构“方案 A”，用于建立“性能基准”)
     */
    @Override
/**
 * 【【【 案卷 #2：方案 A (串行循环) 测试 】】】
 * (一个“天真”的实现，用于建立“性能基准”)
 */
    public void importUsersTest() {

        log.info("【方案 A】启动：开始“串行”导入 1000 个用户...");

        // 1. 计时 (正确)
        long startTime = System.currentTimeMillis();

        // 2. 循环 (正确)
        for (int i = 0; i < 1000; i++) {
            User user = new User();

            // 3. 创建唯一用户 (正确)
            user.setUserAccount("test_user_" + i);
            user.setUsername(i + " 号用户");

            // 【【【 修复：必须传入 String 类型 】】】
            user.setUserPassword("12345678");

            // 4. 串行插入 (正确)
            this.save(user);
        }

        // 5. 计算耗时 (正确)
        long endTime = System.currentTimeMillis();
        long cost = endTime - startTime;

        log.info("【方案 A】完成：“串行”导入 1000 个用户，总耗时：{} 毫秒。", cost);
    }

    @Override
    public User getSafetyUserById(Long id) {
        // 1. 查主表
        User user = this.baseMapper.selectById(id);
        if (user == null) {
            return null;
        }

        // 2. 查标签 (补全技能芯片)
        QueryWrapper<UserTagRelation> relationQw = new QueryWrapper<>();
        relationQw.eq("userId", id);
        List<UserTagRelation> relations = userTagRelationService.list(relationQw);

        if (!relations.isEmpty()) {
            List<Long> tagIds = relations.stream()
                    .map(UserTagRelation::getTagid)
                    .collect(Collectors.toList());
            List<Tag> tags = tagService.listByIds(tagIds);
            List<String> tagNames = tags.stream()
                    .map(Tag::getTagname)
                    .collect(Collectors.toList());
            user.setTags(tagNames);
        } else {
            user.setTags(new ArrayList<>());
        }

        // 3. 脱敏 (复用现有的脱敏逻辑)
        return getSafetyUser(user);
    }

    /**
     * [SOP 重构]
     * 更新当前登录用户的个人信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUserInfo(UserUpdateDTO dto, Long safeUserId) {

        // 【探针】打印收到的包裹内容，看看 phone 和 tags 到底是不是 null
        System.out.println("--- [侦探探针] 收到更新请求 ---");
        System.out.println("DTO Bio: " + dto.getBio());
        System.out.println("DTO Phone: " + dto.getPhone());
        System.out.println("DTO Email: " + dto.getEmail());
        System.out.println("DTO Tags: " + dto.getTags());
        System.out.println("---------------------------");

        // 1. 取出旧档案
        User originalUser = this.baseMapper.selectById(safeUserId);
        if (originalUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        // 2. 更新基础信息 (用户名、日志、邮箱、手机、性别、头像)
        if (StringUtils.hasText(dto.getUsername()) && !dto.getUsername().equals(originalUser.getUsername())) {
            // 查重逻辑
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("username", dto.getUsername());
            User existUser = this.baseMapper.selectOne(queryWrapper);
            if (existUser != null && !existUser.getId().equals(safeUserId)) {
                throw new BusinessException(ErrorCode.USERNAME_TAKEN, "该代号已被占用");
            }
            originalUser.setUsername(dto.getUsername());
        }

        if (dto.getBio() != null) originalUser.setBio(dto.getBio());
        if (StringUtils.hasText(dto.getEmail())) originalUser.setEmail(dto.getEmail());
        if (StringUtils.hasText(dto.getPhone())) originalUser.setPhone(dto.getPhone());
        if (dto.getGender() != null) originalUser.setGender(dto.getGender());
        if (StringUtils.hasText(dto.getAvatarUrl())) originalUser.setAvatarUrl(dto.getAvatarUrl());

        boolean updateResult = this.baseMapper.updateById(originalUser) > 0;
        if (!updateResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新基础信息失败");
        }

        // 3. 【核心新增】更新技能芯片 (Tags)
        List<String> newTags = dto.getTags();
        if (newTags != null) {
            // A. 清理旧标签关联 (删除 user_tag_relation 中该 userId 的所有记录)
            QueryWrapper<UserTagRelation> removeQw = new QueryWrapper<>();
            removeQw.eq("userId", safeUserId);
            userTagRelationService.remove(removeQw);

            // B. 建立新关联
            if (!newTags.isEmpty()) {
                List<UserTagRelation> relationList = new ArrayList<>();
                for (String tagName : newTags) {
                    tagName = tagName.trim();
                    if (!StringUtils.hasText(tagName)) continue; // 消除警告

                    // b1. 查找或创建 Tag (实现“自由标签”)
                    QueryWrapper<Tag> tagQw = new QueryWrapper<>();
                    tagQw.eq("tagName", tagName);
                    Tag tag = tagService.getOne(tagQw);

                    Long tagId;
                    if (tag == null) {
                        // 创建新标签
                        tag = new Tag();
                        tag.setTagname(tagName);
                        tag.setParentid(0L); // 默认为根标签_
                        tag.setIsparent(0);
                        tagService.save(tag);
                        tagId = tag.getId();
                    } else {
                        tagId = tag.getId();
                    }

                    // b2. 创建关联对象
                    UserTagRelation relation = new UserTagRelation();
                    relation.setUserid(safeUserId);
                    relation.setTagid(tagId);
                    relationList.add(relation);
                }


                // c. 批量保存
                if (!relationList.isEmpty()) {
                    userTagRelationService.saveBatch(relationList);
                }

                // 【关键修复】清理缓存 (Cache Invalidation)
                // 既然不知道具体影响了哪个标签搜索结果，干脆把所有标签搜索缓存都清了
                // (生产环境不建议这么干，但开发阶段这是最稳妥的)
                String pattern = "codemate:user:tags:*";
                Set<String> keys = redisTemplate.keys(pattern);
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    System.out.println("--- [侦探清理] 已清除旧缓存: " + keys.size() + " 个 ---");
                }

                // 也要清理“关键词搜索”的缓存 (如果有的话)
                String searchPattern = "codemate:user:searchtext:*";
                Set<String> searchKeys = redisTemplate.keys(searchPattern);
                if (searchKeys != null && !searchKeys.isEmpty()) {
                    redisTemplate.delete(searchKeys);
                }
            }
        }

        return true;
    }

    /**
     * 获取当前登录用户信息 (查自己 - 需要看到所有隐私数据)
     */
    @Override
    public User getCurrent(Long safeUserId) {
        // 1. 查基础表
        User user = this.baseMapper.selectById(safeUserId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到当前用户");
        }

        // 2. 【修复】查标签 (补全 Tags)
        // 这里的逻辑是：去 user_tag_relation 查关联 -> 去 tag 表查名字 -> 塞给 user
        QueryWrapper<UserTagRelation> relationQw = new QueryWrapper<>();
        relationQw.eq("userId", safeUserId);
        List<UserTagRelation> relations = userTagRelationService.list(relationQw);

        if (!relations.isEmpty()) {
            List<Long> tagIds = relations.stream().map(UserTagRelation::getTagid).collect(Collectors.toList());
            List<Tag> tags = tagService.listByIds(tagIds);
            List<String> tagNames = tags.stream().map(Tag::getTagname).collect(Collectors.toList());
            user.setTags(tagNames); // 填入标签
        } else {
            user.setTags(Collections.emptyList());
        }

        // 3. 【修复】不脱敏！直接返回！
        // 因为是查自己，所以需要看到 phone 和 email
        // 我们只把密码擦除即可
        user.setUserPassword(null);
        user.setIsDelete(null);

        return user;
    }

    /**
     * [SOP 重构]
     * 用户注册
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 【校验】
        if (userAccount == null || userPassword == null || checkPassword == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度不能小于 4 位");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不能小于 8 位");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        // 2. 【防重】
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUserAccount, userAccount);
        long count = this.baseMapper.selectCount(queryWrapper);

        if (count > 0) {
            throw new BusinessException(ErrorCode.USERNAME_TAKEN, "该账号已被注册");
        }

        // 3. 【加密】
        String encryptedPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        log.info("【取证-注册】账号: {}, 正在存入数据库的哈希: {}", userAccount, encryptedPassword);

        // 4. 【写入数据库】
        User newUser = new User();
        newUser.setUserAccount(userAccount);
        newUser.setUserPassword(encryptedPassword);
        newUser.setUsername(userAccount); // 默认昵称=账号

        boolean saveResult = this.save(newUser);

        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库写入异常");
        }

        // 5. 【返回新 ID】
        return newUser.getId();
    }

    /**
     * [SOP 重构]
     * 用户登录
     */
    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 【密码加密】
        String encryptedPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        log.info("【取证-登录】账号: {},  hashlib: {}", userAccount, encryptedPassword);

        // 2. 【查数据库】
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUserAccount, userAccount);
        queryWrapper.eq(User::getUserPassword, encryptedPassword);
        User user = this.baseMapper.selectOne(queryWrapper);

        // 3. 【用户不存在或密码错误】
        if (user == null) {
            log.warn("user login failed, userAccount: {}", userAccount);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "账号或密码错误");
        }

        // 4. 【脱敏】
        User safetyUser = getSafetyUser(user);

        // 5. 【写入 Session】
        request.getSession().setAttribute("loginUser", safetyUser);

        // 6. 【返回】
        return safetyUser;
    }

    @Override
    public boolean updateUserAvatar(long userId, String avatarUrl) {
        if (userId <= 0 || avatarUrl == null || avatarUrl.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID或头像地址无效");
        }
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, userId);
        updateWrapper.set(User::getAvatarUrl, avatarUrl);
        return this.update(null, updateWrapper);
    }

    /**
     * 获取脱敏的用户信息 (查别人 - 必须隐藏隐私)
     */
    private User getSafetyUser(User originUser) {
        if (originUser == null) {
            return null;
        }
        User safetyUser = new User();
        BeanUtils.copyProperties(originUser, safetyUser);

        // 【查别人】时，必须脱敏
        safetyUser.setUserPassword(null);
        safetyUser.setPhone(null); // 隐藏手机
        safetyUser.setEmail(null); // 隐藏邮箱
        safetyUser.setUserStatus(null);
        safetyUser.setCreateTime(null);
        safetyUser.setUpdateTime(null);
        safetyUser.setIsDelete(null);

        // 注意：查别人时，如果需要显示标签，这里也应该加上查标签的逻辑(类似 getCurrent)
        // 但为了节省性能，通常在列表页会用 V3 聚合搜索来处理标签。
        // 这里暂时保持基础脱敏。

        return safetyUser;
    }

    /**
     * 【【【 2. 重构 searchUserByTags (植入缓存SOP) 】】】
     */
    @Override
    public List<User> searchUsersByTags(List<String> tagNameList) {
        // [SOP 1. 校验] 如果标签列表为空，直接返回空
        if (CollectionUtils.isEmpty(tagNameList)) {
            return Collections.emptyList();
        }

        // [SOP 2. 定义“缓存钥匙”]
        // a. 解决“陷阱”：必须排序，防止 ["java", "vue"] 和 ["vue", "java"] 生成不同 key
        Collections.sort(tagNameList);
        // b. 生成 Key
        String redisKey = String.format("codemate:user:tags:%s", tagNameList);

        // [SOP 3. 查缓存 (GET)]
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        List<User> userList = (List<User>) valueOperations.get(redisKey);

        // [SOP 4. 缓存命中 (Fast Path)]
        if (userList != null) {
            log.info("【缓存命中】Key: {}", redisKey);
            return userList;
        }

        // [SOP 5. 缓存未命中 (Slow Path)]
        log.warn("【缓存未命中】Key: {}。正在查询 MySQL...", redisKey);

        // (执行“慢”的数据库查询)
        int size = tagNameList.size();
        userList = this.baseMapper.findUsersByAllTags(tagNameList, size);
        if (userList == null) {
            userList = Collections.emptyList();
        }

        // (脱敏 - 必须在“回填”*之前*脱敏)
        List<User> safetyUserList = userList.stream()
                .map(this::getSafetyUser)
                .collect(Collectors.toList());

        // [SOP 6. 回填缓存 (SET)]
        try {
            // (我们设置 1 小时过期，防止“数据一致性” 陷阱)
            valueOperations.set(redisKey, safetyUserList, 1, TimeUnit.HOURS);
        } catch (Exception e) {
            // (即使缓存“回填”失败，也不能影响主流程)
            log.error("【缓存回填失败】Key: {}", redisKey, e);
        }

        return safetyUserList;
    }

    /**
     * 【【【 3. (可选) 重构 searchUserByText (植入缓存SOP) 】】】
     */
    @Override
    public List<User> searchUserByText(String searchText) {

        // [SOP 1. 校验]
        if (!StringUtils.hasText(searchText)) {
            return Collections.emptyList();
        }

        // [SOP 2. 定义“缓存钥匙”]
        String redisKey = String.format("codemate:user:searchtext:%s", searchText);

        // [SOP 3. 查缓存 (GET)]
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        List<User> userList = (List<User>) valueOperations.get(redisKey);

        // [SOP 4. 缓存命中 (Fast Path)]
        if (userList != null) {
            log.info("【缓存命中】Key: {}", redisKey);
            return userList;
        }

        // [SOP 5. 缓存未命中 (Slow Path)]
        log.warn("【缓存未命中】Key: {}。正在查询 MySQL...", redisKey);

        // (执行“慢”的数据库查询)
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(User::getUsername, searchText);
        userList = this.baseMapper.selectList(queryWrapper);

        // (脱敏)
        List<User> safetyUserList = userList.stream()
                .map(this::getSafetyUser)
                .collect(Collectors.toList());

        // [SOP 6. 回填缓存 (SET)]
        try {
            // (搜索结果变化快，我们只缓存 5 分钟)
            valueOperations.set(redisKey, safetyUserList, 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("【缓存回填失败】Key: {}", redisKey, e);
        }

        return safetyUserList;
    }
}