# API接口设计

## 基础信息

- **Base URL**: `https://your-domain.com/api`
- **认证方式**: JWT Token (Header: `Authorization: Bearer {token}`)
- **响应格式**: JSON

## 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 错误响应
```json
{
  "code": 400,
  "message": "错误信息",
  "data": null
}
```

### 响应码说明
| code | 说明 |
|------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未登录 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器错误 |

---

## 用户模块

### 1. 微信登录
**POST** `/user/login`

**请求参数**:
```json
{
  "code": "wx_login_code"
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "token": "jwt_token",
    "userInfo": {
      "id": 1,
      "openid": "xxx",
      "nickname": "用户昵称",
      "avatar": "头像URL",
      "level": 5
    }
  }
}
```

### 2. 更新用户信息
**PUT** `/user/info`

**请求头**: `Authorization: Bearer {token}`

**请求参数**:
```json
{
  "nickname": "新昵称",
  "avatar": "头像URL"
}
```

### 3. 更新用户等级
**PUT** `/user/level`

**请求参数**:
```json
{
  "level": 6
}
```

---

## 活动模块

### 4. 创建活动
**POST** `/activity`

**请求参数**:
```json
{
  "name": "周六羽毛球局",
  "time": "2026-06-01T09:00:00",
  "location": "XX体育馆",
  "latitude": 30.1234567,
  "longitude": 120.1234567,
  "type": "doubles"
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "activityId": 1
  }
}
```

### 5. 获取活动列表
**GET** `/activity/list`

**查询参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| type | String | organized/joined/available/unavailable/search |
| keyword | String | 搜索关键词(可选) |

**响应**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "周六羽毛球局",
      "time": "2026-06-01T09:00:00",
      "location": "XX体育馆",
      "type": "doubles",
      "status": "registering",
      "currentPlayers": 8,
      "maxPlayers": 12,
      "organizerId": 1,
      "organizerName": "张三"
    }
  ]
}
```

### 6. 获取活动详情
**GET** `/activity/{id}`

**响应**:
```json
{
  "code": 200,
  "data": {
    "activity": { ... },
    "registrations": [ ... ],
    "matches": [ ... ],
    "userRegistration": { ... },
    "isOrganizer": true,
    "pendingCancelCount": 2
  }
}
```

### 7. 删除活动
**DELETE** `/activity/{id}`

### 8. 更新活动状态
**PUT** `/activity/{id}/status`

**请求参数**:
```json
{
  "status": "finished"
}
```

---

## 报名模块

### 9. 报名活动
**POST** `/registration/join`

**请求参数**:
```json
{
  "activityId": 1,
  "activityName": "周六羽毛球局",
  "nickname": "张三",
  "level": 5,
  "avatar": "avatar_url",
  "partnerId": null
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "registrationId": 1
  }
}
```

### 10. 取消报名
**POST** `/registration/cancel`

**请求参数**:
```json
{
  "activityId": 1,
  "activityName": "周六羽毛球局",
  "reason": "临时有事"
}
```

### 11. 处理取消请求
**POST** `/registration/handle-cancel`

**请求参数**:
```json
{
  "registrationId": 1,
  "action": "approve"
}
```

---

## 比赛模块

### 12. 开始分组
**POST** `/match/grouping`

**请求参数**:
```json
{
  "activityId": 1
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "teams": [ ... ],
    "matches": [ ... ]
  }
}
```

### 13. 开始比赛
**POST** `/match/start`

**请求参数**:
```json
{
  "activityId": 1,
  "matchId": 1
}
```

### 14. 提交比分
**POST** `/match/score`

**请求参数**:
```json
{
  "activityId": 1,
  "matchId": 1,
  "team1Score": 21,
  "team2Score": 15
}
```

### 15. 确认比分
**POST** `/match/confirm-score`

**请求参数**:
```json
{
  "activityId": 1,
  "matchId": 1,
  "confirmed": true
}
```

### 16. 获取比赛详情
**GET** `/match/{id}`

---

## 队伍模块

### 17. 设置队伍名称
**PUT** `/team/{id}/name`

**请求参数**:
```json
{
  "name": "旋风队"
}
```

---

## 挑战赛模块

### 18. 开始挑战赛
**POST** `/challenge/start`

**请求参数**:
```json
{
  "activityId": 1
}
```

### 19. 获取挑战赛数据
**GET** `/challenge/{activityId}`

### 20. 开始决赛
**POST** `/challenge/final`

**请求参数**:
```json
{
  "activityId": 1
}
```

---

## 排名模块

### 21. 获取排名
**GET** `/ranking/{activityId}`

**响应**:
```json
{
  "code": 200,
  "data": {
    "rankings": [
      {
        "rank": 1,
        "userId": 1,
        "nickname": "张三",
        "level": 6,
        "groupScore": 15,
        "challengeScore": 10,
        "finalScore": 15,
        "totalScore": 40
      }
    ]
  }
}
```

---

## AI模块

### 22. 发送AI消息
**POST** `/ai/chat`

**请求参数**:
```json
{
  "message": "帮我报名炽羽的局",
  "actionResult": null
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "content": "AI回复内容"
  }
}
```

### 23. 获取AI历史
**GET** `/ai/history`

### 24. 清空AI历史
**DELETE** `/ai/history`

---

## 规则模块

### 25. 获取规则
**GET** `/rules`

**响应**:
```json
{
  "code": 200,
  "data": [
    {
      "id": "singles",
      "name": "单打",
      "icon": "🏸",
      "match_rules": [ ... ],
      "scoring_rules": [ ... ],
      "common_rules": [ ... ]
    }
  ]
}
```

---

## 文件上传

### 26. 上传头像
**POST** `/file/avatar`

**请求**: `multipart/form-data`

**响应**:
```json
{
  "code": 200,
  "data": {
    "url": "https://oss.xxx.com/avatar.jpg"
  }
}
```

---

## 接口汇总

| 方法 | 路径 | 说明 | 对应云函数 |
|------|------|------|-----------|
| POST | /user/login | 微信登录 | login |
| PUT | /user/info | 更新用户信息 | update-user-info |
| PUT | /user/level | 更新等级 | update-user-level |
| POST | /activity | 创建活动 | create-activity |
| GET | /activity/list | 活动列表 | get-activities |
| GET | /activity/{id} | 活动详情 | get-activity-detail |
| DELETE | /activity/{id} | 删除活动 | delete-activity |
| PUT | /activity/{id}/status | 更新状态 | update-activity-status |
| POST | /registration/join | 报名 | join-activity |
| POST | /registration/cancel | 取消报名 | cancel-registration |
| POST | /registration/handle-cancel | 处理取消 | handle-cancel-request |
| POST | /match/grouping | 分组 | start-grouping |
| POST | /match/start | 开始比赛 | start-match |
| POST | /match/score | 提交比分 | submit-score |
| POST | /match/confirm-score | 确认比分 | confirm-score |
| GET | /match/{id} | 比赛详情 | get-match-detail |
| PUT | /team/{id}/name | 队伍名称 | set-team-name |
| POST | /challenge/start | 挑战赛 | start-challenge |
| GET | /challenge/{activityId} | 挑战赛数据 | get-challenge-data |
| POST | /challenge/final | 决赛 | start-final |
| GET | /ranking/{activityId} | 排名 | get-rankings |
| POST | /ai/chat | AI聊天 | ai-chat |
| GET | /ai/history | AI历史 | ai-history |
| DELETE | /ai/history | 清空历史 | ai-history |
| GET | /rules | 规则 | get-rules |
| POST | /file/avatar | 上传头像 | - |
