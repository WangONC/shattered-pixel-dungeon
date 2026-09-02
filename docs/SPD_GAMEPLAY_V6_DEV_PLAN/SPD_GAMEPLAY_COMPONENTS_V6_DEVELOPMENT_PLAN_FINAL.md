# Shattered Pixel Dungeon 自塑职业 Gameplay Components v6

# 完整开发施工计划 — FINAL

**文档状态：FINAL PLANNING / 本轮不修改项目源码**  
**目标实现：Contract `0.2-final` / Schema `6`**  
**计划日期：2026-09-02**

## 0. 权威输入与校验

| 输入 | SHA-256 |
|---|---|
| `shattered-pixel-dungeon.7z` | `b066e12c5cbef0ef15c35c1b36c2cfdacc43589b33c2921fc794ee9e6629e599` |
| `CURRENT IMPLEMENTATION AUDIT v0.2` | `6da79a5fd3767070911a173a5db3340c0c43c9005e503383cd6858b98e26d65a` |
| `FINAL Implementation Contract v0.2` | `61f2e37f6cb527f788dbd87ceed3bab227c31b257ab7b731dfbba2c1d1021bd8` |

本计划只把已冻结 Contract 转化为可施工、可验收、可回滚的阶段链。它不修改 Contract，不新增 Talent、Subclass、Specialization、Armor Ability 或其它尚未讨论的系统。

---

## 1. 总施工决策

### 1.1 默认从当前审计快照开工

当前压缩包是唯一已经被独立审计并可精确识别的源码状态，因此它是**默认 operational baseline**。虽然 Audit 建议在真实 Git 历史中寻找“RuleRuntime/Headless 已完成、错误 Full Gameplay Components 尚未大规模引入”的 commit，但只有满足以下条件才允许改用：

1. commit hash 来自真实 `.git`，不是猜测；
2. Audit 列出的 KEEP 资产全部存在；
3. 与当前快照相比，没有丢失已验证的 Hook、调度、HUD、Headless、Fuzz、Delay/Entity 基础；
4. 旧行为回归与构建通过；
5. 独立 Pro Reviewer 批准。

否则不要做历史考古式重建，直接采用当前快照，并通过 **Strangler/隔离式重构** 把 v5 旧公开层降为 migration-only。

### 1.2 不重写整个 SPD；只替换错误公开语言层

最终边界：

```text
KEEP / ADAPT
  RuleHooks + SPD production call sites
  RuleContext / Trace / cause chain
  deterministic RuleRuntime + recursion guard
  native effect helpers + WorldCapabilityValidator
  HUD、Headless、Fuzz/Integrity
  RuleDelayedPayload / RuleOwnedEntity / RuleCarrierTrap 的接入思想

LEGACY / MIGRATION ONLY
  v5 EffectSpec field bag
  v5 mega ClassGameplayComponentSpec
  fixed RuleMark.Type aliases
  raw mode strings
  fire/poison/heal carrier strings
  old final-object fixtures/reports

V6 NEW PUBLIC MODEL
  stable declarations + typed refs
  BuilderCommand/Reducer
  typed Skill/Effect/Entity/Payload/Cost/Constraint
  canonical save/load + migration
  evidence-backed QA
```

### 1.3 “Contract 全部实现”的准确含义

最终完成包括：

- 所有 Contract 标记为 REQUIRED/MUST 的 v6 能力完成全部 DoD；
- Contract 允许保持 UNSUPPORTED/DEFERRED 的能力被正确识别、保存/格式化（如适用），但不在 Builder 暴露、不报价、不计 IMPLEMENTED；
- 12 项 Mandatory Player-path、Localization Sweep、15 Recipes、Deep Save/Load、迁移、Build 与人工 UX 全部通过。

本计划明确不强行实现：

- `PROPORTIONAL` Resource Convert；
- `ALLOW_LETHAL` HP Cost；
- `IGNORE_ARMOR`；
- `REMOVE_OLDEST`（除非独立完成全部 Gate；默认不暴露）；
- `BREAKABLE_COMMITMENT`；
- arbitrary Capability/Behavior Override；
- arbitrary Java Buff/object graph/AI/code capture；
- 原 Actor identity/AI/装备复活；
- 完整悟道/炼成内容库；
- Talent、Subclass、Specialization、Armor Ability 等 Deferred 高层系统。

对 Contract 只给出类型名、但没有冻结完整玩家选项的 support type，实施者只能做最窄、确定性、SPD-backed 的内部映射；不得擅自扩展玩家菜单。无法无歧义映射时保持 UNSUPPORTED，并进入 Pro 复审，而不是猜测。

---

## 2. Checkpoint 与分支纪律

### 2.1 单线基线

```text
原始审计源包
  → P00 accepted checkpoint
  → P01 accepted checkpoint
  → ...
  → P12 FINAL accepted checkpoint
```

每个新 Codex 会话只能使用**上一已接受 checkpoint**。禁止重新从原始 ZIP 开始、混入其它会话工作区、使用同名但 hash 不同的包。

### 2.2 推荐 Git 结构

- 长期分支：`feature/gameplay-components-v6`
- 每阶段可用短分支：`gc-v6/p01-identity` 等；Gate 通过后合并。
- 每个 accepted checkpoint 建 annotated tag：`gc-v6-p01-accepted`。
- 保留阶段提交，不将整个 v6 压成一个无法 bisect 的大提交。
- Pro 复审拒绝时，在同一阶段修订，例如 `P05-R1`，不得把缺陷拖进 P06。

### 2.3 每阶段固定交付物

1. 完整当前工作区 ZIP；
2. ZIP SHA-256 与父 checkpoint SHA-256；
3. `V6_CHECKPOINT_MANIFEST.json`；
4. 实际修改文件列表和源码 diff 摘要；
5. 测试命令、退出码、原始/机器可读结果；
6. BuilderCommand traces；
7. canonical Build/Runtime artifacts；
8. Runtime trace/behavior evidence；
9. Gate 逐项状态；
10. ZIP 重新解压后的复验结果；
11. 仍为 DECLARED/UNSUPPORTED/DEFERRED/LEGACY_ONLY 的清单。

报告不是验收源；所有 PASS 必须能追到源码、命令和行为 oracle。

---

## 3. 跨阶段不可破坏的施工规则

1. **v6 Spec 永不被 Runtime/Load/Resolver 修改。**
2. **名称永远不是 ID。** rename 不改引用；delete 产生 UNRESOLVED；同名重建不接管。
3. **Player UI 与 QA 共用命令模型。** final-object fixture 只能作内部 smoke。
4. **同一 Gameplay Effect 只有一套公开 Schema/Executor。** Skill、Device、Trap、Field、Carrier、Delay、Echo、Attachment 共用。
5. **每个阶段都要同步维护 Save/Load、Formatter、Localization、Budget 与 Migration。** P10 是总清扫，不是允许前面裸奔。
6. **每个 Player-exposed Variant 当阶段就必须有 price key。** 未定价则不暴露。
7. **任何 silent fallback 都按严重缺陷处理。**
8. **旧层只允许单向 v5→v6 migration。** 禁止让 v6 先降级为 v5 field bag 再执行。
9. **阶段 Gate 是二元的。** “大部分通过”“覆盖率很高”不允许进入下一阶段。
10. **Deferred 保持 Deferred。** 不因某阶段顺手接触相邻代码而自行设计。

