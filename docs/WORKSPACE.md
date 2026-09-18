# 工作区目录与维护说明

## 固定目标

本工作区仅面向 **Minecraft 1.21.11 + Fabric + MTR 4.0.5**。保留 `org.mtr.mod.*`、4.0.5 界面与 Mapping holders，不迁移到 MTR 4.1 / Elementa。

## 目录职责

以下路径以 `/home/rong/RongMC_Update/` 为根：

```text
Minecraft-Transit-Railway/
  fabric/src/main/java/       核心代码、Mixin 与生成的 Java
  fabric/src/main/resources/  assets、现代 data-pack 目录、Fabric 元数据
  fabric/src/test/            核心回归测试
  buildSrc/                  资源及代码生成工具
  schema/                    配置、模型资源的数据定义
  website/                   资源包创建器前端
  libs/                      核心构建所需本地依赖
  tools/audit_workspace.py    只读跨模组资源审计
  docs/                      目录、构建、修复与验证说明
  build/                     生成结果、报告与 release；不是源码
  archive/                   从源码根目录移出的旧 JAR、日志与诊断文件
Minecraft-Mappings/           1.21.11 映射和 Mixin 生成源
addons/
  London-Underground/         伦敦地铁内容
  Russian-Metro/              俄罗斯地铁内容
  Joban-Client-Mod/           JCM 设施、界面与资源
  Tianjin-Metro/              天津地铁设施与界面
  Yunzhu-Transit/             云竹交通设施与工具
  MSD/                       自定义标识及站台设施
  Filters-API/                创造栏过滤 API；天津依赖
  MTR-Test-Control/           测试实例控制、方块探测和本机 HTTP 接口
  libs/                      附属模组共享编译依赖
  build/                     既有汇总产物；不代表本次重新构建结果
archive/                     旧移植参考树与维护备份
RongMC_Upadte/                游戏实例；本次未修改或部署到这里
```

不要整体移动 `fabric/`、`libs/`、`schema/` 或相邻项目：构建脚本有跨目录引用。历史 Forge 源码保留为参考，但不参加当前构建。

本次将根目录旧的 1.20.1 JAR、零散命令文件、游戏/构建日志分别归入核心项目的 `archive/legacy/`、`archive/logs/`、`archive/diagnostics/<addon>/`。初期依赖修复之后、主要资源及代码改动之前的源码快照位于父工作区的 `archive/maintenance-2026-09-12/sources-before-main-fixes.tar.gz`；它不是完整游戏备份，也不包含依赖 JAR 和构建缓存。

## 本轮修复清单

| 模块 | 已定位并修复的问题 |
| --- | --- |
| MTR 核心 | 308 个旧配方、掉落表及标签目录适配；生成现代掉落路径；删除不存在的 Jade 入口；约束 Java/Fabric 版本；版本警告显示正确版本；模型测试不再覆盖源码或吞掉读取错误 |
| 核心网站/构建 | 修复 base href、创建器 API 路径；补齐 DTO 和实际嵌入的前端；移除嵌入时无关的 GitHub 下载；缺少前端时直接失败；生成动作移到任务执行阶段；固定 JUnit 版本并补网站回归测试 |
| London Underground | 补齐 `org.mtr.core` 等编译依赖；声明准确的 MTR/Java 运行条件 |
| Russian Metro | 27 个配方迁移到现代目录与 ingredient/result 格式；约束 MTR/Java 版本 |
| JCM | 补全编译依赖；70 个配方、65 个掉落表及标签目录迁移；四个配方将无效的 ingredient count 展开成真实槽位；同步构建过滤路径 |
| MSD | 缩短自定义文字时清空旧尾行；文本更新包在数组分配前检查行数上限、拒绝负数并复制传入数组 |
| Tianjin Metro | 写客户端配置前创建父目录，修复首次保存失败；Fabric Mixin 引用构建实际生成的共享 refmap |
| Yunzhu Transit | 自动楼层工具处理负数、无效输入与整数溢出；修复测试电梯面板引用不存在模型；资源过滤保留 JSON 转义，避免描述中的换行造成最终元数据非法 |
| Filters API | 过滤时复制原创造栏 ItemStack，保留组件数据；合并重叠过滤器避免重复条目；约束滚动索引 |

六个 MTR 附属模组都要求确切的 MTR 4.0.5，避免 Loader 允许在不兼容的 4.1 上启动；Filters API 不依赖 MTR 本体。

## 2026-09-12 验证结果

