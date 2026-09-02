# Shattered Pixel Dungeon 自塑职业 Gameplay Components

# CURRENT IMPLEMENTATION AUDIT v0.2

**审查状态：FINAL — 独立源码审计结论**  
**审查日期：2026-09-02**  
**审查对象：`shattered-pixel-dungeon.7z`**  
**原始压缩包 SHA-256：`b066e12c5cbef0ef15c35c1b36c2cfdacc43589b33c2921fc794ee9e6629e599`**  
**源码标识：`appVersionName = 3.3.8`，`appVersionCode = 896`**

---

## 0. 审查结论先行

### 0.1 总结判定

当前项目不能被判定为：

- “Gameplay Component Language 已完整实现”；
- “210/210 Components Implemented”；
- “全组件已实现”；
- “10/10 或 15/15 Archetypes 可由玩家真实构筑”；
- “Player Builder coverage complete”；
- “QA PASS 因而可以进入内容扩展”。

本次独立审查的结论是：

| 层级 | 判定 | 结论 |
|---|---:|---|
| SPD 原生游戏与规则接入基础 | **KEEP** | `RuleHooks`、真实 Hero/Mob/Level/Buff 接入、确定性调度、递归保护、因果追踪、延迟对象存档等是有价值资产。 |
| Rule Runtime 基础 | **PARTIAL / KEEP** | 多数已有具体效果确实能在真实 Runtime 中执行；但若干语义被硬编码、重复建模或降级。 |
| Class-level Gameplay Components | **PARTIAL / REWORK** | 已有资源流、Ownership、Capacity、Persistence、ClassOperation 等骨架，但仍是共享字段袋、固定枚举和自动补绑。 |
| Skill-level Gameplay Components | **FAIL AS LANGUAGE** | Family 名称和 Operation 枚举很多，但参数 Schema 不足以表达其名称所承诺的语义。 |
| Player Builder | **FAIL** | Builder 能选择“条目”，但不能可靠构造通用、强类型、可引用的玩法定义；复杂参数被压成平铺枚举或通用 `strength/power`。 |
| Stable References / Dependency | **FAIL** | Resource 部分有稳定 ID 基础；Mark、Mode 等没有完整声明模型；删除后还存在自动绑定、默认回退和静默改绑。 |
| Save / Load | **PARTIAL** | Bundle 管线广泛存在，但加载会调用自动补绑；现有 QA 指纹遗漏关键语义，不能证明无损 roundtrip。 |
| Formatter / Localization | **FAIL GATE** | 动态玩家名称与静态本地化键混用，缺少全量玩家路径文本完整性闸门；已观察到 `NO TEXT FOUND` 与 `�`。 |
| Headless / Fuzz / Integrity | **KEEP AS AUXILIARY QA** | 对真实 Runtime、不崩溃、调度、存档和 exploit 检测有价值；不能证明玩家 Builder 的表达能力。 |
| 现有“coverage complete”报告 | **NOT TRUSTWORTHY FOR COMPLETENESS** | 其断言口径主要是“已注册 / `implemented()` 返回 true / 同一 Registry 可反查 / 直接构造对象不崩溃”，不是玩家构筑与行为语义验收。 |

**最终总判定：**

> 当前项目拥有值得保留的 Rule Runtime 与 Headless 基础，但“通用 Gameplay Component Language”这一公开设计层尚未达到 Implementation Contract，也没有达到玩家可构筑完成度。现有 Gameplay Language 层需要结构性重做；不能通过继续补几个枚举、参数或翻译来修复。

这不是“全部推倒重来”。正确方向是：

1. 保留 SPD 接入、事件钩子、调度、因果、存档基础、HUD 和 Headless Runtime；
2. 重写 Gameplay Language 的声明、引用、参数 Schema、Builder 表单、Dependency、Formatter 与 Player-path QA；
3. 用迁移适配器承接旧存档，而不是继续让旧字段袋充当公开 Schema。

---

## 1. 审查依据、方法与限制

### 1.1 实际阅读与追踪范围

本次以源码为唯一事实源，首先完整阅读：

- `docs/SPD_CLASS_GAMEPLAY_COMPONENTS_SPEC_v0.1.md`

随后追踪了以下真实数据流：

```text
WndCreateClass / PlayerBuildAssembler
        ↓
ClassBuild / ClassGameplayComponentSpec / SkillSpec
        ↓
Registry / validation / dependency / budget / formatter
        ↓
SkillSpec.compile() / RuleDefinition
        ↓
RuleRuntime / SkillEffectRuntime / RuleHooks
        ↓
Hero / Mob / Buff / Level / Entity / Trap
        ↓
Bundle save-load / HUD / Headless / Fuzz / Coverage Audit
```

重点审阅的源码包括但不限于：

- `rules/ClassBuild.java`
- `rules/ClassGameplayComponentSpec.java`
- `rules/SkillSpec.java`
- `rules/EffectSpec.java`
- `rules/EffectVocabularyRegistry.java`
- `rules/RuleDefinition.java`
- `rules/RuleRuntime.java`
- `rules/SkillEffectRuntime.java`
- `rules/RuleHooks.java`
- `rules/ResourceSpec.java`
- `rules/RuleResourceState.java`
- `rules/RuleCondition.java`
- `rules/RuleCost.java`
- `rules/SkillConstraint.java`
- `rules/TargetingSpec.java`
- `rules/SkillTargetResolver.java`
- `rules/RuleModifier.java`
- `rules/SkillDelivery.java`
- `rules/ClassOperationSpec.java`
- `rules/ClassOperationRuntime.java`
- `actors/buffs/RuleMark.java`
- `actors/buffs/RuleMode.java`
- `actors/mobs/npcs/RuleOwnedEntity.java`
- `levels/traps/RuleCarrierTrap.java`
- `windows/WndCreateClass.java`
- `qa/GameplayComponentCoverageAudit.java`
- `qa/PlayerBuildEquivalenceAudit.java`
- `qa/PlayerArchetypeReconstructionAudit.java`
- `qa/RuleBuild.java`
- `qa/RuleBuildFuzzer.java`
- `qa/HeadlessGameplayHarness.java`
- `rules/PlayerBuildAssembler.java`

### 1.2 Git 基线限制

压缩包内没有 `.git/`，因此本次不能：

- 读取 commit graph；
- 给出可信的具体 commit hash；
- 判断某一错误抽象究竟在哪个 commit 首次引入；
- 直接执行 `git bisect`。

后文会给出**按代码地标选择基线**的方法，而不会编造 commit。

### 1.3 动态测试限制

尝试运行 Gradle 测试时，Wrapper 需要：

```text
https://services.gradle.org/distributions/gradle-9.4.0-bin.zip
```

当前隔离环境无该 distribution 缓存且无法解析 `services.gradle.org`，因此测试在 Gradle 启动前失败，未执行 `:core:test`。失败原因是 `UnknownHostException`，不是项目测试失败。

因此本文严格区分：

- **源码与数据流已确认的结论**；
- **现有测试源码实际断言了什么**；
- **本环境未亲自复跑的测试结果**。

任何现有报告中的历史运行数字，都没有被本次审查重新认证。

---

## 2. v0.1 规范本身的判定

### 2.1 值得保留的设计方向

`SPD_CLASS_GAMEPLAY_COMPONENTS_SPEC_v0.1.md` 的核心方向总体正确，尤其包括：

1. **禁止固定职业 Domain 冒充通用语言。** 枪手、召唤师、工程师等只能作为拆解样本和验收 Recipe。
2. **明确区分 Class-level 与 Skill-level Components。** 资源经济、基础攻击、Ownership、Capacity、Persistence、ClassOperation 不应伪装成普通技能。
3. **Skill 骨架清晰。** Activation/Trigger、Condition、Primary、可选 Secondary、Delivery、Targeting、Modifier、Cost、Constraint 的分层是合理基线。
4. **HP 与自定义 Resource 有明确语义区分。** HP 是 Built-in Stat / Value Source，不应复制成另一个普通 ResourcePool。
5. **Create Entity 目标不是几个固定召唤模板。** 规范要求 Actor、Device、Carrier、Trap 等拥有真实关系、容量、持续与行为。
6. **Transfer / Copy 要有 Capability 白名单。** 不能任意复制 Java Buff、对象图或运行时实现细节。
7. **Dependency 必须有 `UNRESOLVED`。** 删除依赖不能静默改绑。
8. **“完成”必须同时包含 Data、Player UI、Runtime、Budget、Save/Load、Dependency、Formatter 与 QA。**
9. **明确禁止 Registry count、直接构造 Runtime object、无 crash Fuzz 冒充玩家可构筑。**

换言之，当前失败并不是因为 v0.1 鼓励了固定职业模板；主要是实现没有遵守 v0.1 自己写下的完成标准。

### 2.2 v0.1 的真正不足

v0.1 更像一份高质量设计基线，而不是足够精确的 Implementation Contract。它缺少：

