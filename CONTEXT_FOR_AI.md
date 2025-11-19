# CodeMate 协作宪法 (Context for AI)

## 1. 项目现状 (Backend)
- **技术栈**: Java 17/21 + Spring Boot 3.x + MyBatis Plus
- **核心依赖**: Swagger/Knife4j (文档), Redisson (分布式锁), Hutool/Apache Commons
- **数据库**: MySQL + Redis

## 2. 关键规范 (Protocol)
- **响应格式**: 必须严格使用静态方法 `BaseResponse.success(data)` 或 `BaseResponse.error(code, msg)`。
- **异常处理**: 业务逻辑错误请抛出 `BusinessException`，由全局异常处理器捕获。
- **Session Key**: 用户登录态统一使用 key: `"loginUser"` (禁止使用 "user_login_state")。
- **Controller**: 必须添加 `@Tag` 和 `@Operation` 注解以便生成接口文档。
- **DTO封装**: 禁止 Controller 层直接使用散参数，所有 POST 请求必须封装为 DTO。
- **测试规范**: 单元测试必须在 IDEA 的 `src/test/java` 目录下进行（如 `TeamServiceL2Test`），统一使用 `@SpringBootTest` 启动容器进行集成测试。

## 3. 已完成功能 (基于 Git Log)
- [x] 获取队伍详情 (Get Details) - `bbdb143`
- [x] 搜索队伍 (Search) - `acbdfaa`
- [x] 加入队伍 (Join) - `a30b604`
- [x] 邀请用户 (Invite) - `5f6b763`
- [x] 退出队伍 (Quit) - `45c8efb`
- [x] 更新队伍 (Update) - `52bd93a` (当前版本锚点)

## 4. 当前任务 (Current Task)
- **案卷 #008**: 踢出成员 (Kick Member)
- **状态**:
  - 后端: Controller, Service, Mapper 已就位 (待测试验证)。
  - 前端: 待对接 API, UI 按钮逻辑。
- **要求**:
  - 权限校验: 仅队长可操作。
  - 业务校验: 不能踢自己，目标必须在队内。
  - 事务: 涉及 `user_team` 表删除，需开启事务。