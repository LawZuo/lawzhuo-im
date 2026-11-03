# WebSocket 接口（im-server / Netty 网关）

- 服务：`im-server`（Netty 实现，非 Spring WebSocket）
- 地址：`ws://localhost:18090/im`
- 端口来自 `im-server` 的 `websocket.port`（默认 18090），路径固定为 `/im`
- 协议：**WebSocket 文本帧**，内容为 JSON

---

## 1. 帧格式

客户端与服务端收发统一的 `IMSendInfo` 结构：

| 字段 | 类型 | 说明 |
|---|---|---|
| `cmd` | int | 指令类型，见下表 |
| `data` | object | 指令数据，可为空对象 |

```json
{ "cmd": 0, "data": { "accessToken": "eyJ0eXAi...", "devId": "web-001" } }
```

### 指令类型 `cmd`（`IMCmdType`）

| 值 | 名称 | 方向 | 说明 |
|---|---|---|---|
| 0 | `LOGIN` | C → S | 登录，建立用户会话 |
| 1 | `HEART_BEAT` | C ↔ S | 心跳，服务端原样回应 |
| 2 | `FORCE_LOGOUT` | S → C | 强制下线 |
| 3 | `PRIVATE_MESSAGE` | S → C | 私聊消息推送 |
| 4 | `GROUP_MESSAGE` | S → C | 群消息（未使用） |
| 5 | `SYSTEM_MESSAGE` | S → C | 系统消息（未使用） |

---

## 2. 建立连接

```
ws://localhost:18090/im
```

连接建立后**必须先发登录指令**，服务端不会主动推送任何内容。

---

## 3. 登录（C → S）

### 请求帧

```json
{
  "cmd": 0,
  "data": {
    "accessToken": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
    "devId": "web-001"
  }
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `accessToken` | string | 是 | 来自 `POST /api/auth/login` 的 accessToken |
| `devId` | string | 否 | 设备标识，用于区分同一用户的不同设备 |

### 服务端处理

1. 用 `jwt.public-key` 验签（RS256），并校验 `iss`、`exp`、`token_type=access`（服务端不持有私钥）
2. 校验失败：**直接关闭连接**，不返回任何帧
3. 校验通过：从 token 的 `info` 声明里解析 `IMSessionInfo { userId, terminal }`
4. 查询 Redis `im:user:server_id:{userId}:{terminal}` 判断该用户当前挂在哪个 im-server 节点

### 响应帧

登录成功会收到一帧响应：

```json
{ "cmd": 0, "data": null }
```

token 无效时不会返回帧，直接关闭连接（`code=1000`）。

> ⚠️ 当前实现只做校验和日志：没有把 `channel` 与 `userId/terminal` 绑定、没有写入 Redis 在线状态、也没有做封禁校验。

---

## 4. 心跳（C ↔ S）

### 请求帧

```json
{ "cmd": 1, "data": {} }
```

`data` 结构为空的 `IMHeartbeatInfo`，传 `{}` 即可。

### 响应帧

```json
{ "cmd": 1, "data": null }
```

### 心跳间隔建议

服务端在 pipeline 上挂了 `IdleStateHandler(60, 0, 0)`，即 **60 秒读空闲**。建议客户端 **30 秒**发一次心跳。

> ⚠️ 服务端当前**没有处理 `IdleStateEvent`**（`IMChannelHandler` 未重写 `userEventTriggered`），所以即使超时也不会主动断开；等后续补上后，心跳超过 60 秒的客户端会被踢下线。

---

## 5. 私聊消息推送（S → C）

### 推送帧

```json
{
  "cmd": 3,
  "data": {
    "cmd": 3,
    "sender": { "id": 2115365991718952961, "terminal": 0 },
    "receivers": [ { "id": 2115342677164277762, "terminal": 0 } ],
    "sendResult": true,
    "serviceName": "im-platform",
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
    }
  }
}
```

外层是 `IMSendInfo`，`data` 是 `IMRecvInfo`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `cmd` | int | 与外层 `cmd` 一致（3） |
| `sender` | object | 发送方 `{id, terminal}` |
| `receivers` | object[] | 接收方列表 `[{id, terminal}]` |
| `sendResult` | boolean | 是否需要回推发送结果 |
| `serviceName` | string | 发起方服务名（用于回执路由） |
| `data` | object | 业务消息体，即 `PrivateMessageVO` |

> ⚠️ **当前未实现**：im-server 还没有消费 Redis 私聊队列（`im:message:private:{serverId}`）并把消息写回对应 `channel` 的逻辑，所以这一帧暂时收不到。

---

## 6. 客户端示例（浏览器）

```js
const ws = new WebSocket('ws://localhost:18090/im');

ws.onopen = () => {
  // 1. 先登录
  ws.send(JSON.stringify({
    cmd: 0,
    data: { accessToken: 'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...', devId: 'web-001' }
  }));

  // 2. 每 30 秒心跳（服务端读空闲 60 秒）
  setInterval(() => {
    if (ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ cmd: 1, data: {} }));
    }
  }, 30_000);
};

ws.onmessage = (e) => {
  const frame = JSON.parse(e.data);
  switch (frame.cmd) {
    case 1:  /* 心跳响应 */ break;
    case 3:  /* 私聊消息 frame.data.data */ break;
    case 2:  /* 强制下线 */ ws.close(); break;
  }
};

ws.onclose = () => {
  /* 重连后需重新发送 cmd=0 登录 */
};
```

---

## 7. 实现状态（重要）

| 能力 | 状态 | 说明 |
|---|---|---|
| 握手 / 路径 `/im` | ✅ | `WebSocketServerProtocolHandler("/im")` |
| 文本帧 ↔ `IMSendInfo` 解码 | ✅ | `MessageProtocolDecoder`（`data` 会被解析成 `LinkedHashMap`） |
| 文本帧编码 | ✅ | `MessageProtocolEncoder` 输出 `TextWebSocketFrame` |
| 登录指令 | ✅ | 校验 token、绑定 channel、写 Redis 在线状态、推送上线事件、回 `{"cmd":0}` |
| 心跳指令 | ✅ | 回 `{"cmd":1}`（心跳续期 Redis TTL 仍待接入） |
| 私聊推送 | ❌ | 队列消费未实现 |
| 强制下线 | ❌ | 未实现 |
| 空闲踢人 | ❌ | 有 `IdleStateHandler(60s)`，但无 `userEventTriggered` 处理 |
| 封禁拦截 | ❌ | 登录处有 `TODO`，Redis 里已预留 `im:user:denied` 标记 |

**在当前服务器代码下联调，客户端能做的只有：建立连接、发登录帧（无响应）、观察日志。** 上线前需补齐上表 ❌ / ⚠️ 项。