- 每一种声明、引用、Variant 的精确字段 Schema；
- 不同 Variant 的参数所有权与互斥规则；
- Stable ID 的生成、重命名、删除和迁移语义；
- 声明数据与 Runtime State 的严格分离；
- Resource Convert 的原子性、比例、溢出、部分支付规则；
- Mark / Mode 的玩家自定义声明模型；
- Device / Trap / Field / Delay / Echo / Attachment 共享 Payload 的统一结构；
- Target snapshot、live binding、re-evaluate 的时序规则；
- Transfer / Copy 的类型矩阵和禁止边界；
- Constraint 如何真正执法、如何证明没有旁路、何时才能返还 Budget；
- Builder 与 Headless Player Assembler 必须共享哪一套 Command/Reducer；
- Save/Load 的深语义等价要求；
- 面向悟道、炼成、时序、尸骸、誓约构筑所需的新通用 Primitive。

因此，本轮不是推翻 v0.1，而是把它从“设计原则”收紧为“可执行合约”。

---

## 3. 根因：为什么多轮 QA PASS 后玩家仍然不能构筑

### 3.1 `EffectSpec` 是共享字段袋，不是 Variant Schema

`EffectSpec` 把 11 个 Family、数十个 Operation 全部塞进同一组字段：

```java
int power;
int duration;
int count;
int period;
int lifetime;
int secondaryParameter;
String resourceId;
String targetResourceId;
String templateId;
String stateId;
```

证据位置：`rules/EffectSpec.java:30-45`。

问题不是字段数量少，而是：

- 字段没有 Variant 所有权；
- 同一个 `power` 在 Damage、Push、Resource、Entity、Mark 中分别代表不同语义；
- 无法通过类型系统判断某个 Operation 是否缺字段；
- UI 只能把所有数字生成为相似的选项列表；
- Save/Load 可保存“有字段的对象”，却不能保证字段组合具有正确语义；
- QA 很容易把“family 与 operation 对得上”当成实现完成。

`EffectSpec.implemented()` 除了三个显式 false 的 Operation，主要只检查 `familyFor(operation) == family`。这证明的是枚举映射一致，不是 Runtime 行为完整。

### 3.2 同一语义在三层重复建模，能力彼此不一致

当前至少存在三套并不一致的语义层：

1. **Class-level Component**：例如 `ClassGameplayComponentSpec.RESOURCE_FLOW`；
2. **Skill-level Effect**：例如 `EffectSpec.Operation.RESOURCE_CONVERT`；
3. **Runtime Payload / Entity behavior**：例如 `RuleOwnedEntity` 保存任意 `EffectSpec`，但 Create Device Builder 又用固定字符串。

最明显的例子是 Resource Convert：

- Class-level Resource Flow 有 `amount` 与 `targetAmount`，能表示非 1:1；
- Skill-level Resource Convert 只有一个 `power`；
- Runtime `SkillEffectRuntime.convert()` 又把实际扣除量原样加给目标，固定为 1:1。

这不是“UI 少两个字段”这么简单，而是三层都没有共享同一个 `ResourceOperationSpec`。

### 3.3 Builder Registry 同时充当 UI、合法性与 QA 真相源

`EffectVocabularyRegistry.parameterOptions()` 决定 Builder 可以选什么；`playerReachable()` 又用几乎相同的规则证明对象是“玩家可达”。

例如：

- Convert：只检查 source/target 与一个 `power`；
- Mark：只接受固定 `RuleMark.Type`；
- Carrier：只接受 `fire / poison / heal`；
- Mode：只接受 `ClassBuild.modes()` 返回的原始字符串。

随后 QA 再问同一个 Registry：这个对象是否 exposed / reachable。

这形成循环证明：

```text
Registry 定义了一个缩水参数集合
→ Builder 只能创建这个缩水集合
→ QA 用同一个 Registry 验证“Builder 可创建”
→ 得出 coverage complete
```

它完全没有回答：这个参数集合是否足以表达规范承诺的玩法。

### 3.4 “PlayerBuildAssembler” 接受的仍是最终对象

`PlayerBuildAssembler.addSkill(SkillSpec)`、`addGameplayComponent(ClassGameplayComponentSpec)`、`addResource(ResourceSpec)` 接受的是已经构造完成的最终 Schema 对象。

`reconstruct(ClassBuild)` 的核心行为是：

```text
遍历 source 中的最终 ResourceSpec / Component / SkillSpec
→ copy
→ 再用 Registry 判断 exposed
```

它不是：

```text
玩家点击“新建技能”
→ 选择 Family
→ 选择 Variant
→ 填每个字段
→ 绑定 Reference
→ 处理错误与 UNRESOLVED
```

所以它只能证明“最终对象可以被复制并被同一 Registry 接受”，不能证明玩家能从零构造。

### 3.5 QA 指纹遗漏关键语义

`qa/RuleBuild.fingerprint()` 没有完整纳入：

- Condition 的 `reference`；
- Target Filter 的所有参数与绑定；
- Effect 的 `targetResourceId`、`period`、`secondaryParameter`、`templateId` 等关键字段；
- Secondary Effect 的完整语义；
- Attachment event / charges；
- Constraint 的完整参数；
- Dependency 状态；
- 未解析引用；
- Scheduled Payload、Entity、Mark、Mode 等 Runtime State。

因此即使 roundtrip 丢失关键语义，弱指纹仍可能相同。

### 3.6 自动补绑掩盖 Dependency 错误

`ClassBuild.resolvePendingBindings()` 会：

- 将空 ResourceRef 绑定到第一个资源；
- 将 Convert target 绑定到第一个不同资源；
- 将 Mode condition 绑定到第一个 Mode；
- 将 Trait/Cost/Effect 的空引用补成某个现存对象。

证据位置：`rules/ClassBuild.java:366-428`。

而且它被以下路径调用：

- Builder 展示页面：`WndCreateClass.java:101-102`；
- Builder 修改过程；
- `RuleBuild.from()`；
- `RuleRuntime(ClassBuild)`；
- `ClassBuild.restoreFromBundle()`：`ClassBuild.java:505`。

这会把本应明确暴露的 `UNRESOLVED` 变成静默、顺序相关的改绑。它直接违反 v0.1 和本轮要求。

### 3.7 Runtime Primitive 存在，但 Player Abstraction 缺失

`RuleOwnedEntity` 已能保存：

- 任意 Primary `EffectSpec`；
- 可选 Secondary `EffectSpec`；
- period；
- filter；
- lifetime；
- owner。

这说明底层有一部分通用载荷能力。

但 Create Device/Field/Trap 的 Builder 路径又把 Payload 压成：

```text
fire
poison
heal
```

`SkillEffectRuntime.carrierPayload()` 再把字符串转换成有限效果；其中 `fire` 甚至落入默认 Standard Damage，而不是统一的 Fire Effect。

因此“Runtime 有 Payload 字段”不能算“Create Entity 已是通用组件”。

### 3.8 Coverage 行数不等于语义完成

`GameplayComponentCoverageAudit` 中存在大量直接填 `true` 的行：

- 所有 RuleEvent；
- 所有 RuleCondition；
- 所有 Selector/Coverage/Filter；
- 所有 Modifier；
- 许多 Entity/Carrier/Constraint 条目。

Effect 的 `runtimeVerified` 又直接等于 `EffectSpec.implemented()`。

最终 `passed` 只取决于这些布尔字段是否全真，而不是每个条目是否有独立的 Builder→Save→Runtime 行为断言。

所以 `210/210` 即便是报告真实输出，也只说明该报告自己的布尔口径全部通过，不能外推为 Gameplay Language 完成。

---

## 4. CURRENT IMPLEMENTATION AUDIT

### 4.1 总表

状态定义：

- **IMPLEMENTED**：已经满足本轮目标的核心语义，可作为正式基础保留；
- **PARTIAL**：有真实能力，但公开 Schema、Builder、Runtime、存档或 QA 至少一层不完整；
- **FAIL**：无法表达该组件承诺的基本语义，或存在会破坏稳定引用/玩家构筑的结构性问题；
- **NOT EXECUTED**：本环境未动态复跑，不代表源码失败。

