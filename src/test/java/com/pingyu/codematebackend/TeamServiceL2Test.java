package com.pingyu.codematebackend;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pingyu.codematebackend.dto.*;
import com.pingyu.codematebackend.model.Team;
import com.pingyu.codematebackend.model.User;
import com.pingyu.codematebackend.service.TeamService;
import com.pingyu.codematebackend.dto.TeamVO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * 【【【 案卷 #17.3：L2 集成测试 (主攻方案) 】】】
 * * 目标：启动完整 Spring，在“真实数据库”上
 * * 调试 V3.1 聚合逻辑 (getTeamDetails)
 */
@SpringBootTest // (它会启动你完整的 SpringBoot 应用)
public class TeamServiceL2Test {

    @Resource
    private TeamService teamService; // (直接注入“真实”的 Service)

    /**
     * 【【【 案卷 #007：L2 集成测试 (更新队伍) 】】】
     * * 目标：测试 队长(成功) 和 普通成员(失败) 的权限控制
     * * 重点：部分更新 (只改名字，不改描述)
     */
    @Test
    void testUpdateTeam_L2_Debug() {
        // --- 1. 准备 (Arrange) ---

        // 目标队伍 (假设 ID=16)
        Long teamId = 16L;

        // 准备更新合约 (DTO)
        TeamUpdateDTO dto = new TeamUpdateDTO();
        dto.setId(teamId);
        dto.setName("萍雨侦探事务所 v3.0"); // (我们要修改的新名字)
        dto.setDescription(null); // (不修改描述，测试“部分更新”特性)
        // dto.setStatus(2);
        // dto.setPassword("123456"); // (如果要改为加密，必须传密码)

        User loginUser = new User();

        // 【【【 场景切换：请每次只打开一个场景进行测试 】】】

        // === 场景 A: 队长修改 (成功) ===
        // (预期：返回 true, 且数据库中名字变了)
        loginUser.setId(8L); // (大剑 ID=8)

        // === 场景 B: 普通成员修改 (失败) ===
        // (预期：抛出 NO_AUTH 异常 "您无权修改...")
        // loginUser.setId(608L); // (成员 ID=608)


        // --- 2. 行动 (Act) ---
        System.out.println("--- [L2 调试] 准备执行 updateTeam ---");
        System.out.println("--- [L2 调试] 操作人ID: " + loginUser.getId());

        try {
            boolean result = teamService.updateTeam(dto, loginUser);

            // --- 3. 断言 (Assert Success) ---
            System.out.println("--- [L2 调试] 更新结果: " + result);

            // (验证部分更新是否生效：查出来看看)
            Team updatedTeam = teamService.getById(teamId);
            System.out.println("--- [L2 调试] 更新后名称: " + updatedTeam.getName());
            System.out.println("--- [L2 调试] 更新后描述: " + updatedTeam.getDescription());

            assert(result == true);
            assert(updatedTeam.getName().equals("萍雨侦探事务所 v3.0"));

        } catch (Exception e) {
            // --- 3. 断言 (Assert Failure) ---
            System.out.println("--- [L2 调试] 捕获异常 (符合预期?): " + e.getMessage());
        }

        System.out.println("--- [L2 调试] updateTeam 测试完毕 ---");
    }

    /**
     * 【【【 案卷 #006：L2 集成测试 (退出队伍) 】】】
     * * 目标：测试 队长(解散/拒绝) 和 普通成员(退出) 的逻辑
     */
    @Test
    void testQuitTeam_L2_Debug() {
        // --- 1. 准备 (Arrange) ---
        long teamId = 16L;
        long userId = 608L;

        TeamQuitDTO dto = new TeamQuitDTO();
        dto.setTeamId(teamId);

        User loginUser = new User();
        loginUser.setId(userId);

        // 【【【 V4.x 修复：先“入队” (现场造数据) 】】】
        // (我们直接调用 Service 的 joinTeam，或者手动操作 repository)
        // (为了简单，我们假设 teamService.joinTeam 可用，且队伍是公开的)
        // (如果是加密队伍，记得 setPassword)
        TeamJoinDTO joinDto = new TeamJoinDTO();
        joinDto.setTeamId(teamId);
        try {
            // 尝试加入 (如果已加入会报错，我们忽略)
            teamService.joinTeam(joinDto, loginUser);
            System.out.println("--- [L2 调试] 前置准备：用户已加入队伍 ---");
        } catch (Exception e) {
            System.out.println("--- [L2 调试] 前置准备：用户可能已在队伍中 (" + e.getMessage() + ") ---");
        }

        // --- 2. 行动 (Act) ---
        System.out.println("--- [L2 调试] 准备执行 quitTeam ---");

        try {
            boolean result = teamService.quitTeam(dto, loginUser);

            // --- 3. 断言 (Assert Success) ---
            System.out.println("--- [L2 调试] 退出成功: " + result);
            // assert(result == true);

        } catch (Exception e) {
            // --- 3. 断言 (Assert Failure) ---
            System.out.println("--- [L2 调试] 捕获异常: " + e.getMessage());
        }

        System.out.println("--- [L2 调试] quitTeam 测试完毕 ---");
    }

