# Codex 执行 Prompt — P08 Transfer/Copy/Swap 与 Capability 安全边界

你现在执行 **Shattered Pixel Dungeon 自塑职业 Gameplay Components v6 — P08**。

## 输入与唯一基线

- 源码输入：`SPD_GC_V6_P07_ENTITY_WORLD_CHECKPOINT.zip`
- 冻结审计：`SPD_CLASS_GAMEPLAY_COMPONENTS_CURRENT_IMPLEMENTATION_AUDIT_v0.2.md`
- 冻结合约：`SPD_CLASS_GAMEPLAY_COMPONENTS_IMPLEMENTATION_CONTRACT_v0.2_FINAL.md`
- 开发计划：`SPD_GAMEPLAY_COMPONENTS_V6_DEVELOPMENT_PLAN_FINAL.md`

本阶段目标：**完成明确对象类型的转移/复制语义，关闭“复制第一个状态”“任意 Buff/对象图复制”等不安全路径。**

## 通用执行纪律（本阶段全部适用）

1. 先完整展开输入 checkpoint；以该 checkpoint 为唯一源码基线。不得混入更早 ZIP、其它会话目录或未说明的本地副本。
2. 完整阅读：
   - `SPD_CLASS_GAMEPLAY_COMPONENTS_CURRENT_IMPLEMENTATION_AUDIT_v0.2.md`
   - `SPD_CLASS_GAMEPLAY_COMPONENTS_IMPLEMENTATION_CONTRACT_v0.2_FINAL.md`
   - 项目内 `docs/SPD_CLASS_GAMEPLAY_COMPONENTS_SPEC_v0.1.md`
   - 本阶段计划与上一阶段验收清单。
3. 这是源码实施任务。必须实际修改源码并运行测试；不得只写设计、报告或 TODO。
4. 只做本阶段。不得提前实现后续阶段，也不得自行扩展 Talent、Subclass、Specialization、Armor Ability、完整内容库或其它未冻结系统。
5. 禁止职业 Domain/标签驱动行为。不得加入 `GUNNER_CORE`、`SUMMONER_DOMAIN`、`ENGINEERING_DOMAIN`、`BLUE_MAGE_DOMAIN` 等隐藏分支。
6. v6 Player Builder 与 Player-path QA 必须走同一 `BuilderCommand`/Reducer。直接构造最终 Spec 只允许放在 `qa.runtime` 作为内部 smoke，不能充当完成证据。
7. 禁止自动补绑、首项回退、默认 Mark、默认 Mode、unknown→Standard Damage、删除后静默改绑。
8. 保留审计判定为 KEEP 的 SPD Hook、RuleRuntime 调度/因果/递归保护、真实效果 helper、HUD、Headless 与 Fuzz 基础；除非本阶段明确要求适配，不得重写这些基础。
9. 对 Contract 中未冻结的玩家可见选项，不得自行设计。应保持 `UNSUPPORTED`/`DEFERRED`、不在 Builder 暴露，并在报告中列明。
10. 每个新增或本阶段标记为 IMPLEMENTED 的 Variant，必须同步提交 Schema、Builder path、Dependency、Formatter、Budget、Save/Load、Runtime 与测试证据；缺一项不得标记 IMPLEMENTED。
11. 所有测试结果必须来自实际命令与行为断言。不得以类存在、Registry count、`implemented()==true`、关键词、无 crash 或报告文字判定 PASS。
12. 完成后生成完整当前工作区 checkpoint ZIP；重新解压该 ZIP，并在解压副本上复跑本阶段 Gate。返回 ZIP SHA-256、父 checkpoint SHA-256、实际修改文件列表、测试命令/退出码、失败项与证据路径。
13. 若无法通过全部 Gate，不得伪造 PASS。返回最大限度可复现的同阶段 checkpoint，明确标记 `PHASE_INCOMPLETE`，且不得进入下一阶段。
14. 完成当前阶段后立即停止，不继续下一阶段。

### 本阶段必须完成

- 从 P07 accepted checkpoint 开始。
- 按 Contract 第 14.10、32、33.9 完成四个 required subtests。
- 不要把 Snapshot、悟道或炼成提前塞入本阶段；它们只保留明确 UNSUPPORTED descriptor。
- Status catalog 每个 exposed status 必须含 target/intensity/duration/copy/cleanse/immunity/save adapter 元数据。
- 使用 P07 Entity ResourceStorage 完成 device→hero transfer；禁止假造第二 Hero-only shortcut。
- 输出 Capability 安全负面测试清单和实际结果。

## 本阶段明确范围

- 实现 Resource Transfer：typed source/destination holder/resource、EXACT_ATOMIC、destination overflow、ResourceStorage capability。
- 实现明确 StatusRef 的 Status Copy 与 copy policy；只允许 catalog 中 copyable 状态。
- 实现自定义 Mark Transfer，精确 amount 与原子性。
- 实现 Barrier Swap，仅交换 Barrier；HP/TemporaryHP 不变。
- 建立 Capability catalog/ref/validator 的基础和 forbidden object-copy tests。
- Capability Override 继续 UNSUPPORTED、Behavior Override DEFERRED；不得为 coverage 暴露。
- Snapshot/Ability/Property transfer variants 仅保留 typed placeholder/UNSUPPORTED，待 P11。
- 同步 Builder、formatter、budget、localization、migration、save-load。


## 本阶段验收 Gate

- [ ] Mandatory Test 8 A-D 全部通过，并从 Builder commands 构造。
- [ ] Resource/Mark transfer source不足或目标不合法时两边均不变；trace 含 transaction ID。
- [ ] Status Copy 只复制明确状态，source 其它状态不动。
- [ ] 任意 Buff class、Java object graph、AI state、反射复制在 Builder/validator/architecture tests 被拒绝。
- [ ] Capability/Behavior 未完成项不出现在玩家菜单、不报价、不计完成。


## 代码处理原则

REWORK old transfer/copy switches；KEEP typed Runtime helpers；NEW capability catalog；DELETE first-status copy/default object clone。

## 必须交付

1. `SPD_GC_V6_P08_TRANSFER_COPY_CHECKPOINT.zip`：包含完整当前工作区源码，不是 patch-only。
2. `V6_CHECKPOINT_MANIFEST.json`：父 checkpoint hash、当前 ZIP hash、Audit/Contract hash、schema/price versions、测试命令与结果。
3. `PHASE_P08_IMPLEMENTATION_REPORT.md`：实际修改文件、行为变化、Gate 逐项证据、未通过项、仍 Deferred/Unsupported 项。
4. BuilderCommand traces、canonical serialization artifacts、Runtime traces 与测试报告。
5. 对 checkpoint 重新解压后的复验结果。

完成后立即停止，不执行下一阶段。