---

## 4. 现有代码最终处理矩阵

| 当前代码/能力 | 施工处理 | 最迟完成阶段 | 最终状态 |
|---|---|---:|---|
| `RuleHooks` 与 Hero/Mob/Item/Status 接入 | KEEP，按需要增加通用事件 | P11 | v6 production hook layer |
| `RuleContext`/Trace/cause、确定性排序、guard | KEEP/ADAPT | P06 | v6 runtime foundation |
| SPD native effect helpers、WorldCapabilityValidator | KEEP/ADAPT | P07 | typed executor adapters |
| Resource HUD / Class Action HUD | KEEP/ADAPT | P05/P07 | declaration/state-aware HUD |
| Headless/Fuzz/Integrity | KEEP，改变输入与证据角色 | P12 | auxiliary QA + player-command fuzz |
| `RuleDelayedPayload` | KEEP concept，REWORK payload/state | P06 | unified ScheduledPayload |
| `RuleOwnedEntity` / `RuleCarrierTrap` | KEEP接入基础，REWORK blueprint/instance | P07 | unified Entity Runtime |
| `ClassBuild` 当前模型 | LEGACY；新建 v6 spec/state | P10 | migration-only old model |
| 当前 `EffectSpec` field bag | LEGACY；v6 typed union替代 | P10 | `migration/v5` only |
| 当前 mega `ClassGameplayComponentSpec` | LEGACY；typed variants替代 | P10 | migration-only |
| `EffectVocabularyRegistry` snapshot options/self-proof | 不进入 v6；静态标签可重建 | P10 | 删除 public/QA authority |
| `PlayerBuildAssembler(final object)` | 退出 player-path | P02 | `qa.runtime`/legacy only，最终可删除 |
| `resolvePendingBindings()` | v6 立即禁用，全局最终删除 | P10 | zero non-legacy callsites |
| fixed `RuleMark.Type` | Runtime泛化，旧值迁移 alias | P10 | migration-only |
| raw mode strings | 迁移为 ModeSpec/Ref | P10 | migration-only |
| carrier `fire/poison/heal` strings | 迁移映射，v6删除 | P10 | migration-only |
| AREA modifier | 迁移到 Coverage | P10 | removed |
| weak `RuleBuild.fingerprint()` | 替换为 canonical serializer | P01 | 日志摘要可留，不能断言 |
| no-op Constraint credit | 删除/credit=0，真执法后重建 | P09 | evidence-backed only |
| Java class-name Item Cost | 稳定 ItemFilter migration | P10 | removed from player schema |
| `implemented(){return true;}` completion shortcut | 从 v6证据链删除 | P10 | ImplementationState+evidence IDs |
| 旧 coverage/archetype 报告 | 保留历史、标 Legacy | P00 | 不参与 v6 completion |

---

## 5. 阶段总览

| 阶段 | 主目标 | 输出 checkpoint | Pro 复审 |
|---|---|---|---|
| P00 | 基线确定、Legacy 冻结与防扩债 | SPD_GC_V6_P00_BASELINE_CHECKPOINT.zip | 条件复审：仅当改用历史 commit |
| P01 | Stable Identity、声明/状态分离、Dependency 与 Canonical Save Core | SPD_GC_V6_P01_IDENTITY_CHECKPOINT.zip | — |
| P02 | Player Builder Command/Reducer、Form Schema 与真实 UI 基础 | SPD_GC_V6_P02_BUILDER_CHECKPOINT.zip | PRO-R1：身份/Builder 架构 |
| P03 | Typed Skill Language、Validation、Compiler 与 Executor Framework | SPD_GC_V6_P03_TYPED_SKILL_CHECKPOINT.zip | — |
| P04 | 核心 Effect Families、Resource Transaction、HP 边界与基础 Class Components | SPD_GC_V6_P04_CORE_EFFECTS_CHECKPOINT.zip | — |
| P05 | 动态 Mark/Stack/Charge/Counter 与稳定 Mode Engine | SPD_GC_V6_P05_MARK_MODE_CHECKPOINT.zip | PRO-R2：核心语言/Mark/Mode |
| P06 | 统一 EffectChain/Payload、Delay/Echo 与 Action Attachment | SPD_GC_V6_P06_PAYLOAD_TIMING_CHECKPOINT.zip | — |
| P07 | 统一 Entity、Ownership/Relation/Capacity/Persistence 与 World/Control | SPD_GC_V6_P07_ENTITY_WORLD_CHECKPOINT.zip | PRO-R3：Payload/Timing/Entity |
| P08 | Transfer/Copy/Swap 与 Capability 安全边界 | SPD_GC_V6_P08_TRANSFER_COPY_CHECKPOINT.zip | — |
| P09 | Skill Constraint、Class Vow 真执法与统一 Budget Ledger | SPD_GC_V6_P09_VOW_BUDGET_CHECKPOINT.zip | — |
| P10 | 完整 v5→v6 Migration、Typed Formatter/Localization Sweep 与旧公开路径退役 | SPD_GC_V6_P10_MIGRATION_LOCALIZATION_CHECKPOINT.zip | PRO-R4：Transfer/Vow/Budget/Migration/Legacy |
| P11 | 新增通用 Primitive：Snapshot、Corpse、Observation/Ability、Property/Synthesis | SPD_GC_V6_P11_GENERIC_PRIMITIVES_CHECKPOINT.zip | PRO-R5：新 Primitive 通用性/安全 |
| P12 | 全量对抗性验收、15 Recipes、Fuzz/Build/实机 UX 与 FINAL Checkpoint | SPD_GC_V6_P12_FINAL_ACCEPTED.zip | PRO-R6：最终独立验收 |

**阶段不可并行跨越。** P03 依赖 P02 的命令模型；P07 依赖 P06 的 Payload；P08 依赖 P07 的 ResourceStorage/Entity；P11 必须在 P10 清除双模型后施工。

---

## 6. 各阶段详细施工与 Gate

### P00 — 基线确定、Legacy 冻结与防扩债

**目标**：建立唯一、可复现的 v6 施工基线；冻结旧 Gameplay Language 作为证据和迁移输入，防止继续向错误抽象加字段。

**修改范围**：

- 验证源包/仓库身份，记录源码 SHA、Gradle/SDK 环境与可运行任务。若存在 Git 历史，可按 Audit 的代码地标寻找更干净基线；只有在 KEEP 资产齐全、回归测试通过且有独立复审时才允许切换。否则使用当前审计快照。不得编造 commit。
- 建立单一长期分支（建议 `feature/gameplay-components-v6`）和阶段 tag/checkpoint 规则。
- 将现有“210/210”“coverage complete”等报告明确标为 `LEGACY_EVIDENCE_NOT_PLAYER_PATH`；保留原文件，不篡改历史结果。
- 建立 `legacy.v5` 与 `contract.v6` 的依赖边界/包骨架；旧 Builder 和旧 Runtime 行为本阶段不改语义。
- 加入 architecture guard：禁止再向旧 `EffectSpec` 字段袋、固定 `RuleMark.Type`、carrier payload string、mega ClassGameplayComponent、旧 completion shortcut 增加新的 public gameplay 条目。
- 创建 `V6_CONTRACT_SYMBOL_IMPLEMENTATION_MAP`：逐项列出 Contract 中已完全冻结、可映射到 SPD 原生语义、仅允许最窄内部默认、必须保持 UNSUPPORTED/DEFERRED 的 support type。不得借此新增玩法。
- 冻结当前可运行的 Runtime smoke/headless/fuzz/存档样本，标注只用于 Legacy 回归。
- 引入 v6 开发 feature flag 或等价隔离入口，但不实现新 Gameplay 行为。


