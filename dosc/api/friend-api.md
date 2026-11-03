# 好友接口（FriendController）

- 基础路径：`/api/friend`
- 鉴权：**需要登录**（`Authorization: Bearer {accessToken}`）
- 当前用户身份由 `SecurityUtils.getSession()` 从 JWT claims 还原（`sub` / `terminal` / `userName` / `nickName`），不接受前端传 userId
- 当前只暴露 **3 个接口**：列表（增量同步）、详情、添加
- 数据模型：好友关系是**双向两条记录**（A→B、B→A 各一条），添加好友时由服务端一起写入

---

## FriendVO

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | long | **好友的用户 id**（不是关系记录 id） |
| `nickName` | string | 好友昵称 / 备注 |
| `headImage` | string | 好友头像（取的是缩略图 `head_image_thumb`） |
| `isDnd` | boolean | 是否免打扰 |
| `isTop` | boolean | 是否置顶会话 |
| `deleted` | boolean | 是否已删除（增量同步时用于通知客户端移除） |
| `version` | long | 关系记录的版本号，用于增量同步 |

---

## 1. 好友列表（增量同步）

`GET /api/friend/list?version=0`

### 请求参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `version` | long | 否 | `0` | 客户端上次同步到的最大版本号；传 `0` 表示全量 |

### 响应 `R<List<FriendVO>>`

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 2115342677164277762,
      "nickName": "会话1",
      "headImage": "",
      "isDnd": false,
      "isTop": false,
      "deleted": false,
      "version": 12
    }
  ],
  "timestamp": 1793675764442
}
```

### 语义要点

- 查询条件是 `user_id = 当前用户 AND version > :version`，**不过滤已删除的记录**——被删除的好友会以 `deleted: true` 返回，客户端据此删除本地缓存，这是增量同步的常规做法
- 结果**没有排序**（SQL 未加 `order by`），需要稳定顺序请客户端自己按 `isTop` / `nickName` 排
- 版本号是**全局递增**的（Redis `im:friend:max_version`），所有用户共用同一个计数器，因此 `version` 只适合做「变更检测」，不能当好友序号用

---

## 2. 好友详情

`GET /api/friend/find/{friendId}`

### 路径参数

| 参数 | 类型 | 说明 |
|---|---|---|
| `friendId` | long | 好友的用户 id |

### 响应 `R<FriendVO>`

结构同上（单个对象）。

### 失败场景

| 场景 | HTTP | code | message |
|---|---|---|---|
| 不是好友 | 200 | 500 | 对方不是您的好友 |
| `friendId` 非数字 | 400 | 400 | 参数类型错误: friendId |

> 注意：该查询同样不过滤 `deleted`，所以「已删除的好友」仍会返回 `deleted: true`，不会抛「不是好友」。

---

## 3. 添加好友

`POST /api/friend/add?friendId=123`

### 请求参数（**query string，不是 JSON body**）

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `friendId` | long | 是 | 要添加的好友用户 id |

### 响应

```json
{ "code": 200, "message": "操作成功", "data": null, "timestamp": 1793675763509 }
```

### 服务端行为

1. 校验不能加自己
2. **双向绑定**：写入 `(当前用户 → friendId)` 和 `(friendId → 当前用户)` 两条关系记录，两条都取新的全局版本号
3. 已存在的关系走 `saveOrUpdate` → **天然幂等**，重复调用不会插入重复行，也不会报错
4. 成功后给**双方**各推一条提示消息：

```json
{
  "id": 2117455745851691008,
  "sendId": 2117455637126926337,
  "recvId": 2117455637907066881,
  "content": "你们已成为好友，现在可以开始聊天了",
  "type": 21,
  "status": 0
}
```

`type=21`（提示类消息），通过 IM 长连接推送。

### 失败场景

| 场景 | HTTP | code | message |
|---|---|---|---|
| 添加自己 | 200 | 500 | 不允许添加自己为好友 |
| 缺少 `friendId` | 400 | 400 | 缺少参数: friendId |

> `@RepeatSubmit` 注解（防重复提交）已标注在方法上，但**切面尚未实现**（全仓库没有 `@Aspect`），当前不会拦截重复请求；不过由于第 3 点的幂等性，重复提交不会产生脏数据。

---

## 版本号机制

```
getNextVersion():
  Redis 有 im:friend:max_version → INCR 后返回
  Redis 没有 → 取 DB 中 max(version) + 1，写回 Redis 后返回
```

用途就是上面第 1 个接口的增量拉取：客户端保存已同步的最大 `version`，下次只拉变更。

---

## ✅ 实测结果

| 场景 | 结果 |
|---|---|
| `GET /api/friend/list` | `200` + 好友数组 |
| `GET /api/friend/list?version=<当前最大 version>` | `200` + `[]`（无增量） |
| `GET /api/friend/find/{friendId}` | `200` + `FriendVO` |
| `POST /api/friend/add?friendId=x` | `200`，DB 中生成**双向两条**记录（version 递增） |
| 重复添加同一好友 | `200`，不新增记录（`saveOrUpdate` 幂等） |
| 添加自己 | `200` + `{"code":500,"message":"不允许添加自己为好友"}` |
| 缺少 `friendId` | `400` + `{"code":400,"message":"缺少参数: friendId"}` |
| 无 token / token 无效 | `401` + `{"code":401,...}` |

一次完整验证后的数据库状态（测试数据已清理）：

```
im_friend
  A -> B  deleted=0  version=3
  B -> A  deleted=0  version=4

im_private_message
  A -> B  type=21（打招呼提示）
  A -> B  type=0 （普通文字消息）
```

---

## 暂未开放的接口

`FriendService` 里这些方法存在但**没有对外暴露**（部分被注释）：

| 能力 | 状态 |
|---|---|
| 删除好友（`delFriend`） | 已注释，无接口 |
| 修改备注 / 免打扰 / 置顶（`setTop`、`FriendUpdateDTO`、`FriendTopDTO`） | 无接口，DTO 已备好但没用上 |
| 好友在线终端（`findOnlineTerminals`） | 已注释，无接口 |
| 好友上线状态推送（`sendOnlineStatus`） | 方法体只有日志，TODO |
| 判断是否好友（`isFriend`） | 内部方法，供私聊发送前校验 |

---

## 调用示例

```bash
# 1. 登录拿 token
TOKEN=$(curl -s -X POST http://localhost:19090/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"chat1","password":"Passw0rd!","terminal":0}' | jq -r .data.accessToken)

# 2. 全量拉好友
curl http://localhost:19090/api/friend/list -H "Authorization: Bearer $TOKEN"

# 3. 增量拉（只取 version > 12 的变更）
curl "http://localhost:19090/api/friend/list?version=12" -H "Authorization: Bearer $TOKEN"

# 4. 加好友（参数在 query 上）
curl -X POST "http://localhost:19090/api/friend/add?friendId=2115344235406905345" \
  -H "Authorization: Bearer $TOKEN"

# 5. 查好友详情
curl http://localhost:19090/api/friend/find/2115344235406905345 -H "Authorization: Bearer $TOKEN"
```

---

## 与旧版文档的差异

之前记录的 `PUT /api/friend`（改备注/置顶/免打扰）、`DELETE /api/friend/{friendId}`（删除好友）、`GET /api/friend/online`（在线终端）在当前代码里**已经不存在**（controller 已精简，service 方法被注释），前端不要再按旧接口调用。