| 审查项 | 状态 | 实际存在的能力 | 主要缺口 / 结论 |
|---|---:|---|---|
| `ClassBuild` 聚合结构 | **PARTIAL / REWORK** | 能保存 resources、components、skills、traits、operations、starting kit、progression。 | 声明与状态边界不清；Mode/Mark 无正式声明表；自动补绑；删除依赖不能稳定保留为 UNRESOLVED。 |
| Class-level / Skill-level 分层 | **PARTIAL** | 已有 `ClassGameplayComponentSpec` 与 `SkillSpec` 两层。 | 同一语义重复实现且不共享 Schema，例如两套 Resource Convert。 |
| Resource 声明 | **PARTIAL** | 任意 ID/名称、min/capacity/initial/current，多资源基础存在。 | `ResourceSpec.current` 与 `RuleResourceState.value` 重复；声明和 Runtime State 耦合。 |
| Built-in HP 边界 | **PARTIAL / DIRECTION CORRECT** | HP Cost、Low HP、Missing HP Damage、Heal、Barrier、Temp HP 等已有部分实现。 | 未形成统一 `BuiltinStatRef` 与精确支付/致死/临时 HP 规则；但不应新增 HP ResourcePool。 |
| Class Resource Flow | **PARTIAL** | Gain/Loss/Clear/Convert；Convert 有 source amount 与 target amount。 | Convert 不是统一事务；目标空间不足时会按剩余空间截断，却仍扣完整 source，比例语义不稳定。 |
| Skill Resource Gain/Drain | **PARTIAL** | Runtime 能改变指定资源。 | 使用通用 `power`；缺少强类型 Amount、溢出、支付失败策略。 |
| Skill Resource Convert | **FAIL** | 可以选择 source/target，Runtime 能发生 1:1 转换。 | 不能表示 `Rage -2 → Focus +5`；Runtime 按 `min(available, power)` 等量转入，还是部分支付。 |
| Resource Reserve/Suppress | **PARTIAL** | 有 Buff 与 Runtime helper。 | 参数仍借用通用字段；生命周期、释放、死亡/过层语义未冻结。 |
| Mark / Stack / Charge / Counter | **FAIL AS PLAYER COMPONENT** | `RuleMark` 是可保存的 stack Buff，支持 source owner、transfer depth、duration。 | 玩家词汇固定为 5 个 enum；无 `MarkSpec/MarkRef`、自定义名称、稳定引用；非法 ID 回退为 HUNTED。 |
| Mode | **FAIL AS STABLE DECLARATION** | `RuleMode` 可保存字符串 modeId、duration、persistent；Mode Shift 有 Runtime。 | Mode 只是原始字符串列表；无稳定 ID/显示名分离、Ref、rename/delete；动态模式名被当本地化键。 |
| Trigger / Rule Hooks | **PARTIAL / KEEP** | ACTIVE、turn、wait、move、enter、attack、hit、damaged、kill、item、status、low HP 等真实钩子存在。 | Trigger 参数仍弱；新 Primitive 需要 Death、Observation、Entity-local 等明确上下文；现有 coverage 不能证明每个钩子的玩家语义。 |
| Condition | **PARTIAL / REWORK** | 多种条件可执行，Skill 持有 condition list。 | 共享 `parameter/reference`；Mark/Status 过滤固定或“任意”；Builder 实际编辑能力有限；缺强类型 Ref。 |
| Primary + Secondary | **PARTIAL** | Runtime 支持 Secondary；通常 Primary 成功后执行。 | Delay 会将 Primary/Secondary 分拆成两个独立 Scheduled Payload，不是同一个原子 EffectChain。 |
| Damage Family | **PARTIAL** | Standard、Percent、Missing HP、Execute 等有 Runtime 路径。 | 共用 `power/secondaryParameter`；缺统一 Value Source、保护目标政策和精确参数 Schema。 |
| Status Family | **PARTIAL** | Poison/Burning/Bleeding/Slow/Haste 等真实 Buff 路径。 | 每个状态硬编码 Operation；没有统一 `StatusRef/ApplyStatusSpec`；复制状态也不是玩家明确选择的 Status。 |
| Movement Family | **PARTIAL** | Push/Pull/Throw/Dash/Teleport/Swap 有真实 helper。 | 合法位置、碰撞、失败政策没有在 Schema 中明确冻结。 |
| Recovery / Defense | **PARTIAL** | Heal/Barrier/Temp HP/Mitigate/Cleanse 等存在。 | Redirect、临时生命、护盾与减伤的精确堆叠/来源/目标边界不完整。 |
| Create Actor | **PARTIAL** | 能创建真实 `DirectableAlly`，有 owner、lifetime、capacity、command。 | 固定 Rat/统一属性；没有通用 EntitySpec、行为 Schema、能力 Payload。 |
| Create Device | **FAIL AS GENERIC ENTITY** | 以 `RuleOwnedEntity` 形式存在，可周期执行底层 Payload。 | Builder 仅能选固定 payload 字符串；目标逻辑和半径硬编码；不是通用 Device。 |
| Create Trap | **FAIL AS GENERIC TRAP** | 有真实 `RuleCarrierTrap` 与 enter tile 触发。 | Builder不能定义统一 Trigger→Primary→Secondary；只用固定 carrier payload；Trap 与其他 Entity 重复建模。 |
| Create Field / Carrier | **PARTIAL RUNTIME / FAIL BUILDER** | `RuleOwnedEntity.FIELD` 能周期执行 Primary+Secondary。 | 固定半径 1、行为与目标硬编码；Builder 参数仍是 `power/lifetime/period/fire-poison-heal`。 |
| Ownership / Relation | **PARTIAL / KEEP FOUNDATION** | `RuleOwnership`、owner ID、DirectableAlly command 存在。 | Relation/Link/Inheritance 的 Capability 类型与安全边界不足；Entity filter 粗糙。 |
| Entity Capacity | **PARTIAL** | Actor/Device/Field 数量限制可执行。 | Kind 集合固定且 Trap 不一致；Capacity filter 与 EntitySpec 缺统一稳定引用；溢出政策未定义。 |
| Persistence | **PARTIAL** | lifetime 和 Bundle 基础存在。 | owner death、floor transition、取消、到期、延迟目标绑定等规则未统一。 |
| Transfer Resource | **FAIL / NOT PLAYER-USABLE** | Operation 存在但 `implemented()` 显式 false。 | 需要明确 source/destination actor、ResourceRef、amount、溢出与单 Hero 边界。 |
| Copy Status | **PARTIAL / FAIL AS GENERAL COPY** | Runtime 会复制有限白名单中找到的一个状态。 | 玩家不能明确指定 Status；“复制第一个可复制状态”不是稳定 Schema。 |
| Transfer Mark | **PARTIAL LOW-LEVEL / FAIL PLAYER MODEL** | 固定 `RuleMark.Type` 可转移。 | 无自定义 MarkRef；非法 ID 回退；来源、数量、目标选择不完整。 |
| Barrier Swap | **PARTIAL / REAL RUNTIME** | 有真实交换路径。 | 需要明确双方、缺失目标、上限、临时护盾等语义。 |
| Transform Mode | **PARTIAL** | Mode Shift 能执行。 | Mode 没有稳定声明与引用。 |
| Capability Override | **FAIL / UNSUPPORTED** | enum 存在。 | `implemented()` false，Runtime 返回 false；不应暴露或计入 coverage。 |
| Behavior Override | **FAIL / UNSUPPORTED** | enum 存在。 | 没有 declarative behavior system；不应为 coverage 保留占位。 |
| Delivery | **PARTIAL** | Direct、Projectile、Trace、Ground、Carrier、Attachment 等有编译路径。 | `SkillDelivery.implemented()` 无条件 true；参数分散在 Skill/Effect；缺独立行为证据。 |
| Targeting | **PARTIAL** | Selector/Coverage/Filter 和 resolver 存在。 | MARKED/HAS_STATUS 不是 typed ref；兼容实体过滤过宽；Area 与 Modifier 重复塑形。 |
| Modifier | **PARTIAL** | Repeat、Duration、Intensity、Pierce、Bounce、Delay 等存在。 | 通用 `magnitude`；AREA 与 Targeting Coverage 重叠；兼容矩阵和 wrapper 顺序未冻结。 |
| Cost | **PARTIAL** | Resource/HP/Action/Cooldown/Consumable/State 有 Runtime。 | State 固定 Mark enum；Consumable 用 Java class name；HP 支付边界未冻结；通用 amount/reference。 |
| Skill Constraint | **FAIL AS TRADEOFF CONTRACT** | 少量 Constraint 能转成条件或用次限制。 | `implemented()` 无条件 true；HP_COMMITMENT 没有独立执法；TARGET_MARKED 固定 HUNTED；可返还预算却未证明真实限制。 |
| Class Constraint / Vow | **FAIL** | 只有少量旧 `Restriction`，如等待清资源、弱治疗等。 | 缺 equipment/action/position/commitment enforcement；`WAIT_CLEARS_RESOURCE` 本质是 Resource Flow。 |
| Budget | **PARTIAL / REWORK** | 多数对象有 capacity cost，能计算总量。 | 成本分散在对象；字段袋导致错误同价；Constraint rebate 可来自无效约束；缺可保存、可解释的 Budget Ledger。 |
| Dependency | **FAIL** | 有若干 valid()/dependency helper。 | 自动补绑、默认回退、整个 build 一票否决；缺统一 RESOLVED/UNRESOLVED/HARD_CONFLICT/UNSUPPORTED 图。 |
| Rename | **PARTIAL** | Resource rename 主要只改 display name，引用 ID 可保持。 | Mark/Mode 无正式声明；Operation ID 有时由语义生成；没有全类型 rename acceptance。 |
| Delete | **FAIL** | 某些删除能留下字符串 ref。 | 随后 UI/load/runtime 可自动绑定到第一个对象；也可能以默认 Mark 回退，违背明确 UNRESOLVED。 |
| Save / Load | **PARTIAL** | Class、Skill、Effect、Runtime、Entity、Delay 等普遍 Bundlable。 | 加载后调用 `resolvePendingBindings()`；声明/状态耦合；深语义 roundtrip 未被验证。 |
| Formatter | **FAIL GATE** | 已有大量摘要/详情字符串。 | 无统一 typed formatter；invalid ref 会显示错误默认含义；generic fallback 会隐藏具体参数。 |
| Localization | **FAIL GATE** | 英/中资源广泛存在。 | 动态 Mode 名被拼成 message key；未建立全 Builder 输出枚举测试；已观察 `NO TEXT FOUND`/`�`。 |
| Class Action HUD | **PARTIAL / KEEP** | Reload/Command/Mode Switch/Recycle 的 Runtime 与操作入口是有价值基础。 | Operation ID 与来源绑定需改为稳定 ID；Payload 和 dependency 需强类型化。 |
| Resource HUD | **IMPLEMENTED FOUNDATION / KEEP** | 多资源状态显示和 Buff indicator 基础可保留。 | 需适配声明/状态分离与自定义名称。 |
| Headless Gameplay Harness | **IMPLEMENTED AS HARNESS / KEEP** | 使用真实 Hero/Mob/Actor/Level/Buff/RuleRuntime，适合验证行为。 | 输入仍可直接携带最终 ClassBuild/SkillSpec；不能单独证明 Player Builder。 |
| Fuzz / Integrity | **PARTIAL AUXILIARY** | 对无 crash、递归、资源 exploit、构筑有效性密度有价值。 | 直接生成最终对象；若 Schema 自己缩水，Fuzz 不会发现“表达不了”的语义。 |
| Player Builder path QA | **FAIL** | 有名为 PlayerBuildAssembler 的工具和 UI Registry。 | 不是逐步玩家命令路径；直接接收最终对象；同一 Registry 自证 exposed。 |