**代码处置**：KEEP 全部 Runtime/HUD/Headless 基础；LEGACY 标记当前公开 Schema/Builder/报告；本阶段不 DELETE 行为代码，仅加入防扩债边界。

**验收 Gate**：

- [ ] 源基线、父来源和所有文档 hash 可复现；若采用历史 commit，有完整选择证据和独立批准。
- [ ] 现有可运行测试/构建与输入基线等价；环境无法下载依赖时，必须记录准确失败点，不能记为代码 PASS。
- [ ] 旧报告不会被新 completion matrix 读取为玩家完成证据。
- [ ] v6 包/模块不依赖职业标签；legacy 与 v6 import 边界有自动检查。
- [ ] 没有 Gameplay 语义变化；checkpoint 解压复验通过。


**阶段输出**：`SPD_GC_V6_P00_BASELINE_CHECKPOINT.zip`。该 ZIP 只有在解压副本通过以上 Gate 后才可被下一阶段使用。

---

### P01 — Stable Identity、声明/状态分离、Dependency 与 Canonical Save Core

**目标**：先建立所有后续组件共同依赖的 v6 身份、声明、引用、依赖诊断和无损序列化基础，彻底阻断 v6 自动补绑。

**修改范围**：

- 实现 `StableId`、可注入/确定性 `IdGenerator`、`DisplayName` 验证、所有 Contract typed Ref。
- 实现 `DependencyState`、Resolver、Diagnostic、重复 ID/类型错误/非法循环/Unsupported 检测；Resolver 只读，不修改 Spec。
- 建立 `ClassBuildSpec` schema 6 和独立 `ClassRuntimeState`；玩家顺序与 node IDs 保存。
- 建立 Resource、Mark、ModeGroup、Mode、EntityCapacity、Entity、AbilityPool、Property、Recipe 等声明类型的 v6 数据边界；运行能力可暂为 DECLARED/UNSUPPORTED。
- 实现 rename/delete/rebind 的纯语义服务：rename 不改 ID，delete 不级联，rebind 必须显式。
- 实现 canonical semantic serializer/deserializer 与深等价 oracle；覆盖全部本阶段字段，禁止弱 fingerprint。
- Hero Bundle 分离 `class_build_spec_v6` 与 `class_runtime_state_v6`；未知更高版本明确 UNSUPPORTED。
- 建立 v5→v6 migration 框架、确定性 ID namespace 与 `MigrationReport` 骨架；具体 Variant 映射随阶段补齐。
- 新 v6 代码中零 `resolvePendingBindings`、零 first-item fallback；旧调用只能留在明确 legacy 隔离区。


**代码处置**：REWORK `ClassBuild`/Resource declaration-state；新建 typed refs/resolver/serializer；旧 `ClassBuild`、旧字段袋进入 LEGACY；v6 路径禁止自动补绑。

**验收 Gate**：

- [ ] ID 格式、唯一性、duplicate 新 ID、rename ID 不变、save/load 字节等价测试通过。
- [ ] Resource/Mark/Mode/Entity/Capacity/AbilityPool/Property 的 delete→UNRESOLVED、同名重建不接管、显式 rebind 测试通过。
- [ ] Canonical roundtrip 覆盖每个字段；修改任一语义字段必然改变 canonical output。
- [ ] Load/Resolver/Validator 对 Spec 零 mutation，有对象深拷贝或 immutable 断言。
- [ ] 未知 schema/variant 不会映射成默认效果。


**阶段输出**：`SPD_GC_V6_P01_IDENTITY_CHECKPOINT.zip`。该 ZIP 只有在解压副本通过以上 Gate 后才可被下一阶段使用。

---

### P02 — Player Builder Command/Reducer、Form Schema 与真实 UI 基础

**目标**：建立 UI 与 Headless 共享的唯一玩家构筑命令路径，结束 final-object assembler 与 Registry 自证。

**修改范围**：

- 实现 `BuilderState`、`BuilderCommand`、`BuilderReducer`、Navigation、Undo/Redo、command trace/replay。
- 命令输入只能是 primitive、typed ref、variant key、field key；禁止 `AddSkill(SkillSpec)` 或任何最终对象参数。
- 实现声明类命令：Resource、Mark、ModeGroup、Mode、EntityCapacity、Entity、AbilityPool、Property、Recipe 的 create/edit/rename/delete/rebind。
- 实现 `SaveDraft`、`LoadDraft`、`FinalizeBuild` 框架；draft 可 unresolved，finalize 按当前已实现能力 fail closed。
- 实现 FormSchema 基础类型、Number Stepper、Enum selector、Reference Picker、Nested Variant/List/Diagnostic field。
- 把 `WndCreateClass` 或等价玩家入口接到 v6 BuilderState；保留 v5 feature flag 仅作过渡，不允许 v5 completion 证明 v6。
- 实现 Headless `PlayerBuildSession`，只接受同一命令序列；command trace 可保存和重放。
- 建立 Builder 每次 command 后统一重算 Dependency、Validation 与 Budget draft ledger 的管线。


**代码处置**：REWORK `WndCreateClass` 控制流；REPLACE `PlayerBuildAssembler` 的玩家用途；KEEP 旧 assembler 仅 `qa.runtime`/legacy；引入新 Builder kernel。

**验收 Gate**：

- [ ] 从空白 session 通过 command 创建/编辑/rename/delete/rebind 所有声明；无直接 final object API。
- [ ] Undo/Redo、Save/Load draft、command replay 得到 canonical 等价结果。
- [ ] 删除引用后错误卡片仍显示 lastKnownName+短 ID；不会从 UI 消失或自动换绑。
- [ ] 数值字段使用 stepper，不生成 1..N 平铺长列表。
- [ ] UI 与 Headless 调用同一 Reducer，有代码级/测试级证据。


**阶段输出**：`SPD_GC_V6_P02_BUILDER_CHECKPOINT.zip`。该 ZIP 只有在解压副本通过以上 Gate 后才可被下一阶段使用。

---

### P03 — Typed Skill Language、Validation、Compiler 与 Executor Framework

**目标**：用强类型 Skill/Effect/Targeting/Delivery/Modifier/Cost 架构替代 v6 公开字段袋，并打通第一个真实 Player-path 技能。

**修改范围**：

