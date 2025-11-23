package com.pingyu.codematebackend.controller;

import com.pingyu.codematebackend.common.BaseResponse;
import com.pingyu.codematebackend.common.ErrorCode;
import com.pingyu.codematebackend.dto.UserLoginRequest;
import com.pingyu.codematebackend.dto.UserRegisterRequest;
import com.pingyu.codematebackend.dto.UserUpdateAvatarRequest;
import com.pingyu.codematebackend.dto.UserUpdateDTO;
import com.pingyu.codematebackend.model.User;
import com.pingyu.codematebackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * 用户接口
 * [已重构为使用 BaseResponse + ErrorCode 规范]
 *
 * @author 花萍雨
 * @since 2025-10-29
 */
@RestController
@RequestMapping("/user")
@Tag(name = "1. 用户管理接口 (UserController)")
public class UserController {

    @Resource
    private UserService userService;

    // ... (userRegister, userLogin, searchUsersByTags 等方法保持不变) ...
    // (我们只修复有问题的 updateUser 和 getCurrentUser)

    /**
     * 【22号案】用户注册接口
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest registerRequest) {
        if (registerRequest == null) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        String userAccount = registerRequest.getUserAccount();
        String userPassword = registerRequest.getUserPassword();
        String checkPassword = registerRequest.getCheckPassword();
        if (userAccount == null || userPassword == null || checkPassword == null) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "账号或密码为空");
        }
        long newUserId = userService.userRegister(userAccount, userPassword, checkPassword);
        return BaseResponse.success(newUserId);
    }

    // 在 UserController.java 中
    /**
     * 【【【 案卷 #2：临时测试“扳机” 】】】
     * (用于触发“方案 A”的串行导入)
     */
    @PostMapping("/import-test")
    @Operation(summary = "【性能测试】方案 A：串行导入 1000 用户")
    public BaseResponse<String> importUsersTest() {

        // (已添加到 UserService 接口)
        userService.importUsersTest();

        // (返回成功)
        return BaseResponse.success("【方案 A】任务已触发，请查看后端日志");
    }

    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(@RequestParam("searchText") String searchText) {

        // 1. (安全校验 - 采用你的“进攻性规范”)

        // [修正] 使用 Java 11+ 的标准 null 检查 和 isBlank()
        if (searchText == null || searchText.isBlank()) {

            // [修正] 遵循你的“内心独白”：
            // 不返回错误，而是返回一个“空的成功列表”
            return BaseResponse.success(Collections.emptyList());
        }

        // (或者，使用 Spring 自带的工具，更简洁)
        // if (!StringUtils.hasText(searchText)) {
        //     return BaseResponse.success(Collections.emptyList());
        // }


        // 2. (调用 Service) - 你的代码是完美的
        List<User> userList = userService.searchUserByText(searchText);

        // 3. (返回成功) - 你的代码是完美的
        return BaseResponse.success(userList);
    }