---

## 5. 已观察问题的独立源码确认

### 5.1 Resource Convert

#### 玩家路径

`EffectVocabularyRegistry` 对 Skill-level Convert 只生成：

1. source/target 组合；
2. 一个 `power = 1..6`。

没有 sourceAmount 与 targetAmount 两个独立字段。

`WndCreateClass.effectParameterGameplay()` 又把摘要写成：

```java
msg("gameplay_param_convert",
    effect.power,
    sourceName,
    targetName,
    1,
    1)
```

即 Formatter 也只能硬编码一个 1:1 关系。

#### Runtime

`SkillEffectRuntime.convert()`：

```java
spent = min(available, amount)
source -= spent
target += spent
```

这不仅固定 1:1，还默认允许部分支付。

#### 对照 Class-level

`RuleRuntime.applyResourceFlow()` 对 Class Component 使用独立 `component.amount` 与 `component.targetAmount`。但目标空间不足时会截断 gain，仍扣完整 source，因此还缺原子事务政策。

**判定：Skill Resource Convert = FAIL；Class-level Convert = PARTIAL。** 不能只给 UI 加字段，必须抽出唯一共享的 `ResourceOperationSpec.Convert`。

### 5.2 Mark / Accumulation

`RuleMark.Type` 固定为：

```text
HUNTED
ACCUMULATION
CHARGED
LOW_PHASE
COMPENSATION_LOCK
```

Builder 直接枚举这些值。Runtime 对无法解析的 Mark ID 使用：

```java
return RuleMark.Type.HUNTED;
```

因此：

- “猎印”“蓄势计数”只是固定类型的本地化显示；
- 玩家不能创建稳定 ID 为 `mark_xxx`、显示名为“灼痕”的声明；
- Condition、Cost、Target Filter、Transfer 无法统一引用玩家 Mark；
- 拼写错误或删除后会错误落到 HUNTED，而不是 UNRESOLVED。

**判定：低层 stack Buff 可保留并泛化；玩家级 Mark Language 必须重写。**

### 5.3 Create Entity

当前 `EffectSpec` 构造器会为 Create Variant 预填：

```text
Actor  → templateId=rat
Device → stateId=fire
Trap   → stateId=poison
Field  → stateId=poison
```

Builder 的 Carrier 参数明确限制为：

```text
fire / poison / heal
```

而 `RuleOwnedEntity` 底层其实已经能持有任意 `EffectSpec primary` 和可选 `secondary`。

这证明：

- Runtime 基础有复用价值；
- 公开 Entity Schema 和 Builder 把它降级成固定模板；
- Device/Trap/Field 目前不能被称为统一通用实体能力；
- Trap 不能从玩家路径构造 `Enemy Enter Tile → Push 2 → Poison`。

**判定：Entity Runtime foundation = PARTIAL/KEEP；Entity public abstraction = FAIL/REWORK。**

### 5.4 Transfer / Copy

当前公开条目与实际能力不对齐：

- `TRANSFER_RESOURCE`：Operation 存在，但 `implemented()` 显式 false；
- `COPY_STATUS`：复制有限白名单中碰到的状态，不是明确 StatusRef；
- `TRANSFER_MARK`：依赖固定 Mark enum；
- `SWAP_BARRIER`：有真实 Runtime，但 source/destination 与上限策略未成为强类型 Schema。

**判定：Family 方向正确，Variant 与安全矩阵不完整。**

### 5.5 Transform

- `TRANSFORM_MODE` 有可执行路径；
- `TRANSFORM_CAPABILITY` 与 `TRANSFORM_BEHAVIOR` 由 `implemented()` 明确返回 false；
- 没有通用 declarative behavior / capability replacement model。

**判定：Mode Shift 可保留并重绑到稳定 ModeRef；Capability/Behavior Override 当前应标记 UNSUPPORTED，不应因 coverage 暴露。**

### 5.6 Constraint / Tradeoff

当前：

- `SkillConstraint.implemented()` 无条件返回 true；
- `TARGET_MARKED` 固定成 HUNTED；
- `HP_COMMITMENT` 主要通过要求 HP Cost/返还 Budget 表达，没有独立承诺状态或旁路阻断；
- `WAIT_CLEARS_RESOURCE` 可以由普通 Resource Flow 完整表达，却被当作 Global Restriction；
- 缺少 Empty Slot、No Move、No Basic Attack、Stand Still、No Healing 等真实 Class Vow 执法。

**判定：Constraint/Tradeoff 目前不足以承载“苦行者”，需要单独的 ClassConstraint/Vow 模型。**

### 5.7 Builder 参数 UI

`WndCreateClass` 中资源数值、Carrier lifetime/period、Attachment charges、Targeting 参数等大量使用：

```java
for (int value = min; value <= max; value++)
```

形成 1、2、3、4……平铺选项。

虽然 Family→Variant 已有两层菜单，但 Parameters 仍由 Registry 返回一整批“对象快照选项”，而不是字段表单。

**判定：Builder 需要 Schema-driven typed form，不是扩大选项列表。**

### 5.8 Player-facing 文本

源码存在两个结构性风险：

1. `TraitSpec.bindingName()` 将动态 `stateId` 拼成 `mode_<id>` 交给 `Messages.get()`，把玩家自定义名称误当本地化键；
2. 多处 Formatter 在 unresolved/unsupported 时回退为 generic summary、HUNTED、CHARGED 或 unnamed resource，可能显示错误语义而不是错误状态。

结合已经实际观察到的 `NO TEXT FOUND` 与 replacement character，当前不能通过 Formatter/Localization 完成闸门。

### 5.9 HP 与 Resource 边界

源码已存在独立 HP Cost、Heal、Barrier、Temporary HP、Low HP、Missing HP 等路径。正确修复不是创建名为 HP 的 `ResourceSpec`，而是：

- 引入 `BuiltinStatRef`；
- 把 HP 支付、比较、缩放和恢复写成显式 Variant；
- 自定义 Resource 只保存玩家创造的独立池。

**判定：用户提出的 HP 边界方向正确，应在 v0.2 Contract 正式冻结。**

---

## 6. 哪些已有报告结论不可信，以及原因

“不可信”在这里不是指报告一定伪造，而是指其证据**不能支撑所声称的完成度结论**。