- 实现 `SkillSpec v0.2`、typed Trigger、ConditionExpr/AllOf、EffectSpec interface、EffectChain、Delivery、Targeting、Modifier、Cost、SkillConstraint 的数据结构。
- 所有 Contract Variant 建立 descriptor/ImplementationState；未完成功能保持 DECLARED/UNSUPPORTED/DEFERRED，不在 Builder 暴露。
- 实现 Skill Builder commands：CreateSkill、选择 Trigger/Family/Variant、设置 typed fields/ref、Targeting、Delivery、Modifier、Cost、Constraint。
- 实现 StructuralValidator、CompatibilityValidator、RuntimeCapabilityValidator；诊断不修改 Spec。
- 实现 `ClassCompilePlan`、immutable compiled nodes、EffectResult/Preflight、typed Executor Registry；Registry 与 FormSchema 分离。
- 实现最小可用端到端垂直切片：Active/Always + 受支持 selector/coverage/filter + DirectDamage + 简单 delivery + NoCost。
- 为本阶段暴露项同步 formatter、en/zh、price key/ledger、canonical serialization、v5 migration placeholder 和真实 Headless 行为测试。
- 新 v6 Builder/Compiler 不得 import/依赖旧 `EffectVocabularyRegistry` 或旧 mega `EffectSpec`。


**代码处置**：REPLACE v6 public `EffectSpec`/RuleCondition/RuleCost/RuleModifier/SkillDelivery/Targeting 字段袋；KEEP legacy 类仅迁移；ADAPT RuleRuntime 通过 compile plan 调用 typed executor。

**验收 Gate**：

- [ ] 从空白 Builder command 构造一个技能，保存/加载、finalize、compile、真实 SPD Runtime 命中目标并产生明确 trace。
- [ ] v6 Effect Variant 无通用 `power/duration/stateId/templateId` 字段袋；每个字段归属于具体 Variant。
- [ ] 缺 executor、unsupported、无目标、blocked 可区分；无默认 Standard Damage。
- [ ] FormSchema 暴露不等于 implementation；Completion row 必须有实际证据 ID。
- [ ] 旧 Registry 的 playerReachable/implemented shortcut 不参与 v6 Gate。


**阶段输出**：`SPD_GC_V6_P03_TYPED_SKILL_CHECKPOINT.zip`。该 ZIP 只有在解压副本通过以上 Gate 后才可被下一阶段使用。

---

### P04 — 核心 Effect Families、Resource Transaction、HP 边界与基础 Class Components

**目标**：完成现阶段通用战斗语言的核心执行面，并以 Rage -2→Focus +5 证明强类型参数、原子事务和玩家路径真实成立。

**修改范围**：

- 完成 required Damage：Direct、PercentMaxHP、MissingHP、Execute；未冻结/未测试 policy 不暴露。
- 完成 Status 白名单与 ApplyStatus；禁止 Java Buff class player schema。
- 完成 Movement：Push/Pull/Throw/Dash/Teleport/SwapPosition，并统一合法 cell/碰撞失败语义。
- 完成 Recovery/Defense：Heal、Barrier、TemporaryHP、Mitigate、Redirect、Cleanse，冻结堆叠/结算顺序。
- 完成 ResourceOperation：Gain/Drain/Set/Clear/Convert/Reserve/Suppress；Hero holder 与 HUD 适配。
- 实现 `ResourceTransaction`，Convert/未来 Transfer 复用；EXACT_ATOMIC 是唯一 Convert 模式。
- 实现 BuiltinStatRef/ValueSpec/conditions 与 HP Cost；HP 不得进入 Resource declarations。
- 完成本阶段所需 Delivery、Targeting、filters、Repeat/Intensity/Extend/Pierce/Bounce、Resource/HP/Action/Cooldown/Item Cost。Delay/Echo 留 P06。
- 实现 BasicAttackComponent、ResourceFlowComponent、ActiveResourceOperationComponent、Resource ClassOperation 与稳定 HUD ID。
- 同步每项 formatter、en/zh、Budget entries、migration mapping、save/load 和行为测试。


**代码处置**：REWORK `SkillEffectRuntime` 为 typed executors；KEEP SPD 原生 helper；REWORK ResourceSpec/State、Class components/operations；旧 Convert/HP 路径仅迁移。

**验收 Gate**：

- [ ] Mandatory Test 1 的 Skill/ClassOperation 核心部分通过：Rage 5→3、Focus 0→5；source不足/target空间不足均原子不变。Device Payload 同 executor 的最终条款在 P07 关闭，因此本阶段不得把 Test 1 报为最终 COMPLETE。
- [ ] HP Cost/Low HP/Missing HP/Heal/Barrier/TempHP 全部证明不创建 HP Resource；Barrier/TempHP 不代付 HP Cost。
- [ ] 核心 Effect 每个 IMPLEMENTED row 都有 Layer A-E 证据；无 giant boolean coverage。
- [ ] Ammo Gunner、Blood Berserker、Area Caster 的最低通用 recipe 可由 commands 构造并真实运行；不使用职业标签。
- [ ] Resource HUD 使用 declaration+state 分离，rename 后显示更新且引用不变。


**阶段输出**：`SPD_GC_V6_P04_CORE_EFFECTS_CHECKPOINT.zip`。该 ZIP 只有在解压副本通过以上 Gate 后才可被下一阶段使用。

---

### P05 — 动态 Mark/Stack/Charge/Counter 与稳定 Mode Engine

**目标**：删除固定 HUNTED/CHARGED 与 raw mode strings 的玩家语义，完成自定义 Mark、Mode 及其所有引用点。

**修改范围**：

- 将 `RuleMark` Runtime 承载改为 dynamic markId；实现 MarkSpec/State 的 kind、范围、持续、refresh、overflow、provenance。
- 实现 Add/Set/Consume/Remove Mark；MarkCompare condition、MarkCost、HasMarkFilter、MarkValueSource、Transfer placeholder ref。
- 任何 invalid/deleted Mark 返回 UNRESOLVED/BLOCKED，绝不回退 HUNTED。固定 enum 仅作为 v5 migration alias。
- 实现 ModeGroup/ModeSpec/ModeState、同组互斥、ModeShift、ModeActiveCondition、ModeEngineComponent、稳定 ModeSwitch ClassOperation。
- 动态名称直接显示，不拼本地化 key；rename/delete/rebind/save-load 全链。
- 补齐 v5 Mark/Mode migration mapping 和本阶段 Budget/Formatter/Localization。


**代码处置**：KEEP `RuleMark`/`RuleMode` 的 Buff/Bundle 基础但 REWORK identity；LEGACY 固定 enum/raw strings；DELETE v6 fallback。

**验收 Gate**：

- [ ] Mandatory Test 2 全部通过，包括“灼痕”Add 2、threshold、consume 3、rename/delete/same-name/rebind/save-load。
- [ ] Mandatory Test 9 全部通过，包括同组互斥、active state save/load、动态名称无 NO TEXT FOUND。
- [ ] Mandatory Test 10 对 Resource/Mark/Mode 的参数化路径通过。
- [ ] 全仓新 v6 player/runtime 路径无 `RuleMark.Type` fallback、无 raw mode name identity。
- [ ] Budget rename/order invariance 与 formatter 诊断通过。
- [ ] Assassin/Hunt 与 Transform/Bio 的最低 recipes 分别以自定义 Mark 和自定义 Mode 完成，关闭 Recipe 5 与 10。


