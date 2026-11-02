# 协同开发手册 / CONTRIBUTING

适用仓库：**仅 `plugin-link-submit-next`**。兄弟仓库 `theme-nucma`、`plugin-members` 各有自己的 `CONTRIBUTING.md`；仓库边界、职责与分支模型见本仓 [COLLABORATION.md](COLLABORATION.md)。

本文是本仓流程与规则的唯一入口，自身完整、不依赖维护者本机的其他文档。与 README 冲突时以本文为准；与代码里已有的既定契约冲突时，先提 issue 讨论，不要单方面改契约。

---

## 0. 三条底线

1. **改前先同步，改后先自检**——基于过期代码改动是本项目冲突的第一来源。
2. **协作者不碰 `main`**——一律开分支提 PR；`main` 是保护分支。
3. **只改源码，不动生成物**——`ui/build/**`、`packages/link-submit-widget/` 构建产物由构建任务产出，手改必冲突。

任何一条做不到，宁可不提交，也不要"先推上去再说"。

---

## 1. 角色与权限

| 角色 | `main` 权限 | 要求 |
|---|---|---|
| 维护者 | 可直接 push | 每次发版必须打 annotated tag，并写 CHANGELOG |
| 协作者 | 禁止 push | 开分支 → 推送 → 提 PR → 维护者 review 后合并；无例外 |

协作者不负责打 tag、不建发行版、不改版本号（除非 PR 明确就是要发版，且维护者已指定版本号）。

---

## 2. 环境与拉取

### 2.1 版本要求

| 项目 | 要求 | 说明 |
|---|---|---|
| JDK | **21** | 用仓库自带 `./gradlew`，**不需要**全局装 Gradle |
| pnpm | 10.22 | 由 Gradle 的 node 插件自动装到项目内，不必全局安装 |
| Halo | ≥ 2.27.0 | `plugin.yaml` 的 `spec.requires` |
| PluginLinks | 官方友链插件 | 增强目标；缺失时按钮不注入，功能整体可关 |

（主题仓库需要 Node.js ≥ 22.12，见 theme-nucma 仓手册。）

### 2.2 目录布局（联调时必须并列）

```
<任意父目录>/
├── theme-nucma/
├── plugin-members/
└── plugin-link-submit-next/
```

三者不嵌套。Windows 下路径不要带空格。

### 2.3 拉取

```bash
git clone https://gitee.com/xiui-cc/plugin-link-submit-next.git
```

### 2.4 远程约定（重要）

| 平台 | 地址形态 | 用途 |
|---|---|---|
| **Gitee（主）** | `gitee` → `https://gitee.com/xiui-cc/plugin-link-submit-next.git` | 日常协作、分支、PR、发版，**全部在这里** |
| GitHub（镜像） | `origin` | 只读分发，由 Gitee 的 Push 镜像自动同步（约 5 分钟） |

两条铁律：

- **不要推 GitHub。** GitHub 侧由 Gitee 镜像强制覆盖，直推的提交下次同步就被冲掉。
- **不要在 GitHub 提 PR。** 同上，会被镜像覆盖。

镜像只同步**分支与 tag**，不同步 Release 资产——GitHub 上没有 jar 属正常现象。

---

## 3. 标准流程（六步）

### 第 1 步：同步

```bash
git pull --rebase gitee main
```

- 有未提交改动时先 `git stash -u`，拉完立刻 `git stash pop` 并处理冲突。
- **拉取失败不得继续改动**，先报告，不要在冲突状态上叠改动。

### 第 2 步：开分支

```bash
git switch -c feat/link-preview-tuning
```

命名：`feat/xxx`、`fix/xxx`、`docs/xxx`、`refactor/xxx`。一个 PR 一件事，不混装无关改动。

### 第 3 步：改源码

遵守第 4 节的硬约束。涉及抓取、限流、URL 安全校验时，**先读 `SafeUrlValidator` / `IpAddressUtils` 现有实现与单测**，不凭感觉放行。

### 第 4 步：自检

```bash
./gradlew cleanTest test
```

测试必须全绿；`SafeUrlValidator` / `IpAddressUtils` 的安全语义改动必须补对应单测。

### 第 5 步：出包

```bash
./gradlew.bat build -x test
```

`-x test` 只允许在测试已单独通过后用于打包，**不能替代测试**。产物：`build/libs/link-submit-next-<ver>.jar`。

