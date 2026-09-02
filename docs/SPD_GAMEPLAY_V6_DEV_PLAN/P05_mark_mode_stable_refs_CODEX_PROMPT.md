# Codex 执行 Prompt — P05 动态 Mark/Stack/Charge/Counter 与稳定 Mode Engine

你现在执行 **Shattered Pixel Dungeon 自塑职业 Gameplay Components v6 — P05**。

## 输入与唯一基线

- 源码输入：`SPD_GC_V6_P04_CORE_EFFECTS_CHECKPOINT.zip`
- 冻结审计：`SPD_CLASS_GAMEPLAY_COMPONENTS_CURRENT_IMPLEMENTATION_AUDIT_v0.2.md`
- 冻结合约：`SPD_CLASS_GAMEPLAY_COMPONENTS_IMPLEMENTATION_CONTRACT_v0.2_FINAL.md`
- 开发计划：`SPD_GAMEPLAY_COMPONENTS_V6_DEVELOPMENT_PLAN_FINAL.md`

本阶段目标：**删除固定 HUNTED/CHARGED 与 raw mode strings 的玩家语义，完成自定义 Mark、Mode 及其所有引用点。**

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

- 从 P04 accepted checkpoint 开始。
- 完整实现 Contract 第 8、9、14.6、14.11.1 与所有 Mark/Mode reference sites。
- 搜索 Effect、Condition、Cost、Targeting、Value、Formatter、Budget、Save/Load、HUD、QA 中所有 Mark/Mode 使用点，不得只改 Buff 类。
- fixed enum/raw string 只能留在 `migration/v5` 或明确 legacy fixture；任何 v6 fallback 都是 Gate failure。
- 从 UI/command trace 构造 Test 2 与 Test 9；附 canonical before/after rename/delete snapshots。
- 完成后停止，不实现 Delay/Entity/Transfer。

## 本阶段明确范围

- 将 `RuleMark` Runtime 承载改为 dynamic markId；实现 MarkSpec/State 的 kind、范围、持续、refresh、overflow、provenance。
- 实现 Add/Set/Consume/Remove Mark；MarkCompare condition、MarkCost、HasMarkFilter、MarkValueSource、Transfer placeholder ref。
- 任何 invalid/deleted Mark 返回 UNRESOLVED/BLOCKED，绝不回退 HUNTED。固定 enum 仅作为 v5 migration alias。
- 实现 ModeGroup/ModeSpec/ModeState、同组互斥、ModeShift、ModeActiveCondition、ModeEngineComponent、稳定 ModeSwitch ClassOperation。
- 动态名称直接显示，不拼本地化 key；rename/delete/rebind/save-load 全链。
- 补齐 v5 Mark/Mode migration mapping 和本阶段 Budget/Formatter/Localization。


## 本阶段验收 Gate

- [ ] Mandatory Test 2 全部通过，包括“灼痕”Add 2、threshold、consume 3、rename/delete/same-name/rebind/save-load。
- [ ] Mandatory Test 9 全部通过，包括同组互斥、active state save/load、动态名称无 NO TEXT FOUND。
- [ ] Mandatory Test 10 对 Resource/Mark/Mode 的参数化路径通过。
- [ ] 全仓新 v6 player/runtime 路径无 `RuleMark.Type` fallback、无 raw mode name identity。
- [ ] Budget rename/order invariance 与 formatter 诊断通过。
- [ ] Assassin/Hunt 与 Transform/Bio 的最低 recipes 分别以自定义 Mark 和自定义 Mode 完成，关闭 Recipe 5 与 10。


## 代码处理原则

KEEP `RuleMark`/`RuleMode` 的 Buff/Bundle 基础但 REWORK identity；LEGACY 固定 enum/raw strings；DELETE v6 fallback。

## 必须交付

1. `SPD_GC_V6_P05_MARK_MODE_CHECKPOINT.zip`：包含完整当前工作区源码，不是 patch-only。
2. `V6_CHECKPOINT_MANIFEST.json`：父 checkpoint hash、当前 ZIP hash、Audit/Contract hash、schema/price versions、测试命令与结果。
3. `PHASE_P05_IMPLEMENTATION_REPORT.md`：实际修改文件、行为变化、Gate 逐项证据、未通过项、仍 Deferred/Unsupported 项。
4. BuilderCommand traces、canonical serialization artifacts、Runtime traces 与测试报告。
5. 对 checkpoint 重新解压后的复验结果。

完成后立即停止，不执行下一阶段。
