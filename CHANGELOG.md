# 更新日志

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