**阶段输出**：`SPD_GC_V6_P05_MARK_MODE_CHECKPOINT.zip`。该 ZIP 只有在解压副本通过以上 Gate 后才可被下一阶段使用。

---

### P06 — 统一 EffectChain/Payload、Delay/Echo 与 Action Attachment

**目标**：把即时、延迟、回声和下一次行动统一成同一 EffectChain 语义，修复 Primary/Secondary 被拆分和存档重复执行。

**修改范围**：

- 完成 EffectChain primary+optional secondary 及 activation：Immediate、DelayAfterPrimarySuccess、OnNextActionAfterPrimarySuccess。
- 完成 `PayloadSpec`，Skill immediate execution 也通过统一 payload/chain 执行。
- 实现 TargetBindingPolicy：ACTOR_ID_LIVE、CELL_SNAPSHOT、SELECTOR_REEVALUATE。
- 重做 ScheduledPayloadState、DelayModifier、EchoModifier，保存整个 chain、origin IDs、cause、remaining、execution count。
- 实现 ActionAttachmentDelivery/State 与下一次行动 charge/expiry；不得复制已支付 Cost。
- 固定 wrapper 执行顺序、Primary 成功条件、同一 actor identity、save/load 中间态和递归 guard。
- 适配现有 RuleDelayedPayload/RuleContext/Trace，保留其 Actor/Bundle/causality 基础。
- 同步 Builder Payload/Secondary editor、Formatter、Budget、Localization、Migration。


**代码处置**：KEEP/ADAPT RuleDelayedPayload 与因果 guard；REPLACE Primary/Secondary 分拆调度；NEW unified Payload/Attachment runtime。

**验收 Gate**：

- [ ] Mandatory Test 6 完整通过：Selected Cell、3 turns、任意 supported EffectChain、单 ScheduledPayload、save/load 后精确一次。
- [ ] Mandatory Test 7 完整通过：受击减伤、下一次 ATTACK_HIT 附加 Poison、charge/expiry/save-load/不递归。
- [ ] Primary BLOCKED/FAILED 时 Secondary 不创建；Immediate secondary 保持 actor identity。
- [ ] Delay/Echo 对任意已支持 Effect 不需要新增专用 enum。
- [ ] Canonical RuntimeState 能检测 remaining、binding、origin、attachment charge 的任何丢失。
- [ ] Martial Defender Recipe 的 Counter Attachment 最低机制完成，从而关闭 Recipe 1。


**阶段输出**：`SPD_GC_V6_P06_PAYLOAD_TIMING_CHECKPOINT.zip`。该 ZIP 只有在解压副本通过以上 Gate 后才可被下一阶段使用。

---

### P07 — 统一 Entity、Ownership/Relation/Capacity/Persistence 与 World/Control

**目标**：把 Actor/Device/Trap/Field/Carrier 统一为真实 EntitySpec/Instance，并让全部载体复用 Payload/Effect executor。

**修改范围**：

- 实现 EntitySpec/Instance、typed bodies：Actor、Device、Trap、Field、Carrier；CORPSE 本阶段仅识别声明类型，Death chain 留 P11。
- 实现 Behavior whitelist：FollowOwner、GuardCell、AttackNearest、Stationary；完整 Behavior Override 保持 DEFERRED。
- 实现 Entity triggers、ResourceStorage capability、ownership/relation/link、capacity、persistence、creation/spawn policies。
- 适配 `RuleOwnedEntity`、`RuleCarrierTrap`、DirectableAlly、scheduler 与 Bundle；实例保存 blueprint/owner/source provenance。
- 实现 CreateEntityEffect、PersistentCarrierDelivery；Device/Trap/Field/Carrier 直接使用 PayloadSpec。
- 实现 World/Terrain required variants、capability validation；范围只由 Targeting Coverage 决定。
- 实现 Relation/Control required variants：assign/break ownership、link、command；InheritCapability 保持 UNSUPPORTED。
- 完成 Ownership/Capacity/Persistence/Command/Recycle、ClassOperationGrant、ClassRule 等 Contract required Class components/operations 与 HUD；ClassRule 只能组合通用 Trigger/Condition/Payload，不得隐藏 Law/Domain。
- 使用 Device Payload 再执行一次 P04 的 ConvertResourceSpec 2→5，证明 Skill/Class Flow/Device 共用同一 executor 和 transaction。
- 同步 formatter/budget/localization/migration/save-load。


**代码处置**：KEEP RuleOwnedEntity/Trap/Ownership/DirectableAlly 接入基础；REWORK 为 blueprint/instance；DELETE v6 固定 payload；LEGACY mapper 留迁移。

**验收 Gate**：

- [ ] Mandatory Test 3 Trap Enter→Push2→Poison 完整通过，Secondary 对同一进入者。
- [ ] Mandatory Test 4 Device 每3回合 Radius2 Shield Allies 完整通过，包括周期/过滤/lifetime/save-load。
- [ ] Mandatory Test 5 Device 周期增加自定义 Resource 完整通过，并复用 P04 executor。
- [ ] Mandatory Test 1 的 Device Payload parity 条款通过，至此 Test 1 才可标记 COMPLETE。
- [ ] Owned Summoner、Engineer、Controller、Support 最低 recipes 从 commands 构造并真实运行。
- [ ] v6 路径无 fire/poison/heal carrier payload string、无 Device/Trap/Field 专用 effect enum；Trap/Corpse 可进入 EntityFilter/Capacity 模型。


**阶段输出**：`SPD_GC_V6_P07_ENTITY_WORLD_CHECKPOINT.zip`。该 ZIP 只有在解压副本通过以上 Gate 后才可被下一阶段使用。

---

### P08 — Transfer/Copy/Swap 与 Capability 安全边界

**目标**：完成明确对象类型的转移/复制语义，关闭“复制第一个状态”“任意 Buff/对象图复制”等不安全路径。

**修改范围**：

- 实现 Resource Transfer：typed source/destination holder/resource、EXACT_ATOMIC、destination overflow、ResourceStorage capability。
- 实现明确 StatusRef 的 Status Copy 与 copy policy；只允许 catalog 中 copyable 状态。
- 实现自定义 Mark Transfer，精确 amount 与原子性。
- 实现 Barrier Swap，仅交换 Barrier；HP/TemporaryHP 不变。
- 建立 Capability catalog/ref/validator 的基础和 forbidden object-copy tests。
- Capability Override 继续 UNSUPPORTED、Behavior Override DEFERRED；不得为 coverage 暴露。
- Snapshot/Ability/Property transfer variants 仅保留 typed placeholder/UNSUPPORTED，待 P11。
- 同步 Builder、formatter、budget、localization、migration、save-load。


**代码处置**：REWORK old transfer/copy switches；KEEP typed Runtime helpers；NEW capability catalog；DELETE first-status copy/default object clone。

**验收 Gate**：

- [ ] Mandatory Test 8 A-D 全部通过，并从 Builder commands 构造。
- [ ] Resource/Mark transfer source不足或目标不合法时两边均不变；trace 含 transaction ID。
- [ ] Status Copy 只复制明确状态，source 其它状态不动。
- [ ] 任意 Buff class、Java object graph、AI state、反射复制在 Builder/validator/architecture tests 被拒绝。
- [ ] Capability/Behavior 未完成项不出现在玩家菜单、不报价、不计完成。


