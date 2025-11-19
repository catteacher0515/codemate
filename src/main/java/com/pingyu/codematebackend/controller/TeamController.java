package com.pingyu.codematebackend.controller;

// (导入所有“作案工具”)
import com.pingyu.codematebackend.common.BaseResponse;
import com.pingyu.codematebackend.common.ErrorCode;
import com.pingyu.codematebackend.dto.*;
import com.pingyu.codematebackend.exception.BusinessException; // <-- [修复] 导入异常
import com.pingyu.codematebackend.model.User;
import com.pingyu.codematebackend.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 【【【 案卷 #16：队伍接口 (前台) 】】】
 */
@RestController
@RequestMapping("/team")
@Tag(name = "3. 队伍管理接口 (TeamController)") // (我帮你把 X. 改成了 3.)
public class TeamController {

    // 【【 1. 注入“经理” 】】
    @Resource
    private TeamService teamService;



    /**
     * 【【【 案卷 #006：SOP (退出队伍) 】】】
     * (SOP 1 契约: POST /api/team/quit)
     */
    @PostMapping("/quit")
    @Operation(summary = "退出队伍")
    public BaseResponse<Boolean> quitTeam(
            @RequestBody TeamQuitDTO teamQuitDTO,
            HttpSession session
    ) {
        if (teamQuitDTO == null || teamQuitDTO.getTeamId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGGED_IN);
        }

        // (委托 Service)
        boolean result = teamService.quitTeam(teamQuitDTO, loginUser);

        return BaseResponse.success(result);
    }


    /**
     * 【【【 案卷 #005：SOP (邀请用户) 】】】
     * (SOP 1 契约: POST /api/team/invite)
     */
    @PostMapping("/invite")
    @Operation(summary = "邀请用户加入队伍")
    public BaseResponse<Boolean> inviteUser(
            @RequestBody TeamInviteDTO teamInviteDTO,
            HttpSession session
    ) {
        // 1. (校验)
        if (teamInviteDTO == null || teamInviteDTO.getTeamId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // (校验账号非空)
        if (StringUtils.isBlank(teamInviteDTO.getTargetUserAccount())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "目标账号不能为空");
        }

        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGGED_IN);
        }

        // 2. (委托) 呼叫“经理”(Service)
        boolean result = teamService.inviteUser(teamInviteDTO, loginUser);

        // 3. (返回)
        return BaseResponse.success(result);
    }

    /**
     * 【【【 案卷 #004：SOP (加入队伍) 】】】
     * (SOP 1 契约: POST /api/team/join)
     */
    @PostMapping("/join")
    @Operation(summary = "加入队伍")
    public BaseResponse<Boolean> joinTeam(

            // 1. (接收) 封装的“加入合约”
            @RequestBody TeamJoinDTO teamJoinDTO,
            HttpSession session
    ) {
        // 2. (校验)
        if (teamJoinDTO == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGGED_IN);
        }

        // 3. (委托) 呼叫“经理”(Service)
        // (我们将在 ServiceImpl 中实现所有 SOP 1 和 SOP 2 的决策)
        boolean result = teamService.joinTeam(teamJoinDTO, loginUser);

        // 4. (返回)
        return BaseResponse.success(result);
    }


    /**
     * 【【【 案卷 #18：SOP (搜索/分页) 】】】
     * (V3.x 裁决：使用 POST + DTO 来处理复杂搜索)
     */
    @PostMapping("/list/page")
    @Operation(summary = "搜索队伍 (分页)")
    public BaseResponse<Page<TeamVO>> searchTeams(

            // 1. (接收) 搜索合约
            @RequestBody TeamSearchDTO teamSearchDTO,
            HttpSession session
    ) {
        // 2. (校验)
        if (teamSearchDTO == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGGED_IN);
        }

        // 3. (委托) 呼叫“经理”(Service)
        // (我们将在下一步实现这个 Service 方法)
        Page<TeamVO> teamPage = teamService.searchTeams(teamSearchDTO, loginUser);

        // 4. (返回)
        return BaseResponse.success(teamPage);
    }

    /**
     * 【【 案卷 #17：SOP (获取队伍详情) 】】】
     */
    @GetMapping("/get/{id}")
    @Operation(summary = "按 ID 获取队伍详情")
    public BaseResponse<TeamVO> getTeamById(

            // 1. (接收) 路径参数
            @PathVariable("id") long teamId,
            HttpSession session // 2. (接收) 身份 (用于权限校验)
    ) {

        // 3. (校验)
        if (teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGGED_IN);
        }

        // 4. (委托) 呼叫“经理”(Service)
        // (我们将在下一步实现这个 Service 方法)
        TeamVO teamVO = teamService.getTeamDetails(teamId, loginUser);
        if (teamVO == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "队伍不存在或你无权查看");
        }

        // 5. (返回)
        return BaseResponse.success(teamVO); // 使用 ResultUtils 包装
    }


    /**
     * 【【 案卷 #16：SOP (创建队伍) 】】
     */
    @PostMapping("/create")
    @Operation(summary = "创建队伍")
    public BaseResponse<Long> createTeam(

            // 【【 3. 接收“包裹”和“身份” 】】
            @RequestBody TeamCreateDTO teamCreateDTO,
            HttpSession session
    ) {

        // 【【 4. SOP 校验 (前台) 】】

        // a. 检查“包裹”
        if (teamCreateDTO == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // b. 检查“身份”
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGGED_IN);
        }

        // 【【 5. 呼叫“经理”(Service) 】】
        // [修复] (只传递“变量名”，不传递“类型”)
        Long newTeamId = teamService.createTeam(teamCreateDTO, loginUser);

        // 【【 6. 返回“回执” 】】
        return BaseResponse.success(newTeamId);
    }

    // (我们将在“案卷 #17” 和 “#18” 中
    //  陆续添加“更新队伍”、“加入队伍”、“搜索队伍”等接口...)
}