    /**
     * 个人信息修改接口 (安全版)
     * [【【修复：修正“矛盾”的方法】】]
     */
    @PutMapping("/updateinfo")
    @Operation(summary = "更新当前用户信息")
    // 变化 1：签名必须返回 BaseResponse，而不是 boolean
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateDTO userUpdateDTO, HttpSession session) {

        // 1. 从 Session "保险箱"中获取当前登录的用户信息
        User currentUser = (User) session.getAttribute("loginUser");

        // 2. (安全检查) 检查登录状态
        if (currentUser == null) {
            // 变化 2：现在 return BaseResponse.error 是合法的
            return BaseResponse.error(ErrorCode.NOT_LOGGED_IN);
        }

        // 3. (关键安全点)
        Long safeUserId = currentUser.getId();

        // 4. 调用 Service 层
        // Service 返回一个 boolean
        boolean success = userService.updateUserInfo(userUpdateDTO, safeUserId);

        // 变化 3：将 Service 返回的 boolean 包装成 BaseResponse
        if (!success) {
            // (如果 Service 返回 false，我们也将其视为一个错误)
            return BaseResponse.error(ErrorCode.SYSTEM_ERROR, "更新失败");
        }

        return BaseResponse.success(true);
    }

    /**
     * [【【修复：“幽灵”方法已在 Service 接口中添加】】]
     */
    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpSession session) {

        // 1. (身份验证)
        User currentUser = (User) session.getAttribute("loginUser");

        // 2. (安全检查)
        if (currentUser == null) {
            return BaseResponse.error(ErrorCode.NOT_LOGGED_IN);
        }

        // 3. (获取可信 ID)
        Long safeUserId = currentUser.getId();

        // 4. (调用 Service)
        // 这一行现在可以被正确解析了
        User safeUser = userService.getCurrent(safeUserId);

        // (进阶检查：如果 Service 遵循我们上一个 SOP，它会在查不到时抛异常)
        // (但如果它返回 null，我们在这里补一个保险)
        if (safeUser == null) {
            return BaseResponse.error(ErrorCode.NOT_FOUND_ERROR, "未找到当前用户信息");
        }

        // 5. (返回成功)
        return BaseResponse.success(safeUser);
    }


    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "用户登录")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest loginRequest, HttpServletRequest request) {
        if (loginRequest == null) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        String userAccount = loginRequest.getUserAccount();
        String userPassword = loginRequest.getUserPassword();
        if (userAccount == null || userPassword == null) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "账号或密码为空");
        }
        User user = userService.userLogin(userAccount, userPassword, request);
        return BaseResponse.success(user);
    }

    @GetMapping("/search/tags")
    @Operation(summary = "按标签搜索用户", description = "根据标签列表（AND 逻辑）搜索用户")
    @Parameter(name = "tagNames", description = "要搜索的标签列表, e.g. ?tagNames=Java&tagNames=大一")
    public BaseResponse<List<User>> searchUsersByTags(@RequestParam(required = false) List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return BaseResponse.success(Collections.emptyList());
        }
        List<User> userList = userService.searchUsersByTags(tagNames);
        return BaseResponse.success(userList);
    }

    @PutMapping("/update/avatar")
    @Operation(summary = "更新用户头像", description = "上传新头像后，调用此接口将 URL 持久化到数据库")
    public BaseResponse<Boolean> updateUserAvatar(@RequestBody UserUpdateAvatarRequest request) {
        if (request == null || request.getUserId() == null || request.getUserId() <= 0) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }
        boolean success = userService.updateUserAvatar(
                request.getUserId(),
                request.getAvatarUrl()
        );
        if (!success) {
            return BaseResponse.error(ErrorCode.SYSTEM_ERROR, "更新头像失败");
        }
        return BaseResponse.success(true);
    }

    @GetMapping("/list")
    @Operation(summary = "获取所有用户列表", description = "获取数据库中所有用户（已脱敏）")
    public BaseResponse<List<User>> listUsers() {
        List<User> userList = userService.list();
        if (userList == null) {
            return BaseResponse.success(List.of());
        }
        List<User> safeUserList = userList.stream().map(user -> {
            user.setUserPassword(null);
            user.setIsDelete(null);
            return user;
        }).collect(Collectors.toList());
        return BaseResponse.success(safeUserList);
    }

    @GetMapping("/{id}")
    @Operation(summary = "按 ID 获取单个用户", description = "根据用户ID 获取单个用户（已脱敏且包含标签）")
    @Parameter(name = "id", description = "用户ID", required = true)
    public BaseResponse<User> getUserById(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }

        // 【关键修复】切换到“增强版”查询方法
        // 这个方法会负责：1.查User表 2.查Tag关联表 3.执行脱敏
        User user = userService.getSafetyUserById(id);

        if (user == null) {
            return BaseResponse.error(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        // (Service 已经脱敏过了，这里不需要再手动 set null)
        return BaseResponse.success(user);
    }
}