**阶段输出**：`SPD_GC_V6_P08_TRANSFER_COPY_CHECKPOINT.zip`。该 ZIP 只有在解压副本通过以上 Gate 后才可被下一阶段使用。

---

### P09 — Skill Constraint、Class Vow 真执法与统一 Budget Ledger

**目标**：让 Tradeoff 成为真实、不可轻易旁路的限制，并建立唯一、可解释、可 roundtrip 的预算权威。

**修改范围**：

- 实现 required SkillConstraint：每层次数、站立回合、事件窗口、最小 cooldown、专属 Mode。
- 实现 ClassConstraint/Vow：NoBasicAttack、NoMovement、EmptyEquipmentSlot、NoHealing、WeaponCategoryForbidden、StandStill、MaximumSkillUses、NoItemUse。
- 在 action commit 前接入 BasicAttack/Move/Equipment/Heal/Item/Skill/FloorReset/SaveLoad enforcement hooks；硬阻止并给玩家反馈。
- 将旧 WAIT_CLEARS_RESOURCE、TARGET_MARKED、LOW_HP、IN_WATER、COOLDOWN、LIMITED_USE 按 Contract 正确迁移；无执法 HP_COMMITMENT credit=0。
- 完成中央 BudgetCatalog `gameplay-v0.2.0`、BudgetLedger、每 node entry、typed parameter price、priceVersion diff。
- 实现 EffectiveConstraintValue：冗余、旁路、有效时间、主动规避、包含关系、测试证据；无证据 credit=0。
- 为此前所有 Player-exposed variants 补齐唯一 price key；未定价项改 UNSUPPORTED，而非临时 0 价。
- 同步 Builder、formatter、localization、save-load、migration 和 bypass tests。


**代码处置**：REPLACE old Restriction/SkillConstraint 假返还；KEEP必要 Hook 接入风格；NEW BudgetCatalog/Ledger；DELETE no-op rebate。

**验收 Gate**：

- [ ] Mandatory Test 12 全部通过；2→5 Convert、Device 周期/radius/payload/lifetime 可分项解释。
- [ ] NoBasicAttack 等 Vow 必须实际 block 才有 credit；BasicAttack 已 NONE 时重复 constraint credit=0。
- [ ] NoHealing 的已声明 source 范围具有 Potion/Skill/Item/Buff/ClassOperation 旁路测试；未覆盖范围在 UI 明示并相应扣减 credit。
- [ ] Rename/order/save-load 不改变 ledger；删除 dependency 不静默移除成本。
- [ ] Archetype 15 至少两类 Vow 从 Builder 到 Runtime/credit 全链通过。


**阶段输出**：`SPD_GC_V6_P09_VOW_BUDGET_CHECKPOINT.zip`。该 ZIP 只有在解压副本通过以上 Gate 后才可被下一阶段使用。

---

### P10 — 完整 v5→v6 Migration、Typed Formatter/Localization Sweep 与旧公开路径退役

**目标**：在新增复杂 Primitive 前清除双模型污染：旧数据可迁移，但旧 Schema/Builder/QA 不再驱动新玩家系统或完成结论。

**修改范围**：

- 完成所有已实现 v5→v6 mapping：Resource、Skill/Class Convert、Mark、Mode、Entity payload、AREA、Consumable、旧 Constraint、auto-bound 空引用。
- Migration 必须可重放、幂等、确定性，输出 MigrationReport；行为收紧/不一致明确 warning，不静默美化。
- 完成 typed formatter 和至少 en/zh_CN 的所有 exposed Variant、字段、诊断、Budget、Builder 页面与 Build Sheet。
- 完成 resolved/unresolved/unsupported/min/default/max/中文自定义名的 localization sweep。
- 旧 Builder 退役或转为只读 migration entry；`PlayerBuildAssembler` final-object interface 不再进入玩家/acceptance 包。
- 移除所有非 legacy/migration 的 `resolvePendingBindings`、first-resource/mode、Mark fallback、fixed carrier payload、AREA modifier、Java class-name cost、completion shortcut。
- 完成当前范围的 Full Save/Load roundtrip 与 migration fixtures；新 P11 states 可后续扩展。
- 更新 completion matrix 证据结构，但不得提前宣告 final complete。


**代码处置**：LEGACY 旧字段袋/enum/string 仅留 `migration/v5`；DELETE 所有 public/Runtime fallback；REWORK Formatter/Localization；RETIRE v5 Builder。

**验收 Gate**：

- [ ] Mandatory localization sweep 当前全部 exposed surface 通过：零 `NO TEXT FOUND`、`�`、raw enum、Java class、placeholder。
- [ ] 代表性 v5 saves 构建→迁移→v6 load→canonical→runtime 行为验证通过；unknown 保持 unsupported/unresolved。
- [ ] 静态搜索在 legacy/migration 之外零命中：`resolvePendingBindings`、player-facing `RuleMark.Type`、carrier payload string、AREA modifier、first-item fallback、`implemented() { return true; }` completion shortcut。
- [ ] 旧 UI/QA 不再能产生 v6 `IMPLEMENTED` 行。
- [ ] Checkpoint 解压后 migration、localization、core roundtrip 重跑通过。


**阶段输出**：`SPD_GC_V6_P10_MIGRATION_LOCALIZATION_CHECKPOINT.zip`。该 ZIP 只有在解压副本通过以上 Gate 后才可被下一阶段使用。

---

### P11 — 新增通用 Primitive：Snapshot、Corpse、Observation/Ability、Property/Synthesis

**目标**：以最小、跨构筑、声明式实现覆盖新增 5 个参考 Archetype 所需底层能力，同时严格保持内容库和高层系统 Deferred。

**修改范围**：

- Temporal：Capture/Restore Snapshot、whitelist fields、position/resource/mark/mode/status policy、snapshot state/save-load；Delay/Echo 已复用 P06。
- Death Residue：统一 DEATH event、Corpse Entity/instance/provenance/persistence/filter/EntityCost/consume；消费 Corpse 创建声明 Actor 可做，复活原 identity/AI/装备 Deferred。
- Observation/Ability：AbilityDescriptor/Signature、ObservationSpec、AbilityPool、provenance、capacity、learned state；只接入至少一个真实敌技 declarative adapter。
- Property/Ingredient：PropertySpec/inventory/provenance、DecomposeRule、SynthesisRecipe、Imbue/PropertyCost；至少一个 item/entity/world 输入与一个通用输出。
- 补齐 AbilityChargeCost、PropertyCost、AbilityPoolCount condition、相关 refs/dependency/budget/formatter/localization/builder/runtime/save/migration。
- Capability 安全：禁止反射、Java class、lambda、AI/object graph capture；unsupported enemy ability 明确返回。
- 完成 Archetype 11-14 最小 player-path recipes；15 的 Vow 已在 P09。


**代码处置**：NEW generic declarations/runtime adapters；KEEP P06/P07 infrastructure；Capability/Behavior arbitrary override 继续 UNSUPPORTED/DEFERRED；无职业专用系统。

