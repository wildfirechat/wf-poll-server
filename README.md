# wf-poll-server 投票功能服务端

基于 Spring Boot + JPA 实现的聊天软件投票功能模块。

## 功能特性

- ✅ 创建投票（单选/多选、实名/匿名、群内/公开）
- ✅ 参与投票（一人一票，防重复投票）
- ✅ 投票前隐藏结果（防刷票机制）
- ✅ 多选投票支持（限制最大选项数）
- ✅ 实名投票支持导出明细
- ✅ 提前结束投票
- ✅ 群组权限检查（降级策略保证可用性）

## 技术栈

- Spring Boot 2.7.14
- Spring Data JPA
- H2 Database（开发）/ MySQL（生产）
- 野火 IM SDK（认证和群组检查）

## 快速开始

### 启动项目

```bash
cd wf-poll-server
mvn spring-boot:run
```

启动后访问：http://localhost:8088

H2控制台：http://localhost:8088/h2-console
- JDBC URL: `jdbc:h2:file:./poll`
- 用户名: `sa`
- 密码: 空

## API 接口

### 1. 创建投票

```http
POST /api/polls
Headers:
  authCode: xxx
Body:
{
  "groupId": "2001",           // 群组ID（visibility=1时必填）
  "title": "周末去哪里聚餐？",   // 投票标题（必填）
  "description": "请大家投票",   // 描述（可选）
  "options": ["火锅", "烧烤", "日料", "西餐"],  // 选项（2-10个，必填）
  "visibility": 1,             // 1=仅群内, 2=公开
  "type": 1,                   // 1=单选, 2=多选
  "maxSelect": 1,              // 多选时最多选几项（type=2时有效）
  "anonymous": 0,              // 0=实名, 1=匿名
  "endTime": 1708500000000     // 截止时间（毫秒时间戳，可选）
}
```

### 2. 获取投票详情

```http
GET /api/polls/{pollId}?groupId=2001
Headers:
  authCode: xxx
```

**响应说明：**
- 投票前未投票：返回选项列表，不返回票数
- 投票后/发起者/投票结束：返回完整结果统计

**响应字段：**
```json
{
  "code": 0,
  "data": {
    "id": 1001,
    "title": "周末去哪里聚餐？",
    "type": 1,                   // 1=单选, 2=多选
    "anonymous": 0,              // 0=实名, 1=匿名
    "status": 0,                 // 0=进行中, 1=已结束
    "totalVotes": 25,            // 总票数（多选时可能大于人数）
    "voterCount": 15,            // 投票人数（去重后的实际人数）
    "hasVoted": false,           // 当前用户是否已投票
    "isCreator": false,          // 当前用户是否是创建者
    "options": [...],
    "voterDetails": [...]        // 仅实名投票且是创建者时返回
  }
}
```

### 3. 参与投票

```http
POST /api/polls/{pollId}/vote
Headers:
  authCode: xxx
Body:
{
  "groupId": "2001",
  "optionIds": [1, 2]           // 单选传1个，多选可传多个
}
```

**限制：**
- 单选：只能选 1 个选项
- 多选：不能超过 `maxSelect` 限制
- 一人只能投一次（不能追加投票）

### 4. 结束投票

```http
POST /api/polls/{pollId}/close
Headers:
  authCode: xxx
Body:
{
  "groupId": "2001"
}
```

**说明：** 仅投票创建者可以结束投票

### 5. 导出投票明细

```http
POST /api/polls/{pollId}/export
Headers:
  authCode: xxx
```

**说明：** 
- 仅实名投票的创建者可导出
- 返回投票人详情列表

### 6. 删除投票

```http
POST /api/polls/{pollId}/delete
Headers:
  authCode: xxx
```

**说明：** 仅投票创建者可以删除

### 7. 获取我的投票列表

```http
GET /api/polls?groupId=2001
Headers:
  authCode: xxx
```

**说明：** 返回当前用户创建的所有投票

## 错误码

| 错误码 | 说明 |
|--------|------|
| 4000 | 投票不存在 |
| 4001 | 投票已结束 |
| 4002 | 已经投过票了 |
| 4003 | 选项不存在 |
| 4004 | 选择数量超出限制 |
| 4005 | 仅群成员可参与此投票 |
| 4006 | 无权操作此投票 |
| 4007 | 选项数量错误（2-10个）|
| 4008 | 匿名投票无法导出明细 |
| 4009 | 投票已过期 |

## 业务规则

### 可见性规则

| 场景 | 可见内容 |
|------|----------|
| 投票前 + 未投票 | 仅选项列表（隐藏票数）|
| 投票后/发起者/已结束 | 选项 + 票数统计 + 百分比 |

### 权限规则

| 功能 | 仅群内投票 | 公开投票 |
|------|-----------|----------|
| 查看 | 群成员 | 任何人（需登录）|
| 投票 | 群成员 | 登录用户 |
| 结束 | 发起者 | 发起者 |
| 导出 | 发起者 | 发起者 |
| 删除 | 发起者 | 发起者 |

### 防刷票机制

1. **投票前不可见结果**：防止跟风投票
2. **一人一票**：数据库唯一约束 `(poll_id, user_id, option_id)`
3. **截止时间控制**：过期后不能投票
4. **提前结束**：发起者可手动结束投票

## 消息通知机制

| 时机 | 消息类型 | 接收方 | 说明 |
|------|----------|--------|------|
| 创建投票 | Type 18 | 群组 | 发送投票卡片消息 |
| 结束投票 | Type 19 | 创建者（单聊）| 发送投票结果 |
| 结束投票 | Type 19 | 群组（仅群内投票）| 发送投票结果 |

## 配置说明

### application.properties

```properties
# 服务器端口
server.port=8088

# H2 数据库（开发环境）
spring.datasource.url=jdbc:h2:file:./poll
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update

# MySQL 数据库（生产环境）
# spring.datasource.url=jdbc:mysql://localhost:3306/wfpoll
# spring.datasource.username=root
# spring.datasource.password=password
# spring.jpa.hibernate.ddl-auto=update

# 野火 IM 服务器配置
im.admin_url=http://localhost:18080
im.admin_secret=123456

# 投票通知用户ID（发送投票结果消息的账号）
poll.notification.user_id=system
```

## 打包部署

```bash
mvn clean package
java -jar target/wf-poll-server-1.0.0.jar
```

## 相关文档

- [详细设计文档](POLL_DESIGN.md) - 包含完整的数据模型、接口设计和多端实现指南
