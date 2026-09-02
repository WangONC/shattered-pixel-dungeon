# Codex 执行 Prompt — P03 Typed Skill Language、Validation、Compiler 与 Executor Framework

你现在执行 **Shattered Pixel Dungeon 自塑职业 Gameplay Components v6 — P03**。

## 输入与唯一基线

- 源码输入：`SPD_GC_V6_P02_BUILDER_CHECKPOINT.zip`
- 冻结审计：`SPD_CLASS_GAMEPLAY_COMPONENTS_CURRENT_IMPLEMENTATION_AUDIT_v0.2.md`
- 冻结合约：`SPD_CLASS_GAMEPLAY_COMPONENTS_IMPLEMENTATION_CONTRACT_v0.2_FINAL.md`
- 开发计划：`SPD_GAMEPLAY_COMPONENTS_V6_DEVELOPMENT_PLAN_FINAL.md`

本阶段目标：**用强类型 Skill/Effect/Targeting/Delivery/Modifier/Cost 架构替代 v6 公开字段袋，并打通第一个真实 Player-path 技能。**

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

- 从 P02 accepted checkpoint 开始。
- 实现 Contract 第 11-19、24、30 节的架构骨架，但只把已完成垂直切片标为 Player-exposed/IMPLEMENTED。
- `EffectChainSpec` 本阶段支持 Primary 与 Immediate Secondary 基础；Delay/Echo/NextAction 的运行语义留到 P06，但 Schema 可识别并标为 UNSUPPORTED。
- `AnyOf`/`Not`、Capability Override、Behavior Override 等没有全链证据时不得暴露。
- 完成一个 command trace 驱动的真实战斗示例，并把对应 Layer A-E 测试 ID写入 completion row。
- 增加静态规则：`rules/v6` 或等价新包禁止引用 legacy mega-object、旧 Registry、raw enum fallback。

## 本阶段明确范围

- 实现 `SkillSpec v0.2`、typed Trigger、ConditionExpr/AllOf、EffectSpec interface、EffectChain、Delivery、Targeting、Modifier、Cost、SkillConstraint 的数据结构。
- 所有 Contract Variant 建立 descriptor/ImplementationState；未完成功能保持 DECLARED/UNSUPPORTED/DEFERRED，不在 Builder 暴露。
- 实现 Skill Builder commands：CreateSkill、选择 Trigger/Family/Variant、设置 typed fields/ref、Targeting、Delivery、Modifier、Cost、Constraint。
- 实现 StructuralValidator、CompatibilityValidator、RuntimeCapabilityValidator；诊断不修改 Spec。
- 实现 `ClassCompilePlan`、immutable compiled nodes、EffectResult/Preflight、typed Executor Registry；Registry 与 FormSchema 分离。
- 实现最小可用端到端垂直切片：Active/Always + 受支持 selector/coverage/filter + DirectDamage + 简单 delivery + NoCost。
- 为本阶段暴露项同步 formatter、en/zh、price key/ledger、canonical serialization、v5 migration placeholder 和真实 Headless 行为测试。
- 新 v6 Builder/Compiler 不得 import/依赖旧 `EffectVocabularyRegistry` 或旧 mega `EffectSpec`。


## 本阶段验收 Gate

- [ ] 从空白 Builder command 构造一个技能，保存/加载、finalize、compile、真实 SPD Runtime 命中目标并产生明确 trace。
- [ ] v6 Effect Variant 无通用 `power/duration/stateId/templateId` 字段袋；每个字段归属于具体 Variant。
- [ ] 缺 executor、unsupported、无目标、blocked 可区分；无默认 Standard Damage。
- [ ] FormSchema 暴露不等于 implementation；Completion row 必须有实际证据 ID。
- [ ] 旧 Registry 的 playerReachable/implemented shortcut 不参与 v6 Gate。


## 代码处理原则

REPLACE v6 public `EffectSpec`/RuleCondition/RuleCost/RuleModifier/SkillDelivery/Targeting 字段袋；KEEP legacy 类仅迁移；ADAPT RuleRuntime 通过 compile plan 调用 typed executor。

## 必须交付

1. `SPD_GC_V6_P03_TYPED_SKILL_CHECKPOINT.zip`：包含完整当前工作区源码，不是 patch-only。
2. `V6_CHECKPOINT_MANIFEST.json`：父 checkpoint hash、当前 ZIP hash、Audit/Contract hash、schema/price versions、测试命令与结果。
3. `PHASE_P03_IMPLEMENTATION_REPORT.md`：实际修改文件、行为变化、Gate 逐项证据、未通过项、仍 Deferred/Unsupported 项。
4. BuilderCommand traces、canonical serialization artifacts、Runtime traces 与测试报告。
5. 对 checkpoint 重新解压后的复验结果。

完成后立即停止，不执行下一阶段。
