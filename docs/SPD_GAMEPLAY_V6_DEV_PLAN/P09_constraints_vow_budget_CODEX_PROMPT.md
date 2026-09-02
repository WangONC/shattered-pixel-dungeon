# Codex 执行 Prompt — P09 Skill Constraint、Class Vow 真执法与统一 Budget Ledger

你现在执行 **Shattered Pixel Dungeon 自塑职业 Gameplay Components v6 — P09**。

## 输入与唯一基线

- 源码输入：`SPD_GC_V6_P08_TRANSFER_COPY_CHECKPOINT.zip`
- 冻结审计：`SPD_CLASS_GAMEPLAY_COMPONENTS_CURRENT_IMPLEMENTATION_AUDIT_v0.2.md`
- 冻结合约：`SPD_CLASS_GAMEPLAY_COMPONENTS_IMPLEMENTATION_CONTRACT_v0.2_FINAL.md`
- 开发计划：`SPD_GAMEPLAY_COMPONENTS_V6_DEVELOPMENT_PLAN_FINAL.md`

本阶段目标：**让 Tradeoff 成为真实、不可轻易旁路的限制，并建立唯一、可解释、可 roundtrip 的预算权威。**

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

- 从 P08 accepted checkpoint 开始。
- 按 Contract 第 20、27、33.13 完成；不得把普通 Condition 包装成返还型 Constraint。
- 对 Contract 中尚未精确冻结的额外 policy，不增加玩家选项；使用最窄 deterministic internal policy，并在 implementation map 标记，不得扩展玩法。
- Budget Catalog 必须是独立版本化数据且被 Builder、Validator、Headless、Fuzz、Report 共用；禁止多个公式源。
- 每个 credit 都要链接到 enforcement test ID；缺少测试时强制 effectiveCredit=0。
- 返回 bypass matrix、ledger canonical sample、priceVersion diff sample。

## 本阶段明确范围

- 实现 required SkillConstraint：每层次数、站立回合、事件窗口、最小 cooldown、专属 Mode。
- 实现 ClassConstraint/Vow：NoBasicAttack、NoMovement、EmptyEquipmentSlot、NoHealing、WeaponCategoryForbidden、StandStill、MaximumSkillUses、NoItemUse。
- 在 action commit 前接入 BasicAttack/Move/Equipment/Heal/Item/Skill/FloorReset/SaveLoad enforcement hooks；硬阻止并给玩家反馈。
- 将旧 WAIT_CLEARS_RESOURCE、TARGET_MARKED、LOW_HP、IN_WATER、COOLDOWN、LIMITED_USE 按 Contract 正确迁移；无执法 HP_COMMITMENT credit=0。
- 完成中央 BudgetCatalog `gameplay-v0.2.0`、BudgetLedger、每 node entry、typed parameter price、priceVersion diff。
- 实现 EffectiveConstraintValue：冗余、旁路、有效时间、主动规避、包含关系、测试证据；无证据 credit=0。
- 为此前所有 Player-exposed variants 补齐唯一 price key；未定价项改 UNSUPPORTED，而非临时 0 价。
- 同步 Builder、formatter、localization、save-load、migration 和 bypass tests。


## 本阶段验收 Gate

- [ ] Mandatory Test 12 全部通过；2→5 Convert、Device 周期/radius/payload/lifetime 可分项解释。
- [ ] NoBasicAttack 等 Vow 必须实际 block 才有 credit；BasicAttack 已 NONE 时重复 constraint credit=0。
- [ ] NoHealing 的已声明 source 范围具有 Potion/Skill/Item/Buff/ClassOperation 旁路测试；未覆盖范围在 UI 明示并相应扣减 credit。
- [ ] Rename/order/save-load 不改变 ledger；删除 dependency 不静默移除成本。
- [ ] Archetype 15 至少两类 Vow 从 Builder 到 Runtime/credit 全链通过。


## 代码处理原则

REPLACE old Restriction/SkillConstraint 假返还；KEEP必要 Hook 接入风格；NEW BudgetCatalog/Ledger；DELETE no-op rebate。

## 必须交付

1. `SPD_GC_V6_P09_VOW_BUDGET_CHECKPOINT.zip`：包含完整当前工作区源码，不是 patch-only。
2. `V6_CHECKPOINT_MANIFEST.json`：父 checkpoint hash、当前 ZIP hash、Audit/Contract hash、schema/price versions、测试命令与结果。
3. `PHASE_P09_IMPLEMENTATION_REPORT.md`：实际修改文件、行为变化、Gate 逐项证据、未通过项、仍 Deferred/Unsupported 项。
4. BuilderCommand traces、canonical serialization artifacts、Runtime traces 与测试报告。
5. 对 checkpoint 重新解压后的复验结果。

完成后立即停止，不执行下一阶段。
