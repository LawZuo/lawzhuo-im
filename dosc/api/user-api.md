# 用户接口（UserController）

- 基础路径：`/api/user`
- 鉴权：**需要登录**（`Authorization: Bearer {accessToken}`）

## UserVO 字段

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | long | 用户 id |
| `userName` | string | 账号，≤20 字符 |
| `nickName` | string | 昵称，≤20 字符 |
| `sex` | int | 性别 0 男 / 1 女 |
| `type` | int | 用户类型 1 普通 / 2 审核账户 |
| `signature` | string | 个性签名，≤128 字符 |
| `headImage` | string | 头像地址 |
| `headImageThumb` | string | 头像缩略图 |
| `online` | boolean | 是否在线（由 `imClient.isOnline` 实时计算） |
| `isBanned` | boolean | 是否被封禁 |
| `reason` | string | 封禁原因 |

---

## 1. 修改密码

`PUT /api/user/update-pwd`

### 请求体 `ModifyPwdDTO`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `oldPassword` | string | 是 | 旧密码 |
| `newPassword` | string | 是 | 新密码 |

```json
{ "oldPassword": "Passw0rd!", "newPassword": "NewPassw0rd!" }
```

### 响应

```json
{ "code": 200, "message": "操作成功", "data": null, "timestamp": 1793178718921 }
```

> ⚠️ **当前实现为空方法**（仅打印日志），调用会返回成功但不修改密码。

---

## 2. 获取当前登录用户

`GET /api/user/self`

无请求参数，用户身份取自 accessToken。

### 响应 `R<UserVO>`

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 2115342677164277762,
    "userName": "chat1",
    "nickName": "会话1",
    "sex": 0,
    "type": 1,
    "signature": "",
    "headImage": "",
    "headImageThumb": "",
    "online": true,
    "isBanned": false,
    "reason": ""
  },
  "timestamp": 1793178718921
}
```

> ✅ 已修复：`/self` 不再绑定不存在的 `@PathVariable`，当前用户直接取 `SecurityUtils.getUserId()`（JWT 的 `sub`）。

---

## 3. 查询用户在线终端

`GET /api/user/terminal/online?userIds=1,2,3`

### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `userIds` | string | 是 | 用户 id 列表字符串，逗号分隔 |

### 响应 `R<List<OnlineTerminalVO>>`

| 字段 | 类型 | 说明 |
|---|---|---|
| `userId` | long | 用户 id |
| `terminals` | int[] | 在线终端类型集合：0 web / 1 app / 2 pc |

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    { "userId": 2115342677164277762, "terminals": [0, 2] }
  ],
  "timestamp": 1793178718921
}
```

> ⚠️ **当前实现返回空数组** `[]`，未接入 Redis 在线状态。

---

## 4. 根据 id 查询用户

`GET /api/user/find/{id}`

### 路径参数

| 参数 | 类型 | 说明 |
|---|---|---|
| `id` | long | 用户 id |

### 响应 `R<UserVO>`

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { "id": 2115342678234567088, "userName": "chat2", "nickName": "会话2", "online": false },
  "timestamp": 1793178718921
}
```

| 场景 | HTTP | code | message |
|---|---|---|---|
| id 非数字 | 400 | 400 | 参数类型错误: id |
| 用户不存在 | 200 | 500 | 系统繁忙，请稍后再试（`BeanUtils.copyProperties` 空指针，属已知问题） |

---

## 5. 修改用户信息

`PUT /api/user/update`

### 请求体 `UserVO`

`id`、`userName`、`nickName` 必填，其余可选（只更新非空字段）。

```json
{
  "id": 2115342677164277762,
  "userName": "chat1",
  "nickName": "新昵称",
  "signature": "这个人很懒",
  "sex": 0,
  "headImage": "https://cdn.example.com/a.png"
}
```

### 响应

```json
{ "code": 200, "message": "操作成功", "data": null, "timestamp": 1793178718921 }
```

> ⚠️ **当前实现为空方法**（仅打印日志）。

---

## 6. 搜索用户

`GET /api/user/search?name=会话`

### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `name` | string | 是 | 昵称/账号关键字 |

### 响应 `R<List<UserVO>>`

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [ { "id": 2115342678234567088, "userName": "chat2", "nickName": "会话2" } ],
  "timestamp": 1793178718921
}
```

> ⚠️ **当前实现返回空数组** `[]`。