- 核心 `:fabric:setupFiles` 离线通过；前端生产构建通过。
- 核心 `:fabric:build` 通过，包含 JUnit、access widener 验证、shadow/remap 打包；13 项测试全部通过、无跳过（模型验证 2、MQO 转换 9、网站嵌入 2）。
- 七个附属模组完整 `build` 均通过，包括资源处理和 remap；附属项目没有现有单元测试，不能把 `test NO-SOURCE` 算作测试覆盖。
- 源码资源审计：6079 个 JSON、405 个配方，0 错误；包含生成/展开资源的构建审计：6432 个 JSON、405 个配方，0 错误。
- 八个最终 JAR 的入口类、Mixin 类、refmap、版本依赖、目标 Java 字节码检查通过；未发现附属 JAR 打入 `org/mtr/`。核心嵌入包含创建器首页及脚本。
- 构建仍有原有弃用 API、unchecked、Javadoc 和 Gradle 10 兼容性警告；没有以隐藏警告或跳过核心测试的方式获得通过结果。
- 新增 `addons/MTR-Test-Control/` 独立测试辅助模组，构建通过；它只监听本机随机端口并使用启动时生成的 token，不打开公网控制面。

本次已验证的八个 JAR 单独汇总在核心项目 `build/release/maintenance-2026-09-12/`，附 `SHA256SUMS`；没有覆盖共享 `addons/build/`、`addons/libs/`，也没有部署到游戏实例。审计报告在 `build/maintenance/`，本次构建日志在 `build/maintenance/logs/`，JUnit HTML 报告在 `fabric/build/reports/tests/test/index.html`。

## 构建方法

需要 Java 21 字节码。此机已验证可用的 JDK 是 `~/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2`，默认 Java 17 不可用于本项目构建。

```sh
export JAVA_HOME="$HOME/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2"
export PATH="$JAVA_HOME/bin:$PATH"
```

在核心根目录按顺序执行：

```sh
./gradlew :fabric:setupWebsiteFiles
npm ci --prefix website
npm run build --prefix website
./gradlew :fabric:setupFiles
./gradlew :fabric:build
```

本机若 wrapper 下载不可达，可将 `./gradlew` 换成本地的 `../archive/porting-old/gradle-home-fresh/wrapper/dists/gradle-9.5.1-bin/iq79hdu3mqx29lgffhp8bfmx/gradle-9.5.1/bin/gradle`。继续使用默认 `~/.gradle` 缓存；不要把 `GRADLE_USER_HOME` 指向归档中的旧缓存。只有依赖缓存齐全时才使用 `--offline`。网络代理通过 Gradle JVM 系统属性传入，不写入项目公共配置。

附属模组需保留 `addons/libs/MTR-fabric-4.0.5+1.21.11-compile.jar`。需要 core/shaded 类型的模组同时引用运行 JAR，均为 `compileOnly`，不得把 MTR 或旧 Mapping 类再打进附属 JAR：

```sh
./gradlew -p ../addons/Filters-API build
for addon in London-Underground Russian-Metro Joban-Client-Mod MSD Tianjin-Metro Yunzhu-Transit; do
  ./gradlew -p "../addons/$addon" :fabric:build || exit 1
done
```

Filters API 的产物位于它的 `build/libs/`，其他附属模组在各自的 `fabric/build/libs/`。只使用最终 remap JAR，不使用 `-sources`、`-dev` 或历史 1.21.4 产物。共享 `addons/libs/` 不会随源码构建自动更新。

## 资源检查与验证边界

```sh
python3 tools/audit_workspace.py --report build/maintenance/resource-audit.json
python3 tools/audit_workspace.py --built --report build/maintenance/built-resource-audit.json
python3 tools/audit_workspace.py --jars --report build/maintenance/jar-audit.json
```

脚本检查核心与存在于相邻目录的七个附属项目。内容包括 JSON 解析、旧 data-pack 目录、配方形状与 ingredient 格式、item-tag 引用、模型引用、Mixin/入口源码和图标。默认使用本地缓存的原版 1.21.11 客户端 JAR 验证原版资源，也可用 `--minecraft-jar` 指定。源码模式容许已知 Gradle 模板占位符；`--built` 检查实际展开后的资源。`--jars` 额外检查最终产物的版本依赖、入口/Mixin 类、refmap、Java 字节码、旧 data-pack 路径和意外打包的 MTR 类，并输出 SHA-256。

这些检查不等于游戏内测试。尚需在单独测试实例验证客户端和专用服务器启动、Mixin 注入、方块放置/掉落、列车运行、电梯、联机同步、标牌界面和资源包创建器。MSD/YTE 仍有旧物品 override 数据，动态选中/持有状态显示需要按 1.21.11 的物品模型系统进一步实机验证；没有凭猜测添加新行为。没有为原本无生存配方的装饰内容随意补配方或掉落。
