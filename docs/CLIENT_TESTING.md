# 客户端实测记录

测试日期：2026-09-13。环境为 Minecraft 1.21.11、Fabric Loader 0.19.3、MTR 4.0.5；测试使用隔离实例 `fabric/client-run/` 和隔离服务器 `fabric/run/`，没有修改正式游戏实例。

## 本轮结果

- 客户端加载 60 个模组，服务端加载 52 个模组；车辆、标牌、轨道、对象和电梯资源均完成初始化。
- 最终模型检查：31,122 个模型状态，`missing` 数量为 0，报告见 `build/client-testing/2026-09-13/models-final.json`。
- GUI 冒烟测试 8/8 通过：MTR PIDS、JCM LCD PIDS、MSD YUUNI PIDS、天津自定义颜色、云竹空楼层、London Underground Northern PIDS、Russian Metro Moscow Ticket Machine、MSD 接触网节点。
- 配置保存、关闭、重新打开和服务端 NBT 回读 7/7 通过：4 组 PIDS、天津颜色、MSD 接触网偏移和云竹空楼层。
- MSD 手持物品模型轮换通过：`hold_num` 从 0、1、2 变化时模型身份连续变化，模型非空；带 `hold_num: 2` 放置后方块 `type=2`，证据见 `build/client-testing/2026-09-13/msd-hold-placement.json`。
- 1.21.11 物品模型兼容层通过：旧版 `overrides` 已转换为 `minecraft:range_dispatch`，MSD 的 24 个模型和 YTE 的带选中态链接器均保留默认模型与全部变体。
- 双格 PIDS 的数据正确写入数据半块 `(0,120,1)`；读取外观半块 `(0,120,0)` 会产生“保存失败”的误判。
- 天津颜色 `336699` 保存为 `3368601` 并重新打开回读；云竹空楼层保存并回读 `B2`、`Client QA Concourse`、`should_ding: 1b`；MSD 接触网 X 偏移保存并回读 `0.5d`。
- 资源重载后重新打开云竹空楼层 GUI，配置仍保持正确，说明重载没有破坏客户端交互。
- Russian ticket machine 的余额包实测成功：1 个绿宝石增加余额 10；Filters 创造栏完成分类、滚动和选项界面操作。
- 客户端保持连接期间未重现实体属性包断线；MTR 专用 rendering entity 使用负 ID。

证据目录：`build/client-testing/2026-09-13/`。主要结果包括 `gui-smoke.json`、`config-roundtrip.json`、`models-final.json`、`msd-hold-placement.json` 和 `jar-audit-final.json`。

## 资源与日志说明

- 源资源中的无效 `#missing` 引用已经修复；源资源审计剩余无效纹理数量为 0。
- 客户端日志仍有 76 个模型报告 `particle` 或 `#0` 纹理引用告警，共 152 条（资源首次加载和重载各一轮）。这些是模型引用完整性告警，不等同于 `models-final.json` 的 missing model，也没有导致本轮 GUI 或模型状态测试失败；完整清单保留在 `build/client-testing/2026-09-12/resource-warnings.json`。
- London Underground Northern Line 的 5 个声音事件已经补齐对应 OGG 资源，避免事件指向不存在文件；补齐音频使用现有 London 音频资源作兼容占位，并非声称恢复了原始录音：`this_south_wimbledon_terminates_morden.ogg`、`tootingbroadway_terminates.ogg`、`sw_chx_highb.ogg`、`arrival.ogg`、`next_southwimbledon.ogg`。
- 日志中的 Mojang 401、Realms/session 认证失败、profile key 401 和 Linux `flite` narrator 缺失属于离线测试环境告警；它们没有阻止进入本地服务器或执行 GUI 测试。

## 构建与审计

- 核心、Mapping 主编译和 8 个附属项目均构建成功；核心 remap 成功。Mapping 的完整 `build` 在离线模式下因本地缺少测试依赖 `org.reflections:reflections:0.10.2` 而未完成，但不影响已生成并部署的主 JAR。
- JAR 审计见 `build/client-testing/2026-09-13/jar-audit-final.json`：各项目 0 errors，未发现附属模组打包 `org/mtr/**`。
- 测试控制模组 `MTR-Test-Control` 提供客户端状态、截图、GUI 控件值、复选框、滚轮、资源模型检查和连接测试接口，构建成功。

## 尚未覆盖

- 本轮没有完整覆盖列车发车、乘车、车辆运行时序、电梯实际运行、红石时序、声音听感和全部方块变体。
- 因此本报告只确认列出的客户端加载、资源、GUI、配置往返和代表性功能通过，不宣称所有方块及所有列车功能均已无 bug。
