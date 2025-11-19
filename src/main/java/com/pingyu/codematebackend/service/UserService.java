package com.pingyu.codematebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pingyu.codematebackend.dto.UserUpdateDTO;
import com.pingyu.codematebackend.model.User;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author 花萍雨
 * @description 针对表【user(用户表)】的数据库操作Service
 * [已重构为策略 B - 补丁版]
 * @since 2025-10-26
 */
public interface UserService extends IService<User> {

    List<User> searchUsersByTags(List<String> tagNameList);

    boolean updateUserAvatar(long userId, String avatarUrl);

    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * [【新案子】]
     * 按关键词(searchText)模糊搜索用户
     * (对应 第二章-任务8)
     *
     * @param searchText 搜索关键词 (来自 @RequestParam)
     * @return 脱敏后的用户列表
     */
    List<User> searchUserByText(String searchText);

    /**
     * [合同修正]
     * Service 层的职责是执行业务并返回 *原始* 结果 (boolean)。
     * Controller 层负责将这个 boolean 包装成 BaseResponse。
     */
    boolean updateUserInfo(UserUpdateDTO userUpdateDTO, Long safeUserId); // <-- 保持 boolean

    /**
     * 【【【 案卷 #2：临时测试“靶子” 】】】
     * (用于“方案 A”的性能基准测试)
     */
    public void importUsersTest();

    /**
     * [【【修复：新增幽灵方法】】]
     * 获取当前登录用户信息
     * @param safeUserId 可信的用户 ID
     * @return 脱敏后的 User 对象 (或在实现类中抛出异常)
     */
    User getCurrent(Long safeUserId);

    /**
     * 是否为管理员
     *
     * @param user 用户信息
     * @return true-是管理员
     */
    boolean isAdmin(User user);
}