| 报告 / QA 入口 | 它实际证明什么 | 它不能证明什么 | 结论 |
|---|---|---|---|
| `GameplayComponentCoverageAudit` | Registry 中有行、对象可 Bundle、成本函数返回值、`implemented()`/布尔值为 true。 | 玩家是否能填出完整参数；Runtime 是否产生承诺行为；rename/delete/unresolved；深 roundtrip。 | 不能作为 210/210 完成证据。 |
| `BuilderVocabularyExposureAudit` | Registry 暴露的 Operation 可被同一 Registry 反查。 | Vocabulary 是否足够通用；参数是否丢失；实体 Payload 是否任意。 | 循环自证。 |
| `PlayerBuildAssembler` | 最终对象能复制进 `ClassBuild` 并通过 Registry。 | 玩家逐步创建声明、选择 Variant、编辑字段、绑定引用、修复 unresolved。 | 不是等价 Player Builder。 |
| `PlayerBuildEquivalenceAudit` | 直接对象 copy 后弱指纹相近。 | 所有字段和依赖无损；UI 路径可达。 | 证据不足。 |
| `PlayerArchetypeReconstructionAudit` | 预制 `ClassBuild` 能被 assembler copy，并可交给 Headless。 | Archetype 能由真实 Builder 从空白构造；没有隐式 auto-binding。 | 不能证明 constructible。 |
| `RuleBuild.fingerprint()` | 若干枚举/数字相同。 | Secondary、完整 Condition/Target/Effect 参数、依赖状态、Runtime State 完整一致。 | roundtrip oracle 过弱。 |
| `ArchetypeReferenceBuilds` | 一组直接构造的 Runtime fixtures 可用于压力测试。 | 它们是通用语言表达、不是硬编码 HUNTED/fire/poison/raw mode。 | 可保留为 smoke fixtures，不是验收。 |
| `RuleBuildFuzzer` | 在自己生成的最终对象空间里不崩溃、可发现部分 exploit。 | 设计空间中被 Schema 排除的表达能力。 | 辅助 QA。 |
| `HeadlessGameplayHarness` | 真实 Hero/Mob/Actor/Level/Buff/RuleRuntime 行为可测试。 | 输入对象来自真实 Builder。 | 很有价值，但必须接新 PlayerCommand Assembler。 |

### 6.1 新的完成判据

今后任何 Component / Variant 只有同时具备以下证据才能标记 `IMPLEMENTED`：

1. 强类型 Data Schema；
2. 真实 Player Builder 字段路径；
3. Dependency / UNRESOLVED 行为；
4. Formatter / Localization；
5. Budget quote；
6. Save / Load 深语义等价；
7. Runtime 行为断言；
8. 从 Builder Command 创建的 adversarial test；
9. 没有依赖默认回退、自动补绑或职业专用标签。

Registry count、enum count、Fuzz no crash、直接 new 对象都只能作为附加证据。

---

## 7. v0.1 SPEC GAP ANALYSIS

### 7.1 设计层缺口

这些是 v0.1 在“玩法语言设计”上的新缺口，不等同于代码没实现：

| 设计缺口 | 为什么是设计层问题 | v0.2 决策 |
|---|---|---|
| Resource/Mark/Mode/Entity/Ability/Property 的统一声明与引用哲学 | v0.1 提到多个概念，但没有统一 Declaration→StableRef 模型。 | 所有玩家可命名对象采用 immutable ID + displayName + typed Ref。 |
| Payload 的一等结构 | Skill、Device、Trap、Field、Delay、Echo、Attachment 都需要表达相同效果链。 | 冻结统一 `EffectChainSpec` / `PayloadSpec`。 |
| Observation / Ability Capture | 悟道者不能仅由现有 Damage/Status 表达。 | 新增可跨构筑复用的 Observation、Ability Provenance、Learned Ability Pool Primitive。 |
| Property / Ingredient / Synthesis | 炼成师需要“性质”而非单纯资源数字。 | 新增 Property/Ingredient/Decompose/Synthesis Primitive；内容库 Deferred。 |
| Temporal state | Delay 已有，但 Snapshot/Restore/target binding 尚无。 | 新增 SnapshotSpec、Restore policy、Temporal binding；任意对象图复制禁止。 |
| Death Residue | 死亡后实体与普通 summon 不同。 | 在 Entity System 中加入 CORPSE/REMAINS 与 consume/convert。 |
| Effective Constraint Value | “有负面 Trait”不等于真实牺牲。 | Class Vow 必须有 enforcement、旁路分析和可证明 Budget credit。 |
| Capability/Behavior 的边界 | Transform/Copy 容易越过安全边界。 | 只允许 declarative whitelist；没有行为 Schema 时保持 UNSUPPORTED。 |

### 7.2 Implementation Contract 层缺口

这些问题不需要重新讨论玩法方向，而需要冻结工程契约：

- 精确类/字段/Variant Schema；
- Variant 必填、可选、默认、范围、互斥字段；
- Runtime result 类型和失败语义；
- Resource Convert 原子事务；
- rename/delete/save-load 的 Ref 行为；
- unresolved draft 是否可保存、何时阻止开局；
- target snapshot/live/re-evaluate；
- EffectChain 中 Secondary 的触发条件；
- Delivery、Coverage、Modifier 的参数所有权；
- Entity trigger/payload/lifetime/capacity 的统一关系；
- Budget Ledger 和 price version；
- Migration v5→v6；
- UI Number Stepper、Ref Picker、错误展示；
- PlayerBuilder Command API；
- QA oracle 必须比较的完整字段与 Runtime State。

**两类缺口不能混为一谈。**

- “没有 Snapshot Primitive”是设计层缺口；
- “Delay 的 Primary/Secondary 分成两个 Payload”是 Contract/实现缺口；
- “Mark 没有稳定声明”同时涉及设计契约与代码；
- “Builder 把数值平铺”主要是实现契约缺口。

---

## 8. 15 ARCHETYPE DECOMPOSITION MATRIX

这些 Archetype 只作为 Mechanic Coverage 与验收 Recipe，不是 Domain、标签或预制职业。

分类规则：

- **A**：现有组件原则上已能表达，主要是修实现、Builder 或 QA；
- **B**：现有 Family 正确，但 Variant / Parameter Schema 不完整；
- **C**：确实缺少跨构筑可复用的新 Primitive。

| # | Archetype | Gameplay Mechanic Decomposition | 去职业语义后的 Generic Primitive | 当前覆盖 | 分类 | Missing / Rework | Cross-archetype Reuse |
|---:|---|---|---|---|:---:|---|---|
| 1 | Martial Defender | 接触攻击、受击触发、减伤、护盾、反击附加、Push | Basic Attack profile；ON_DAMAGED；Mitigate；Barrier；Action Attachment；Push | 多数 Runtime 存在 | A/B | Attachment effect chain、Mitigate 参数、反击次序、Budget | Support、Vow、Engineer guard device |
| 2 | Blood Berserker | HP 支付、低 HP 条件、受伤蓄力、Missing HP Scaling、Temp HP | BuiltinStatRef；HpCost；StatCompare；ValueSource；DamageTaken Trigger；TempHP | 部分存在 | A/B | HP 支付边界、统一 ValueSpec、非资源化 HP | Martial、Temporal risk、Vow |
| 3 | Ranger / Ammo Gunner | 有限 Ammo、主动装填、Projectile、Damage、Pierce、Resource Cost | ResourceSpec；Reload ClassOperation；Projectile Delivery；Damage；Pierce；ResourceCost | 骨架较完整 | A/B | 稳定 Operation ID、typed fields、精确装填与成本 | 所有有限资源构筑、Engineer |
| 4 | Mage / Area Caster | Mana、主动施法、Ground/Projectile、Radius/Cone/Line、Damage/Status/Terrain | Resource；Delivery；Coverage；EffectChain；World Effect | 多数存在 | A/B | 移除 AREA 重复、typed targeting、统一状态 | Controller、Support、Engineer field |
| 5 | Assassin / Hunt | 自定义印记、叠层门槛、消费印记、Execute、Dash | MarkSpec/Ref；MarkAtLeast；MarkCost；Execute；Dash | 固定 HUNTED 路径 | B | 动态 Mark identity、所有引用点、rename/delete | Counter、Alchemy tag、Learner observation marks |
| 6 | Summon / Owned Summoner | 创建 Actor、Ownership、Capacity、Persistence、Command、成长/能力 | EntitySpec(ACTOR)；Ownership；Relation；Capacity；BehaviorSpec；Payload | 低层 actor/command 存在 | B/C | 通用 Actor blueprint、行为白名单、能力/成长高层 Deferred | Engineer、Corpse revive、Support pet |
| 7 | Engineer / Automation | Device/Trap/Field、周期/进入触发、任意 Payload、资源生产、Recycle | EntitySpec；EntityTrigger；EffectChain Payload；Capacity；ClassOperation | Runtime carrier 基础存在 | B/C | Builder 固定 payload 必须删除；统一 Entity/Payload；资源产出设备 | Summon、Controller、Temporal delayed devices |
| 8 | Controller / Terrain | Push/Pull/Root、地形生成/销毁、Hazard、Field | Movement；Status；WorldCapability；Targeting；Persistent Field | 多数 helper 存在 | A/B | Terrain capability 参数、合法性反馈、统一 Field | Mage、Engineer、Corpse zone |
| 9 | Support / Defense | Heal、Shield、Cleanse、Mitigate、Redirect、Link、Ally targeting | Recovery/Defense；Relation；Typed Ally filter；EffectChain | 部分存在 | A/B | Redirect/Link 目标与比例；Barrier/TempHP 区分 | Martial、Summon、Device support |
| 10 | Transform / Bio | Mode shift、Mode-specific condition、能力覆盖、行为变化 | ModeSpec/Ref；ModeShift；Capability whitelist；BehaviorSpec | 仅 Mode Runtime 部分 | B/C | Mode 稳定声明；Capability 只在白名单落地后暴露；Behavior Deferred | Vow stances、Learner、Summon actor behavior |
| 11 | 悟道者 / Enemy-Ability Learner | 观察/承受真实敌技、满足学习条件、捕获能力、来源记录、容量、装备/释放能力 | ObservationSpec；AbilityCaptureSpec；AbilityProvenance；LearnedAbilityPool；AbilityCapacity | 无通用模型 | C | 新 Primitive；不得加入 BLUE_MAGE_DOMAIN；完整内容库 Deferred | Copy/Transfer、Transform、Temporal replay、Alchemy property capture |
| 12 | 炼成师 / Decompose & Synthesis | 拆物品/实体/环境、提取材料/性质、配方重组、Imbue、消费性质 | PropertySpec；IngredientSpec；Decompose；SynthesisRecipe；Imbue；Item/Entity/Terrain Filter | 资源转换不足以表达 | C | 新 Property/Ingredient Primitive；配方内容 Deferred | Corpse material、Engineer production、World terrain |
| 13 | 时序术士 / Temporal Manipulator | Delay、Scheduled Payload、Echo、Snapshot、Restore、Temporal displacement、风险资源 | TimingSpec；EffectChain scheduling；SnapshotSpec；RestorePolicy；TargetBinding；Echo | Delay/echo 雏形 | A/B/C | Delay 链原子化；Snapshot/Restore 新 Primitive；存档与目标绑定 | 所有 delayed skills、Learner replay、Engineer timers |
| 14 | 尸骸利用者 / Death Residue User | Death 产生 Corpse、持久存在、以 Corpse 为目标/成本、爆炸/资源/材料/复活 | Death Trigger；EntitySpec(CORPSE)；PersistAfterDeath；EntityCost；ConsumeEntity；Entity→Effect/Resource/Property | 无真实 corpse language | C | 新 Death Residue Primitive；跨局遗产 Deferred | Alchemy、Summon、Terrain、Vow sacrifice |
| 15 | 苦行者 / Vow / Self-Restriction | 放弃装备槽/移动/基础攻击/治疗/频率，换取真实预算 | ClassConstraintSpec；EnforcementHook；CommitmentState；EffectiveConstraintValue；BudgetCredit | 旧 Restriction 不足 | C | 独立 Vow 模型、旁路测试、零执法零返还 | 所有 Archetype；尤其 Blood/Defender/Transform |

