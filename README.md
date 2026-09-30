# 友链自助提交 Next

> 基于 Halo 官方 PluginLinks 的前台申请增强插件，提供申请弹窗、验证码、网站信息预览和主题集成。

友链自助提交 Next 面向需要开放“申请友链”入口的 Halo 站点。它负责前台交互和网站信息提取，正式申请、审核、通知与友链数据由 Halo 官方 `PluginLinks` 插件负责。

## 重要边界

> 本插件不是友链存储插件，也不会替代 Halo 官方 `PluginLinks` 的链接管理页面、数据模型和正式友链数据。

本插件不会建立第二套正式友链数据，也不会替代官方链接管理页面。旧版本遗留申请记录仅保留只读兼容查询；新申请直接调用 `PluginLinks >= 2.3.0` 提供的验证码和申请接口。

## 核心能力

### 前台申请增强

- 在 `/links` 页面自动注入 `LinkSubmitWidget`，可配置是否显示浮动“提交友链”按钮。
- 调用官方申请接口，收集网址、标题、Logo、RSS、描述、回链和邮箱。
- 使用官方 CAPTCHA 接口，避免在插件内重复实现验证码和匿名提交限流。
- 通过后端代理获取网站标题、描述和 favicon，避免浏览器跨域限制。

### 管理入口与兼容

- 后台增强页提供返回官方链接管理、打开前台友链页和增强设置入口。
- 新申请、审核、邮件和定时清理由官方 `PluginLinks` 统一提供，避免重复通知与重复数据模型；本插件不再注册旧版通知模板或旧版定时任务页面。

## 环境要求与依赖

| 项目 | 要求 |
| --- | --- |
| Halo | `>= 2.26.0` |
| 官方 PluginLinks | `>= 2.3.0`，必需 |
| Java | 21，源码构建需要 |

请先安装并启用官方 [PluginLinks](https://www.halo.run/store/apps/app-hfbQg)。没有它时，本插件不会自行建立第二套友链数据。

本项目目前尚未创建 Halo 应用商店条目，因此源码 manifest 不填商店 `app-id`。发布到商店后只会写入平台分配的真实 ID，不会复用上游或占位 ID。

## 安装与启用

1. 下载 `link-submit-next-<version>.jar`。
2. 在 Halo 控制台进入“插件管理”，上传 JAR；插件默认启用，若已被手动停用请重新启用“友链自助提交 Next”。
3. 确认官方 PluginLinks 已启用，并在官方插件中完成申请和审核配置。
4. 打开本插件设置，选择是否加载前台资源、是否显示默认提交按钮和是否允许网站信息预览。
5. 访问站点 `/links` 页面验证申请入口；需要自定义按钮时可调用 `LinkSubmitWidget.open()`。

## 设置说明

| 设置组 | 用途 |
| --- | --- |
| 前台增强 | 是否加载 Widget、显示提交按钮和网站信息预览 |
| 申请与审核 | 由官方 PluginLinks 管理 |
| 消息通知 | 由官方 PluginLinks 管理 |

关闭“加载插件资源”后，主题可以自行提供按钮和样式；申请、审核、通知和正式友链数据仍由官方 PluginLinks 负责。

## 主题集成

默认情况下，插件只在 `/links` 路由注入脚本与样式，避免在全站加载提交组件。适配主题可以使用以下调用打开已经注入的表单：

```html
<button type="button" onclick="LinkSubmitWidget.open()">申请友链</button>
```

NUCMA 主题已适配该入口。主题不应复制提交表单、审核流程或官方友链存储逻辑。

## 安全与运行说明

- 匿名接口只开放配置读取和站点信息预览；申请和验证码由官方 PluginLinks 接口负责，审核需要 Halo 权限。
- 网站信息抓取会由服务器访问申请者填写的网址。请按站点的网络策略使用，并为超时或不可访问站点保留手动填写入口。
- 申请、审核和邮件通知依赖官方 PluginLinks 及 Halo 的通知能力；未配置时，本插件不会自行创建替代流程。

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
- 本项目是对上游友链提交插件的二次开发与重构：适配 `PluginLinks >= 2.3.0`，规范命名空间、保留主题集成与网站信息预览，并保持申请、审核、通知和正式友链数据由官方 PluginLinks 管理。
- 当前维护仓库：[chenhufei/plugin-link-submit-next](https://github.com/chenhufei/plugin-link-submit-next)。

## 反馈与许可

- 仓库：<https://github.com/chenhufei/plugin-link-submit-next>
- 问题反馈：<https://github.com/chenhufei/plugin-link-submit-next/issues>
- 许可证：GPL-3.0
