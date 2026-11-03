# 武大教务课表接口说明

正方新版教务（`jwgl.whu.edu.cn`）课表查询接口。本文记录 Hamenda 从教务系统导入课表所需的请求/响应格式。

## 1. 登录与会话

课表接口依赖教务系统的登录会话，两种方式二选一：

**方式 A：内嵌 WebView 登录（Hamenda 采用）**

1. WebView 打开 `https://jwgl.whu.edu.cn/xtgl/login_slogin.html`
2. 用户在教务页面自行登录，成功后 URL 落到 `/xtgl/index*`
3. 会话 cookie 保存在 WebView 的 CookieManager 中，后续请求复用同一会话

**方式 B：CAS 单点登录**

```http
GET https://cas.whu.edu.cn/authserver/login?service=https%3A%2F%2Fjwgl.whu.edu.cn%2Fsso%2Fjznewsixlogin
Cookie: <CAS 会话 cookie>
```

- 返回页包含「教学管理信息服务平台」→ 登录成功
- 跳转 URL 含 `ReAuth` → 需要重新认证
- 否则视为登录失败

## 2. 课表请求

```http
POST https://jwgl.whu.edu.cn/kbcx/xskbcx_cxXsgrkb.html?gnmkdm=N2151
User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36
Content-Type: application/x-www-form-urlencoded
Cookie: <教务会话 cookie>

validate=sl<随机串>&xnm=2024&xqm=3&xzlx=ck
```

参数说明：

| 参数 | 位置 | 说明 |
|---|---|---|
| `gnmkdm` | query | 功能模块号，课表固定 `N2151` |
| `xnm` | body | 学年起始年，如 `2024` |
| `xqm` | body | 学期编码，见下表 |
| `xzlx` | body | 固定 `ck` |
| `validate` | body | 客户端生成的随机串，格式见下 |

学期编码映射：

| 学期 | `xqm` |
|---|---|
| 秋季学期 | `3` |
| 春季学期 | `12` |
| 小学期 | `16` |

`validate` 生成方式（任意同等格式的随机串即可）：

```js
"sl" + Math.floor(1e10 * Math.random()).toString(36) + Date.now().toString(36)
```

## 3. 响应格式

成功时返回 JSON：

```json
{
  "xsxx": { "XH": "2021302111001" },
  "kbList": [
    {
      "kcmc": "大学物理",
      "jxbmc": "PHYS001",
      "xqj": "5",
      "cdmc": "教四 401",
      "jcs": "5-6",
      "xm": "王老师",
      "zcmc": "主讲",
      "kcxz": "公共基础必修",
      "xf": "4",
      "zcd": "1-16周"
    }
  ]
}
```

字段说明：

| 字段 | 类型 | 含义 |
|---|---|---|
| `xsxx.XH` / `xsxx.XH_ID` | string | 学号 |
| `kbList[].kcmc` | string | 课程名称 |
| `kbList[].jxbmc` | string | 教学班名称，可作课程唯一标识 |
| `kbList[].xm` | string | 教师姓名 |
| `kbList[].zcmc` | string | 教师职称 |
| `kbList[].kcxz` | string | 课程性质（必修/选修等） |
| `kbList[].xf` | string | 学分 |
| `kbList[].xqj` | string | 星期，`1`–`7`，`7` 表示周日 |
| `kbList[].jcs` | string | 节次，如 `"5-6"` |
| `kbList[].cdmc` | string | 上课地点 |
| `kbList[].zcd` | string | 周次描述，如 `"1-16周"`、`"1-16周(单)"` |

所有数值字段均以字符串返回，解析时需自行转换；可选字段可能缺失。

## 4. 字段解析规则

**星期 `xqj`**

- 取值 `1`–`7`，其中 `7` 为周日
- 若内部约定 0 表示周日，则 `xqj === 7` 时映射为 `0`，其余保持不变

**节次 `jcs`**

- 形如 `"5-6"`，按 `-` 拆分得到起止节
- 单节可能只返回一个数字，需兼容

**周次 `zcd`**

- 多段以 `,`、`，`、`、` 分隔
- 每段可能形如 `"1-16周"`、`"1-16周(单)"`、`"3-5周(双)"`、`"第3周"` 等
- 去掉 `(` `)` `（` `）`、`第`、`周` 后按 `-` 解析起止周
- 段内标注 `单`/`双` 时，按星期几的奇偶过滤周次
- 最终入库时通常取最小周为 `week_from`、最大周为 `week_to`；单双周如需精确表示，应展开为逐周记录

## 5. 注意事项

- 请求必须携带有效会话 cookie，否则会返回登录页 HTML，`JSON.parse` 失败
- `kbList` 中同一门课可能有多条（不同时间/地点），按 `jxbmc` 归并
- 未选课或学期为空时，`kbList` 可能为空数组或不存在
- 换学校时接口路径可能带 `/jwglxt` 前缀，`gnmkdm` 也可能不同
- `validate` 为客户端生成，服务端不校验其唯一性