**验收 Gate**：

- [ ] 悟道者：至少一个真实敌技 descriptor 被按观察条件捕获入 pool，容量/provenance/charges/save-load 正确，无 BLUE_MAGE_DOMAIN。
- [ ] 炼成师：至少一个 Decompose→Property→Synthesis/Imbue 端到端，事务与来源可保存，无完整内容库扩张。
- [ ] 时序术士：Snapshot/Restore whitelist 端到端，死亡不复活、非法位置按明确 policy、save-load 精确。
- [ ] 尸骸利用者：DEATH→Corpse→EntityCost→Effect/Resource 至少一条真实链，Corpse 不是 Summon Actor。
- [ ] Test 10 扩展到 AbilityPool/Property；Test 11 加入 snapshot/learned/property/corpse state；安全负面测试通过。


**阶段输出**：`SPD_GC_V6_P11_GENERIC_PRIMITIVES_CHECKPOINT.zip`。该 ZIP 只有在解压副本通过以上 Gate 后才可被下一阶段使用。

---

### P12 — 全量对抗性验收、15 Recipes、Fuzz/Build/实机 UX 与 FINAL Checkpoint

**目标**：不再新增架构和玩法，只收敛缺陷；用真实玩家路径和真实 SPD Runtime 证明 Contract 达成。

**修改范围**：

- 冻结 feature surface 与 priceVersion；只修测试、行为、文本、迁移、兼容、性能和 UI 缺陷。
- 运行 12 项 Mandatory Adversarial Player-path Suite、Localization Sweep、15 Archetype Recipe Suite。
- 运行 QA Layers A-G：Schema、Builder、Dependency/Migration/Formatter/Budget、Headless Runtime、Save/Load、Fuzz/Integrity、Manual UX。
- Fuzzer 以 BuilderCommand 序列为主，单独报告 player_command_valid_density、finalized density、runtime failures、unresolved drafts、budget exploits、causal loops、roundtrip failures。
- 运行 core tests、desktop build/run、Android debug build/真机或 AVD smoke；记录实际环境和退出码。
- 完成全量 canonical Build/Runtime roundtrip，与未保存控制组比较后续行为。
- 生成 evidence-backed completion matrix；只有 15 条 DoD 全满足的 Variant 标 IMPLEMENTED。
- 执行 final static searches、完整 checkpoint ZIP、解压复验和独立 Pro 最终审查。


**代码处置**：不再新增模型；DELETE 剩余临时 adapter/feature flag（migration-only 除外）；KEEP final v6 + migration reader + Runtime/QA assets。

**验收 Gate**：

- [ ] Tests 1-12 与 localization sweep 全部 PASS，无跳过核心行为。
- [ ] 15 recipes 全部由 BuilderCommand 从空白构造并在真实 Runtime 运行；Deferred 部分以最小 primitive fixture 验证且明确不计内容库。
- [ ] Canonical Build/Runtime roundtrip diff=empty；mid-runtime control group 行为等价。
- [ ] 零职业 Domain、零 silent fallback、零 public legacy path、零 completion shortcut。
- [ ] Core/Desktop/Android/Manual Builder smoke 有实际证据；环境阻塞必须解决后才能 FINAL ACCEPT。
- [ ] 独立 Pro Reviewer 从源码和玩家路径复核通过，而不是只审报告。


**阶段输出**：`SPD_GC_V6_P12_FINAL_ACCEPTED.zip`。该 ZIP 只有在解压副本通过以上 Gate 后才可被下一阶段使用。

---


## 7. Mandatory Tests 与阶段映射

| Contract 验收 | 首次完成阶段 | 最终重跑 |
|---|---:|---:|
| Test 1 Resource Convert 2→5 | P04 完成 Skill/Class 核心；P07 完成 Device parity | P12 |
| Test 2 自定义 Mark“灼痕” | P05 | P12 |
| Test 3 Trap Enter→Push→Poison | P07 | P12 |
| Test 4 Device period/radius/shield allies | P07 | P12 |
| Test 5 Device 增加自定义 Resource | P07 | P12 |
| Test 6 Delay arbitrary EffectChain | P06 | P12 |
| Test 7 Action Attachment / Counter | P06 | P12 |
| Test 8 Transfer/Copy/Swap 四子项 | P08 | P12 |
| Test 9 自定义 Mode | P05 | P12 |
| Test 10 Stable References | P01/P02 起，P05/P07/P11扩展 | P12 |
| Test 11 Full Save/Load | 每阶段增量；P10当前面，P11补新状态 | P12 |
| Test 12 Budget/Constraint | P09 | P12 |
| Localization Sweep | 每阶段新增项；P10全扫 | P12 |
| 15 Archetype Recipes | 分阶段形成；P11齐备 | P12 |

### 7.1 15 Archetype Recipe 关闭阶段

| # | Recipe | 首次完整关闭阶段 |
|---:|---|---:|
| 1 | Martial Defender | P06 |
| 2 | Blood Berserker | P04 |
| 3 | Ranger / Ammo Gunner | P04 |
| 4 | Mage / Area Caster | P04 |
| 5 | Assassin / Hunt | P05 |
| 6 | Summon / Owned Summoner | P07 |
| 7 | Engineer / Automation | P07 |
| 8 | Controller / Terrain | P07 |
| 9 | Support / Defense | P07 |
| 10 | Transform / Bio（Mode required；Capability/Behavior 按 Contract 保持 unsupported/deferred） | P05 |
| 11 | 悟道者 / Enemy-Ability Learner | P11 |
| 12 | 炼成师 / Decompose & Synthesis | P11 |
| 13 | 时序术士 / Temporal Manipulator | P11 |
| 14 | 尸骸利用者 / Death Residue User | P11 |
| 15 | 苦行者 / Vow | P09 |

所有 Recipe 必须在 P12 从空白 BuilderCommand 再次重放。

### 7.2 QA 分层要求

- **Layer A**：typed Schema / validation unit；
- **Layer B**：BuilderCommand/Reducer/undo/replay；
- **Layer C**：Dependency/Migration/Formatter/Budget；
- **Layer D**：真实 SPD Headless Runtime；
- **Layer E**：Build/Runtime canonical Save/Load；
- **Layer F**：Builder-command Fuzz/Integrity/exploit；
- **Layer G**：人工 Builder UX、desktop/android smoke。

A-E 是 IMPLEMENTED 的必要证据；F/G 额外必需，但不能替代 A-E。

### 7.3 每个 Variant 的证据行

```text
variantKey
state
schemaTestId
builderPathTestId
dependencyTestId
formatterTestId
budgetTestId
saveLoadTestId
runtimeBehaviorTestId
adversarialTestId
```

任何证据 ID 为空，state 不能是 IMPLEMENTED。

---

## 8. Pro 独立复审节点

### PRO-R1 — P02 后：身份与 Builder 架构

重点不是 UI 好看，而是检查：

- Stable ID/Ref/UNRESOLVED 是否真的不可变；
- BuilderCommand 是否接受 final object；
- UI 与 Headless 是否同一 Reducer；
- Save/Load/Resolver 是否 mutation/auto-bind；
- FormSchema 是否又变成对象快照列表。