### 8.1 关键判断

- 15 个 Archetype **不要求 15 个 Domain**。
- 新增的 C 类 Primitive 均能服务多个构筑：
  - Observation/Ability provenance 不只服务悟道者；
  - Property/Ingredient 不只服务炼成师；
  - Snapshot 不只服务时序术士；
  - Corpse Entity 可服务召唤、炼成、地形与资源；
  - Vow 是所有职业都可采用的 Class-level tradeoff。
- Capability/Behavior Override 不能为了第 10 或第 11 个样本强行挂一个 enum；只有当 declarative runtime 能安全执行时才暴露。

---

## 9. GENERIC COMPONENT / PRIMITIVE MATRIX

| Primitive | 层级 | 当前资产 | 当前状态 | v0.2 决策 | 是否新增 Family |
|---|---|---|---:|---|---:|
| Stable Declaration ID | 全局 | Resource 有部分 ID | **REWORK** | Resource/Mark/Mode/Entity/Skill/Component/Operation 等全部使用 immutable opaque ID | 否 |
| Typed Ref + Dependency State | 全局 | 字符串 ID + valid helper | **REWORK** | 统一 RESOLVED/UNRESOLVED/HARD_CONFLICT/UNSUPPORTED；禁止 auto-bind/fallback | 否 |
| ResourceSpec / ResourceState | Class / Runtime | 已存在 | **REWORK** | 声明与可变状态分离；共享 ResourceOperation | 否 |
| BuiltinStatRef | Skill / Condition / Value | 分散 HP 判断 | **NEW GENERIC PRIMITIVE** | HP/MAX/MISSING/BARRIER/TEMP_HP/LEVEL 等只作 built-in source | 否 |
| MarkSpec / MarkState | Class / Actor | 固定 RuleMark enum | **REWORK** | 动态声明、typed MarkRef、值/持续/来源 | 否 |
| ModeSpec / ModeState | Class / Actor | 原始字符串 RuleMode | **REWORK** | 稳定 ModeRef、display name、group/exclusivity | 否 |
| EffectSpec typed union | Skill / Payload | 大字段袋 | **REPLACE** | 每个 Variant 独立参数类；禁止无关字段 | 否 |
| EffectChainSpec | Skill / Entity / Time | Primary/Secondary 分散 | **NEW STRUCTURAL PRIMITIVE** | Primary + optional Secondary，统一成功与调度语义 | 否 |
| PayloadSpec | Entity / Delay / Attachment | 多套专用表示 | **NEW STRUCTURAL PRIMITIVE** | Targeting + EffectChain + timing/source policy | 否 |
| TriggerSpec | Skill / Entity | RuleEvent +硬编码 Entity tick | **REWORK** | Skill 与 Entity 共用 typed trigger 基础、明确上下文 | 否 |
| ConditionExpr | Skill / Entity | 列表 + generic fields | **REWORK** | typed leaf；至少 AllOf；Ref 明确 | 否 |
| DeliverySpec | Skill | enum +分散参数 | **REWORK** | Variant 自带参数；禁止 `implemented() = true` 无证据 | 否 |
| TargetingSpec | Skill / Payload | Selector/Coverage/Filter | **REWORK** | Coverage 独占几何；Filter 可绑定 Mark/Status/Entity/Relation | 否 |
| ModifierSpec | Skill / Payload | type+magnitude | **REWORK** | typed wrapper；删除 AREA 重复；冻结组合顺序 | 否 |
| CostSpec | Skill / Operation | generic amount/reference | **REWORK** | Resource/HP/Mark/Item/Entity 等 typed cost | 否 |
| EntitySpec / EntityInstance | Class / Runtime | RuleOwnedEntity/Trap | **REWORK** | Actor/Device/Trap/Field/Carrier/Corpse 的统一 blueprint/instance | 否 |
| EntityFilter / Relation / Ownership / Link | 多层 | 粗 enum/RuleOwnership | **REWORK** | composable typed filters、RelationRef、Capability whitelist | 否 |
| EntityCapacity / Persistence | Class | component fields | **REWORK** | 稳定 capacity policy + lifetime/transition/death policy | 否 |
| TimingSpec / Scheduled Payload | Skill / Runtime | RuleDelayedPayload | **KEEP + REWORK** | 调度整个 EffectChain；冻结 target binding/save/cancel | 否 |
| SnapshotSpec / RestorePolicy | Temporal | 无 | **NEW GENERIC PRIMITIVE** | 只复制白名单状态，不复制任意对象图 | 否；归 Transfer/Copy |
| Transfer / Copy Matrix | Effect | 少量固定实现 | **REWORK** | Resource/Status/Mark/Barrier/Position/Capability 各自 typed Variant | 否 |
| Observation / Ability Capture | Trigger / Transfer | 无 | **NEW GENERIC PRIMITIVE** | 来源、捕获条件、learned pool、容量 | 否；归 Transfer/Copy 扩展 |
| Property / Ingredient / Synthesis | Item/Entity/World | 无 | **NEW GENERIC PRIMITIVE** | 可提取性质与配方；内容库 Deferred | 否；可由 Transform/Resource/Cost 组合 |
| Death Residue / Corpse | Entity / Trigger / Cost | 无 | **NEW GENERIC PRIMITIVE** | CORPSE Entity、Death spawn、consume target/cost | 否 |
| ClassConstraint / Vow | Class / Budget | 旧 Restriction | **REPLACE** | 真执法、commitment、effective credit | 否 |
| BudgetLedger | 全局 | 分散 capacityCost | **REWORK** | 中央报价、版本化、可解释、可 roundtrip | 否 |
| Formatter / Localization | Player | 多处分散 formatter | **REWORK** | typed formatter；动态名称不查 message key；完整性 gate | 否 |
| BuilderCommand / Reducer | Player / QA | UI + final-object assembler | **NEW STRUCTURAL PRIMITIVE** | UI 与 Headless 使用同一命令路径 | 否 |
| Deep Semantic Snapshot | QA / Save | 弱 fingerprint | **REPLACE** | canonical serialization + runtime state assertions | 否 |

---

## 10. CURRENT CODE KEEP / REWORK / DELETE MATRIX

### 10.1 KEEP：保留并围绕其重建

