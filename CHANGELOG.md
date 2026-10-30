# 更新日志

## [2.0.1] - 2026-10-30

### Changed
- 「获取信息」按钮改为网址输入框右侧的小地球图标（`mdi:web-refresh`，与官方友链后台编辑链接弹窗一致），带「获取网站信息」提示与请求中转圈状态，不再占用表单下方单独一行。
- `site-info` 解析顺序对齐官方后台 `link.detail`：`<title>` / `meta[name=description]` / `link[rel~=icon]` 优先，`og:*` 仅在标准标签缺失时兜底；Logo 不再优先取 `og:image`。

### Verification
- 组件单测 14 项通过；主题契约检查与完整包构建结果见交付说明。

## [2.0.0] - 2026-10-29

### Removed
- 删除自建申请弹窗（Lit `LinkSubmitWidget` 组件、`var.css` 主题变量）以及插件内的验证码调用与申请提交逻辑：表单、提交、验证码、匿名限流、审核与通知统一由官方 `PluginLinks` 和主题负责。
- 删除旧版 `LinkSubmit` / `CronLinkSubmit` 扩展模型、对应 Console 列表页与 CSV 工具、OpenAPI 生成客户端。
- 删除前台浮动“提交友链”按钮和 `GET configuration` 匿名接口；设置项从 3 个开关减为 2 个（移除 `displayTheSubmitButton`）。

### Added
- 新增官方表单增强脚本 `link-apply-enhance.iife.js`：识别 `form[action*="links/apply/submit"]`，在网址字段旁注入“获取信息”按钮，调用 `site-info` 后只填充空白或上一次自动填入的 `displayName` / `logo` / `description`，并给出 `role="status"` 的无障碍反馈。
- 注入脚本带 `data-link-preview-enabled`，后台关闭“自动获取网站信息”时不再出现按钮，接口返回 403。

### Changed
- `AnonymousEndpoint` 仅保留 `GET site-info`，安全边界（公网地址校验、逐跳重定向复核、DNS 固定解析、响应体与超时限制、按来源 IP 限流与 RFC 7807 错误）保持不变。
- 页脚注入器更名为 `LinkApplyEnhanceFooterProcessor`，只输出一个脚本标签。
- 增强边界与运行时资源校验任务改为断言新契约：不得出现 `LinkSubmit` 数据模型、提交写入 API、`link-applications`/验证码调用或旧弹窗资源。

### Compatibility
- 仍要求 Halo `>= 2.27.0` 与官方 `PluginLinks >= 2.3.0 & < 3.0.0`。
- 破坏性变更：升级后前台申请入口必须由主题按官方接口渲染申请表单（NUCMA 主题已适配），不再存在 `LinkSubmitWidget.open()`。

## [1.2.9] - 2026-10-12

### Changed
- 网站信息抓取使用 Halo 2.26 的安全 HTTP 客户端与响应体大小过滤器，同时保留 DNS 固定解析和逐跳公网地址校验。

### Verification
- Java 测试通过；完整 JAR 打包待执行。

## [1.2.8] - 2026-09-22

### Changed
- 加强网站信息抓取的公网地址校验，逐个重定向重新解析并拒绝保留地址段、IPv4-mapped IPv6 和本机主机名。
- 保留官方 PluginLinks 申请边界与原有安全限制。

### Verification
- 通过 Java 测试、Console lint/type checks、Widget 构建、测试和 lint。

## [1.2.6] - 2026-08-20

### Changed
- 插件安装后默认启用；仍要求 Halo 官方 `PluginLinks >= 2.3.0`。
- 保持申请、审核、通知、限流、清理和正式友链数据由官方链接插件负责，本插件仅提供前台增强与旧记录只读兼容。
- 前端 Console 与 Widget 增加不修改源码的 lint 质量门禁，并忽略 OpenAPI 生成代码的格式噪声。
- 同步 Gradle、插件 manifest、Widget 包版本，并校验 `gradle.properties` 版本一致性。

### Verification
- 通过 Java 测试、Console 类型检查、Console lint、Widget 构建、Widget 测试、Widget lint、增强边界、发布元数据和运行时资源契约。
- 完整安装包为 `link-submit-next-1.2.6.jar`。

## [1.2.5] - 2026-08-20

### Changed
- 改为 Halo 官方 `PluginLinks` 的友链申请增强插件，删除新的自有申请、审核和通知写入流程。
- 保留旧版申请记录只读兼容，并禁止复用上游应用商店 ID。
