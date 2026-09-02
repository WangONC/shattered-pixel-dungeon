# Codex 执行 Prompt — P07 统一 Entity、Ownership/Relation/Capacity/Persistence 与 World/Control

你现在执行 **Shattered Pixel Dungeon 自塑职业 Gameplay Components v6 — P07**。

## 输入与唯一基线

- 源码输入：`SPD_GC_V6_P06_PAYLOAD_TIMING_CHECKPOINT.zip`
- 冻结审计：`SPD_CLASS_GAMEPLAY_COMPONENTS_CURRENT_IMPLEMENTATION_AUDIT_v0.2.md`
- 冻结合约：`SPD_CLASS_GAMEPLAY_COMPONENTS_IMPLEMENTATION_CONTRACT_v0.2_FINAL.md`
- 开发计划：`SPD_GAMEPLAY_COMPONENTS_V6_DEVELOPMENT_PLAN_FINAL.md`

本阶段目标：**把 Actor/Device/Trap/Field/Carrier 统一为真实 EntitySpec/Instance，并让全部载体复用 Payload/Effect executor。**

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

- 从 P06 accepted checkpoint 开始。
- 严格按 Contract 第 10、14.7-14.9、16 PersistentCarrier、22.5、23、30.5 实施。
- 不允许为 Device/Trap/Field 新建 effect string/enum；所有 payload 必须能引用任意已经 IMPLEMENTED 的 EffectChain。
- Actor blueprint 只实现 Contract whitelist 的最小 declarative behavior；成长、复杂 AI、召唤内容库不在本阶段。
- CORPSE 类型要能被 schema/filter/save 识别，但不得提前实现死亡残留玩法。
- 用实际 UI/Builder command trace生成 Tests 3-5，并在真实 Headless Level 中断言 cell、周期、relation、capacity、persistence。

## 本阶段明确范围

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


## 本阶段验收 Gate

- [ ] Mandatory Test 3 Trap Enter→Push2→Poison 完整通过，Secondary 对同一进入者。
- [ ] Mandatory Test 4 Device 每3回合 Radius2 Shield Allies 完整通过，包括周期/过滤/lifetime/save-load。
- [ ] Mandatory Test 5 Device 周期增加自定义 Resource 完整通过，并复用 P04 executor。
- [ ] Mandatory Test 1 的 Device Payload parity 条款通过，至此 Test 1 才可标记 COMPLETE。
- [ ] Owned Summoner、Engineer、Controller、Support 最低 recipes 从 commands 构造并真实运行。
- [ ] v6 路径无 fire/poison/heal carrier payload string、无 Device/Trap/Field 专用 effect enum；Trap/Corpse 可进入 EntityFilter/Capacity 模型。


## 代码处理原则

KEEP RuleOwnedEntity/Trap/Ownership/DirectableAlly 接入基础；REWORK 为 blueprint/instance；DELETE v6 固定 payload；LEGACY mapper 留迁移。

## 必须交付

1. `SPD_GC_V6_P07_ENTITY_WORLD_CHECKPOINT.zip`：包含完整当前工作区源码，不是 patch-only。
2. `V6_CHECKPOINT_MANIFEST.json`：父 checkpoint hash、当前 ZIP hash、Audit/Contract hash、schema/price versions、测试命令与结果。
3. `PHASE_P07_IMPLEMENTATION_REPORT.md`：实际修改文件、行为变化、Gate 逐项证据、未通过项、仍 Deferred/Unsupported 项。
4. BuilderCommand traces、canonical serialization artifacts、Runtime traces 与测试报告。
5. 对 checkpoint 重新解压后的复验结果。

完成后立即停止，不执行下一阶段。