| 代码 / 能力 | 保留理由 | 必要适配 |
|---|---|---|
| `RuleHooks` 与 Hero/Mob/Item/Status 调用点 | 是小而真实的 SPD 接入面；避免在全仓库散布职业判断。 | 为 Death、Observation、Equipment/Vow 等增加明确通用事件，禁止 Domain 分支。 |
| `RuleContext`、event/cause ID、RuleTrace | 对递归、因果、延迟、调试和 QA 有价值。 | 扩展 source/owner/entity/ability provenance，但保持稳定。 |
| `RuleRuntime` 确定性排序与执行 guard | 是可复用 Runtime 基础。 | 编译新 typed schema；运行时不得修改 ClassBuild/自动绑定。 |
| `RuleDelayedPayload` / 保存与恢复骨架 | 支持真实时序。 | 改为保存整个 `EffectChainSpec/PayloadSpec`，而不是拆 Primary/Secondary。 |
| `RuleOwnedEntity` 的 Actor/Bundle/owner/lifetime 基础 | 证明真实实体可接入 SPD Actor 系统。 | 拆成 EntityInstance + typed blueprint；去固定 Rat/硬编码 target/payload。 |
| `RuleCarrierTrap` 的真实 Level Trap 接入 | 可作为通用 TrapInstance 基础。 | Trigger 与 Payload 改为统一 Schema，支持 Secondary。 |
| `RuleOwnership`、DirectableAlly command | Ownership/Command 的真实基础。 | 关系与能力通过 typed Relation/Capability 暴露。 |
| `ClassOperationRuntime` 与操作 HUD | Reload/Command/Mode Switch/Recycle 作为 Class Operation 的方向正确。 | Stable ID、source component ref、typed payload、dependency。 |
| Resource HUD / Indicator | 玩家反馈资产。 | 读取 `ResourceState`，名称从声明解析；支持 unresolved 诊断。 |
| `HeadlessGameplayHarness` | 使用真实 Actor/Level/Buff/Runtime，价值很高。 | 输入切换为 PlayerBuilderCommand 产物；保留 direct object API 仅作 internal smoke。 |
| Fuzz / Integrity / exploit policies | 适合检测 no-crash、递归、无限资源等。 | 不再参与“组件完成”的充分证明。 |
| SPD 原生效果 helper 与 `WorldCapabilityValidator` | 复用真实战斗/地形实现。 | 由 typed Effect Runtime Adapter 调用，明确失败结果。 |

### 10.2 REWORK：保留意图，重写公开结构

| 代码 / 层 | 重写目标 |
|---|---|
| `ClassBuild` | 新 schema v6；声明表、runtime state 分离；typed refs；无 auto-binding；deep migration。 |
| `ClassGameplayComponentSpec` | 从共享字段 tagged union 改为每个 Variant 独立类；统一 ResourceOperation、Entity policy、Vow。 |
| `SkillSpec` | 保留骨架；字段改为 TriggerSpec/ConditionExpr/EffectChain/Delivery/Targeting/Modifier/Cost/Constraint。 |
| `EffectSpec` | 彻底替换公开大字段袋；保留 legacy adapter 仅迁移。 |
| `RuleCondition` | 改 typed leaf/boolean expression；ResourceRef/MarkRef/ModeRef/StatusRef。 |
| `TargetingSpec` | 保留 Selector/Coverage/Filter 概念；强类型参数和兼容矩阵；AREA 归 Coverage。 |
| `SkillDelivery` | 改 typed DeliverySpec；每个 Variant 有实现证据和字段。 |
| `RuleModifier` | 改 typed wrapper；删除 generic magnitude 与 AREA 重复。 |
| `RuleCost` | 改 typed costs；ItemFilter/MarkRef/EntityCost；HP 精确边界。 |
| `SkillConstraint` / `Restriction` | 分裂为 Condition、SkillConstraint、ClassConstraint/Vow。 |
| `ResourceSpec` / `RuleResourceState` | declaration/state 分离；所有转换走一个事务引擎。 |
| `RuleMark` | 保留 Buff 实现方式，但从固定 enum 改动态 markId + MarkSpec resolver。 |
| `RuleMode` | 改 ModeRef；显示名从 declaration 解析；互斥组与持续规则明确。 |
| `RuleOwnedEntity` / `RuleCarrierTrap` | 统一 EntitySpec/EntityInstance/EntityTrigger/Payload。 |
| `SkillEffectRuntime` | 从大 switch + fallback 改 typed executor registry；所有失败返回显式 result。 |
| Formatter / localization | 每个 typed Variant 独立 formatter；动态文本与静态 message key 分离。 |
| Budget | 中央 BudgetPolicy + BudgetLedger + price version。 |
| QA | PlayerCommand 路径、canonical deep snapshot、行为 oracle。 |

### 10.3 DELETE / REPLACE：不应继续兼容为公开语言

| 应删除或隔离的结构 | 原因 | 允许保留的位置 |
|---|---|---|
| 公开 `EffectSpec` 共享字段袋 | 是 UI、Runtime、QA 失真的根源。 | `legacy.v5.LegacyEffectSpec` 迁移适配器。 |
| `EffectVocabularyRegistry` 的对象快照参数列表 | 把字段表单退化成平铺对象选项；同时自证 reachability。 | 静态标签注册可迁入新 Schema metadata，但不能作为 Runtime 证明。 |
| `resolvePendingBindings()` | 会静默改绑，破坏 Stable Ref 与 UNRESOLVED。 | 无。只能由显式迁移向导提出候选，玩家确认后写入。 |
| fixed `RuleMark.Type` 作为玩家词汇 | 不能自定义、rename/delete；非法 ID 回退。 | 旧 trait/internal mechanics 的 migration alias。 |
| raw mode strings | ID 与名称混同。 | v5 migration input。 |
| `fire/poison/heal` carrier payload 字符串 | Entity 专用固定效果 enum，重复 Effect Language。 | v5 migration mapping。 |
| `markType()` 的 HUNTED fallback | 错误 ref 被伪装成有效效果。 | 无。必须 UNRESOLVED/fail closed。 |
| 第一 Resource/Mode 自动绑定 | 顺序相关、静默改语义。 | 无。 |
| `implemented(){ return true; }` 作为覆盖证据 | 不验证 Runtime。 | 可作为临时 feature flag，但名称必须改为 `declared`，不能作为 QA。 |
| QA 行中手写 `true` 的 runtimeVerified | 没有行为证据。 | 无。 |
| 弱 `RuleBuild.fingerprint()` 作为 roundtrip oracle | 丢字段仍可能 PASS。 | 可保留为日志摘要，不可作断言。 |
| `WAIT_CLEARS_RESOURCE` Global Constraint | 是普通 Resource Flow，不是承诺。 | 迁移成 ResourceFlowSpec。 |
| 无执法的 `HP_COMMITMENT` rebate | 假 tradeoff 可换真钱包。 | 在真正 Commitment 实现前移除返还。 |
| `AREA` Modifier | 与 `Targeting.Coverage` 重复，可能双重扩区。 | v5 migration 映射到 Coverage。 |
| Consumable 的 Java class name 公共 Schema | 泄露实现、不可稳定迁移、玩家不能表达属性筛选。 | legacy migration only。 |
| 直接 final-object Archetype tests 作为 acceptance | 绕过 Builder。 | Internal Runtime smoke/fuzz。 |

---

## 11. MANDATORY ADVERSARIAL PLAYER-PATH ACCEPTANCE SUITE — 审计要求摘要

完整规范在 `SPD_CLASS_GAMEPLAY_COMPONENTS_IMPLEMENTATION_CONTRACT_v0.2_FINAL.md` 中冻结。最低必须包括：

1. 两个自定义 Resource：`Rage -2 → Focus +5`，验证原子性、容量、失败与存档；
2. 自定义 Mark“灼痕”：Add 2、requires ≥3、consume 3、rename/delete/save-load；
3. Trap：Enemy Enter Tile → Push 2 → Secondary Poison；
4. Device：每 3 回合、Radius 2、Shield Allies；
5. Device：周期增加玩家自定义 Resource；
6. Delay：选定 Cell，3 turns later 执行任意受支持 EffectChain；
7. Action Attachment / Counter：受击减伤并令下一次攻击附加 Effect；
8. Resource Transfer、指定 Status Copy、Mark Transfer、Barrier Swap；
9. 自定义 Mode、Condition 引用、Mode-specific behavior；
10. Resource/Mark/Mode rename 保持引用；delete 进入 UNRESOLVED，绝不 crash/改绑/静默删除；
11. 完整 Save/Load roundtrip；
12. Budget Ledger roundtrip、rename/order invariant、Constraint credit 必须有执法证据。

每项都必须从真实 `PlayerBuilderCommand` 或 UI 使用的同一 Reducer 构建。禁止直接 `new SkillSpec`/`new ClassBuild` 作为完成证明。

---

## 12. 推荐的新 Codex 实施基线

### 12.1 不能给出的内容

由于压缩包没有 Git metadata，本次不能诚信地给出：

```text
从 commit abc123 开 branch
```

任何具体 hash 都会是编造。

### 12.2 理想历史基线的代码地标

在真实仓库历史中，应寻找**最新一个同时满足以下条件的 commit**：

