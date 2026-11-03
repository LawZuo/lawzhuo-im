# 私聊消息接口（PrivateMessageController）

- 基础路径：`/message/private`
- 鉴权：**需要登录**，发送者 `sendId` 取自 accessToken
- 发送流程：参数校验 → 好友关系校验 → 分配会话序列号 → 落库 → 投递到 im-server 的 Redis 队列 → 由长连接推送给接收方

---

## 1. 发送私聊消息

`POST /message/private/send`

### 请求体 `PrivateMessageDTO`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `localId` | string | 是 | 前端生成的本地消息 id（雪花算法），用于去重/重发对账 |
| `recvId` | long | 是 | 接收方用户 id |
| `content` | string | 是 | 消息内容。文字消息 ≤1024 字符；非文字类型要求是合法 JSON（如图片消息的 `{"url":"...","width":100}`） |
| `type` | int | 是 | 消息类型，见下表 |

```json
{
  "localId": "2115370982613516290",
  "recvId": 2115342677164277762,
  "content": "在吗？",
  "type": 0
}
```

### 消息类型 `type`

| 值 | 含义 |
|---|---|
| 0 | 文字消息 |
| 10 | 撤回 |
| 80 | 新增好友 |
| 81 | 删除好友 |
| 82 | 好友在线状态变化 |
| 83 | 好友免打扰 |
| 84 | 好友会话置顶 |

> 编码规则：0-9 需落库的真实消息、10-19 状态类、20-29 提示类、30-39 UI 交互、40-49 通话类、50-60 后台操作、80-89 好友变化、90-99 群聊变化、100+ RTC 信令。
> 当前枚举只实现了 `0`、`10`、`80`~`84`。

### 响应 `R<PrivateMessageVO>`

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | long | 消息 id（雪花 id，超出 JS 安全整数范围，建议按字符串处理） |
| `localId` | string | 回传前端生成的本地 id |
| `seqNo` | long | 会话内连续递增序号，用于消息排序与去重 |
| `sendId` | long | 发送者 id |
| `recvId` | long | 接收者 id |
| `content` | string | 消息内容 |
| `type` | int | 消息类型 |
| `status` | int | 消息状态，发送成功后为 `0`（等待推送） |
| `sendTime` | long | 发送时间，**毫秒时间戳** |
| `deleted` | boolean | 是否删除 |

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 2115371098234567088,
    "localId": "2115370982613516290",
    "seqNo": 1,
    "sendId": 2115365991718952961,
    "recvId": 2115342677164277762,
    "content": "在吗？",
    "type": 0,
    "status": 0,
    "sendTime": 1793178718921,
    "deleted": false
  },
  "timestamp": 1793178718921
}
```

### 消息状态 `status`

| 值 | 含义 |
|---|---|
| 0 | 等待推送（已落库，尚未送达） |
| 1 | 已送达（未读） |
| 2 | 撤回 |
| 3 | 已读 |

### 失败场景

| 场景 | HTTP | code | message |
|---|---|---|---|
| `localId` / `recvId` / `content` / `type` 为空 | 400 | 400 | 如「本地消息id不可为空」「接收用户id不可为空」 |
| `content` 超过 1024 字符 | 400 | 400 | 内容长度不得大于1024 |
| 已不是好友 | 200 | 500 | 您已不是对方好友，无法发送消息 |
| 非文字类型 content 不是合法 JSON | 200 | 500 | 消息格式异常 |

> ✅ 当前用户已改为 `SecurityUtils.getSession()`（JWT claims），发送链路可正常执行。

### 实现细节

- **会话 key**：`convKey = min(sendId, recvId) + "_" + max(sendId, recvId)`，保证同一对用户的会话 id 唯一且与谁先发起无关
- **序号分配**：Redis `INCR` 计数器（按 convKey）拿 `seqNo`；计数器不存在时先查库取该会话最大 `seqNo` 再 +1，并用 Redisson 锁防并发（`saveMessage` 上的 `@RedisLock`，按 `convKey` 加锁）
- **落库与推送**：先落库（状态 `0`），再通过 `im-client` 的 `IMSender` 找到接收方所在的 im-server 节点，把消息投递到该节点的 Redis 队列（`im:message:private:{serverId}`）
- **离线**：接收方不在线时进入离线消息分支（离线消息保留 60 天）

> ⚠️ **当前链路未闭环**：im-server 侧还没有消费 Redis 队列并把消息写回 WS 连接的逻辑，所以消息虽然落库成功，接收方暂时收不到。详见 [websocket-api.md](websocket-api.md)。
