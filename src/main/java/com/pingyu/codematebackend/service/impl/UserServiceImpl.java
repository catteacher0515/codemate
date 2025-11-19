package com.pingyu.codematebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pingyu.codematebackend.common.ErrorCode;       // [SOP] 导入
import com.pingyu.codematebackend.exception.BusinessException; // [SOP] 导入
import com.pingyu.codematebackend.dto.UserUpdateDTO;
import com.pingyu.codematebackend.mapper.UserMapper;
import com.pingyu.codematebackend.model.User;
import com.pingyu.codematebackend.service.UserService;
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

    /**
     * [SOP 重构]
     * 更新当前登录用户的个人信息
     */
    @Override
    @Transactional
    public boolean updateUserInfo(UserUpdateDTO dto, Long safeUserId) {

        // 1. “取出旧档案”
        User originalUser = this.baseMapper.selectById(safeUserId);
        if (originalUser == null) {
            // [SOP] 抛出异常，而不是返回 error
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        // 2. “验证情报” (用户名查重)
        String newUsername = dto.getUsername();
        if (newUsername != null && !newUsername.equals(originalUser.getUsername())) {
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("username", newUsername);
            User existUser = this.baseMapper.selectOne(queryWrapper);

            if (existUser != null && !existUser.getId().equals(safeUserId)) {
                // [SOP] 抛出异常
                throw new BusinessException(ErrorCode.USERNAME_TAKEN);
            }
            originalUser.setUsername(newUsername);
        }

        // 3. “合并情报” (手动、安全地覆盖)
        if (dto.getEmail() != null) {
            originalUser.setEmail(dto.getEmail());
        }
        if (dto.getGender() != null) {
            originalUser.setGender(dto.getGender());
        }
        if (dto.getAvatarUrl() != null) {
            originalUser.setAvatarUrl(dto.getAvatarUrl());
        }

        // 4. “存档归案”
        boolean updateResult = this.baseMapper.updateById(originalUser) > 0;

        if (!updateResult) {
            // [SOP] 兜底，万一更新失败
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新用户信息失败");
        }

        return true; // [SOP] 成功则返回 true
    }

    /**
     * [SOP 新增]
     * 获取当前登录用户信息
     */
    @Override
    public User getCurrent(Long safeUserId) {
        // 1. (查询)
        User originalUser = this.baseMapper.selectById(safeUserId);

        // 2. (检查)
        if (originalUser == null) {
            // [SOP] 抛出异常
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到当前用户");
        }

        // 3. (脱敏)
        User safeUser = this.getSafetyUser(originalUser);

        // 4. (返回)
        return safeUser;
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
     * 获取脱敏的用户信息
     */
    private User getSafetyUser(User originUser) {
        if (originUser == null) {
            return null;
        }
        User safetyUser = new User();
        BeanUtils.copyProperties(originUser, safetyUser);

        // 【关键】统一脱敏
        safetyUser.setUserPassword(null);
        safetyUser.setPhone(null);
        safetyUser.setEmail(null);
        safetyUser.setIsDelete(null);

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