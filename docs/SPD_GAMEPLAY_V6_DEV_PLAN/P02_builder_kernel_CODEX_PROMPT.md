# Codex 执行 Prompt — P02 Player Builder Command/Reducer、Form Schema 与真实 UI 基础

你现在执行 **Shattered Pixel Dungeon 自塑职业 Gameplay Components v6 — P02**。

## 输入与唯一基线

- 源码输入：`SPD_GC_V6_P01_IDENTITY_CHECKPOINT.zip`
- 冻结审计：`SPD_CLASS_GAMEPLAY_COMPONENTS_CURRENT_IMPLEMENTATION_AUDIT_v0.2.md`
- 冻结合约：`SPD_CLASS_GAMEPLAY_COMPONENTS_IMPLEMENTATION_CONTRACT_v0.2_FINAL.md`
- 开发计划：`SPD_GAMEPLAY_COMPONENTS_V6_DEVELOPMENT_PLAN_FINAL.md`

本阶段目标：**建立 UI 与 Headless 共享的唯一玩家构筑命令路径，结束 final-object assembler 与 Registry 自证。**

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

- 从 P01 accepted checkpoint 开始。
- 按 Contract 第 29 节实现命令模型；本阶段先覆盖声明与依赖编辑，不提前实现完整 Skill Runtime。
- 为每类声明提供真实 UI 页面/控件和 Headless command 测试；UI 控件不得直接 mutate Spec。
- `SetFieldValue` 必须受 FormSchema/typed field key 约束，禁止 reflection 向任意字段写值。
- 旧 `PlayerBuildAssembler.addSkill(SkillSpec)` 等接口不得被任何 `qa.playerpath` 测试调用；添加 architecture test。
- 返回至少三条可重放 command trace：双 Resource、中文 Mark、同组双 Mode，并证明 rename/delete/rebind/save-load。

## 本阶段明确范围

- 实现 `BuilderState`、`BuilderCommand`、`BuilderReducer`、Navigation、Undo/Redo、command trace/replay。
- 命令输入只能是 primitive、typed ref、variant key、field key；禁止 `AddSkill(SkillSpec)` 或任何最终对象参数。
- 实现声明类命令：Resource、Mark、ModeGroup、Mode、EntityCapacity、Entity、AbilityPool、Property、Recipe 的 create/edit/rename/delete/rebind。
- 实现 `SaveDraft`、`LoadDraft`、`FinalizeBuild` 框架；draft 可 unresolved，finalize 按当前已实现能力 fail closed。
- 实现 FormSchema 基础类型、Number Stepper、Enum selector、Reference Picker、Nested Variant/List/Diagnostic field。
- 把 `WndCreateClass` 或等价玩家入口接到 v6 BuilderState；保留 v5 feature flag 仅作过渡，不允许 v5 completion 证明 v6。
- 实现 Headless `PlayerBuildSession`，只接受同一命令序列；command trace 可保存和重放。
- 建立 Builder 每次 command 后统一重算 Dependency、Validation 与 Budget draft ledger 的管线。


## 本阶段验收 Gate

- [ ] 从空白 session 通过 command 创建/编辑/rename/delete/rebind 所有声明；无直接 final object API。
- [ ] Undo/Redo、Save/Load draft、command replay 得到 canonical 等价结果。
- [ ] 删除引用后错误卡片仍显示 lastKnownName+短 ID；不会从 UI 消失或自动换绑。
- [ ] 数值字段使用 stepper，不生成 1..N 平铺长列表。
- [ ] UI 与 Headless 调用同一 Reducer，有代码级/测试级证据。


## 代码处理原则

REWORK `WndCreateClass` 控制流；REPLACE `PlayerBuildAssembler` 的玩家用途；KEEP 旧 assembler 仅 `qa.runtime`/legacy；引入新 Builder kernel。

## 必须交付

1. `SPD_GC_V6_P02_BUILDER_CHECKPOINT.zip`：包含完整当前工作区源码，不是 patch-only。
2. `V6_CHECKPOINT_MANIFEST.json`：父 checkpoint hash、当前 ZIP hash、Audit/Contract hash、schema/price versions、测试命令与结果。
3. `PHASE_P02_IMPLEMENTATION_REPORT.md`：实际修改文件、行为变化、Gate 逐项证据、未通过项、仍 Deferred/Unsupported 项。
4. BuilderCommand traces、canonical serialization artifacts、Runtime traces 与测试报告。
5. 对 checkpoint 重新解压后的复验结果。

完成后立即停止，不执行下一阶段。