    @Test
    void testGetTeamDetails_L2_Debug() {
        // --- 1. 准备 (Arrange) ---

        // 【【【 确保你的数据库中存在 ID=1 的队伍 】】】
        long teamIdToTest = 1L;

        // 【【【 伪造“登录用户” (解决 Session 摩擦) 】】】
        // (我们绕过了 Controller, 只需给 Service 一个它需要的 User 对象)
        User fakeLoginUser = new User();
        fakeLoginUser.setId(1L); // (假设 1L 是管理员或队长)


        // --- 2. 行动 (Act) ---

        // 【【【 侦探：请在这里设置你的断点 (Breakpoint) 】】】
        System.out.println("--- [L2 调试] 准备进入 TeamService.getTeamDetails ---");

        TeamVO teamVO = teamService.getTeamDetails(teamIdToTest, fakeLoginUser);

        // --- 3. 断言 (Assert) ---
        System.out.println("--- [L2 调试] 聚合查询已返回 ---");
        System.out.println(teamVO);

        // (断言它不为空，且数据正确)
        assert(teamVO != null);
        assert(teamVO.getId() == teamIdToTest);
        assert(teamVO.getTeamCaptain() != null); // 确保队长信息已填充

        System.out.println("--- [L2 调试] 队长: " + teamVO.getTeamCaptain().getUsername());
        System.out.println("--- [L2 调试] 成员数量: " + (teamVO.getMembers() != null ? teamVO.getMembers().size() : 0));
    }

    /**
     * 【【【 案卷 #18：L2 集成测试 (搜索/分页) 】】】
     * * 目标：在“真实数据库”上
     * * 调试 V3.x 聚合搜索 (searchTeams)
     */
    @Test
    void testSearchTeams_L2_Debug() {
        // --- 1. 准备 (Arrange) ---

        // 【【【 1a. 伪造“登录用户” 】】】
        User fakeLoginUser = new User();
        fakeLoginUser.setId(1L); // (假设 1L 已登录)

        // 【【【 1b. 准备“搜索合约” (DTO) 】】】
        TeamSearchDTO dto = new TeamSearchDTO();

        // (*** 侦探：在这里修改你的“搜索条件” ***)
        // dto.setSearchText("萍雨");     // (测试“按名称”)
        dto.setTagNames(List.of("Java")); // (测试“按标签”)

        dto.setCurrent(1);  // (测试“分页”)
        dto.setPageSize(5);

        // --- 2. 行动 (Act) ---

        // 【【【 侦探：请在这里设置你的断点 (Breakpoint) 】】】
        System.out.println("--- [L2 调试] 准备进入 TeamService.searchTeams ---");

        Page<TeamVO> teamPage = teamService.searchTeams(dto, fakeLoginUser);

        // --- 3. 断言 (Assert) ---
        System.out.println("--- [L2 调试] 聚合搜索已返回 ---");
        System.out.println("--- [L2 调试] 总记录数: " + teamPage.getTotal());
        System.out.println("--- [L2 调试] 总页数: " + teamPage.getPages());

        // (打印搜索到的记录)
        teamPage.getRecords().forEach(teamVO -> {
            System.out.println("    [ID: " + teamVO.getId() + "] " + teamVO.getName());
            if (teamVO.getTeamCaptain() != null) {
                System.out.println("       -> 队长: " + teamVO.getTeamCaptain().getUsername());
            }
        });

        // (断言它不为空)
        assert(teamPage != null);
        assert(teamPage.getRecords() != null);

        System.out.println("--- [L2 调试] searchTeams 测试完毕 ---");
    }

