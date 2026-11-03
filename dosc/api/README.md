# LawIM 接口文档

## 服务一览

| 服务 | 模块 | 协议 | 默认地址                  | 说明 |
|---|---|---|---------------------------|---|
| 业务 API | `im-platform` | HTTP/JSON | `http://localhost:19090`  | 登录、用户、好友、私聊消息 |
| 长连接网关 | `im-server` | WebSocket | `ws://localhost:19091/im` | Netty 实现，收发 IM 指令（登录 / 心跳 / 消息推送） |

## 通用约定

### 1. 鉴权

除白名单外的所有接口都要求请求头携带 accessToken：

```http
Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...
```

- `accessToken` 由 `POST /api/auth/login` 返回，默认有效期 **1800 秒**
- 过期后用 `PUT /api/auth/refreshToken` 换新（`refreshToken` 默认 **604800 秒**）
- 白名单：`/api/auth/**`、`/public/**`

未携带 / 无效 / 过期时返回：

```json
{ "code": 401, "message": "未登录或登录已过期", "data": null, "timestamp": 1793178718921 }
```

### 2. 统一响应结构 `R<T>`

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1793178718921
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `code` | int | 业务状态码，见下表 |
| `message` | string | 提示信息 |
| `data` | T | 业务数据，可能为 `null` |
| `timestamp` | long | 服务端时间戳（毫秒） |

### 3. 状态码

| code | HTTP | 含义 |
|---|---|---|
| 200 | 200 | 成功 |
| 400 | 400 | 参数校验失败 |
| 401 | 401 | 未登录 / token 无效或过期 |
| 403 | 403 | 无访问权限 |
| 404 | 404 | 资源不存在 |
| 405 | 405 | 请求方法不支持 |
| 415 | 415 | Content-Type 不支持 |
| 500 | 200 或 500 | 业务失败（如"密码错误"）或服务器异常 |

> 说明：
> - **业务失败**（`R.fail()`，如「账号已存在」「对方已经是你的好友」）HTTP 仍为 `200`，`code` 为 `500`；
> - **业务异常**（`throw new GlobalException(...)`）HTTP `200`，`code`/`message` 取自异常本身（如密码错误 `code=10001`）；
> - **框架级错误**（参数校验、请求体格式、404、405、415）HTTP 状态与 `code` 一致。

### 4. Token 结构

accessToken / refreshToken 都是 **RS256 签名的 JWT**（`Authorization: Bearer <accessToken>`）：

| claim | 说明 |
|---|---|
| `iss` | `im-platform` |
| `sub` | userId（字符串） |
| `exp` / `iat` | 过期时间 / 签发时间 |
| `token_type` | `access` 或 `refresh`（**用 refreshToken 调业务接口会被拒，反之亦然**） |
| `terminal` | 登录终端 0 web / 1 app / 2 pc |
| `userName` / `nickName` | 登录时的用户信息快照 |

- 签发方（im-platform）持有 RSA 私钥，校验方（im-platform 资源服务器、im-server）只用公钥
- 用 `https://jwt.io` 或任意 JWT 库都能解出上述 claims

### 5. 时间格式

- 普通字段：`yyyy-MM-ddTHH:mm:ss.SSS+08:00`（ISO-8601）
- 标注了 `DateToLongSerializer` 的字段（如私聊消息的 `sendTime`）：**毫秒时间戳**（`long`）

## 模块索引

| 文档 | 对应 Controller | 基础路径 |
|---|---|---|
| [auth-api.md](auth-api.md) | `AuthController` | `/api/auth` |
| [user-api.md](user-api.md) | `UserController` | `/api/user` |
| [friend-api.md](friend-api.md) | `FriendController` | `/api/friend` |
| [PrivateMessage-api.md](PrivateMessage-api.md) | `PrivateMessageController` | `/message/private` |
| [websocket-api.md](websocket-api.md) | `im-server` Netty 网关 | `ws://.../im` |

## 模块状态一览

| 模块 | 接口 | 状态 |
|---|---|---|
| 认证 | 登录 / 注册 / 刷新 token | ✅ 可用 |
| 用户 | 改密码 / 查用户 / 搜索 | ✅ 可用（`/self`、`/terminal/online` 有缺陷，见文档） |
| 好友 | 列表（增量）/ 详情 / 添加 | ✅ 可用 |
| 好友 | 删除 / 改备注 / 置顶 / 在线终端 | ❌ 未开放（service 方法被注释） |
| 私聊 | 发送消息 | ✅ 可用（落库成功；WS 推送仍未闭环） |
| 长连接 | 登录指令 | ✅ 校验 token、绑定 channel、写在线状态，回 `{"cmd":0}` |
| 长连接 | 心跳指令 | ✅ 回 `{"cmd":1}`（TTL 续期待接入） |
| 长连接 | 消息推送 | ⚠️ 队列消费未实现 |