R1 不通过，不得开始大规模 Effect 实现。

### PRO-R2 — P05 后：核心 Gameplay Language

独立重放 Tests 1、2、9，检查：

- Convert 2→5 是否真正两数量、原子事务；
- HP 是否没有被复制为 Resource；
- Mark/Mode 是否动态稳定引用；
- typed fields 是否又被 generic `power` 替代；
- completion rows 是否有真实 Layer A-E 证据。

### PRO-R3 — P07 后：Payload/Timing/Entity 架构

独立重放 Tests 3-7，检查：

- Skill/Device/Trap/Field/Carrier/Delay/Attachment 是否同一 EffectChain；
- Trap Secondary 是否绑定同一进入者；
- Device targeting/filter/lifetime/period 是否来自 typed Schema；
- save/load 是否重复/丢失 scheduled payload；
- 是否仍有专用 payload enum/string。

### PRO-R4 — P10 后：Transfer、Vow、Budget、Migration、Legacy 退役

重点检查：

- Test 8/12 行为和原子性；
- Vow 是否真实阻止且无明显旁路；
- Budget 是否单一 Catalog；
- v5 migration 是否忠实而非美化；
- localization sweep；
- legacy/public fallback 静态搜索。

### PRO-R5 — P11 后：新增 Primitive 的通用性与安全

重点检查：

- 是否出现 BLUE_MAGE/ALCHEMY/TEMPORAL/CORPSE 职业 Domain；
- Ability capture 是否只复制 descriptor；
- Property 是否不是换名 Resource；
- Corpse 是否真实 Entity 而非 Summon；
- Snapshot 是否 whitelist、不可复活、不可复制对象图；
- 高层内容库是否被错误扩张。

### PRO-R6 — P12：最终独立验收

Reviewer 必须从源码和空白 Builder 重新抽样，不以实施报告为事实源。只有 `ACCEPT` 才能称为 Gameplay Components v6 Final。

如 P00 选择了历史 commit 而非当前审计快照，必须额外执行 **PRO-R0 基线复审**。

---

## 9. 风险与阻断规则

| 风险 | 典型症状 | 阻断措施 |
|---|---|---|
| 双模型污染 | v6 Spec 转成旧 EffectSpec 执行 | P03/P10 architecture import gate |
| Registry 自证 | FormSchema exposed 即 coverage PASS | Executor evidence registry 独立；completion row 强制 IDs |
| 自动补绑残留 | load/打开 UI 后引用“自己好了” | Resolver immutability + delete/same-name tests + static search |
| Entity 再硬编码 | Device 多几个 Stun/Slow enum | Tests 3-5 必须任意 EffectChain；无专用 payload |
| Save/Load 假等价 | 弱 fingerprint相同 | canonical every-field serializer；mutation sensitivity tests |
| Constraint 假返还 | 只显示负面文案就加预算 | enforcement hook + bypass tests + no evidence credit=0 |
| Migration 美化历史 | fire 被改 Burning、partial convert被隐藏 | MigrationReport behavior warning；旧实际语义 fixtures |
| 新 Archetype 变 Domain | BLUE_MAGE 等 tag 分支 | generic recipe + source search + Pro-R5 |
| QA 只测无 crash | 1000 fuzz 0 failure | 12 adversarial player-path 为主，Fuzz仅辅助 |
| 阶段越界 | P04 顺手做 Talent/复杂 AI | Gate diff review；越界代码退回或保持 unsupported |

---

## 10. 最终发布 Gate

P12 只有同时满足以下条件才可输出 FINAL：

1. Schema=6、Contract=`0.2-final`；
2. 12 Mandatory Tests 全过；
3. Localization Sweep 全过；
4. 15 Recipes 全部 command-built + real Runtime；
5. canonical Build/Runtime roundtrip diff=empty；
6. v5→v6 migration corpus通过；
7. Budget Catalog完整、可解释、无无效 credit；
8. desktop/android/core/headless/fuzz/manual UX 有实际证据；
9. legacy/public fallback 静态搜索通过；
10. Completion Matrix 每个 IMPLEMENTED 行证据齐全；
11. Deferred/Unsupported 清单准确；
12. final ZIP 解压复验通过；
13. 独立 Pro-R6 `ACCEPT`。

在此之前，允许的项目状态描述只能是“v6 Phase N checkpoint”，不得宣布“全组件完成”或“coverage complete”。

---

## Appendix A — 各阶段可直接交给新 Codex 的 Prompt

以下 Prompt 均已另存为独立文件，正文也在本计划交付包中。使用时必须同时提供上一 accepted checkpoint、Audit、FINAL Contract 与本计划。

- `P00_baseline_legacy_freeze_CODEX_PROMPT.md` — 基线确定、Legacy 冻结与防扩债
- `P01_identity_dependency_save_CODEX_PROMPT.md` — Stable Identity、声明/状态分离、Dependency 与 Canonical Save Core
- `P02_builder_kernel_CODEX_PROMPT.md` — Player Builder Command/Reducer、Form Schema 与真实 UI 基础
- `P03_typed_skill_compiler_CODEX_PROMPT.md` — Typed Skill Language、Validation、Compiler 与 Executor Framework
- `P04_core_effects_resource_hp_CODEX_PROMPT.md` — 核心 Effect Families、Resource Transaction、HP 边界与基础 Class Components
- `P05_mark_mode_stable_refs_CODEX_PROMPT.md` — 动态 Mark/Stack/Charge/Counter 与稳定 Mode Engine
- `P06_payload_timing_attachment_CODEX_PROMPT.md` — 统一 EffectChain/Payload、Delay/Echo 与 Action Attachment
- `P07_entity_world_relation_CODEX_PROMPT.md` — 统一 Entity、Ownership/Relation/Capacity/Persistence 与 World/Control
- `P08_transfer_copy_capability_CODEX_PROMPT.md` — Transfer/Copy/Swap 与 Capability 安全边界
- `P09_constraints_vow_budget_CODEX_PROMPT.md` — Skill Constraint、Class Vow 真执法与统一 Budget Ledger
- `P10_migration_formatter_localization_retire_legacy_CODEX_PROMPT.md` — 完整 v5→v6 Migration、Typed Formatter/Localization Sweep 与旧公开路径退役
- `P11_new_generic_primitives_CODEX_PROMPT.md` — 新增通用 Primitive：Snapshot、Corpse、Observation/Ability、Property/Synthesis
- `P12_final_acceptance_CODEX_PROMPT.md` — 全量对抗性验收、15 Recipes、Fuzz/Build/实机 UX 与 FINAL Checkpoint


另附：`PRO_INDEPENDENT_REVIEW_PROMPT_TEMPLATE.md`。

---

# FINAL PLANNING STATEMENT

这条施工路线的核心不是“把旧 210 个条目补齐”，而是先重建能被玩家真实编辑、引用、保存和执行的语言骨架，再逐层迁移已有 Runtime 资产。

任何阶段都不得用旧 Registry、final-object fixture、无 crash 或报告数字替代玩家路径。只有 P12 与独立 Pro-R6 同时通过，Gameplay Components v6 才达到 `FINAL Implementation Contract v0.2` 所定义的完成状态。