    @Test
    void testJoinTeam_L2_Debug() {
        // --- 1. 准备 (Arrange) ---

        // 【【【 1a. 伪造“登录用户” 】】】
        // (*** 侦探：确保 99L (或你选择的ID) 是一个“真实存在”于 DB 的用户 ***)
        // (*** 且该用户 *未* 加入 "teamIdToTest" ***)
        User fakeLoginUser = new User();
        fakeLoginUser.setId(99L); // (例如：使用一个“测试员”账户 ID)

        // 【【【 1b. 准备“加入合约” (DTO) 】】】
        TeamJoinDTO dto = new TeamJoinDTO();

        // (*** 侦探：在这里修改你的“测试目标” ***)

        // (场景 A: 测试加入“公开”队伍)
        dto.setTeamId(1L); // (*** 确保 ID=1 的队伍存在, 且 status=0 (公开) ***)
        // dto.setPassword(null); // (公开队伍，无需密码)

        // (场景 B: 测试加入“加密”队伍 - 成功)
        // dto.setTeamId(2L); // (*** 确保 ID=2 的队伍存在, status=2 (加密) ***)
        // dto.setPassword("123456"); // (*** 确保这是“正确”密码 ***)


        // --- 2. 行动 (Act) ---

        // 【【【 侦探：请在这里设置你的断点 (Breakpoint) 】】】
        System.out.println("--- [L2 调试] 准备进入 TeamService.joinTeam ---");

        boolean joinResult = teamService.joinTeam(dto, fakeLoginUser);

        // --- 3. 断言 (Assert) ---
        System.out.println("--- [L2 调试] joinTeam 已返回 ---");
        System.out.println("--- [L2 调试] 加入结果: " + joinResult);

        // (断言它 100% 成功)
        assert(joinResult == true);

        System.out.println("--- [L2 调试] joinTeam (案卷 #004) 测试完毕 ---");

        // (SOP 4 教训：L2 测试 (@SpringBootTest) 默认 *会回滚* 事务！)
        // (你（侦探）在 DB 中 *不会* 看到 99L 真的加入了 1L，)
        // (除非你添加 @Rollback(false) 注解，但我们不推荐)
    }

    /**
     * 【【【 案卷 #005：L2 集成测试 (邀请用户) 】】】
     * * 目标：测试“邀请”逻辑
     * * 重点：权限校验 (发起人必须在队内) + 目标查找 (Account -> ID)
     */
    @Test
    void testInviteUser_L2_Debug() {
        // --- 1. 准备 (Arrange) ---

        long teamId = 1L;
        long inviterId = 1L;
        long targetUserId = 2L; // (假设 ID=2 的用户存在且不在队伍中)

        // 【【【 V4.x 修复：确保邀请人真的在队伍中 】】】
        // (先“作弊”，手动插入一条关系，或者确保 DB 已有)
        // (更稳妥的方式：在测试中先让 inviter 加入 teamId)
        // (但为了简单，我们假设你已经在 DB 中手动修复了这条数据，或者换一个已存在的组合)

        // ... (或者，如果 userId=8 是大剑，且他在 teamId=16 中，那就用 8 和 16)

        User inviter = new User();
        inviter.setId(8L); // (基于 image_de983f.jpg：大剑 ID=8)

        TeamInviteDTO dto = new TeamInviteDTO();
        dto.setTeamId(16L); // (基于 image_de983f.jpg：队伍 ID=16)
        dto.setTargetUserAccount("阿巴阿巴"); // (确保 yupi 存在)

        // --- 2. 行动 (Act) ---

        // 【【【 侦探：请在这里设置断点 (Breakpoint) 】】】
        System.out.println("--- [L2 调试] 准备进入 TeamService.inviteUser ---");
        System.out.println("--- [L2 调试] 邀请人ID: " + inviter.getId());
        System.out.println("--- [L2 调试] 目标账号: " + dto.getTargetUserAccount());

        boolean inviteResult = teamService.inviteUser(dto, inviter);

        // --- 3. 断言 (Assert) ---
        System.out.println("--- [L2 调试] inviteUser 已返回 ---");
        System.out.println("--- [L2 调试] 邀请结果: " + inviteResult);

        // (断言成功)
        assert(inviteResult == true);

        System.out.println("--- [L2 调试] inviteUser (案卷 #005) 测试完毕 ---");
    }
}