#### 已经存在并稳定

- `RuleHooks` 已接入 Hero/Mob/Item/Status；
- `RuleRuntime` 有确定性调度、递归 guard、RuleContext/Trace；
- Resource Runtime 与 HUD 基础可用；
- Delay/Scheduled Payload 的保存恢复基础可用；
- Class Action HUD / Runtime 基础可用；
- Headless 使用真实 Hero/Mob/Actor/Level/Buff；
- Build Integrity / Fuzz 基础存在。

#### 尚未或刚开始引入以下错误公开层

- 当前大字段袋 `EffectSpec`；
- 以对象快照和平铺列表为核心的 `EffectVocabularyRegistry`；
- `ClassGameplayComponentSpec` 共享字段 mega union；
- `resolvePendingBindings()`；
- fixed carrier payload `fire/poison/heal`；
- fixed player Mark enum；
- schema-2 coverage / “210 implemented” 自证体系。

这就是最合适的 clean branch point。

### 12.3 如果历史中不存在干净 commit

不要回退整个 SPD 仓库，也不要从零重做 Runtime。应从当前源码建新分支并执行“隔离式重构”：

```text
legacy.v5
  ├─ LegacyEffectSpec
  ├─ LegacyClassGameplayComponentSpec
  ├─ LegacyRuleMarkAlias
  ├─ LegacyCarrierPayloadMapper
  └─ V5ToV6Migration

contract.v6
  ├─ declarations
  ├─ refs
  ├─ effects
  ├─ entities
  ├─ builder
  ├─ runtime-compiler
  └─ qa-playerpath
```

旧层只允许：

- 读取旧存档；
- 映射到 v6；
- 作为 internal smoke fixture。

旧层不得继续驱动新 Builder 或新 QA completion status。

---

## 13. 推荐分阶段施工顺序

### Phase 0 — 冻结与防回归

- 保存当前源码、报告、示例存档；
- 给现有 Runtime 写行为快照，但明确标记 `LEGACY_SMOKE`；
- 引入 feature flag，使 v5 Builder 与 v6 Builder 可短期并存；
- 禁止新增 Domain/职业标签。

### Phase 1 — Stable Declarations / References / Save

- ClassBuild schema v6；
- `StableId`、ResourceSpec/MarkSpec/ModeSpec/EntitySpec；
- typed Ref 与 DependencyGraph；
- 删除所有 auto-bind/fallback；
- declaration/runtime state 分离；
- deep canonical save-load；
- rename/delete/unresolved tests。

### Phase 2 — Typed Skill Core

- typed Trigger/Condition/Effect/Delivery/Targeting/Modifier/Cost/Constraint；
- 唯一 ResourceOperationSpec；
- `Rage -2 → Focus +5` 端到端；
- HP/BuiltinStatRef 边界；
- Builder Number Stepper / Ref Picker。

### Phase 3 — Unified Payload / Entity

- `EffectChainSpec` 与 `PayloadSpec`；
- Device/Trap/Field/Carrier/Delay/Echo/Attachment 共享；
- EntitySpec/Instance、trigger、relation、capacity、persistence；
- 完成 Trap 与两个 Device adversarial tests。

### Phase 4 — Transfer / Copy / Mode

- Resource Transfer；
- explicit Status Copy；
- dynamic Mark Transfer；
- Barrier Swap；
- ModeSpec/ModeRef/Mode condition；
- Capability/Behavior 只有在真实 declarative runtime 完成后才 expose。

### Phase 5 — Constraint / Vow / Budget

- Condition、SkillConstraint、ClassConstraint 分层；
- Action/Equipment/Position/Healing constraints 的真实 enforcement；
- EffectiveConstraintValue 与旁路检查；
- BudgetLedger、price version、roundtrip。

### Phase 6 — Formatter / Localization / UX

- typed formatter；
- 动态名称不查 message key；
- unresolved 明示；
- 全参数 Number Picker/Stepper；
- Family→Variant→Parameters；
- 英/简中完整枚举测试，禁止 `NO TEXT FOUND`/`�`。

### Phase 7 — 新通用 Primitive 基础

按跨构筑复用优先级加入：

1. Death event + Corpse Entity + EntityCost；
2. Snapshot/Restore whitelist；
3. Observation/Ability provenance/LearnedAbilityPool；
4. Property/Ingredient/Decompose/Synthesis 基础；
5. 内容库、完整炼成配方、完整悟道能力保持 Deferred。

### Phase 8 — Mandatory Adversarial Acceptance

- 12 个 Player-path 核心测试全部通过；
- 15 个 Archetype Recipe 从 BuilderCommand 构建；
- Headless 执行真实 Runtime；
- Fuzz/Integrity 继续作为辅助；
- 只有这时才能发布新的 completion matrix。

---

## 14. 最终审计结论

### 14.1 值得保留的投入

此前工作并非“全部无效”。最有价值的成果是：

- 真实 SPD 事件接入；
- Rule Runtime 的调度与保护；
- 多资源与 HUD 基础；
- ClassOperation 概念；
- 延迟、Attachment、Owned Entity 的低层原型；
- Headless 真实游戏环境；
- Fuzz / Integrity 工具链。

这些足以让新实现不必从零开始。

### 14.2 必须停止继续扩建的层

不能再围绕以下结构继续“补实现”：

- generic `power/duration/stateId` 字段袋；
- fixed Mark enum；
- fixed carrier payload；
- raw mode strings；
- auto-binding；
- Registry 自证；
- direct final-object archetype reconstruction；
- no-crash 即完成。

继续在这套抽象上增加 Stun、Slow、更多 Device behavior 或更多 coverage 行，只会扩大迁移债务。

### 14.3 最准确的项目状态描述

> 当前项目已经拥有可用的“规则运行时实验基础”，但尚未拥有符合 v0.1 目标的“通用玩家 Gameplay Component Language”。下一步应以 v0.2 FINAL Contract 为唯一新实现依据，保留 Runtime foundation，隔离并替换旧 Gameplay Language 层。

---

## Appendix A — 关键源码证据索引

| 事实 | 文件与位置 |
|---|---|
| Effect 大字段袋、默认 Actor/Device/Trap/Field/Mark | `rules/EffectSpec.java:30-72` |
| `implemented()` 主要验证 family/operation | `rules/EffectSpec.java:85-95` |
| Skill Convert 无 target amount | `rules/EffectSpec.java:129-135`；`rules/EffectVocabularyRegistry.java:209-215` |
| Carrier 固定 fire/poison/heal | `rules/EffectVocabularyRegistry.java:228-235` |
| Mark 固定 enum | `actors/buffs/RuleMark.java:10-11`；`EffectVocabularyRegistry.java:236-243` |
| Mode raw string | `EffectVocabularyRegistry.java:247-253`；`actors/buffs/RuleMode.java` |
| Player reachability 使用同 Registry | `rules/EffectVocabularyRegistry.java:258-295` |
| auto-bind 第一个 Resource/Mode | `rules/ClassBuild.java:366-428` |
| load 后自动补绑 | `rules/ClassBuild.java:454-505` |
| Builder 打开 sheet 就补绑 | `windows/WndCreateClass.java:101-102` |
| Skill Convert formatter 硬编码 1:1 | `windows/WndCreateClass.java:1252-1254` |
| Skill Convert Runtime 1:1 + partial | `rules/SkillEffectRuntime.java:148` |
| invalid Mark 回退 HUNTED | `rules/SkillEffectRuntime.java:150` |
| Create Entity 固定 carrier payload | `rules/SkillEffectRuntime.java:152-175` |
| RuleOwnedEntity 底层已保存任意 primary/secondary | `actors/mobs/npcs/RuleOwnedEntity.java:41-92` |
| Delay 拆分 primary/secondary | `rules/RuleDefinition.java:174-185` |
| Coverage Audit 大量直接 true | `qa/GameplayComponentCoverageAudit.java:44-88` |
| Effect runtimeVerified=`implemented()` | `qa/GameplayComponentCoverageAudit.java:92-93` |
| PlayerBuildAssembler 直接接 final object | `rules/PlayerBuildAssembler.java:92-105, 143-174` |
| RuleBuild 自动补绑 | `qa/RuleBuild.java:39-44` |
| RuleBuild 指纹遗漏关键字段 | `qa/RuleBuild.java:77-129` |
| 动态 Mode ID 被当 message key | `rules/TraitSpec.java:55-61` |

## Appendix B — 本环境未执行项

- Gradle `:core:test`：**NOT EXECUTED**；Wrapper distribution 下载因 DNS/网络隔离失败。
- Android/desktop 构建：**NOT EXECUTED**。
- 真机 Player Builder 手动回归：**NOT EXECUTED**。
- 历史报告中的 210/210、1800 runs、archetype counts：**NOT RE-CERTIFIED**。

这些限制不会改变已经由源码直接证明的 Schema、Builder、Dependency 与 QA 口径问题，但 Runtime 行为仍应由新实施会话在可联网/有 Gradle 缓存的环境中复跑。
