# 友链申请增强

> 基于 Halo 官方 PluginLinks 的前台申请增强插件：访客在官方申请表单填写网址后，可一键获取网站标题、描述和 Logo 并自动填充。

本插件只做一件事——把“逐个字段手抄网站信息”变成一次点击。申请表单本身、提交、验证码、限流、审核、通知和正式友链数据全部由 Halo 官方 `PluginLinks` 插件负责。

## 重要边界

> 本插件不是友链存储插件，也不会替代 Halo 官方 `PluginLinks` 的链接管理页面、数据模型和正式友链数据。

- 不注册任何 Extension，不建立第二套申请数据，不提供审核或通知流程。
- 不渲染申请表单、不采集验证码、不提交申请；表单由主题按官方接口渲染。

## 核心能力

- 在 `/links` 页面识别官方申请表单（`form[action*="links/apply/submit"]`），在网址字段旁注入“获取信息”按钮。
- PluginLinks 2.5 起访客申请改走 `api.link.halo.run` 的 POST 接口，表单 `action` 不再固定；脚本额外按字段特征（`url` + `displayName` + `logo`）兜底识别，两种契约都能注入按钮。
- 通过服务端安全代理读取目标网站的 `og:title`、`og:description`、`og:image`（并回退到 `meta[name=description]`、`link[rel~=icon]`、`/favicon.ico`），避免浏览器跨域限制。
- 只填充空白字段和上一次自动填入的值，访客手改过的内容不会被覆盖。
- 提供后台开关：可关闭增强脚本，或单独关闭网站信息获取接口。
- 官方链接管理页提供一个非关键的“增强设置”快捷入口；审核仍在官方链接管理中完成。

## 环境要求与依赖

| 项目 | 要求 |
| --- | --- |
| Halo | `>= 2.27.0` |
| 官方 PluginLinks | `>= 2.3.0`，必需；已在 2.5（REST 申请契约）上验证 |
| Java | 21，源码构建需要 |

请先安装并启用官方 [PluginLinks](https://www.halo.run/store/apps/app-hfbQg)。没有它时，本插件不会自行建立第二套友链数据。

本项目目前尚未创建 Halo 应用商店条目，因此源码 manifest 不填商店 `app-id`。发布到商店后只会写入平台分配的真实 ID，不会复用上游或占位 ID。

## 安装与启用

1. 下载 `link-submit-next-<version>.jar`。
2. 在 Halo 控制台进入“插件管理”，上传 JAR；插件默认启用。
3. 确认官方 PluginLinks 已启用，并在官方插件中完成申请和审核配置。
4. 打开本插件设置，按需保留“加载增强脚本”和“自动获取网站信息”。
5. 访问站点 `/links` 页面，在官方申请表单的网址字段旁确认“获取信息”按钮可用。

## 设置说明

| 设置组 | 用途 |
| --- | --- |
| 前台增强 | 是否加载增强脚本、是否允许服务端获取网站信息 |
| 申请表单与提交 | 由官方 PluginLinks 与主题负责，本插件不提供 |
| 申请与审核、消息通知 | 由官方 PluginLinks 管理 |

两个开关的作用范围：

- 关闭“加载增强脚本”后不再注入按钮；`site-info` 接口仍存在但不再被前台调用。
- 关闭“自动获取网站信息”后，注入脚本带 `data-link-preview-enabled="false"`，按钮不再出现，接口直接返回 403。

## 主题集成

主题负责按官方接口渲染申请表单，字段名使用官方约定：`url`、`displayName`、`logo`、`description`、`email`、`backlink`、`feedUrls`、`captchaCode`，提交到 `POST /links/apply/submit`（携带 `_csrf`）。插件注入的元素如下，主题可据此定制样式：

```html
<span class="link-enhance-tools">
  <button type="button" class="link-enhance-fetch" data-link-enhance-fetch="true">获取信息</button>
  <span class="link-enhance-status" role="status" aria-live="polite"></span>
</span>
```

被增强的表单会带上 `data-link-enhanced="true"`；状态节点通过 `data-status="idle|pending|success|error"` 表示当前结果。NUCMA 主题已适配该结构。

主题不应复制提交表单的校验逻辑，也不应另建一套友链存储或审核流程。

## 公开接口

| 方法与路径 | 用途 |
| --- | --- |
| `GET /apis/api.link.submit.halo.run/v1alpha1/site-info?url=` | 服务端代理获取网站标题、描述与 Logo |

接口匿名可用，但受服务端保护：仅允许公网 HTTP(S) 地址、逐跳重定向重新校验并固定 DNS 解析、限制响应体大小与超时、按访问来源 IP 限流（每分钟 12 次），超限返回符合 RFC 7807 的 `429` 并带 `Retry-After`。

## 安全与运行说明

- 网站信息抓取会由服务器访问申请者填写的网址。请按站点的网络策略使用，并为超时或不可访问站点保留手动填写入口。
- 匿名接口只开放站点信息预览，不提供任何写入能力；申请、验证码、审核和邮件通知依赖官方 PluginLinks 及 Halo 的通知能力。
- 自动填充的标题、描述和 Logo 只是访客填写的便利输入，仍会经过官方申请校验与人工审核。

## 从源码构建

```bash
./gradlew.bat test
./gradlew.bat build -x test
```

完整插件包输出到：

```text
build/libs/link-submit-next-<version>.jar
```

Console bundle 和增强脚本运行时资源均由 Gradle 关联任务生成和打包，不要手工修改构建生成物。

## 参与开发

三仓（主题 + 成员插件 + 本插件）共用一套协同流程，**动手前先读**
<https://gitee.com/xiui-cc/theme-nucma/blob/main/CONTRIBUTING.md>：

- 改前先同步；协作者不推 `main`，一律开分支提 PR。
- 只改源码，`static/**` 与 `link-submit-widget.iife.js` 由构建任务生成。
- 自检 `./gradlew cleanTest test`，打包 `./gradlew.bat build -x test`，出了 jar 才算完成。
- 版本四处一致：`build.gradle` + `gradle.properties` + `plugin.yaml` + `packages/link-submit-widget/package.json`；会进包才升版本。
- 只推 Gitee，GitHub 由镜像自动同步。

## 来源与致谢

- 上游项目：[chengzhongxue/link-submit](https://github.com/chengzhongxue/link-submit)。原作者为困困鱼（GitHub [chengzhongxue](https://github.com/chengzhongxue)）。
- 上游 README 还注明部分代码由 [柳意梧情](https://github.com/liuyiwuqing) 提供，本项目保留这一致谢。
- 本项目是对上游友链提交插件的二次开发与重构：把自建申请弹窗、验证码和提交流程收缩为仅保留服务端网站信息获取，申请、审核、通知和正式友链数据改由官方 `PluginLinks` 提供。
- 当前维护仓库：[chenhufei/plugin-link-submit-next](https://github.com/chenhufei/plugin-link-submit-next)。

## 反馈与许可

- 仓库（主）：<https://gitee.com/xiui-cc/plugin-link-submit-next>
- 镜像：<https://github.com/chenhufei/plugin-link-submit-next>
- 问题反馈：<https://gitee.com/xiui-cc/plugin-link-submit-next/issues>
- 许可证：GPL-3.0