### 第 6 步：提交与 PR

提交格式见第 6 节，然后：

```bash
git push gitee HEAD
```

到 Gitee 提 PR 到 `main`。**不要**自行合并，也不要 `--force` 推别人的分支。

---

## 4. 改动中的硬约束

### 4.1 只改源码

以下**只能由构建任务生成**，禁止手改、禁止手工合并：

| 生成物 | 来源 |
|---|---|
| `packages/link-submit-widget/dist/**` 等构建产物 | widget 经 vite 构建 |
| `ui/build/**` | `ui/` 经 rsbuild 构建 |

### 4.2 抓取链路（本仓核心风险面）

- 抓取只读 `site-info`（标题 / 描述 / Logo），**绝不提交申请、不复制官方表单、不引用官方申请端点字样**（构建期校验会拦）。
- WebClient 构建器复用静态实例，每次请求只 clone 替换 resolver；禁止每请求新建连接池。
- 抓取链全响应式：不在事件循环 / 共享池上 `.block()`；单跳 `timeout` 与外层总超时对齐，超时真正断连。
- 目标 URL 必须过 `SafeUrlValidator`：拒绝非 http(s)、内网 IPv4 / IPv6（含 `::a.b.c.d`、NAT64、6to4 内嵌 IPv4）、直连裸 IP；新增绕过形态必须先加单测再修。

### 4.3 限流与配置

- 限流器临界状态不得在事件循环上做万级全表排序；淘汰与建桶放 `boundedElastic`。
- 配置读取失败一律 fail closed（关闭增强），不得"读不到就放开"。

### 4.4 行尾

`.gitattributes` 已统一文本文件 LF（`*.bat` / `*.cmd` 为 CRLF）。不要把编辑器产生的整文件 CRLF 转换提交进去；误转用 `sed -i 's/\r$//'` 还原，再 `git diff --stat` 确认没有整文件 diff。

---

## 5. 验证

| 场景 | 命令 |
|---|---|
| 全量测试 | `./gradlew cleanTest test` |
| 打包 | `./gradlew.bat build -x test` |

### 5.1 集成验证矩阵（至少覆盖）

1. 未安装官方 PluginLinks → 按钮不注入，官方表单完全手动可用。
2. 插件停用 / 关闭增强 → 注入消失，无报错。
3. 表单 `action` 为相对路径、异步渲染 → 按钮仍能注入（观察者兜底）。
4. 非 UTF-8 站点 → 标题不乱码。
5. 目标为内网地址 / 非 http(s) → 拒绝抓取。
6. 匿名访客限流生效，超限返回明确错误。

---

## 6. 提交与 PR 规范

### 6.1 提交信息

格式：`type(scope): 简述；版本号`

```
fix(link): 补内嵌 IPv6 绕过判定；0.0.2
feat(link): 抓取超时可配置
```

`type`：`feat` / `fix` / `docs` / `refactor` / `chore` / `test`。
纯文档改动不必带版本号。

### 6.2 PR 描述模板

```markdown
## 改了什么
（一两句，指向具体类 / 模块）

## 为什么
（对应 issue 或现象；复现步骤）

## 验证
- [ ] ./gradlew cleanTest test 通过
- [ ] 已出完整 jar
- [ ] 安全语义改动（URL 判定 / 限流）已补单测
- [ ] 官方插件缺失 / 停用两态正常

## 文档
README / CHANGELOG 是否需要同批更新（需要则已在本 PR 内）
```

### 6.3 review 关注点（双方都看）

- 抓取是否仍然只读 site-info，有没有触碰官方申请端点。
- URL 安全校验有没有被放松、新增绕过有没有单测。
- 响应式链有没有新增 `.block()`、超时是否对齐。
- 生成物有没有被手改。
- 版本文件是否一致、`spec.requires` 是否诚实。
- 是否顺手改了无关文件（尤其是整文件 CRLF）。

### 6.4 合并后

删除远程分支；不要在共享分支上 `--force` 推送；不要 `rebase` 已推送的公共分支。

---

## 7. 版本与发版

### 7.1 三档级别（先判级，再选号）

