# Codex 执行 Prompt — P11 新增通用 Primitive：Snapshot、Corpse、Observation/Ability、Property/Synthesis

你现在执行 **Shattered Pixel Dungeon 自塑职业 Gameplay Components v6 — P11**。

## 输入与唯一基线

- 源码输入：`SPD_GC_V6_P10_MIGRATION_LOCALIZATION_CHECKPOINT.zip`
- 冻结审计：`SPD_CLASS_GAMEPLAY_COMPONENTS_CURRENT_IMPLEMENTATION_AUDIT_v0.2.md`
- 冻结合约：`SPD_CLASS_GAMEPLAY_COMPONENTS_IMPLEMENTATION_CONTRACT_v0.2_FINAL.md`
- 开发计划：`SPD_GAMEPLAY_COMPONENTS_V6_DEVELOPMENT_PLAN_FINAL.md`

本阶段目标：**以最小、跨构筑、声明式实现覆盖新增 5 个参考 Archetype 所需底层能力，同时严格保持内容库和高层系统 Deferred。**

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

- 从 P10 accepted checkpoint 开始。
- 严格按 Contract 第 21.6-21.7、31、32 与 Recipe 11-14 实施最小通用垂直切片。
- 不开发完整悟道敌技库、完整炼成配方库、跨局尸体、原 actor 复活、Temporal Domain、Nemesis 或 progression。
- 每个新 Primitive 必须从 Builder commands 建立声明/技能，经过 save/finalize/headless/mid-runtime save-load；不能用内部 fixture 冒充。
- Ability capture 只接受显式 `AbilityDescriptor` adapter；未知能力必须 UNSUPPORTED。
- 返回四个 recipe 的 command traces、provenance snapshots、安全拒绝测试和 completion rows。

## 本阶段明确范围

- Temporal：Capture/Restore Snapshot、whitelist fields、position/resource/mark/mode/status policy、snapshot state/save-load；Delay/Echo 已复用 P06。
- Death Residue：统一 DEATH event、Corpse Entity/instance/provenance/persistence/filter/EntityCost/consume；消费 Corpse 创建声明 Actor 可做，复活原 identity/AI/装备 Deferred。
- Observation/Ability：AbilityDescriptor/Signature、ObservationSpec、AbilityPool、provenance、capacity、learned state；只接入至少一个真实敌技 declarative adapter。
- Property/Ingredient：PropertySpec/inventory/provenance、DecomposeRule、SynthesisRecipe、Imbue/PropertyCost；至少一个 item/entity/world 输入与一个通用输出。
- 补齐 AbilityChargeCost、PropertyCost、AbilityPoolCount condition、相关 refs/dependency/budget/formatter/localization/builder/runtime/save/migration。
- Capability 安全：禁止反射、Java class、lambda、AI/object graph capture；unsupported enemy ability 明确返回。
- 完成 Archetype 11-14 最小 player-path recipes；15 的 Vow 已在 P09。


## 本阶段验收 Gate

- [ ] 悟道者：至少一个真实敌技 descriptor 被按观察条件捕获入 pool，容量/provenance/charges/save-load 正确，无 BLUE_MAGE_DOMAIN。
- [ ] 炼成师：至少一个 Decompose→Property→Synthesis/Imbue 端到端，事务与来源可保存，无完整内容库扩张。
- [ ] 时序术士：Snapshot/Restore whitelist 端到端，死亡不复活、非法位置按明确 policy、save-load 精确。
- [ ] 尸骸利用者：DEATH→Corpse→EntityCost→Effect/Resource 至少一条真实链，Corpse 不是 Summon Actor。
- [ ] Test 10 扩展到 AbilityPool/Property；Test 11 加入 snapshot/learned/property/corpse state；安全负面测试通过。


## 代码处理原则

NEW generic declarations/runtime adapters；KEEP P06/P07 infrastructure；Capability/Behavior arbitrary override 继续 UNSUPPORTED/DEFERRED；无职业专用系统。

## 必须交付

1. `SPD_GC_V6_P11_GENERIC_PRIMITIVES_CHECKPOINT.zip`：包含完整当前工作区源码，不是 patch-only。
2. `V6_CHECKPOINT_MANIFEST.json`：父 checkpoint hash、当前 ZIP hash、Audit/Contract hash、schema/price versions、测试命令与结果。
3. `PHASE_P11_IMPLEMENTATION_REPORT.md`：实际修改文件、行为变化、Gate 逐项证据、未通过项、仍 Deferred/Unsupported 项。
4. BuilderCommand traces、canonical serialization artifacts、Runtime traces 与测试报告。
5. 对 checkpoint 重新解压后的复验结果。

完成后立即停止，不执行下一阶段。
