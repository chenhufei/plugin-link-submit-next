# 友链自助提交 Next

> 基于 Halo 官方 PluginLinks 的友链自助提交与审核插件，提供表单、健康检测、预览、通知和定时处理。

友链自助提交 Next 面向需要开放“申请友链”入口的 Halo 站点。它负责收集申请、审核和辅助检测；通过审核后的正式友链仍由 Halo 官方 `PluginLinks` 插件存储和管理。

## 重要边界

> 本插件不是友链存储插件，也不会替代 Halo 官方 `PluginLinks` 的链接管理页面、数据模型和正式友链数据。

申请记录保存在本插件中，用于审核与通知。审核通过后，插件通过官方公开模型将正式链接写入 `PluginLinks`；定时健康检查也只消费和处理官方插件已有的友链数据。

## 核心能力

### 访客提交

- 在 `/links` 页面自动注入 `LinkSubmitWidget`，可配置是否显示浮动“提交友链”按钮。
- 支持新增和更新两类申请，收集网址、标题、Logo、RSS、描述、留言、原网址和邮箱。
- 通过后端代理获取网站标题、描述和 favicon，避免浏览器跨域限制。
- 可选链接健康检测，检查可达性与 SSL 状态；提交记录会保存检测结果。
- 提供按 IP 的每日提交上限和重复提交保护。

### 审核与消息

- Console 审核页支持按状态、类型和排序方式筛选申请记录，并查看提交详情。
- 可选自动审核；新增申请通过后由官方 `PluginLinks` 创建正式友链。
- 可选邮件通知覆盖新申请、审核结果和管理员提醒。
- 申请者填写邮箱时可以接收审核结果与拒绝说明。

### 友链维护任务

- 可创建定时任务检查官方 `PluginLinks` 中的友链可达性。
- 支持按小时、每天、每周、每月或 Spring Cron 表达式运行。
- 可以跳过指定分组，并将无法访问的友链删除或移动到指定分组。
- 定时任务默认关闭，启用前请确认删除或移动策略符合站点管理规则。

## 环境要求与依赖

| 项目 | 要求 |
| --- | --- |
| Halo | `>= 2.25.0` |
| 官方 PluginLinks | `>= 2.0.0`，必需 |
| Java | 21，源码构建需要 |

请先安装并启用官方 [PluginLinks](https://www.halo.run/store/apps/app-hfbQg)。没有它时，本插件不会自行建立第二套友链数据。

## 安装与启用

1. 下载 `link-submit-next-<version>.jar`。
2. 在 Halo 控制台进入“插件管理”，上传 JAR 并启用“友链自助提交 Next”。
3. 确认官方 PluginLinks 已启用，并至少创建需要开放申请的友链分组。
4. 打开插件设置，选择默认分组、是否自动审核、每日提交上限和检测策略。
5. 访问站点 `/links` 页面验证申请入口；需要自定义按钮时可调用 `LinkSubmitWidget.open()`。

## 设置说明

| 设置组 | 用途 |
| --- | --- |
| 提交审核 | 是否加载 Widget、显示提交按钮、自动审核、提交上限、健康检测和网站预览 |
| 消息通知 | 审核人与申请人的邮件通知 |
| 友链分组 | 默认写入分组和禁止自助提交的分组 |

关闭“加载插件资源”后，主题可以自行提供按钮和样式；提交、审核、检测和官方友链写入逻辑仍由插件后端负责。

## 主题集成

默认情况下，插件只在 `/links` 路由注入脚本与样式，避免在全站加载提交组件。适配主题可以使用以下调用打开已经注入的表单：

```html
<button type="button" onclick="LinkSubmitWidget.open()">申请友链</button>
```

NUCMA 主题已适配该入口。主题不应复制提交表单、审核流程或官方友链存储逻辑。

## 安全与运行说明

- 匿名接口只开放分组、配置读取、站点信息预览和提交所需能力；审核需要 Halo 权限。
- 网站信息抓取与健康检测会由服务器访问申请者填写的网址。请按站点的网络策略使用，并为超时或不可访问站点保留合理的审核流程。
- 邮件通知依赖 Halo 已配置的通知 / 邮件能力；未配置时，申请和审核主流程仍可使用。
- 定时任务可能删除或移动官方友链，建议先在测试分组中验证规则。

## 从源码构建

```bash
./gradlew.bat test
./gradlew.bat build -x test
```

完整插件包输出到：

```text
build/libs/link-submit-next-<version>.jar
```

Console、公开 Widget 和 API Client 均由 Gradle 关联任务生成和打包，不要手工修改构建生成物。

## 来源与致谢

- 上游项目：[chengzhongxue/link-submit](https://github.com/chengzhongxue/link-submit)。原作者为困困鱼（GitHub [chengzhongxue](https://github.com/chengzhongxue)）。
- 上游 README 还注明部分代码由 [柳意梧情](https://github.com/liuyiwuqing) 提供，本项目保留这一致谢。
- 本项目是对上游友链提交插件的二次开发与重构：适配 `PluginLinks >= 2.0.0`、规范命名空间、完善审核和通知、增加健康检测与定时处理，并保持正式友链数据由官方 PluginLinks 管理。
- 当前维护仓库：[chenhufei/plugin-link-submit-next](https://github.com/chenhufei/plugin-link-submit-next)。

## 反馈与许可

- 仓库：<https://github.com/chenhufei/plugin-link-submit-next>
- 问题反馈：<https://github.com/chenhufei/plugin-link-submit-next/issues>
- 许可证：GPL-3.0