| 级别 | 判定 | 典型改动 | 选号 |
|---|---|---|---|
| **大** | 体系级变化，破坏已有契约 | 注入契约变更、settings 分组重构、抬高 Halo 最低版本 | 升 `MINOR`，`PATCH` 归零（0.0.2 → **0.1.0**） |
| **中** | 新增能力或成批修复，不破坏契约 | 新抓取字段、新设置项、同批修 3 个以上问题 | 升 `PATCH`（0.1.0 → **0.1.1**） |
| **小** | 单点修复与打磨 | 单条 bug 修复、文案、依赖小版本跟进 | 升 `PATCH`（0.1.1 → **0.1.2**） |

只有会进入安装包的内容变化才升版本；README、规则文档、测试代码改动不强制升版本。

### 7.2 版本文件清单（任一处不一致不得发版）

> **版本号只在发版提交里动（硬规则）**：本仓版本文件在**日常功能 / 修复提交中一律不改**。版本号由维护者在发版时统一升（见 7.3 第 1 步）；协作者发现改动需要升号，在 PR 描述或 issue 里提即可，**不要动手改版本文件**——包含版本号改动的 PR 会被要求先摘掉再合。日常对齐以 tag 为锚点：开工先 `git pull --ff-only`，发版后两边对齐同一个 tag 即版本一致。

| 需同步的文件 |
|---|
| `build.gradle`、`gradle.properties`、`src/main/resources/plugin.yaml`、`packages/link-submit-widget/package.json` |

`plugin.yaml` 的 `spec.requires` 必须是**实际验证过**的最低兼容版本，不能按编译 SDK 推断。

### 7.3 发版七步（维护者）

1. 汇总改动 → 按 7.1 判级选号 → 同步 7.2 全部版本文件。
2. `./gradlew cleanTest test` → `./gradlew.bat build -x test`。
3. 本地 Halo 实例安装验证（安装、启用、配置、主要流程、停用、卸载）。
4. 追加 `CHANGELOG.md` 一条（版本号与 tag 一致，写清功能变化、修复、影响面）。
5. 提交并打 **annotated** tag：

   ```bash
   git add -A
   git commit -m "feat(link): 改动要点；x.y.z"
   git tag -a x.y.z -m "x.y.z"
   git push gitee main --follow-tags
   ```

6. Gitee 建发行版，挂原始 jar，发布说明按「级别 + 影响面 + 验证结果 + 未验证项」写。
7. 校验资产：`curl -L` 下载一次比对字节数。**发行版 `assets[].size` 恒为 0，不能作为校验依据。**

### 7.4 铁律

- **tag 名直接用版本号，不加 `v` 前缀。**
- **必须 `-a`**：`--follow-tags` 只推 annotated tag，轻量 tag 不会被带走。
- **不跳号、不攒批**：一次发版一个 tag，中间版本缺失会让回退定位失效。
- **不覆盖已发布 tag**：已推送的 tag 不删不重打，修复只能发新版本。
- **回退用 `git revert`**，禁止 `reset --hard` 后强推。
- 版本号一旦发出即为对外契约，只增不改；有问题按级别发新版本。

### 7.5 当前版本

| 版本 | 下一版 |
|---|---|
| **0.0.2** | 中/小级 → 0.0.3，大级 → 0.1.0 |

覆盖安装：插件在「插件 → 安装」上传 jar，重复上传即为覆盖，设置保留。

---

## 8. 文档同步（不做"下次一起改"）

| 文件 | 何时必须更新 |
|---|---|
| `README.md` | 新增抓取字段、设置项、环境要求、仓库地址变化 |
| `CHANGELOG.md` | 每次发版追加一条，版本号与 tag 一致 |
| `COLLABORATION.md` | 分支模型、发版流程、远端与权限变化 |
| `CONTRIBUTING.md`（本文） | 流程或硬约束变化 |

---

## 9. 完成定义（DoD）

一个改动只有全部满足才算"做完"：

- [ ] 自检通过（`./gradlew cleanTest test`），失败即未完成
- [ ] 抓取仍只读 site-info；URL 安全语义改动带单测
- [ ] 响应式链无新增 `.block()`、超时对齐
- [ ] 安装包内容有变化时：版本号按 7.1 判级、7.2 全部文件同步、出了完整 jar
- [ ] 官方插件缺失 / 停用两态正常
- [ ] `CHANGELOG.md` 已追加，相关 `README` 同批更新
- [ ] 提交信息合规，PR 描述含验证结果
- [ ] 发版后：annotated tag 已推 Gitee、发行版已挂资产并做过字节校验
