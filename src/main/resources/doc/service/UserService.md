# 用户服务拆分方案 (Service Split Strategy)

**页面对应**：`个人中心页.md`, `登录与注册页.md`  
**阶段口径**：V1 先基于 `Spring + MyBatis-Plus + MySQL` 打通，不把 Redis 作为必选依赖。

---

## 1. 公开基础资料 (Public Profile)
> **特征**：所有人可见，包括未登录访客。  
> **V1 查询策略**：直接查 MySQL。

| 本次查询字段 | 来源表        | 建议查询方式 |
| :----------- | :------------ | :----------- |
| `uid`        | `t_user_info` | `selectOne by user_id` |
| `nickname`   | `t_user_info` | `selectOne by user_id` |
| `avatar`     | `t_user_info` | `selectOne by user_id` |
| `sign`       | `t_user_info` | `selectOne by user_id` |

---

## 2. 社交统计数据 (Social Stats)
> **特征**：变化较快，但 V1 不要求缓存。  
> **V1 查询策略**：直接查 MySQL，保证实现简单稳定。

| 本次查询字段          | 来源表        | 建议查询方式 |
| :-------------------- | :------------ | :----------- |
| `followerCount`       | `t_user_info` | `selectOne by user_id` |
| `followingCount`      | `t_user_info` | `selectOne by user_id` |
| `videoCount` (稿件数) | `t_video`     | `count(*) where user_id=? and status=0` |

---

## 3. 私有交互状态 (Private Interaction)
> **特征**：依赖当前登录用户，不能复用公共缓存。  
> **V1 查询策略**：实时查 MySQL。

| 本次查询字段 | 来源表        | 逻辑判断 |
| :----------- | :------------ | :------- |
| `isFollowed` | `t_following` | `exists(user_id=me, following_user_id=uid, status=0)` |
| `relation`   | 逻辑计算      | `0:无关系, 1:我关注他, 2:互相关注` |

---

## 4. V2 可选增强

- Redis 缓存公开资料与计数。
- 引入安全框架后再补充更完整的“私有状态”鉴权语义。
