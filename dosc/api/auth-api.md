# 认证接口（AuthController）

- 基础路径：`/api/auth`
- 鉴权：**无需登录**（`SecurityConfig` 放行 `/api/auth/**`）
- 终端类型 `terminal`：`0` web、`1` app、`2` pc

---

## 1. 登录

`POST /api/auth/login`

### 请求体

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `terminal` | int | 是 | 登录终端：0 web / 1 app / 2 pc，取值 0~2 |
| `username` | string | 是 | 账号 |
| `password` | string | 是 | 密码（明文传输，服务端用 BCrypt 校验） |

```json
{
  "terminal": 0,
  "username": "chat1",
  "password": "Passw0rd!"
}
```

### 响应 `R<LoginVO>`

| 字段 | 类型 | 说明 |
|---|---|---|
| `accessToken` | string | 访问令牌，后续所有请求放在 `Authorization: Bearer {accessToken}` |
| `accessTokenExpiresIn` | int | accessToken 有效期（秒），默认 1800 |
| `refreshToken` | string | 刷新令牌，仅用于换新 token |
| `refreshTokenExpiresIn` | int | refreshToken 有效期（秒），默认 604800（7 天） |

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "accessToken": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
    "accessTokenExpiresIn": 1800,
    "refreshToken": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
    "refreshTokenExpiresIn": 604800
  },
  "timestamp": 1793178718921
}
```

### 失败场景

| 场景 | HTTP | code | message |
|---|---|---|---|
| 参数缺失/格式错误 | 400 | 400 | 如「账号不能为空」「登录终端类型取值范围:0,2」 |
| 密码错误 | 200 | **10001** | 密码不正确 |
| 账号不存在 | 200 | 500 | 系统繁忙，请稍后再试（先 `user.getPassword()` 空指针，属已知缺陷） |

> 说明：`GlobalException` 由统一异常处理器接管，HTTP 200，`code`/`message` 取自异常本身；`new GlobalException(String)` 的 `code` 固定为 `500`，要精确业务码需用 `new GlobalException(ResultCode.XXX)`。
>
> ⚠️ 登录目前**不校验封禁状态**（只有 `refreshToken` 校验了 `isBanned`）。

---

## 2. 注册

`POST /api/auth/register`

### 请求体

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `nickName` | string | 是 | 昵称，≤20 字符 |
| `username` | string | 是 | 账号，≤20 字符 |
| `password` | string | 是 | 密码，5~20 字符 |

```json
{
  "nickName": "会话1",
  "username": "chat1",
  "password": "Passw0rd!"
}
```

### 响应 `R<String>`

`data` 为新建用户的 id（字符串形式，雪花 id 超出 JS 安全整数范围，故用字符串）：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "2115370982613516290",
  "timestamp": 1793178718656
}
```

### 失败场景

| 场景 | HTTP | code | message |
|---|---|---|---|
| 账号已存在 | 200 | 500 | 账号已存在 |
| 参数不合法 | 400 | 400 | 如「密码长度必须在5-20个字符之间」 |

---

## 3. 刷新 Token

`PUT /api/auth/refreshToken`

### 请求头

| 头 | 必填 | 说明 |
|---|---|---|
| `refreshToken` | 是 | 登录时拿到的 refreshToken（注意：**不是** `Authorization` 头） |

```http
PUT /api/auth/refreshToken
refreshToken: eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...
```

### 响应 `R<LoginVO>`

与登录响应结构相同，返回**新的** accessToken 和 refreshToken（旧 refreshToken 不续期，客户端需整体替换）。

### 失败场景

| 场景 | HTTP | code | message |
|---|---|---|---|
| refreshToken 缺失 | 400 | 400 | 缺少请求头: refreshToken |
| refreshToken 无效或过期 | 200 | **401** | 您的登录信息已过期，请重新登录 |
| 用户不存在 | 200 | 500 | 系统繁忙，请稍后再试 |
| 账号被封禁 | 200 | 500 | 您的账号因'xxx'被管理员封禁,请联系客服! |

> 刷新成功后会清除 Redis 里残留的拒绝访问标记 `im:user:denied:{userId}`（防解封后仍在黑名单）。

---

## 时序建议

```
POST /api/auth/login          → 保存 accessToken / refreshToken
      ↓ accessToken 过期（1800s）
PUT  /api/auth/refreshToken   → 换新 token（携带 refreshToken 头）
      ↓ refreshToken 过期（7d）
POST /api/auth/login          → 重新登录
```

> 业务接口返回 `401`（`未登录或登录已过期`）时，客户端应先用 refreshToken 刷新，刷新失败再跳登录页。
