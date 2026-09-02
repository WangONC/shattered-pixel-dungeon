# Shattered Pixel Dungeon 自塑职业 Gameplay Components

# SPD_CLASS_GAMEPLAY_COMPONENTS_IMPLEMENTATION_CONTRACT_v0.2_FINAL

**状态：FINAL / NORMATIVE**  
**Contract Version：`0.2-final`**  
**首个实现 Schema Version：`6`**  
**日期：2026-09-02**

---

## 0. 文档权威与规范用语

本文件是后续新 Codex 会话实现“自塑职业 / Create A Class” Gameplay Component Language 的唯一 v0.2 Implementation Contract。

它基于：

- `SPD_CLASS_GAMEPLAY_COMPONENTS_SPEC_v0.1.md`；
- 当前实际源码与玩家 Builder 路径审计；
- Rule Runtime、Headless、HUD 等已有可保留资产；
- 15 个参考 Archetype 的通用机制拆解。

本文件不把任何现有 QA 报告视为完成事实。

规范词：

- **MUST / 必须**：实现与验收不可省略；
- **MUST NOT / 禁止**：违反即不符合 Contract；
- **SHOULD / 应**：除非有记录充分的工程原因，否则必须遵守；
- **MAY / 可以**：可选扩展；
- **DEFERRED**：本轮只保留数据边界，不得伪装为已实现；
- **UNSUPPORTED**：Schema 可以识别，但 Player Builder 不得暴露，Runtime 必须 fail closed。

---

## 1. 目标与非目标

### 1.1 核心目标

系统必须建立一套不依赖固定职业身份的：

> **通用 Gameplay Component Language**

玩家通过通用声明、触发、条件、效果、传递、目标、修改、成本、约束和 Class-level 规则构造职业。

例如枪手必须由以下组合涌现：

```text
有限 Resource
+ 主动 Reload ClassOperation
+ Projectile Delivery
+ Damage Effect
+ Pierce Modifier
+ Resource Cost
```

禁止通过以下标签获得隐藏行为：

```text
GUNNER_CORE
SUMMONER_DOMAIN
ENGINEERING_DOMAIN
BLUE_MAGE_DOMAIN
BLOOD_CLASS
```

### 1.2 Class-level 与 Skill-level 边界

Class-level Components 表达持续存在的职业结构，例如：

- 自定义 Resource 声明与 Resource Flow；
- Basic Attack Model；
- Ownership / Relation policy；
- Entity Capacity / Persistence；
- Mode Engine；
- ClassOperation；
- ClassConstraint / Vow；
- 全局 Rule / Law。

Skill-level Components 表达一次技能或反应规则：

```text
Skill
├─ Activation / Trigger
├─ Condition
├─ Primary Core Effect
├─ Secondary Effect [0..1]
├─ Delivery
├─ Targeting
├─ Modifier [0..1]
├─ Cost [0..1]
└─ Skill Constraint [0..1]
```

Class-level 行为不得为了复用旧 Runtime 而伪装成普通 Skill；Skill 也不得携带隐藏 Class Domain。

### 1.3 Deferred 高层系统

本 Contract 可以定义底层 Primitive，但不要求本轮完成：

- Talent；
- Subclass；
- Specialization；
- Armor Ability progression；
- 完整 Luck System；
- 完整器官 Slot；
- 正式 Gun Item System；
- Nemesis；
- 尸体跨局遗产；
- 完整炼成内容库；
- 完整悟道内容库。

这些项目不得被计入 v0.2 Gameplay Component completion。

---

## 2. 不可违反的架构不变量

### 2.1 无职业 Domain

Runtime、Builder、Budget、Formatter、QA 都禁止根据职业名称或 Archetype 标签分支。

允许：

```java
if (effect instanceof CreateEntityEffect) { ... }
```

禁止：

```java
if (build.hasTag("ENGINEER")) { ... }
```

### 2.2 声明、引用、Runtime State 必须分离

每个玩家可命名对象分成：

1. **Declaration Spec**：设计定义；
2. **Typed Ref**：其他节点的稳定引用；
3. **Runtime State**：当前值、持续时间、实例、冷却等可变状态。

禁止把 Runtime 当前值写回声明对象来保存。

### 2.3 名称不是 ID

- ID 创建后永久不变；
- rename 只改 display name；
- 同名声明允许存在；
- 引用永远只存 ID；
- 删除后引用必须成为 `UNRESOLVED`；
- 新建同名对象不得自动接管旧引用。

### 2.4 禁止自动补绑和默认回退

禁止：

- 绑定到第一个 Resource；
- 绑定到第一个 Mode；
- 无效 Mark 回退 HUNTED/CHARGED；
- 删除依赖后自动换绑；
- unknown variant 静默映射为 Standard Damage；
- Formatter 用另一个合法对象掩盖错误。

### 2.5 HP 不是 ResourcePool

HP、Max HP、Missing HP、Barrier、Temporary HP 是 Built-in Stat / State。

禁止创建第二套普通 `ResourceSpec("HP")` 来实现：

- HP Cost；
- Low HP Condition；
- Missing HP Scaling；
- Heal；
- Temporary HP；
- Barrier。

### 2.6 同一种 Gameplay Effect 只有一套公开 Schema

Skill、Device、Trap、Field、Carrier、Delay、Echo、Action Attachment 执行 Damage/Status/Push/Heal/Resource/Mark 时，必须复用同一个 `EffectSpec` 与 executor。

禁止每个载体维护自己的固定 effect enum 或 payload string。

### 2.7 Builder 与 QA 使用同一玩家命令模型

UI 和 Headless Player-facing Assembler 必须调用同一个 `BuilderCommand`/Reducer。

验收禁止直接接收已经构造完成的：

- `ClassBuildSpec`；
- `SkillSpec`；
- `EffectSpec`；
- `EntitySpec`。

### 2.8 Runtime 不得修改 Build Spec

`RuleRuntime` 构造、compile、load、tick、execute 均不得：

- 填空引用；
- 改声明 ID；
- 重排玩家节点；
- 改 Builder draft；
- 将 Runtime 值写回 declaration。

---

## 3. 版本与推荐代码边界

### 3.1 Schema 版本

首个符合本 Contract 的 Class Build：

```text
contractVersion = "0.2-final"
schemaVersion   = 6
```

所有 Bundle 必须同时保存 schemaVersion。未知更高版本必须进入 `UNSUPPORTED`，不得猜测读取。

### 3.2 推荐模块边界

包名可以按项目风格调整，但职责必须等价：

```text
rules/spec/             纯声明与 typed variants
rules/ref/              Stable IDs、typed refs、dependency resolver
rules/builder/          BuilderCommand、Reducer、FormSchema
rules/compile/          Spec → Runtime compile plan
rules/runtime/          RuleRuntime、executors、transactions、state
rules/runtime/entity/   Entity instances
rules/migration/v5/     当前旧 schema 适配
rules/format/           player-facing formatter
qa/playerpath/          mandatory player-path acceptance
qa/runtime/             direct runtime smoke/fuzz
```

旧字段袋必须隔离为 migration/legacy 类型，不得继续作为新 Builder 的 model。

---

## 4. 通用基础类型

### 4.1 Stable ID

所有玩家可引用节点必须拥有 opaque immutable ID。

```java
final class StableId {
    String value; // immutable
}
```

新 ID 格式：

```text
<prefix>_<32 lowercase hex>
```

示例：

```text
res_7e0f824069724eddb750890f944fd176
mark_1ec54fb5b2454ce492afe948405df481
mode_2a67482977f944d68949cfdd39317ac7
```

必需前缀：

```text
build res mark mode modegrp entity capacity skill effect component op
abilitypool ability property recipe constraint link snapshot
```

规则：

- ID 不得包含 display name、枚举名或列表序号；
- 新建使用可注入 `IdGenerator`；
- 测试使用 deterministic generator；
- v5→v6 migration 使用 build namespace + old path 生成确定性 ID；
- copy/duplicate 创建新 ID；
- rename 不改 ID；
- save/load 必须保持字节等价 ID。

### 4.2 Display Name

```java
final class DisplayName {
    String text;
}
```

验证：

- Unicode NFC；
- trim 后 1..24 Unicode code points；
- 禁止控制字符；
- 允许同名；
- 玩家名称按原文显示，不调用 `Messages.get()`；
- 本地化只用于字段标签、Variant 名和句法模板。

### 4.3 Typed Ref

每种声明使用独立 Ref 类型；禁止公共字符串字段模拟多种引用。

```java
final class ResourceRef        { StableId targetId; String lastKnownDisplayName; }
final class MarkRef            { StableId targetId; String lastKnownDisplayName; }
final class ModeGroupRef       { StableId targetId; String lastKnownDisplayName; }
final class ModeRef            { StableId targetId; String lastKnownDisplayName; }
final class EntitySpecRef      { StableId targetId; String lastKnownDisplayName; }
final class CapacityRef        { StableId targetId; String lastKnownDisplayName; }
final class ComponentRef       { StableId targetId; String lastKnownDisplayName; }
final class AbilityPoolRef     { StableId targetId; String lastKnownDisplayName; }
final class PropertyRef        { StableId targetId; String lastKnownDisplayName; }
final class SynthesisRecipeRef { StableId targetId; String lastKnownDisplayName; }
```

`lastKnownDisplayName` 仅用于 unresolved UI，不参与解析。

### 4.4 Dependency State

```java
enum DependencyState {
    RESOLVED,
    UNRESOLVED,
    HARD_CONFLICT,
    UNSUPPORTED
}
```

定义：

- `RESOLVED`：目标存在、类型正确、能力兼容；
- `UNRESOLVED`：目标 ID 不存在；
- `HARD_CONFLICT`：ID 重复、类型错误、非法循环、互斥参数冲突；
- `UNSUPPORTED`：节点/Variant 可读取但当前 Runtime 没有正式能力。

Resolver 输出：

```java
final class DependencyDiagnostic {
    StableId ownerNodeId;
    String fieldPath;
    DependencyState state;
    StableId targetId;
    String messageKey;
}
```

不得将 resolution state 保存为权威数据；每次由 declaration graph 计算。诊断可以缓存。

### 4.5 Runtime 结果

每个 Effect executor 必须返回显式结果：

```java
enum EffectResultCode {
    APPLIED,
    NO_VALID_TARGET,
    BLOCKED,
    UNSUPPORTED,
    FAILED
}

final class EffectResult {
    EffectResultCode code;
    int affectedTargets;
    List<RuntimeDiagnostic> diagnostics;
}
```

禁止只用 `boolean` 掩盖“不支持”“无目标”“被免疫”“Schema 错误”的区别。

### 4.6 数值表达式

v0.2 不引入任意脚本表达式。所有可缩放数值使用受限 `ValueSpec`：

```java
interface ValueSpec {}

final class FixedValueSpec implements ValueSpec {
    int value;
}

final class ScaledValueSpec implements ValueSpec {
    int base;
    ValueSourceSpec source;
    int numerator;     // >= 0
    int denominator;   // >= 1
    int minimum;
    int maximum;
    RoundingMode rounding; // FLOOR required in v0.2
}
```

计算：

```text
raw = base + floor(sourceValue * numerator / denominator)
result = clamp(raw, minimum, maximum)
```

v0.2 每个 ValueSpec 最多一个 scaling term。多项表达式 DEFERRED。

```java
interface ValueSourceSpec {}
final class BuiltinStatSource implements ValueSourceSpec {
    SubjectSelector subject;
    BuiltinStatRef stat;
}
final class ResourceValueSource implements ValueSourceSpec {
    ResourceHolderSelector holder;
    ResourceRef resource;
}
final class MarkValueSource implements ValueSourceSpec {
    SubjectSelector subject;
    MarkRef mark;
}
```

---

## 5. ClassBuildSpec v6

```java
final class ClassBuildSpec {
    int schemaVersion;                  // MUST be 6
    String contractVersion;             // MUST be "0.2-final"
    StableId buildId;
    DisplayName displayName;

    List<ResourceSpec> resources;
    List<MarkSpec> marks;
    List<ModeGroupSpec> modeGroups;
    List<ModeSpec> modes;
    List<EntityCapacitySpec> capacities;
    List<EntitySpec> entities;
    List<AbilityPoolSpec> abilityPools;
    List<PropertySpec> properties;
    List<SynthesisRecipeSpec> recipes;

    List<ClassGameplayComponentSpec> classComponents;
    List<ClassConstraintSpec> classConstraints;
    List<ClassOperationSpec> classOperations;
    List<SkillSpec> skills;

    StartingKitSpec startingKit;
    ClassProgression progression;       // preserved; higher progression is Deferred
    BudgetMetadata budgetMetadata;
}
```

约束：

- 所有 list 的玩家顺序必须保存；
- Runtime 编译可建立独立排序，但不得改原顺序；
- 声明 ID 在 build 内必须唯一；
- Skill、Effect、Component、Constraint、Operation 也必须有 stable node ID；
- `ClassBuildSpec` 不保存当前 Resource 值、Mode duration、Entity instances、cooldown 等 Runtime State；
- unresolved draft 可以保存；
- 开始新游戏/应用到 Hero 前必须通过 finalization validation。

---

## 6. Resource Contract

### 6.1 ResourceSpec

```java
final class ResourceSpec {
    StableId id;
    DisplayName displayName;
    int minimum;             // default 0
    int maximum;             // MUST > minimum
    int initialValue;        // minimum <= initial <= maximum
    ResourceOverflowPolicy defaultOverflowPolicy;
    ResourceHudSpec hud;
}

enum ResourceOverflowPolicy {
    FAIL,
    CLAMP,
    DISCARD_EXCESS
}
```

Resource 是玩家创建的独立池，例如 Ammo、Rage、Focus、Heat。

禁止内置职业语义字段：

```text
isAmmo
isRage
isMana
```

### 6.2 ResourceState

```java
final class ResourceState {
    ResourceRef resource;
    int current;
    List<ResourceReservationState> reservations;
    List<ResourceSuppressionState> suppressions;
}
```

`ResourceState.current` 是唯一权威当前值。`ResourceSpec` 中禁止 `current`。

### 6.3 Resource holder

Hero 是默认 Resource Holder。Entity 只有显式声明 `ResourceStorageCapability` 才能持有 Resource：

```java
final class ResourceStorageCapability {
    List<EntityResourceSlotSpec> slots;
}

final class EntityResourceSlotSpec {
    ResourceRef resource;
    int initialValue;
    int maximumOverride; // 0 = use declaration maximum
}
```

这为真正 Resource Transfer、Device battery、Summon ammo 提供通用基础。

### 6.4 统一 ResourceOperationSpec

Class Resource Flow、Skill Effect、Device Payload、Recycle、ClassOperation 必须复用同一组 Variant：

```java
interface ResourceOperationSpec {}

final class GainResourceSpec implements ResourceOperationSpec {
    ResourceHolderSelector holder;
    ResourceRef resource;
    ValueSpec amount; // positive
    ResourceOverflowPolicy overflowPolicy;
}

final class DrainResourceSpec implements ResourceOperationSpec {
    ResourceHolderSelector holder;
    ResourceRef resource;
    ValueSpec amount; // positive
    InsufficientResourcePolicy insufficientPolicy;
}

enum InsufficientResourcePolicy {
    FAIL,
    DRAIN_AVAILABLE
}

final class SetResourceSpec implements ResourceOperationSpec {
    ResourceHolderSelector holder;
    ResourceRef resource;
    ValueSpec value;
}

final class ClearResourceSpec implements ResourceOperationSpec {
    ResourceHolderSelector holder;
    ResourceRef resource;
}

final class ConvertResourceSpec implements ResourceOperationSpec {
    ResourceHolderSelector holder;
    ResourceRef source;
    int sourceAmount;  // >= 1
    ResourceRef target;
    int targetAmount;  // >= 1
    ConversionPolicy conversionPolicy;
    ResourceOverflowPolicy targetOverflowPolicy;
}

enum ConversionPolicy {
    EXACT_ATOMIC
    // PROPORTIONAL is reserved and UNSUPPORTED in v0.2
}

final class ReserveResourceSpec implements ResourceOperationSpec {
    ResourceHolderSelector holder;
    ResourceRef resource;
    int amount;
    int durationTurns;
    ReservationExpiryPolicy expiryPolicy;
}

final class SuppressResourceSpec implements ResourceOperationSpec {
    ResourceHolderSelector holder;
    ResourceRef resource;
    int durationTurns;
    ResourceSuppressionMode mode;
}
```

### 6.5 Convert 精确语义

`EXACT_ATOMIC` 是 v0.2 唯一支持的转换模式。

以：

```text
Rage -2 → Focus +5
```

为例：

1. source 与 target 必须 RESOLVED 且不同；
2. source 当前值必须至少 2；
3. 若 target policy 为 FAIL，则 target 必须有完整 5 点空间；
4. 所有检查通过后，在同一 ResourceTransaction 中：
   - Rage 精确减 2；
   - Focus 精确加 5；
5. 任一检查失败，两个池都不得变化；
6. 不允许 `min(available, amount)`；
7. 不允许目标只加 2、3、4 而 source 仍扣 2，除非未来显式加入另一 policy；
8. Runtime trace 必须记录 source ID、source delta、target ID、target delta 与 transaction ID。

`CLAMP`/`DISCARD_EXCESS` 可用于普通 Gain；Convert 默认和 Builder 必选 `FAIL`。未来若开放非 FAIL，必须另加明确验收。

### 6.6 Resource rename / delete

- rename 只改 `ResourceSpec.displayName`；
- 所有 ResourceRef 保持 targetId；
- delete 只删除 ResourceSpec；
- 现有 Ref 进入 UNRESOLVED；
- 新建同名 Resource 有新 ID，旧 Ref 仍 unresolved；
- 恢复原 declaration（同 ID）后自动重新 RESOLVED；
- 禁止绑定到第一个 Resource。

---

## 7. Built-in Stat Contract

```java
enum BuiltinStatRef {
    HP_CURRENT,
    HP_MAX,
    HP_MISSING,
    HP_PERCENT,
    BARRIER,
    TEMPORARY_HP,
    HERO_LEVEL,
    DAMAGE_TAKEN_CURRENT_EVENT,
    DISTANCE_CURRENT_EVENT
}
```

规则：

- BuiltinStatRef 不出现在 `resources` list；
- Builder 的 Ref Picker 必须把 Built-in Stat 与 Resource 分组显示；
- Built-in Stat 不可 rename/delete；
- HP_CURRENT 永远指真实 HP，不含 Barrier；
- HP_MISSING = max(0, HP_MAX - HP_CURRENT)；
- HP_PERCENT 使用整数百分比 0..100，向下取整；
- Barrier 与 Temporary HP 必须由不同 executor/state 表达；
- HP Cost 默认不能致死，且 Barrier/Temporary HP 不代付真实 HP Cost。

---

## 8. Mark / Stack / Charge / Counter Contract

### 8.1 MarkSpec

Mark、Stack、Charge、Counter 共用一个通用数值状态模型，区别是声明语义，不是不同硬编码 Runtime enum。

```java
final class MarkSpec {
    StableId id;
    DisplayName displayName;
    MarkKind kind;
    int minimum;               // default 0
    int maximum;               // >= 1
    int initialValue;          // normally 0
    MarkDurationPolicy durationPolicy;
    int defaultDurationTurns;  // used only for TURN_BASED
    MarkRefreshPolicy refreshPolicy;
    MarkOverflowPolicy overflowPolicy;
    MarkProvenancePolicy provenancePolicy;
}

enum MarkKind {
    MARK,
    STACK,
    CHARGE,
    COUNTER,
    FLAG
}

enum MarkDurationPolicy {
    PERMANENT_UNTIL_REMOVED,
    TURN_BASED,
    END_OF_LEVEL
}

enum MarkRefreshPolicy {
    REPLACE_DURATION,
    KEEP_LONGER,
    ADD_DURATION
}

enum MarkOverflowPolicy {
    CLAMP,
    REJECT
}

enum MarkProvenancePolicy {
    NONE,
    TRACK_LAST_SOURCE
}
```

### 8.2 MarkState

```java
final class MarkState {
    MarkRef mark;
    RuntimeActorId subjectActorId;
    RuntimeActorId sourceActorId; // optional
    int value;
    int remainingTurns;           // -1 = non-turn-based
    int transferDepth;
}
```

现有 `RuleMark` Buff 可以作为 Runtime 承载方式，但必须将 enum 改为动态 `markId`。

### 8.3 Mark operations

Mark Effect Family 必须提供：

```java
AddMarkEffect      { MarkRef mark; int amount; OptionalInt durationOverride; }
SetMarkEffect      { MarkRef mark; int value;  OptionalInt durationOverride; }
ConsumeMarkEffect  { MarkRef mark; int amount; }
RemoveMarkEffect   { MarkRef mark; }
```

`STACK/CHARGE/COUNTER` 不再是不同 Effect Operation；它们由 `MarkSpec.kind` 决定显示和语义标签。

传播/转移属于 Transfer/Copy Family：

```java
TransferMarkEffect { MarkRef mark; SubjectSelector source; SubjectSelector destination; int amount; }
```

### 8.4 Mark refs 的所有使用点

以下位置必须使用同一 `MarkRef`：

- Effect：Add/Set/Consume/Remove/Transfer；
- Condition：MarkAtLeast/AtMost/Present；
- Cost：MarkCost；
- Target Filter：HasMark；
- Value Source：MarkValueSource；
- Formatter；
- Budget；
- Save/Load；
- QA。

禁止任何 invalid mark fallback。

---

## 9. Mode Contract

### 9.1 ModeGroupSpec 与 ModeSpec

```java
final class ModeGroupSpec {
    StableId id;
    DisplayName displayName;
    ModeGroupPolicy policy; // EXCLUSIVE required in v0.2
}

enum ModeGroupPolicy {
    EXCLUSIVE,
    INDEPENDENT
}

final class ModeSpec {
    StableId id;
    DisplayName displayName;
    ModeGroupRef group;
    boolean initial;
    ModeDurationPolicy durationPolicy;
    int defaultDurationTurns;
}

enum ModeDurationPolicy {
    PERSISTENT,
    TURN_BASED
}
```

约束：

- EXCLUSIVE group 最多一个 initial Mode；
- 同组切换时原 Mode 被明确退出；
- 不同 group 可同时 active；
- Mode ID 与显示名分离；
- Mode 名称不得被拼成 message key。

### 9.2 ModeState

```java
final class ModeState {
    ModeRef mode;
    RuntimeActorId subjectActorId;
    int remainingTurns; // -1 persistent
}
```

### 9.3 ModeShift

```java
final class ModeShiftEffect implements EffectSpec {
    ModeRef mode;
    ModeTransition transition;
    OptionalInt durationOverride;
}

enum ModeTransition {
    ACTIVATE,
    DEACTIVATE,
    TOGGLE,
    REPLACE_GROUP
}
```

Mode-specific behavior必须通过其他 Skill/Component 的 `ModeActiveCondition` 引用 ModeRef 实现，不允许 Mode 名字触发隐藏逻辑。

---

## 10. Entity Contract

### 10.1 Blueprint 与 Instance 分离

```java
final class EntitySpec {
    StableId id;
    DisplayName displayName;
    EntityType type;
    EntityBodySpec body;
    SpawnPolicySpec spawnPolicy;
    OwnershipSpec ownership;
    RelationSpec relation;
    Optional<CapacityRef> capacity;
    PersistenceSpec persistence;
    List<EntityCapabilitySpec> capabilities;
}

enum EntityType {
    ACTOR,
    DEVICE,
    TRAP,
    FIELD,
    CARRIER,
    CORPSE
}
```

Runtime：

```java
final class EntityInstanceState {
    RuntimeEntityId instanceId;
    EntitySpecRef blueprint;
    RuntimeActorId ownerActorId;
    RuntimeActorId sourceActorId;
    int cell;
    int remainingTurns;
    int createdOrder;
    List<EntityCapabilityState> capabilityStates;
}
```

EntitySpec 是玩家设计声明；EntityInstanceState 是一局中的具体实例。

### 10.2 EntityBodySpec typed variants

禁止一个 EntitySpec 同时拥有所有类型字段。

```java
interface EntityBodySpec {}

final class ActorBodySpec implements EntityBodySpec {
    int maxHp;
    int actionPeriodTurns;
    CollisionProfile collision;
    BehaviorSpec behavior;
    Optional<PayloadSpec> basicActionPayload;
}

final class DeviceBodySpec implements EntityBodySpec {
    int maxHp;
    EntityTriggerSpec trigger;
    PayloadSpec payload;
}

final class TrapBodySpec implements EntityBodySpec {
    EntityTriggerSpec trigger; // ENEMY/ACTOR_ENTER_TILE required
    PayloadSpec payload;
    boolean oneShot;
    TrapVisibility visibility;
}

final class FieldBodySpec implements EntityBodySpec {
    EntityTriggerSpec trigger;
    PayloadSpec payload; // 区域完全由 payload.targeting.coverage 定义
}

final class CarrierBodySpec implements EntityBodySpec {
    CarrierAnchor anchor;
    EntityTriggerSpec trigger;
    PayloadSpec payload;
}

final class CorpseBodySpec implements EntityBodySpec {
    EntityFilterSpec sourceFilter;
    List<PropertyGrantSpec> residueProperties;
    boolean targetable;
    boolean consumable;
}
```

### 10.3 BehaviorSpec

v0.2 只允许 declarative whitelist：

```java
interface BehaviorSpec {}

FollowOwnerBehavior
GuardCellBehavior
AttackNearestBehavior { EntityFilterSpec targetFilter; int searchRange; }
StationaryBehavior
```

完整 Behavior Override DEFERRED。没有 declarative BehaviorSpec 时，Builder 不得暴露 `TRANSFORM_BEHAVIOR`。

### 10.4 EntityTriggerSpec

Entity trigger 使用同一事件基础，但有 Entity-local context：

```java
interface EntityTriggerSpec {}

PeriodicEntityTrigger       { int initialDelayTurns; int periodTurns; }
ActorEnterTileEntityTrigger { EntityFilterSpec actorFilter; }
ContactEntityTrigger        { EntityFilterSpec actorFilter; }
EntityDamagedTrigger        { OptionalInt minimumDamage; }
EntityCreatedTrigger
EntityDestroyedTrigger
OwnerActionTrigger          { ActionEventRef event; }
```

Trap 的 `ActorEnterTileEntityTrigger` 必须把进入者作为 `EVENT_TARGET` 传给整个 EffectChain，即使 Primary Push 后其 cell 改变，Secondary 仍作用于同一进入者，除非 Secondary 明确要求重新选目标。

### 10.5 Ownership / Relation / Link

```java
final class OwnershipSpec {
    OwnerSelector ownerSelector;
    OwnershipTransferPolicy transferPolicy;
}

enum OwnerSelector {
    CLASS_OWNER,
    EVENT_SOURCE,
    EFFECT_TARGET
}

enum OwnershipTransferPolicy {
    FIXED,
    TRANSFERABLE
}

final class RelationSpec {
    RelationAlignment alignment;
    boolean controllable;
}

enum RelationAlignment {
    ALLY,
    ENEMY,
    NEUTRAL
}

final class LinkSpec {
    StableId id;
    LinkKind kind;
    SubjectSelector source;
    SubjectSelector destination;
    int strength;
    int durationTurns;
}

enum LinkKind {
    DAMAGE_REDIRECT,
    RESOURCE_CHANNEL,
    COMMAND,
    CUSTOM_CAPABILITY // unsupported unless a matching whitelist entry exists
}
```

### 10.6 Entity capacity

```java
final class EntityCapacitySpec {
    StableId id;
    DisplayName displayName;
    EntityFilterSpec filter;
    int maximum;
    CapacityOverflowPolicy overflowPolicy;
}

enum CapacityOverflowPolicy {
    REJECT_NEW,
    REMOVE_OLDEST
}
```

v0.2 Builder 默认 `REJECT_NEW`。`REMOVE_OLDEST` 只有在独立行为测试和明确 UI 警告完成后才可暴露。

Capacity 检查必须基于 EntitySpec/Instance filter，不得依赖 `RuleOwnedEntity.Kind` 的不完整硬编码；Trap、Corpse 也必须可纳入 filter。

### 10.7 Persistence

Entity 使用统一 `PersistenceSpec`，见第 20 节。不同 EntityType 的非法组合由 validator 返回 HARD_CONFLICT，不得静默改写。

### 10.8 Create Entity Effect

```java
final class CreateEntityEffect implements EffectSpec {
    EntitySpecRef entity;
    int count;
    SpawnPlacementSpec placement;
    EntityCreationPolicy creationPolicy;
}
```

Create Device/Trap/Field 不再拥有专用 fire/poison/heal payload 字段。Payload 定义在 EntitySpec body 中，使用统一 `PayloadSpec`。


---

## 11. TriggerSpec 与 Runtime Context

### 11.1 Event Context

所有 Trigger、Condition、Targeting 与 Effect 共享不可变事件上下文：

```java
final class GameplayEventContext {
    long eventId;
    long causeEventId;
    RuleEventType eventType;
    RuntimeActorId classOwnerId;
    RuntimeActorId sourceActorId;   // optional
    RuntimeActorId targetActorId;   // optional
    RuntimeEntityId sourceEntityId; // optional
    int sourceCell;                 // -1 if absent
    int targetCell;                 // -1 if absent
    int numericAmount;              // event-specific; 0 if absent
    Optional<ItemRuntimeRef> item;
    Optional<StatusRef> status;
    Optional<AbilitySignature> ability;
}
```

Context 在一次规则执行链中不可变。Effect 需要更新目标位置等状态时，只修改 Runtime State，不反向修改原 Context；后续 Secondary 默认仍持有同一 actor identity。

### 11.2 TriggerSpec typed variants

```java
interface TriggerSpec {
    StableId nodeId();
}

final class ActiveTrigger implements TriggerSpec {}
final class EventTrigger implements TriggerSpec {
    RuleEventType event;
}
final class PeriodicTrigger implements TriggerSpec {
    int initialDelayTurns;
    int periodTurns;
}
final class ThresholdCrossedTrigger implements TriggerSpec {
    SubjectSelector subject;
    BuiltinStatRef stat;
    ComparisonOperator direction;
    int threshold;
}
final class AbilityObservedTrigger implements TriggerSpec {
    ObservationSpec observation;
}
```

`RuleEventType` 至少包含：

```text
TURN_START
WAIT
MOVE
ENTER_TILE
ATTACK_DECLARED
ATTACK_HIT
DAMAGED
KILL
DEATH
ITEM_USED
STATUS_APPLIED
LOW_HP_ENTERED
EQUIPMENT_CHANGED
ENTITY_CREATED
ENTITY_DESTROYED
```

现有 `RuleHooks` 对应事件应继续保留。新增事件必须通过同一小型 Hook 层接入，禁止直接在 Hero/Mob 中判断职业标签。

### 11.3 触发去重与因果

- 每个 Gameplay Event 有唯一 eventId；
- 派生事件记录 causeEventId；
- 同一 Skill 在自己的执行链内不得无限重入；
- 递归深度上限必须保留并可配置，默认 4；
- Guard key 必须至少包含 Skill ID 与 cause chain；
- `finally` 中必须释放 guard；
- Delay/Echo/Attachment 触发时创建新 eventId，但保留 originating cause metadata；
- Fuzz 必须检测循环资源、Mark、Entity 和 Event chain。

---

## 12. ConditionSpec

### 12.1 Boolean 结构

```java
interface ConditionExpr {}

final class AllOfCondition implements ConditionExpr {
    List<ConditionExpr> children;
}
final class AnyOfCondition implements ConditionExpr {
    List<ConditionExpr> children;
}
final class NotCondition implements ConditionExpr {
    ConditionExpr child;
}
final class LeafCondition implements ConditionExpr {
    StableId nodeId;
    ConditionSpec condition;
}
```

v0.2 Player Builder **MUST** 支持：

- 空条件 = Always；
- `AllOf` 0..8 个 leaf。

`AnyOf` 与 `Not` Schema 可以实现，但若 UI/Runtime/QA 未全通，不得暴露并必须标记 UNSUPPORTED。

### 12.2 Required leaf variants

```java
interface ConditionSpec {}

AlwaysCondition
TargetExistsCondition
BuiltinStatCompareCondition {
    SubjectSelector subject;
    BuiltinStatRef stat;
    ComparisonOperator op;
    ValueSpec value;
}
ResourceCompareCondition {
    ResourceHolderSelector holder;
    ResourceRef resource;
    ComparisonOperator op;
    int value;
}
MarkCompareCondition {
    SubjectSelector subject;
    MarkRef mark;
    ComparisonOperator op;
    int value;
}
ModeActiveCondition {
    SubjectSelector subject;
    ModeRef mode;
}
StatusPresentCondition {
    SubjectSelector subject;
    StatusRef status;
}
TerrainCondition {
    CellSelectorRef cell;
    WorldCapabilityRef capability;
    boolean expected;
}
DistanceCompareCondition {
    SubjectOrCellSelector from;
    SubjectOrCellSelector to;
    ComparisonOperator op;
    int distance;
}
AdjacentActorCountCondition {
    SubjectOrCellSelector center;
    EntityFilterSpec filter;
    ComparisonOperator op;
    int count;
}
EntityCountCondition {
    EntityFilterSpec filter;
    ComparisonOperator op;
    int count;
}
RelationExistsCondition {
    RelationFilterSpec filter;
}
StationaryTurnsCondition {
    SubjectSelector subject;
    int turns;
}
AbilityPoolCountCondition {
    AbilityPoolRef pool;
    ComparisonOperator op;
    int count;
}
```

```java
enum ComparisonOperator {
    LT, LTE, EQ, GTE, GT
}
```

### 12.3 Condition 规则

- 每个 Ref 必须独立解析；
- unresolved leaf 返回 `BLOCKED` 并产生 diagnostic，不能当 false 后静默；
- Formatter 必须显示具体 Resource/Mark/Mode/Status 名；
- `RESOURCE_AT_LEAST` 不得在 ref 为空时使用 primary resource；
- `MARKED` 不得表示“拥有任意 RuleMark”；必须绑定 MarkRef；
- `HAS_STATUS` 若允许任意负面状态，必须显式使用 `StatusFilterSpec.ANY_NEGATIVE`，不能省略 StatusRef 后猜测；
- Condition 不产生 Budget credit；它只限定触发资格。

---

## 13. EffectSpec 总则

### 13.1 Typed discriminated union

`EffectSpec` 必须由一个接口和每个 Variant 的独立 concrete class 组成。由于 Android/Java 兼容性，可以使用接口 + tag + 独立类，不要求 Java sealed class。

```java
interface EffectSpec {
    StableId effectId();
    EffectFamily family();
    EffectVariantKey variantKey();
}
```

禁止重新引入包含以下公共字段的 mega-object：

```text
power duration count period lifetime secondaryParameter
resourceId targetResourceId templateId stateId
```

每个 Variant 只能保存自己的字段。

### 13.2 Effect Family

保持 11 个高层 Family：

```text
1. DAMAGE
2. STATUS
3. MOVEMENT
4. RECOVERY_DEFENSE
5. RESOURCE_OPERATION
6. MARK_ACCUMULATION
7. CREATE_ENTITY
8. WORLD_TERRAIN
9. RELATION_CONTROL
10. TRANSFER_COPY
11. TRANSFORM
```

新增 Archetype 所需 Primitive 优先作为这些 Family 的通用 Variant、Trigger、Cost、Entity 或 supporting declaration；只有未来证明无法归类时才新增 Family。

### 13.3 Variant completion state

每个 Variant 注册时必须声明：

```java
enum ImplementationState {
    DECLARED,
    PLAYER_EXPOSED,
    IMPLEMENTED,
    UNSUPPORTED,
    DEFERRED,
    LEGACY_ONLY
}
```

只有满足第 34 节 Definition of Done 才能标记 `IMPLEMENTED`。

`PLAYER_EXPOSED` 不是 `IMPLEMENTED` 的同义词。

---

## 14. Effect Family 与 Variant 参数 Schema

## 14.1 DAMAGE

### 14.1.1 DirectDamageEffect

```java
final class DirectDamageEffect implements EffectSpec {
    StableId effectId;
    ValueSpec amount;
    DamageType damageType;
    NativeDefensePolicy defensePolicy;
}

enum DamageType {
    UNTYPED,
    PHYSICAL,
    MAGICAL,
    FIRE,
    POISON,
    BLEEDING
}

enum NativeDefensePolicy {
    SPD_NATIVE,
    IGNORE_ARMOR // only expose after independent balance/runtime test
}
```

v0.2 required：`SPD_NATIVE`。`IGNORE_ARMOR` 未完成时 UNSUPPORTED。

### 14.1.2 PercentMaxHpDamageEffect

```java
final class PercentMaxHpDamageEffect implements EffectSpec {
    StableId effectId;
    int percent;       // 1..100
    int absoluteCap;   // >= 1
    ProtectedTargetPolicy protectedTargetPolicy;
}
```

Boss/unique target 上限必须显式，不允许 executor 内隐藏特判。

### 14.1.3 MissingHpDamageEffect

```java
final class MissingHpDamageEffect implements EffectSpec {
    StableId effectId;
    ValueSpec baseAmount;
    int missingHpNumerator;
    int missingHpDenominator;
    int absoluteCap;
    DamageType damageType;
}
```

计算：

```text
amount = base + floor(target.HP_MISSING * numerator / denominator)
amount = min(amount, absoluteCap)
```

### 14.1.4 ExecuteEffect

```java
final class ExecuteEffect implements EffectSpec {
    StableId effectId;
    int hpPercentThreshold;        // 1..99
    Optional<ValueSpec> fallbackDamage;
    ProtectedTargetPolicy protectedTargetPolicy;
}
```

若目标受保护，按 policy 执行 fallback 或 BLOCKED；不得静默直接击杀。

---

## 14.2 STATUS

状态不再用一个 Operation 对应一个硬编码类；统一使用：

```java
final class ApplyStatusEffect implements EffectSpec {
    StableId effectId;
    StatusRef status;
    ValueSpec intensity;
    DurationSpec duration;
    StatusStackingPolicy stacking;
}

enum StatusStackingPolicy {
    SPD_NATIVE,
    REPLACE,
    EXTEND,
    KEEP_LONGER
}
```

`StatusRef` 必须来自受控白名单，至少：

```text
POISON BURNING BLEEDING SLOW HASTE PARALYSIS ROOTS AMOK TERROR VULNERABLE
```

白名单项必须声明：

- 允许目标类型；
- intensity 是否使用；
- duration 是否使用；
- 可否复制；
- 可否清除；
- save/load adapter；
- immunity check。

禁止任意 Java Buff class name 作为玩家 Schema。

---

## 14.3 MOVEMENT

```java
final class PushEffect implements EffectSpec {
    StableId effectId;
    int distance;
    CollisionPolicy collisionPolicy;
}

final class PullEffect implements EffectSpec {
    StableId effectId;
    int distance;
    CollisionPolicy collisionPolicy;
}

final class ThrowEffect implements EffectSpec {
    StableId effectId;
    int distance;
    CollisionPolicy collisionPolicy;
}

final class DashEffect implements EffectSpec {
    StableId effectId;
    int maximumDistance;
    PathPolicy pathPolicy;
}

final class TeleportEffect implements EffectSpec {
    StableId effectId;
    int maximumRange;
    DestinationPolicy destinationPolicy;
}

final class SwapPositionEffect implements EffectSpec {
    StableId effectId;
    SwapLegalityPolicy legalityPolicy;
}
```

```java
enum CollisionPolicy {
    STOP_BEFORE_BLOCKED,
    SPD_NATIVE_COLLISION
}

enum PathPolicy {
    REQUIRE_CLEAR_PATH,
    ALLOW_PASSABLE_PATH
}

enum DestinationPolicy {
    EXACT_CELL_OR_FAIL,
    NEAREST_VALID_CELL
}
```

默认必须是可预测、可格式化的 policy。所有移动 Effect 必须先通过 `WorldCapabilityValidator`/合法位置检查；失败返回 BLOCKED，不得传送到随机 unrelated cell。

---

## 14.4 RECOVERY_DEFENSE

```java
final class HealEffect implements EffectSpec {
    StableId effectId;
    ValueSpec amount;
}

final class BarrierEffect implements EffectSpec {
    StableId effectId;
    ValueSpec amount;
    BarrierOverflowPolicy overflowPolicy;
}

final class TemporaryHpEffect implements EffectSpec {
    StableId effectId;
    ValueSpec amount;
    DurationSpec duration;
    TemporaryHpStackingPolicy stacking;
}

final class MitigateEffect implements EffectSpec {
    StableId effectId;
    int percent;              // 1..100
    DurationSpec duration;
    MitigationStackingPolicy stacking;
}

final class RedirectDamageEffect implements EffectSpec {
    StableId effectId;
    int percent;              // 1..100
    SubjectSelector recipient;
    DurationSpec duration;
}

final class CleanseEffect implements EffectSpec {
    StableId effectId;
    StatusFilterSpec filter;
    int maximumCount;
}
```

规则：

- Heal 只恢复真实 HP；
- Barrier 与 Temporary HP 是不同 state；
- Temporary HP 到期时不得扣真实 HP；
- Mitigation 与 Redirect 的顺序必须固定为：`Mitigation → Redirect → Barrier/TempHP/HP`，除非 SPD 原生机制要求另一顺序，届时必须写成单一全局策略并测试；
- Revive DEFERRED，不得用 Heal 冒充。

---

## 14.5 RESOURCE_OPERATION

唯一 Variant 容器：

```java
final class ResourceOperationEffect implements EffectSpec {
    StableId effectId;
    ResourceOperationSpec operation;
}
```

所有参数与语义见第 6 节。Skill-level 与 Class-level 不得各自复制一套 Convert。

---

## 14.6 MARK_ACCUMULATION

```java
final class AddMarkEffect implements EffectSpec {
    StableId effectId;
    MarkRef mark;
    int amount;
    OptionalInt durationOverride;
}

final class SetMarkEffect implements EffectSpec {
    StableId effectId;
    MarkRef mark;
    int value;
    OptionalInt durationOverride;
}

final class ConsumeMarkEffect implements EffectSpec {
    StableId effectId;
    MarkRef mark;
    int amount;
}

final class RemoveMarkEffect implements EffectSpec {
    StableId effectId;
    MarkRef mark;
}
```

Mark/Stack/Charge/Counter 是 declaration kind，不再作为互相重复的 effect variant。

---

## 14.7 CREATE_ENTITY

```java
final class CreateEntityEffect implements EffectSpec {
    StableId effectId;
    EntitySpecRef entity;
    int count;
    SpawnPlacementSpec placement;
    EntityCreationPolicy creationPolicy;
}
```

执行：

1. resolve blueprint；
2. 验证 EntityType/placement；
3. 验证 Ownership；
4. 验证 Capacity；
5. 原子创建每个 instance；
6. 若 capacity 或 cell 不足，按 explicit creation policy 处理；默认 `ALL_OR_NOTHING`；
7. instance 保存 blueprint ID 与 owner/source provenance。

```java
enum EntityCreationPolicy {
    ALL_OR_NOTHING,
    CREATE_AS_MANY_AS_POSSIBLE
}
```

Builder v0.2 默认 `ALL_OR_NOTHING`。

---

## 14.8 WORLD_TERRAIN

```java
final class CreateTerrainEffect implements EffectSpec {
    StableId effectId;
    TerrainKind terrain;
}

final class DestroyTerrainEffect implements EffectSpec {
    StableId effectId;
    WorldCapabilityRef requiredCapability;
}

final class CreateHazardEffect implements EffectSpec {
    StableId effectId;
    HazardRef hazard;
    int intensity;
    DurationSpec duration;
}

final class ClearHazardEffect implements EffectSpec {
    StableId effectId;
    HazardFilterSpec filter;
}
```

至少映射：

```text
WATER
GRASS
TOXIC_GAS
FIRE
CLEAR_HAZARD
DESTROY_TERRAIN
```

所有作用范围由技能或 Payload 的 `TargetingSpec.coverage` 决定；World Effect 本身不得再保存 `radius`，避免与 Targeting 重复塑形。

Trap 应使用 CreateEntity，不再同时作为 World Effect 维护另一套 Trap enum。

---

## 14.9 RELATION_CONTROL

```java
final class AssignOwnershipEffect implements EffectSpec {
    StableId effectId;
    SubjectSelector entitySubject;
    OwnerSelector newOwner;
}

final class BreakOwnershipEffect implements EffectSpec {
    StableId effectId;
    SubjectSelector entitySubject;
}

final class CreateLinkEffect implements EffectSpec {
    StableId effectId;
    LinkSpec link;
}

final class BreakLinkEffect implements EffectSpec {
    StableId effectId;
    LinkFilterSpec filter;
}

final class CommandEntityEffect implements EffectSpec {
    StableId effectId;
    EntityFilterSpec entityFilter;
    CommandSpec command;
}
```

```java
interface CommandSpec {}
FollowOwnerCommand
AttackTargetCommand { SubjectSelector target; }
GuardCellCommand    { CellSelectorRef cell; }
```

`InheritCapability` 只有在 capability whitelist 完成后才可实现：

```java
final class InheritCapabilityEffect implements EffectSpec {
    CapabilityRef capability;
    SubjectSelector source;
    SubjectSelector destination;
    DurationSpec duration;
}
```

否则 state = UNSUPPORTED，Builder 不暴露。

---

## 14.10 TRANSFER_COPY

Transfer/Copy 每个对象类型必须使用独立 Variant，不允许 `Object payload` 或 arbitrary Buff clone。

### 14.10.1 Resource Transfer

```java
final class TransferResourceEffect implements EffectSpec {
    StableId effectId;
    ResourceHolderSelector sourceHolder;
    ResourceRef sourceResource;
    ResourceHolderSelector destinationHolder;
    ResourceRef destinationResource;
    int amount;
    TransferAtomicity atomicity; // EXACT_ATOMIC required
    ResourceOverflowPolicy destinationOverflowPolicy;
}
```

两个 holder 必须拥有对应 ResourceStorage capability。Hero 默认拥有 build resources。若任何一方不支持，Dependency/Capability validation 返回 HARD_CONFLICT 或 Runtime BLOCKED。

### 14.10.2 Status Copy

```java
final class CopyStatusEffect implements EffectSpec {
    StableId effectId;
    SubjectSelector source;
    SubjectSelector destination;
    StatusRef status;
    StatusCopyPolicy copyPolicy;
}

enum StatusCopyPolicy {
    COPY_REMAINING_DURATION,
    COPY_CONFIGURED_DURATION
}
```

禁止“复制找到的第一个状态”。Status 必须明确且在 whitelist 中 `copyable=true`。

### 14.10.3 Mark Transfer

```java
final class TransferMarkEffect implements EffectSpec {
    StableId effectId;
    SubjectSelector source;
    SubjectSelector destination;
    MarkRef mark;
    int amount;
    TransferAtomicity atomicity;
}
```

默认 EXACT_ATOMIC：source 不足则不变化。

### 14.10.4 Barrier Swap

```java
final class SwapBarrierEffect implements EffectSpec {
    StableId effectId;
    SubjectSelector first;
    SubjectSelector second;
    BarrierSwapPolicy policy;
}
```

只交换 Barrier，不交换 Temporary HP 或真实 HP。目标缺失时 BLOCKED。

### 14.10.5 Snapshot / Ability / Property extensions

以下同属 Transfer/Copy，但详细规范见第 30 节：

- CaptureSnapshotEffect；
- RestoreSnapshotEffect；
- CaptureAbilityEffect；
- GrantLearnedAbilityEffect；
- ExtractPropertyEffect；
- TransferPropertyEffect。

任意对象图、任意 Java class、任意 method capture 禁止。

---

## 14.11 TRANSFORM

### 14.11.1 Mode Shift — REQUIRED

使用第 9.3 节 `ModeShiftEffect`。

### 14.11.2 Capability Override — CONDITIONAL

```java
final class CapabilityOverrideEffect implements EffectSpec {
    StableId effectId;
    SubjectSelector subject;
    CapabilityRef capability;
    CapabilityOverrideValue value;
    DurationSpec duration;
}
```

只有当：

- Capability 在有限 whitelist；
- 有 typed value；
- Runtime executor；
- save/load；
- rollback；
- Builder；
- Budget；
- adversarial test

全部完成时才可 `IMPLEMENTED`。否则 UNSUPPORTED。

### 14.11.3 Behavior Override — DEFERRED

在通用 BehaviorSpec、状态迁移、恢复和 AI 安全测试完成前：

```text
TRANSFORM_BEHAVIOR = DEFERRED / NOT PLAYER EXPOSED
```

### 14.11.4 Decompose / Synthesize / Imbue — NEW GENERIC EXTENSION

炼成相关操作归入 Transform，但不得内嵌职业语义：

```java
DecomposeEffect { TargetMaterialSelector target; DecomposeRuleRef rule; }
SynthesizeEffect { SynthesisRecipeRef recipe; OutputPlacementPolicy output; }
ImbueEffect { ItemOrEntitySelector target; PropertyRef property; int amount; }
```

底层 Schema 见第 30 节；完整内容库 Deferred。

---

## 15. EffectChainSpec 与 Payload 复用

### 15.1 EffectChainSpec

```java
final class EffectChainSpec {
    StableId chainId;
    EffectSpec primary;
    Optional<SecondaryEffectSpec> secondary;
}

final class SecondaryEffectSpec {
    EffectSpec effect;
    SecondaryActivationSpec activation;
}

interface SecondaryActivationSpec {}
ImmediateOnPrimarySuccess
DelayAfterPrimarySuccess { int turns; TargetBindingPolicy targetBinding; }
OnNextActionAfterPrimarySuccess {
    ActionEventRef actionEvent;
    int charges;
    int expiryTurns;
}
```

限制：

- Secondary 最多 1 个；
- Primary 返回 APPLIED 才激活 Secondary；
- Primary 的每个成功目标分别执行/挂载 Secondary；
- Immediate Secondary 默认绑定同一 actor identity；
- Delay/NextAction 必须保存 origin skill ID、effect ID、event/cause ID；
- Secondary 自身不得再包含 Secondary，防止无限嵌套；
- 未来复杂 combo 使用多个 Skill/Trigger，不扩大单链深度。

### 15.2 PayloadSpec

```java
final class PayloadSpec {
    StableId payloadId;
    TargetingSpec targeting;
    EffectChainSpec effects;
}
```

以下必须直接保存/引用 `PayloadSpec` 或 `EffectChainSpec`：

- Skill immediate execution；
- Device payload；
- Trap payload；
- Field payload；
- Carrier payload；
- Delayed payload；
- Echo payload；
- Action attachment；
- Actor basic action payload。

禁止：

```text
DeviceBehavior enum: FIRE/POISON/HEAL
TrapPayload enum
FieldEffect enum
DelayedEffectLegacy-only
```

### 15.3 原子链语义

- Delay 整个 EffectChain 作为一个 ScheduledPayload 保存；
- 禁止 Primary/Secondary 分成两个平级 scheduled actors；
- Scheduled payload 恢复后只执行一次；
- Primary 失败时 Secondary 不执行；
- Save 恰好发生在 Primary 成功、Attachment 尚未消费之间时，Attachment state 必须保存；
- EffectChain execution trace 记录 chainId、primary result、secondary activation/result。

---

## 16. DeliverySpec

```java
interface DeliverySpec {
    StableId nodeId();
}

SelfDelivery
ContactDelivery
DirectDelivery {
    boolean requiresLineOfSight;
}
ProjectileDelivery {
    ProjectilePathPolicy pathPolicy;
    int speedClass;
    String presentationKey;
}
TraceDelivery {
    int width;
    boolean stopsAtFirstBlockingCell;
}
GroundDelivery {
    boolean requiresVisibleCell;
}
PersistentCarrierDelivery {
    EntitySpecRef carrierBlueprint;
}
ActionAttachmentDelivery {
    ActionEventRef actionEvent;
    int charges;
    int expiryTurns;
}
```

规则：

- Delivery 参数属于 DeliverySpec，不放进 EffectSpec；
- Range、area、filter 属于 TargetingSpec；
- PersistentCarrier 创建/绑定一个 `CARRIER` EntitySpec，并把 Skill Payload 作为 carrier payload；
- ActionAttachmentDelivery 挂载整个 Skill EffectChain；
- 第 15.1 的 Secondary `OnNextAction...` 用于“Primary 立即发生、Secondary 挂下一次行动”；
- 每个 Delivery 必须有 compatibility validator 与行为测试；
- 禁止 `implemented() { return true; }` 作为证据。

---

## 17. TargetingSpec

### 17.1 总结构

```java
final class TargetingSpec {
    StableId nodeId;
    SelectorSpec selector;
    CoverageSpec coverage;
    EntityFilterExpr filter;
    int range;
    int maximumTargets;
    LineOfSightPolicy lineOfSight;
    TargetOrdering ordering;
}
```

### 17.2 SelectorSpec

```java
interface SelectorSpec {}
SelfSelector
EventSourceSelector
EventTargetSelector
SelectedActorSelector
SelectedCellSelector
NearestActorSelector
RandomActorSelector
AllMatchingActorsSelector
OwnedEntitySelector { EntityFilterExpr filter; }
```

### 17.3 CoverageSpec

```java
interface CoverageSpec {}
SingleCoverage
AdjacentCoverage
RadiusCoverage { int radius; }
LineCoverage   { int length; int width; }
ConeCoverage   { int length; int angleClass; }
RingCoverage   { int innerRadius; int outerRadius; }
ChainCoverage  { int jumps; int jumpRange; boolean repeatTargets; }
```

几何只由 CoverageSpec 决定。`AREA` Modifier 必须删除并迁移到 Coverage。

### 17.4 Filter

```java
interface EntityFilterExpr {}

AllOfEntityFilter { List<EntityFilterExpr> children; }
AnyOfEntityFilter { List<EntityFilterExpr> children; }
NotEntityFilter   { EntityFilterExpr child; }

AnyActorFilter
RelationFilter       { RelationAlignment relationToClassOwner; boolean includeSelf; }
SelfFilter
OwnedByFilter        { OwnerSelector owner; }
EntityTypeFilter     { Set<EntityType> types; }
EntityBlueprintFilter{ EntitySpecRef blueprint; }
HasMarkFilter        { MarkRef mark; ComparisonOperator op; int value; }
HasStatusFilter      { StatusRef status; }
HpPercentFilter      { ComparisonOperator op; int percent; }
HasCapabilityFilter  { CapabilityRef capability; }
```

`ALLY` 与 `ALLY_OR_SELF` 必须通过 `includeSelf` 明确区分。

### 17.5 解析与顺序

- 先解析 selector center/seed；
- 再生成 coverage cells/actors；
- 再应用 filter；
- 再按 ordering 排序；
- 最后截断 maximumTargets；
- 排序必须确定性：默认 distance → cell → runtime actor ID；
- Random 使用 Rule Runtime seed，Trace 记录抽样；
- `MARKED` 不得匹配“任意 Mark”；
- `HAS_STATUS` 不得匹配“任意负面 Buff”，除非 explicit filter；
- compatibility error 在 Builder 显示 HARD_CONFLICT，不得等 Runtime no-op。

---

## 18. ModifierSpec

v0.2 每个 Skill 最多 1 个 Modifier。

```java
interface ModifierSpec {
    StableId nodeId();
}

RepeatModifier {
    int repeatCount; // additional executions, 1..5
    RepeatTargetPolicy targetPolicy;
}
IntensityModifier {
    int numerator;
    int denominator;
}
ExtendDurationModifier {
    int additionalTurns;
}
PierceModifier {
    int additionalTargets;
}
BounceModifier {
    int bounces;
    int bounceRange;
}
DelayModifier {
    int turns;
    TargetBindingPolicy targetBinding;
}
EchoModifier {
    int initialDelayTurns;
    int repeats;
    int intervalTurns;
}
```

规则：

- `AREA` 不存在；
- Intensity 只修改声明兼容的 numeric fields；不允许 generic reflection 扫描所有 int；
- Repeat 重复整个 EffectChain；Cost 默认只支付一次，必须在 formatter 明示；
- ExtendDuration 只作用于有 DurationSpec 的 effect；
- Pierce 只兼容 Projectile/Trace；
- Bounce 只兼容 actor-targeted Direct/Projectile；
- Delay/Echo 包装整个 EffectChain；
- wrapper 执行顺序固定：

```text
Resolve Targeting
→ Commit Cost
→ Delivery
→ Delay/Echo scheduling (if any)
→ Repeat/Bounce/Pierce expansion
→ Intensity/Duration transformation
→ EffectChain
```

如某组合无定义，Builder 标记 HARD_CONFLICT，不能自动忽略 Modifier。

---

## 19. CostSpec

### 19.1 Typed variants

```java
interface CostSpec {
    StableId nodeId();
}

NoCost
ResourceCost {
    ResourceHolderSelector holder;
    ResourceRef resource;
    int amount;
}
HpCost {
    int amount;
    HpLethalPolicy lethalPolicy;
    int minimumRemainingHp;
}
ActionTimeCost {
    int turns;
}
CooldownCost {
    int turns;
}
ItemCost {
    ItemFilterSpec itemFilter;
    int count;
}
MarkCost {
    SubjectSelector subject;
    MarkRef mark;
    int amount;
}
EntityCost {
    EntityFilterExpr entityFilter;
    int count;
    EntityConsumptionPolicy consumptionPolicy;
}
AbilityChargeCost {
    AbilityPoolRef pool;
    int charges;
}
PropertyCost {
    PropertyRef property;
    int amount;
}
```

### 19.2 HP Cost

```java
enum HpLethalPolicy {
    REJECT_IF_WOULD_KILL,
    ALLOW_LETHAL
}
```

Builder 默认：

```text
REJECT_IF_WOULD_KILL
minimumRemainingHp = 1
```

Barrier 与 Temporary HP 不支付 HpCost。`ALLOW_LETHAL` 在有清晰死亡/取消/奖励规则前不得暴露。

### 19.3 支付事务

Skill/Operation 执行顺序：

1. resolve dependencies；
2. preflight targeting；
3. preflight cost；
4. 原子支付 Cost；
5. commit action/time/cooldown；
6. 执行 Delivery/Effect。

规则：

- 无合法目标时不支付；
- dependency unresolved 时不支付；
- Resource/Mark/Item/Entity Cost 不足时不支付任何子成本；
- 支付后目标因免疫导致 Effect BLOCKED，默认不退款；
- 若未来支持 CompositeCost，必须同一事务；v0.2 Skill 仍最多一个 Cost；
- ItemCost 不能保存 Java class name，必须用稳定 ItemFilter/Property；
- State Cost 必须使用 MarkRef 或 ModeRef 的明确 Variant，不得 generic string。


---

## 20. Constraint、Tradeoff 与 Persistence/Timing

## 20.1 Condition、Skill Constraint、Class Constraint 的边界

三者必须严格分离：

| 类型 | 含义 | 是否产生 Budget credit |
|---|---|---:|
| Condition | 当前是否满足使用条件 | 否 |
| SkillConstraint | 永久限制该 Skill 的使用方式/次数/位置 | 可降低该 Skill 价格，但必须有执法与测试 |
| ClassConstraint / Vow | 持续限制整个职业的行动、装备、恢复或位置自由 | 可返还 Class Budget，但必须计算 Effective Constraint Value |

禁止把普通 Condition 包装成 Constraint 以获得返还。

### 20.2 SkillConstraintSpec

```java
interface SkillConstraintSpec {
    StableId constraintId();
}

LimitedUsesPerFloorConstraint {
    int maximumUses;
}
RequiresStationaryTurnsConstraint {
    int turns;
}
OnlyAfterEventConstraint {
    RuleEventType event;
    int windowTurns;
}
SkillCooldownFloorConstraint {
    int minimumCooldownTurns;
}
ExclusiveModeSkillConstraint {
    ModeRef requiredMode;
}
```

`TargetMarked`、`SelfLowHp`、`SelfInWater` 属于 Condition，不属于返还型 Constraint。

旧 `HP_COMMITMENT` 在没有独立 commitment state/enforcement 前必须删除返还；HP Cost 自己已是 Cost。

### 20.3 ClassConstraintSpec / Vow

```java
interface ClassConstraintSpec {
    StableId constraintId();
    DisplayName displayName();
    ConstraintEnforcementMode enforcementMode();
}

enum ConstraintEnforcementMode {
    HARD_BLOCK
    // BREAKABLE_COMMITMENT is Deferred
}

NoBasicAttackConstraint
NoMovementConstraint
EmptyEquipmentSlotConstraint { EquipmentSlotRef slot; }
NoHealingConstraint { HealingSourceFilter sources; }
WeaponCategoryForbiddenConstraint { ItemFilterSpec weaponFilter; }
StandStillConstraint { int minimumTurnsBeforePower; int resetPolicy; }
MaximumSkillUsesPerFloorConstraint { int maximumUses; }
NoItemUseConstraint { ItemFilterSpec filter; }
```

### 20.4 执法要求

每个 ClassConstraint 必须声明：

```java
final class ConstraintEnforcementDescriptor {
    Set<EnforcementHook> hooks;
    ConstraintViolationPolicy violationPolicy;
    String budgetPriceKey;
}
```

至少包含对应 Runtime Hook：

```text
BASIC_ATTACK_VALIDATE
MOVE_VALIDATE
EQUIPMENT_VALIDATE
HEAL_VALIDATE
ITEM_USE_VALIDATE
SKILL_USE_VALIDATE
FLOOR_RESET
SAVE_LOAD
```

约束必须在 action commit 前阻止；仅在 Formatter 显示负面文字不算实现。

### 20.5 Effective Constraint Value

Budget credit 计算必须考虑：

1. **已由其他组件造成的冗余**：Basic Attack 已设 NONE 时，NoBasicAttack 不再返还；
2. **旁路**：No Healing 必须覆盖 Potion、Skill、Item、Buff、ClassOperation 等定义范围；
3. **有效时间**：只在极小窗口生效的限制不能拿完整 credit；
4. **可主动规避性**：玩家能零成本关闭限制时，credit 为 0；
5. **相互包含**：No Movement 已包含更弱的位置限制时，不能双重返还；
6. **Runtime 证据**：没有 adversarial enforcement test，credit 必须为 0。

```java
final class ConstraintCreditQuote {
    int rawCredit;
    int redundancyReduction;
    int bypassReduction;
    int effectiveCredit;
    List<String> reasons;
}
```

### 20.6 旧 Constraint 迁移

- `WAIT_CLEARS_RESOURCE` → `ResourceFlowComponent`，不是 Vow；
- `TARGET_MARKED` → `MarkCompareCondition`；
- `SELF_LOW_HP` → `BuiltinStatCompareCondition`；
- `SELF_IN_WATER` → `TerrainCondition`；
- `COOLDOWN` → `CooldownCost` 或 SkillConstraint，按旧语义迁移；
- 无执法 `HP_COMMITMENT` → Legacy diagnostic，credit=0；
- `LIMITED_USE` → `LimitedUsesPerFloorConstraint`。

---

## 21. Duration、Persistence、Delay、Echo 与 Snapshot

### 21.1 DurationSpec

```java
final class DurationSpec {
    DurationKind kind;
    int turns; // used for TURN_BASED
}

enum DurationKind {
    INSTANT,
    TURN_BASED,
    UNTIL_LEVEL_END,
    UNTIL_REMOVED
}
```

每个 Effect/Mark/Mode/Entity 只接受兼容的 DurationKind。

### 21.2 PersistenceSpec

```java
final class PersistenceSpec {
    DurationSpec lifetime;
    OwnerDeathPolicy ownerDeathPolicy;
    LevelTransitionPolicy levelTransitionPolicy;
    SourceDeathPolicy sourceDeathPolicy;
    ManualRemovalPolicy manualRemovalPolicy;
}

enum OwnerDeathPolicy {
    REMOVE,
    PERSIST
}

enum LevelTransitionPolicy {
    REMOVE,
    FOLLOW_OWNER,      // only compatible entity types
    SAVE_ON_CURRENT_LEVEL
}

enum SourceDeathPolicy {
    KEEP,
    REMOVE
}
```

非法组合必须 HARD_CONFLICT，例如普通 Trap 使用 FOLLOW_OWNER。

### 21.3 TargetBindingPolicy

所有 Delay/Echo/Attachment 必须显式选择目标绑定：

```java
enum TargetBindingPolicy {
    ACTOR_ID_LIVE,
    CELL_SNAPSHOT,
    SELECTOR_REEVALUATE
}
```

语义：

- `ACTOR_ID_LIVE`：保存 actor ID；到期时 actor 不存在/死亡则 BLOCKED；
- `CELL_SNAPSHOT`：保存 cell；不追随 actor；
- `SELECTOR_REEVALUATE`：到期时按保存的 TargetingSpec 重新求值；
- 禁止默认猜测。

### 21.4 ScheduledPayloadState

```java
final class ScheduledPayloadState {
    long payloadInstanceId;
    StableId originSkillId;
    StableId originChainId;
    PayloadSpec payload;
    int remainingTurns;
    TargetBindingPolicy targetBinding;
    Optional<RuntimeActorId> actorId;
    int cell;
    long originatingEventId;
    long originatingCauseId;
    int executionCount;
}
```

Save/Load 必须保留 remainingTurns、binding、payload、origin 与 executionCount。恢复后不得重复注册或提前触发。

### 21.5 Echo

Echo 是对整个 EffectChain 的定时副本：

- 每次 echo 有独立 payloadInstanceId；
- 保留 cause chain；
- 默认不复制已支付 Cost；
- echo 触发的事件不得重入原 Skill，除非明确允许且通过 guard；
- repeats/interval 必须保存；
- Primary/Secondary 仍保持同一个 chain。

### 21.6 SnapshotSpec

时序能力只能保存白名单状态：

```java
final class SnapshotSpec {
    StableId snapshotSpecId;
    SnapshotSubjectSelector subject;
    Set<SnapshotField> fields;
    Set<StatusRef> statuses;   // fields 包含 WHITELISTED_STATUS 时必填
    Set<ResourceRef> resources;// fields 包含 RESOURCE_VALUES 时必填
    Set<MarkRef> marks;        // fields 包含 MARK_VALUES 时必填
    Set<ModeRef> modes;        // fields 包含 ACTIVE_MODES 时必填
    int lifetimeTurns;
}

enum SnapshotField {
    HP_CURRENT,
    BARRIER,
    TEMPORARY_HP,
    POSITION,
    WHITELISTED_STATUS,
    RESOURCE_VALUES,
    MARK_VALUES,
    ACTIVE_MODES
}
```

```java
final class SnapshotState {
    StableId snapshotInstanceId;
    SnapshotSpec spec;
    RuntimeActorId subjectActorId;
    int capturedTurn;
    OptionalInt hp;
    OptionalInt barrier;
    OptionalInt temporaryHp;
    OptionalInt cell;
    Map<StatusRef, StatusSnapshotValue> statuses;
    Map<ResourceRef, Integer> resources;
    Map<MarkRef, MarkSnapshotValue> marks;
    Set<ModeRef> modes;
}
```

### 21.7 Restore policy

```java
final class RestoreSnapshotEffect implements EffectSpec {
    StableId effectId;
    SnapshotSelector snapshot;
    RestoreSnapshotPolicy policy;
}

final class RestoreSnapshotPolicy {
    boolean allowRevive;                 // MUST be false in v0.2
    InvalidPositionPolicy positionPolicy;
    ResourceOverflowPolicy resourcePolicy;
    StatusRestorePolicy statusPolicy;
}
```

规则：

- 不复制 Inventory、Item object graph、Actor class、AI object、Java Buff instances；
- `allowRevive=false` 时死亡目标不能恢复；
- 非法位置默认 `FAIL_POSITION_ONLY` 或整个 restore fail，必须由 policy 明确；
- HP 恢复 clamp 1..HP_MAX；
- Resource/Mark/Mode 按 Ref 恢复，缺声明则 diagnostic，禁止绑定同名；
- snapshot state 本身必须 Bundle roundtrip。

---

## 22. ClassGameplayComponentSpec

### 22.1 Typed union

```java
interface ClassGameplayComponentSpec {
    StableId componentId();
    DisplayName displayName();
    ComponentVariantKey variantKey();
}
```

Required variants：

```java
BasicAttackComponent
ResourceFlowComponent
ActiveResourceOperationComponent
OwnershipPolicyComponent
EntityCapacityComponent
EntityPersistenceComponent
CommandCapabilityComponent
RecycleCapabilityComponent
ModeEngineComponent
ClassOperationGrantComponent
ClassRuleComponent
```

不得继续使用一个包含所有字段的 `ClassGameplayComponentSpec` mega-object。

### 22.2 BasicAttackComponent

```java
final class BasicAttackComponent implements ClassGameplayComponentSpec {
    StableId componentId;
    BasicAttackAvailability availability;
    int damageNumerator;
    int damageDenominator;
    ItemFilterSpec allowedWeapons;
    int actionTimeTurns;
}

enum BasicAttackAvailability {
    FULL,
    WEAK,
    NONE
}
```

NoBasicAttack Vow 与 availability=NONE 重复时不得双重 credit。

### 22.3 ResourceFlowComponent

```java
final class ResourceFlowComponent implements ClassGameplayComponentSpec {
    StableId componentId;
    TriggerSpec trigger;
    ConditionExpr condition;
    ResourceOperationSpec operation;
}
```

所有 Gain/Loss/Clear/Convert 共享第 6 节 executor。

### 22.4 ActiveResourceOperationComponent

```java
final class ActiveResourceOperationComponent implements ClassGameplayComponentSpec {
    StableId componentId;
    DisplayName operationDisplayName;
    ResourceOperationSpec operation;
    CostSpec cost;
    int actionTimeTurns;
}
```

Builder 添加该 Component 时创建并持久化对应 `ClassOperationSpec`；不得每次 load 由名称重算 ID。

### 22.5 Ownership / Capacity / Persistence

```java
final class OwnershipPolicyComponent implements ClassGameplayComponentSpec {
    StableId componentId;
    OwnershipSpec policy;
}

final class EntityCapacityComponent implements ClassGameplayComponentSpec {
    StableId componentId;
    CapacityRef capacity;
}

final class EntityPersistenceComponent implements ClassGameplayComponentSpec {
    StableId componentId;
    EntityFilterExpr filter;
    PersistenceSpec override;
}
```

EntitySpec 自身 persistence 是默认；Class component 可以对 filter 明确覆盖。冲突按优先级：具体 EntitySpec override > Class filter override > global default，并由 Formatter 显示最终值。

### 22.6 ModeEngineComponent

```java
final class ModeEngineComponent implements ClassGameplayComponentSpec {
    StableId componentId;
    List<ModeGroupRef> groups;
    boolean grantSwitchOperation;
}
```

Mode declaration 本身不藏在 component 的字符串列表中。

---

## 23. ClassOperationSpec

```java
final class ClassOperationSpec {
    StableId operationId;
    DisplayName displayName;
    ClassOperationVariant variant;
    Optional<ComponentRef> sourceComponent;
    TriggerSpec activation; // Active required in v0.2
    ConditionExpr condition;
    CostSpec cost;
    int actionTimeTurns;
    ClassOperationPayload payload;
    HudPresentationSpec hud;
}
```

```java
interface ClassOperationPayload {}
ResourceOperationPayload { ResourceOperationSpec operation; }
CommandOperationPayload  { EntityFilterExpr entityFilter; CommandSpec command; }
ModeSwitchOperationPayload { ModeGroupRef group; ModeSelectionPolicy selection; }
RecycleOperationPayload {
    EntityFilterExpr filter;
    int count;
    ResourceOperationSpec reward;
}
GenericPayloadOperation { PayloadSpec payload; } // only if explicitly granted
```

规则：

- Operation ID 创建一次后持久化；
- ID 不得由 `reload_<resourceId>`、显示名或 list index 每次生成；
- rename Resource/Mode/Component 不改 operation ID；
- 删除 source component 后 operation 保留但 sourceComponent UNRESOLVED；
- Builder 必须提供显式“删除依赖 Operation”或“重新绑定”操作，禁止静默删除；
- HUD 顺序与 visibility 保存；
- cooldown/action state 在 RuntimeState，不在 Operation Spec。

---

## 24. SkillSpec v0.2

```java
final class SkillSpec {
    StableId skillId;
    DisplayName displayName;
    TriggerSpec activation;
    ConditionExpr condition;
    EffectChainSpec effects;
    DeliverySpec delivery;
    TargetingSpec targeting;
    Optional<ModifierSpec> modifier;
    Optional<CostSpec> cost;
    Optional<SkillConstraintSpec> constraint;
    SkillPresentationSpec presentation;
}
```

### 24.1 Structural validation

必须检查：

- 所有 node ID 唯一；
- Primary 必须存在；
- Secondary ≤1；
- 每个 Ref 的 dependency state；
- Trigger 与 Delivery compatibility；
- Delivery 与 Targeting compatibility；
- Effect 与 Targeting subject/cell compatibility；
- Modifier compatibility；
- Cost preflight capability；
- Constraint enforcement support；
- 所有数值范围；
- Budget quote 存在；
- Formatter 可生成完整文本。

禁止自动把 Radius targeting 改成 AREA modifier，或反向改写玩家字段。

### 24.2 Runtime compilation

`SkillSpec` 编译为 immutable `CompiledSkillRule`：

```java
final class CompiledSkillRule {
    StableId skillId;
    CompiledTrigger trigger;
    CompiledCondition condition;
    CompiledDelivery delivery;
    CompiledTargeting targeting;
    CompiledEffectChain effects;
    CompiledModifier modifier;
    CompiledCost cost;
    CompiledSkillConstraint constraint;
}
```

编译不得修改 SkillSpec。Compiled object 可以缓存，但 cache key 必须包括 canonical spec hash 与 schema/price/runtime version。

---

## 25. Stable References、Rename、Delete 与 UNRESOLVED

### 25.1 Rename

对于 Resource、Mark、Mode、ModeGroup、Entity、Capacity、Skill、Component、Operation、AbilityPool、Property、Recipe、Constraint：

```text
rename(oldId, newDisplayName)
```

必须：

- 保持 oldId；
- 更新 declaration displayName；
- 更新 Ref 的 lastKnownDisplayName 可以延迟，但 targetId 不变；
- 不触发 Runtime recompile semantic change；
- Budget 总额不变；
- Save/load 后保持；
- Formatter 使用新名称。

### 25.2 Delete

```text
delete(declarationId)
```

必须：

- 只移除 declaration；
- 不级联删除依赖节点；
- 依赖节点保留原 targetId；
- resolver 返回 UNRESOLVED；
- Formatter 显示：`未解析：灼痕 [mark_xxx]`；
- draft 仍可保存/加载；
- finalization 阻止应用到 Hero；
- Runtime 已开始的现有 build 不接受热删除；编辑发生在独立 draft；
- 新建同名 declaration 获得新 ID，不自动解析旧 Ref。

### 25.3 Explicit rebind

只有玩家执行明确命令才可改变 targetId：

```java
RebindReferenceCommand {
    StableId ownerNodeId;
    String fieldPath;
    StableId newTargetId;
}
```

UI 必须显示旧目标、缺失 ID、新目标，并记录到 undo/redo。

### 25.4 Finalization

Draft 可以含 UNRESOLVED/UNSUPPORTED。应用/开局必须满足：

- 无 UNRESOLVED；
- 无 HARD_CONFLICT；
- 无 PLAYER_EXPOSED 但 Runtime unsupported 节点；
- Budget ≤ limit；
- 至少一个合法 gameplay action/basic attack path，除非 Vow/特殊规则明确允许；
- 所有 static localization keys 完整。

---

## 26. Save / Load / Migration

### 26.1 Declaration 与 Runtime State Bundle

必须分开保存：

```text
Hero bundle
├─ class_build_spec_v6
└─ class_runtime_state_v6
```

`ClassRuntimeState` 至少包含：

```java
final class ClassRuntimeState {
    StableId buildId;
    Map<ResourceRef, ResourceState> resources;
    List<ModeState> modes;
    List<EntityInstanceState> entities;
    List<ScheduledPayloadState> scheduledPayloads;
    List<ActionAttachmentState> attachments;
    Map<StableId, Integer> cooldowns;
    Map<StableId, Integer> usesThisFloor;
    List<SnapshotState> snapshots;
    List<LearnedAbilityState> learnedAbilities;
    List<PropertyInventoryState> properties;
    long nextRuntimeEntityId;
    long nextPayloadInstanceId;
    long nextEventId;
}
```

MarkState 如果继续以 Buff 附着于 Actor 保存，必须仍能通过 markId 与 ClassBuildSpec 解析；不能仅保存 enum ordinal/name。

### 26.2 Canonical semantic serialization

QA 必须提供 canonical serializer，覆盖每一个字段：

- 所有 declaration 与 Ref；
- Primary/Secondary 与 activation；
- Delivery/Targeting/Modifier/Cost/Constraint 全参数；
- Entity blueprint/body/trigger/payload/capacity/persistence；
- Budget metadata；
- Runtime Resource/Mode/Mark/Entity/Delay/Attachment/Snapshot/Ability/Property state。

禁止使用遗漏字段的手写摘要作为 roundtrip oracle。

### 26.3 Load 规则

Load 必须：

1. 读取 schemaVersion；
2. 若 v6，原样恢复；
3. 运行 resolver 与 validator，但不得修改 spec；
4. 恢复 Runtime State；
5. 对不存在的 declaration 产生 diagnostic；
6. 不得调用任何 `resolvePendingBindings()`；
7. 不得选择第一个 Resource/Mode/Mark；
8. unknown variant 保留为 `UnsupportedNodeSpec` 或拒绝加载并给出明确错误，禁止映射到 Standard Damage。

### 26.4 v5→v6 migration

迁移必须是独立、可测试、可重放的过程，并输出 MigrationReport。

#### Resource

- 保留非空旧 ID；
- 空/重复 ID 生成确定性新 ID；
- name → displayName；
- minimum/maximum/initial 转入 declaration；
- current 转入 ResourceState；
- 引用按旧 ID 映射。

#### Skill Resource Convert

旧 Skill-level Convert 的真实语义是 1:1：

```text
sourceAmount = old.power
targetAmount = old.power
conversionPolicy = EXACT_ATOMIC
```

MigrationReport 必须注明旧实现曾允许 partial；迁移后的行为变得严格时，应提供兼容模式或明确一次性告警。不得伪造 2→5。

#### Class Resource Convert

- `amount` → sourceAmount；
- `targetAmount` → targetAmount；
- target overflow 默认设置为 FAIL；
- 若旧存档依赖截断行为，MigrationReport 标记 `BEHAVIOR_TIGHTENED`。

#### Mark

- 为旧 build 实际使用的每个 `RuleMark.Type` 创建一个 v6 MarkSpec；
- 使用确定性 ID；
- 本地化名称只作为 displayName；
- 所有 Effect/Condition/Cost/Filter/Runtime Buff 转为 MarkRef；
- invalid legacy string → UNRESOLVED，不回退 HUNTED。

#### Mode

- 每个旧 raw mode string 创建 ModeSpec；
- displayName 为原字符串；
- 生成一个默认 EXCLUSIVE ModeGroup；
- Trait/Condition/Effect 全部更新为 ModeRef。

#### Entity payload

旧 carrier payload 按**实际旧 Runtime 行为**迁移：

- `heal` → `HealEffect`；
- `poison` → `ApplyStatusEffect(POISON)`；
- `fire` 若旧 executor 实际为 Standard Damage，则迁移为 DirectDamage，并在 report 中标记显示名/行为不一致；不得静默改成 Burning；
- 其他字符串 → UNSUPPORTED。

#### AREA

旧 AREA modifier 迁移为 Targeting Coverage radius。若已有非 Single coverage 造成冲突，标记 HARD_CONFLICT，不能叠加猜测。

#### Consumable

已知旧 Java class 映射到稳定 `ItemFilterSpec`；未知 class → UNSUPPORTED。

#### Auto-bound data

迁移不得再次补绑。旧字段为空就形成 UNRESOLVED，并在 Migration UI 提供候选，但必须由玩家确认。

### 26.5 MigrationReport

```java
final class MigrationReport {
    int sourceSchema;
    int targetSchema;
    List<MigrationChange> changes;
    List<DependencyDiagnostic> unresolved;
    List<MigrationWarning> warnings;
    boolean behaviorPreserved;
}
```

---

## 27. Budget Contract

### 27.1 单一权威 Budget Catalog

所有 Builder、Runtime validation、Headless、Fuzz、Report 必须使用同一个版本化 Catalog：

```java
final class BudgetCatalog {
    String priceVersion; // e.g. "gameplay-v0.2.0"
    Map<BudgetPriceKey, BudgetRule> rules;
}
```

禁止 Player Builder、Stress fixtures、Legacy 和 QA 各自维护公式。

### 27.2 Budget Ledger

```java
final class BudgetLedger {
    String priceVersion;
    int baseBudget;
    List<BudgetLedgerEntry> entries;
    int totalCost;
    int totalCredit;
    int remaining;
}

final class BudgetLedgerEntry {
    StableId nodeId;
    BudgetPriceKey priceKey;
    int baseCost;
    int parameterCost;
    int compatibilityAdjustment;
    int constraintCredit;
    int finalAmount;
    List<String> explanationKeys;
}
```

### 27.3 预算不变量

- 每个 Player-exposed Variant 必须有 price key；
- 无 price key = UNSUPPORTED，不可暴露；
- Rename 不改变预算；
- 列表顺序不改变预算；
- Save/load 不改变 Ledger；
- 同一语义只有一份成本，例如 Resource Convert 不因出现在 Skill 或 Device 而使用不同核心参数价格；载体/触发/持续另计；
- 参数成本读取 typed fields，不读取通用 `power`；
- Constraint credit 只来自第 20 节有效性计算；
- 未执法或未通过 adversarial test 的 Constraint credit=0；
- 预算报价必须可解释到每个 node ID；
- priceVersion 变化必须显式重算并向玩家显示差异。

### 27.4 数值平衡边界

本 Contract 冻结 Budget 的结构、权威来源、roundtrip 与 anti-exploit 规则，不凭静态审查重新发明完整平衡数字。

首次 `gameplay-v0.2.0` Catalog 必须：

1. 对与旧语义等价的 Variant，以当前有效成本为迁移起点；
2. 对新 Variant 给出显式、reviewable 数据项；
3. 任何未定价新 Variant 保持 UNSUPPORTED；
4. Catalog 作为独立数据文件纳入版本控制；
5. 由 15 Archetype recipes 与 Fuzz 做经济压力测试。

这不是允许留空：实施提交必须同时提交完整 Catalog；只是 Contract 不把静态代码审计冒充最终数值平衡。

---

## 28. Formatter 与 Localization Contract

### 28.1 Typed formatter

每个 Variant 必须有：

```java
interface PlayerFacingFormatter<T> {
    String shortSummary(T spec, FormatContext context);
    String detailedDescription(T spec, FormatContext context);
    List<InlineDiagnostic> diagnostics(T spec, FormatContext context);
}
```

禁止 giant switch 最后回退到：

```text
power X / duration Y / range Z
```

来掩盖缺失字段。

### 28.2 动态名称

Resource、Mark、Mode、Entity、Skill 等 displayName：

- 直接作为玩家文本插入；
- 不拼接成 message key；
- 不翻译；
- unresolved 时显示 lastKnownDisplayName + short ID；
- 同名对象在选择器中附短 ID 或类型区分。

### 28.3 静态本地化

所有静态：

- Family；
- Variant；
- 字段标签；
- enum option；
- validation error；
- unresolved/unsupported 文案；
- Budget explanation；
- Builder button

至少必须有英文与简体中文。

### 28.4 文本完整性 Gate

Mandatory test 必须遍历：

- 每个 Player-exposed Variant；
- 每个字段类型；
- resolved/unresolved/unsupported；
- min/default/max 示例；
- 自定义中文/英文/符号名称；
- 所有 Builder page 与 Build Sheet。

任何玩家可见输出含以下内容即失败：

```text
NO TEXT FOUND
�
null
unknown key
未替换的 %s / {0}
Java class name
raw enum name（除 debug 模式）
```

---

## 29. Player Builder Contract

### 29.1 单一 Builder State 与 Reducer

```java
final class BuilderState {
    ClassBuildSpec draft;
    DependencyReport dependencies;
    ValidationReport validation;
    BudgetLedger budget;
    BuilderNavigationState navigation;
    UndoRedoState history;
}

interface BuilderCommand {}

final class BuilderReducer {
    BuilderState apply(BuilderState state, BuilderCommand command);
}
```

UI、Headless Player Assembler、回放测试都必须调用该 Reducer。

### 29.2 Required Builder commands

```text
CreateResource
EditResourceField
CreateMark
EditMarkField
CreateModeGroup
CreateMode
CreateEntityCapacity
CreateEntity
CreateClassComponent
CreateSkill
SelectTriggerVariant
SelectEffectFamily
SelectEffectVariant
SetFieldValue
SetReference
SetTargetingSelector
SetTargetingCoverage
SetTargetingFilter
SetDelivery
SetModifier
SetCost
SetSkillConstraint
CreateClassConstraint
RenameDeclaration
DeleteDeclaration
RebindReference
SaveDraft
LoadDraft
FinalizeBuild
Undo
Redo
```

命令使用 primitive/typed field input，不接受最终 `EffectSpec` 或 `SkillSpec` 参数。

错误命令：

```java
AddSkillCommand(SkillSpec alreadyConstructed)
```

正确命令序列：

```text
CreateSkill
SelectEffectFamily(RESOURCE_OPERATION)
SelectEffectVariant(CONVERT)
SetReference(primary.operation.source, RageId)
SetNumber(primary.operation.sourceAmount, 2)
SetReference(primary.operation.target, FocusId)
SetNumber(primary.operation.targetAmount, 5)
```

### 29.3 FormSchema

每个 typed Variant 提供字段描述：

```java
interface FormFieldSchema {}
TextFieldSchema
NumberFieldSchema { int min; int max; int step; }
EnumFieldSchema
ReferenceFieldSchema { RefKind expectedKind; Filter allowed; }
BooleanFieldSchema
NestedVariantFieldSchema
ListFieldSchema
ReadOnlyDiagnosticFieldSchema
```

FormSchema 只描述 UI 字段，不决定 Runtime 是否完成。Runtime support 由独立 executor registry 和 acceptance evidence 决定。

### 29.4 数值 UI

Numeric Parameter 必须使用 Number Picker / Stepper：

```text
[-] 3 [+]
```

要求：

- 显示当前值；
- 单击按 step；
- 长按可加速但不能越界；
- min/max 可见；
- 不生成 1..100 的长列表；
- 数值改变是单独 BuilderCommand；
- undo/redo 生效。

### 29.5 枚举与复杂参数

- Family → Variant → Parameters；
- Enum 使用 selector/submenu；
- Ref 使用按类型分组的 picker；
- Entity Payload 进入统一 Payload editor；
- Trap editor 显示 Trigger、Targeting、Primary、Secondary，而不是 Behavior enum；
- unresolved Ref 不从选项中消失，必须保留错误卡片；
- Builder 不调用 auto-bind。

### 29.6 Player-facing Assembler

Headless assembler 只接受命令序列：

```java
PlayerBuildSession session = PlayerBuildSession.empty(idGenerator);
session.dispatch(new CreateResourceCommand(...));
...
ClassBuildSpec build = session.finalizeOrThrow();
```

可以有 internal fixture helper 直接 new object，但必须放在 `qa.runtime`，不得用于 Player-path completion。

---

## 30. Runtime Compiler 与执行 Contract

### 30.1 Pipeline

```text
ClassBuildSpec
→ DependencyResolver
→ StructuralValidator
→ BudgetValidator
→ RuntimeCapabilityValidator
→ CompilePlan
→ CompiledClassRules
→ RuleRuntime
```

任一阶段不得修改输入 Spec。

### 30.2 CompilePlan

```java
final class ClassCompilePlan {
    StableId buildId;
    List<CompiledClassComponent> classComponents;
    List<CompiledSkillRule> skills;
    List<CompiledClassOperation> operations;
    Map<StableId, CompiledEntityBlueprint> entities;
    List<CompileDiagnostic> diagnostics;
}
```

只有 RESOLVED 且 IMPLEMENTED 的节点进入 executable plan。Finalized build 若有其他节点必须拒绝启动；Draft preview 可以生成 partial plan，但 UI 必须明确。

### 30.3 Executor registry

```java
interface EffectExecutor<T extends EffectSpec> {
    EffectResult execute(T spec, RuntimeExecutionContext context);
    PreflightResult preflight(T spec, RuntimeExecutionContext context);
}
```

每个 `EffectVariantKey` 精确映射一个 executor。注册表必须独立于 Builder FormSchema。

注册完成不等于 IMPLEMENTED；还需 acceptance evidence ID。

### 30.4 Resource transaction

```java
final class ResourceTransaction {
    List<ResourceDelta> deltas;
    TransactionResult preflight();
    TransactionResult commit();
}
```

- preflight 不改变 state；
- commit 要么全部成功，要么全部失败；
- Resource Convert/Transfer 必须使用；
- trace 有 transaction ID；
- Save 不得发生在半提交状态。

### 30.5 Entity Runtime

- Entity instance 保存 blueprint ref；
- Entity payload 通过统一 EffectExecutor；
- periodic tick 使用 Rule Runtime scheduler；
- Trap enter event 使用事件上下文；
- ownership、capacity、persistence 统一 validator；
- Device/Field 不能直接 switch payload string；
- Entity target resolution 使用 PayloadSpec Targeting；
- Relation/owner/source provenance 写入 trace 与 save。

### 30.6 Fail closed

遇到：

- unresolved ref；
- unsupported variant；
- missing executor；
- invalid target type；
- unknown status/capability；
- illegal cell；
- missing resource holder

必须返回 BLOCKED/UNSUPPORTED + diagnostic。禁止执行相近默认效果。

### 30.7 Existing Runtime assets to preserve

实施应保留并适配：

- RuleHooks 小型接入面；
- RuleContext/Trace/causality；
- deterministic priority/runtime order；
- recursion guard；
- RuleDelayedPayload 保存基础；
- DirectableAlly/ownership command；
- Class Action HUD；
- Resource HUD；
- Headless 真实 Actor/Level/Buff 环境；
- WorldCapabilityValidator 与 SPD 原生效果 helper。

---

## 31. 新增 5 个 Archetype 所需通用 Primitive

## 31.1 Observation / Ability Capture / Learned Ability

### 31.1.1 禁止捕获任意代码

悟道者只能捕获敌人显式暴露的 declarative `AbilityDescriptor`，禁止：

- 反射复制 method；
- 保存 Java class；
- 序列化任意 lambda；
- 直接复制敌人 AI；
- 根据敌人名称分支。

### 31.1.2 AbilityDescriptor

```java
final class AbilityDescriptor {
    AbilitySignature signature;
    DisplayName displayName;
    TriggerCategory observedTrigger;
    PayloadSpec normalizedPayload;
    Set<AbilityTag> capabilities;
    AbilityCapturePolicy capturePolicy;
}

final class AbilitySignature {
    String stableProviderId;
    String abilityKey;
    int descriptorVersion;
}
```

只有实现了 descriptor adapter 的敌技可被观察；其他能力返回 UNSUPPORTED，不得猜测。

### 31.1.3 ObservationSpec

```java
final class ObservationSpec {
    ObservationMode mode;
    AbilityFilterSpec abilityFilter;
    int requiredObservations;
    boolean requireDamageReceived;
    boolean requireSurvival;
    int memoryWindowTurns;
}

enum ObservationMode {
    SEE_EXECUTION,
    BE_TARGETED,
    RECEIVE_EFFECT
}
```

### 31.1.4 LearnedAbilityPool

```java
final class AbilityPoolSpec {
    StableId id;
    DisplayName displayName;
    int capacity;
    AbilityOverflowPolicy overflowPolicy;
}

final class LearnedAbilityState {
    StableId learnedAbilityId;
    AbilityPoolRef pool;
    AbilitySignature signature;
    AbilityProvenance provenance;
    PayloadSpec capturedPayload;
    int charges;
}

final class AbilityProvenance {
    String providerTypeId;
    String sourceDisplayNameSnapshot;
    int dungeonDepth;
    long learnedTurn;
    int observations;
}
```

Ability capacity 是通用 capacity；完整敌技适配库 Deferred。

## 31.2 Property / Ingredient / Decompose / Synthesis

### 31.2.1 PropertySpec

```java
final class PropertySpec {
    StableId id;
    DisplayName displayName;
    PropertyValueKind valueKind;
    int maximumStack;
}

enum PropertyValueKind {
    MATERIAL,
    ELEMENT,
    BEHAVIOR_TRAIT,
    EFFECT_TRAIT
}
```

### 31.2.2 Property inventory

```java
final class PropertyInventoryState {
    PropertyRef property;
    int amount;
    List<PropertyProvenance> provenance;
}
```

Property 不是普通 Resource 的别名：它带有来源/性质语义，可作为配方输入、Item/Entity imbue 目标。

### 31.2.3 Decompose

```java
final class DecomposeRuleSpec {
    StableId id;
    ItemEntityTerrainFilter inputFilter;
    List<PropertyYieldSpec> yields;
    ConsumptionPolicy consumption;
}
```

### 31.2.4 Synthesis

```java
final class SynthesisRecipeSpec {
    StableId id;
    DisplayName displayName;
    List<PropertyCost> inputs;
    SynthesisOutputSpec output;
}
```

Output 可为：

- Resource operation；
- Item property/imbue；
- Entity blueprint；
- Payload/temporary ability；
- World effect。

完整配方和物质内容库 Deferred；底层事务、来源、save/load 必须真实。

## 31.3 Temporal

已有 Delay/Echo 按第 21 节重做。新增：

- SnapshotSpec；
- CaptureSnapshotEffect；
- RestoreSnapshotEffect；
- target binding；
- Snapshot runtime state；
- Temporal displacement 由 position snapshot/restore 组合；
- 风险资源用普通 ResourceSpec，不新增 TemporalResource Domain。

## 31.4 Death Residue / Corpse

### 31.4.1 Death event

`RuleHooks` 必须暴露统一 `DEATH` event，Context 至少含：

- dead actor；
- killer/source；
- death cell；
- actor descriptor/filter data；
- cause event ID。

### 31.4.2 Corpse Entity

死亡残留使用 `EntitySpec(type=CORPSE)`：

- 是真实 targetable/consumable entity 或 level object；
- 保存 source provenance；
- 有 persistence/lifetime；
- 可携带 Property；
- 可被 EntityFilter 选中；
- 可作为 EntityCost；
- 不等同于 Summon Actor。

### 31.4.3 Corpse operations

通过现有通用 Primitive 组合：

```text
Corpse → EntityCost → Damage/Explosion
Corpse → EntityCost → ResourceOperation
Corpse → Decompose → Property
Corpse → EntityCost + CreateEntity → Revive-like summon
```

真正复活原 actor identity/AI/装备 DEFERRED；v0.2 可以消费 Corpse 创建一个声明的 Actor blueprint。

## 31.5 Vow / Self Restriction

使用第 20 节 ClassConstraint/Vow。不新增“Ascetic Domain”。每个 Vow 必须：

- 全局执法；
- 可存档；
- 有明确阻止反馈；
- 有旁路测试；
- 有 EffectiveConstraintValue；
- 无执法时 credit=0。

---

## 32. Capability 安全边界

### 32.1 CapabilityRef

```java
final class CapabilityRef {
    String stableKey;
    int version;
}
```

Capability 必须来自有限 catalog，包含：

- applicable subject types；
- typed value schema；
- grant/revoke/override executor；
- save/load；
- formatter；
- budget；
- security policy。

### 32.2 禁止复制对象

Transfer/Copy/Transform 禁止：

- arbitrary Buff；
- arbitrary Actor field；
- Java object graph；
- class name + reflection；
- code pointer；
- AI state；
- inventory object without ItemSpec migration；
- private/internal combat state not declared in whitelist。

### 32.3 Unsupported visibility

Schema 中存在但 Runtime 未实现的 Capability/Behavior Variant：

- 可以被 migration 读取；
- Formatter 显示 UNSUPPORTED；
- Builder 创建菜单不显示；
- Budget 不报价；
- Coverage 不计为 IMPLEMENTED。

---

## 33. MANDATORY ADVERSARIAL PLAYER-PATH ACCEPTANCE SUITE

### 33.1 总体规则

每个测试必须依次覆盖：

```text
Player Builder Commands
→ Builder State / typed Schema
→ Dependency / Validation
→ Formatter / Localization
→ Budget Ledger
→ Save Draft / Load Draft
→ Finalize
→ Headless real SPD Runtime
→ Mid-runtime Save / Load（适用时）
→ Behavior assertions
```

禁止：

- 直接 `new ClassBuildSpec`；
- 直接 `new SkillSpec`；
- 直接 `new EffectSpec`；
- 使用 preset object copy 冒充 Builder；
- 只检查不 crash；
- 只比较枚举/fingerprint；
- 只检查 Registry count；
- 用 hardcoded Archetype tag 激活行为。

每个测试必须保存完整 BuilderCommand trace，失败时可重放。

### 33.2 Test 1 — Resource Convert 2→5

#### Builder 路径

1. Create Resource：displayName=`Rage`，min=0，max=10，initial=5；
2. Create Resource：displayName=`Focus`，min=0，max=10，initial=0；
3. Create Skill；
4. Activation=Active；
5. Effect Family=Resource Operation；
6. Variant=Convert；
7. source=RageRef；sourceAmount=2；
8. target=FocusRef；targetAmount=5；
9. targetOverflow=FAIL；
10. Targeting=Self；无 Cost。

#### Schema 断言

- Convert 有两个独立 Ref、两个独立 Amount；
- 没有 generic `power` 代表两个数量；
- canonical serialization 保存 2 与 5。

#### Runtime 断言

- 初始 Rage=5/Focus=0；执行后 Rage=3/Focus=5；
- 再执行后 Rage=1/Focus=10；
- Rage=1 时 preflight BLOCKED，两个值不变；
- Focus=8 时 +5 空间不足，BLOCKED，两个值不变；
- transaction trace 精确记录 -2/+5；
- save/load 后比例不变；
- Device Payload 使用同一 Convert executor 时结果一致。

### 33.3 Test 2 — 自定义 Mark“灼痕”

#### Builder 路径

1. Create Mark：displayName=`灼痕`，kind=STACK，max=10，duration=8 turns；
2. Skill A：命中目标 → AddMark(灼痕, 2)；
3. Skill B：Condition MarkCompare(target, 灼痕, GTE, 3) → Damage；
4. Skill C：Active / selected target → ConsumeMark(灼痕, 3) → Effect；

#### Runtime 断言

- A 一次后 stack=2；B 不触发；
- A 两次后按 refresh policy 得到 stack=4；B 可触发；
- C 消耗精确 3，剩 1；
- source/provenance 与 duration 保存；
- 不出现 HUNTED/CHARGED fallback。

#### Rename/Delete

- rename `灼痕`→`灼烧记号`：A/B/C targetId 全不变，Formatter 更新名称；
- delete Mark declaration：A/B/C 全部 UNRESOLVED；draft 可保存/加载；finalize 失败；Runtime 不 crash；
- 新建另一个同名“灼烧记号”：旧 Ref 仍 UNRESOLVED；
- 显式 Rebind 后才解析；
- Budget rename 前后相同。

### 33.4 Test 3 — Trap：Enter → Push → Poison

#### Builder 路径

1. Create Entity type=TRAP；
2. Trigger=`ActorEnterTile`，filter=Enemy relative to owner；
3. Payload Targeting=`EventTarget`；
4. Primary=`PushEffect(distance=2)`；
5. Secondary=`ApplyStatusEffect(POISON)`，activation=ImmediateOnPrimarySuccess；
6. oneShot=true；
7. Create Skill：Selected Cell → CreateEntity(trapRef)。

#### Runtime 断言

- Ally/self entering does not trigger；
- enemy entering triggers once；
- same entering actor is pushed 2 or to exact legal stopping cell；
- Poison applied to that same actor after movement；
- trap removed after oneShot；
- save before entry/load 后仍可触发一次；
- 无 Trap-specific payload enum；canonical payload 是统一 EffectChain。

### 33.5 Test 4 — Device：每 3 回合 Radius 2 Shield Allies

#### Builder 路径

1. Create Device Entity；
2. trigger=Periodic(initialDelay=3, period=3)；
3. Payload selector=self entity cell，coverage=Radius(2)；
4. filter=Relation(ALLY, includeSelf=false)；
5. Primary=Barrier(amount=4)；
6. lifetime=9 turns；
7. Capacity=2；
8. Skill creates device at selected cell。

#### Runtime 断言

- turn 1/2 不触发；turn 3 触发；
- radius≤2 的 ally +4 Barrier；
- owner 若 filter excludeSelf 不获得；enemy 不获得；
- turn 6/9 再触发；到期移除；
- save/load 后 remaining period 正确；
- Builder 未出现 fire/poison/heal 专用菜单。

### 33.6 Test 5 — Device 周期增加自定义 Resource

1. Create Resource `Charge`；
2. Device trigger every 2 turns；
3. Payload target=ClassOwner；
4. Primary=`GainResource(Charge,+2,CLAMP)`；
5. 创建并运行。

断言：

- 周期精确；
- 只改绑定的 Charge；
- rename Charge 后仍执行；
- delete Charge 后 Entity Payload UNRESOLVED，draft finalize 失败；
- 证明 Entity Payload 复用 Resource Effect，而非固定 Damage/Poison/Heal。

### 33.7 Test 6 — Delay arbitrary EffectChain

1. Skill Targeting=SelectedCell；
2. Primary 选一个非固定专用效果，例如 CreateHazard(FIRE) 或 AddMark；
3. 可选 Secondary；
4. Modifier=Delay(turns=3,targetBinding=CELL_SNAPSHOT)。

断言：

- cast turn 不执行；
- 3 turns later 精确执行一次；
- 整个 Primary+Secondary 保存在一个 ScheduledPayload；
- turn 1 save/load 后 remaining=2；
- load 后不重复、不丢失；
- cell snapshot 不追随原目标；
- 将 Primary 替换为任意 Player-exposed supported Effect 时无需新增 Delay enum。

### 33.8 Test 7 — Action Attachment / Counter

目标构筑：

```text
受到攻击
→ Primary: Mitigate 30% for current/next damage window
→ Secondary: 下一次 ATTACK_HIT 附加 Poison（1 charge）
```

Builder：

- Trigger=DAMAGED；
- Primary=Mitigate；
- Secondary=ApplyStatus(POISON)；
- Secondary activation=`OnNextActionAfterPrimarySuccess(ATTACK_HIT, charges=1, expiry=10)`。

断言：

- 受击触发 Mitigate；
- Attachment state 创建并保存；
- 非攻击行动不消耗；
- 下一次命中对命中目标施加 Poison；
- 精确消耗 1 charge；
- save/load 在 attachment 等待期间保持；
- 原 Skill 不递归重入；
- Formatter 明确“立即减伤；下一次攻击附加中毒”。

### 33.9 Test 8 — Transfer / Copy / Swap

必须有四个独立 player-path subtests：

#### A. Resource Transfer

- Hero 与一个具有 ResourceStorageCapability 的 owned device；
- Transfer 2 `Charge` from device to hero；
- source 不足/target full 时 EXACT_ATOMIC，无部分变化。

#### B. Status Copy

- source 有明确 POISON；
- CopyStatus(POISON) 到 destination；
- source 的其他状态不得被复制；
- 不允许“第一个状态”。

#### C. Mark Transfer

- 自定义“灼痕”从 A 转 2 到 B；
- source 精确减 2，destination 加 2；
- rename/save-load 保持；
- delete 后 unresolved。

#### D. Barrier Swap

- A barrier=3，B barrier=8；执行后 A=8、B=3；
- HP/TempHP 不变；
- 缺一方目标时无变化。

非法 capability 或任意 Buff copy 必须在 Builder/validator 被拒绝。

### 33.10 Test 9 — 自定义 Mode

1. Create ModeGroup `姿态`；
2. Create Modes `进攻`、`防守`；
3. ClassOperation Mode Switch；
4. Skill A：ModeShift(防守)；
5. Skill B：Condition ModeActive(防守) → Barrier；
6. Skill C：Condition ModeActive(进攻) → Damage。

断言：

- 同组互斥；
- 切换后 B/C 行为正确；
- rename `防守`→`壁垒`，所有 Ref 不变；
- Formatter 不查 `mode_壁垒` message key；
- delete Mode 后相关 Skill/Operation UNRESOLVED；
- save/load active mode state 保持。

### 33.11 Test 10 — Stable References 全类型

参数化覆盖：

```text
Resource
Mark
Mode
Entity
Capacity
AbilityPool
Property
```

每种执行：

1. create declaration；
2. create at least two dependent nodes；
3. rename；
4. save/load；
5. delete；
6. save/load unresolved；
7. create same-name replacement；
8. verify no auto-bind；
9. explicit rebind；
10. verify resolved。

任何 crash、silent delete、first-item bind、default enum fallback 都失败。

### 33.12 Test 11 — Full Save / Load roundtrip

构筑必须同时含：

- 2 Resources；
- custom Mark；
- 2 Modes；
- Trap 与 Device；
- Delay；
- Action Attachment；
- ClassOperation；
- Constraint；
- runtime Resource values；
- active Mark/Mode；
- live Entity；
- scheduled payload；
- pending attachment；
- cooldown/uses；
- Budget metadata。

断言：

- canonical ClassBuild serialization 完全相同；
- canonical RuntimeState serialization 完全相同；
- next IDs 不回退；
- load 不调用 auto-bind；
- 后续行为与未保存控制组一致。

### 33.13 Test 12 — Budget roundtrip / constraint enforcement

断言：

- 每个 node 有 ledger entry；
- total 可由 entries 精确重算；
- rename/order/save-load 不变；
- 2→5 Convert 的两个 amount 都进入价格解释；
- Device periodic/radius/payload/lifetime 分项可解释；
- 添加 NoBasicAttack Vow 时，实际 Basic Attack 被 block 才有 credit；
- BasicAttack 已 NONE 时再加同一 Vow credit=0；
- 无 enforcement test 的 Constraint credit=0；
- 删除 dependency 后 ledger 标记 unresolved，不能静默移除成本；
- priceVersion 改变时产生 diff report。

### 33.14 Mandatory localization sweep

该测试与 12 项同等级 mandatory：

- 遍历所有 Player-exposed Variant 与字段；
- 英文/简中；
- resolved/unresolved/unsupported；
- 中文自定义名称；
- assert 无 `NO TEXT FOUND`、`�`、raw enum、Java class、placeholder。

---

## 34. 15 Archetype Player-path Recipe Suite

这 15 个 Recipe 必须全部从 BuilderCommand 构造。它们不要求每个新高层内容库完整，但必须验证所依赖的通用 Primitive。

| # | Recipe 最低验收机制 | v0.2 完成要求 |
|---:|---|---|
| 1 Martial Defender | ON_DAMAGED、Mitigate、Barrier、Push/Counter Attachment | 必须真实运行 |
| 2 Blood Berserker | HpCost、Low HP Condition、Missing HP Scaling、TempHP | 必须真实运行；无 HP Resource |
| 3 Ammo Gunner | custom Ammo、Reload Operation、Projectile、Pierce、ResourceCost | 必须真实运行 |
| 4 Area Caster | Resource、Ground/Projectile、Radius/Line、Damage/Status | 必须真实运行；无 AREA 重复 |
| 5 Assassin/Hunt | custom Mark、threshold、consume、Execute、Dash | 必须真实运行 |
| 6 Owned Summoner | Actor Entity、Ownership、Capacity、Persistence、Command | 基础 actor/command 必须真实；成长内容 Deferred |
| 7 Engineer | Trap、periodic Device、Resource Device、Recycle | 必须真实运行 |
| 8 Controller/Terrain | Push/Pull、Root/Slow、Water/Fire/Gas/Clear | 必须真实运行并通过 capability validation |
| 9 Support/Defense | Heal、Barrier、Cleanse、Mitigate、ally filter/link | required variants 真实运行 |
| 10 Transform/Bio | custom Mode + Mode condition | Mode 必须真实；Capability/Behavior 未完成则明确 UNSUPPORTED，不可计完成 |
| 11 悟道者 | Observation event、declarative AbilityDescriptor、capture into pool、capacity/provenance | 至少一个真实敌技 descriptor 端到端；完整内容库 Deferred |
| 12 炼成师 | Decompose 一个 item/entity/world property、Synthesize 一个 output、provenance/save | 至少一个通用端到端 Recipe；完整库 Deferred |
| 13 时序术士 | Delay、Echo、Snapshot、Restore whitelist、save/load | 至少一个 snapshot/restore 端到端 |
| 14 尸骸利用者 | Death→Corpse Entity、Corpse target/cost、Corpse→Effect/Resource | 至少一个真实 corpse chain；跨局遗产 Deferred |
| 15 苦行者 | 至少两类 Class Vow、真实 block、Effective credit、旁路测试 | 必须真实执行；负面文案不算 |

如果某 Recipe 依赖标记为 DEFERRED 的高层内容，它可以以最小 declarative fixture 验证 Primitive；不得用职业标签替代。

---

## 35. QA 架构与报告规则

### 35.1 QA 分层

```text
Layer A — Schema unit tests
Layer B — BuilderCommand / Reducer tests
Layer C — Dependency / migration / formatter / budget tests
Layer D — Headless real Runtime behavior tests
Layer E — Save/load equivalence tests
Layer F — Fuzz / integrity / exploit tests
Layer G — Manual player UX smoke
```

只有 A-E 均有证据，某 Variant 才可能 IMPLEMENTED。F/G 是额外要求，不替代 A-E。

### 35.2 Completion Matrix row

```java
final class ComponentCompletionRow {
    String variantKey;
    ImplementationState state;
    String schemaTestId;
    String builderPathTestId;
    String dependencyTestId;
    String formatterTestId;
    String budgetTestId;
    String saveLoadTestId;
    String runtimeBehaviorTestId;
    String adversarialTestId;
}
```

任何证据 ID 缺失：state 不能是 IMPLEMENTED。

### 35.3 禁止的 QA 替代品

以下均不能单独设置 `runtimeVerified=true`：

- `implemented()` 返回 true；
- enum/family match；
- class 存在；
- Registry exposed；
- Bundle 不抛异常；
- price > 0；
- 直接对象 fixture 跑一次；
- Fuzzer 0 crash；
- 报告中出现关键词；
- 15 个 recipe 名称存在。

### 35.4 Fuzz 的正确用途

新 Fuzzer 应生成 `BuilderCommand` 序列，而不是 final object。可附加 direct schema fuzz，但报告分开：

```text
player_command_valid_density
finalized_build_density
runtime_failure_count
unresolved_draft_count
budget_exploit_count
causal_loop_count
save_roundtrip_failure_count
```

Fuzz 不负责证明某设计样本可表达；Adversarial recipes 负责。

---

## 36. Definition of Done

一个 Gameplay Component / Variant 只有同时满足以下条件才算 `IMPLEMENTED`：

1. **Typed Schema**：没有借用无关 generic field；
2. **Stable Identity**：所有声明/引用遵守 ID 规则；
3. **Player Builder**：真实字段路径可从空白构造；
4. **Validation**：范围、兼容性、依赖明确；
5. **UNRESOLVED**：rename/delete/save-load 行为正确；
6. **Formatter**：英/简中完整，无错误占位；
7. **Budget**：有唯一 price key 与 ledger；
8. **Save/Load**：canonical deep roundtrip；
9. **Runtime**：真实 SPD Actor/Level/Buff 行为断言；
10. **Failure semantics**：BLOCKED/UNSUPPORTED/NO_TARGET 可区分；
11. **Player-path adversarial test**：不直接 new final object；
12. **No profession tag**：实现只依赖通用组件；
13. **Migration**：旧数据可映射或明确 unsupported；
14. **No silent fallback**：无默认 Mark/Resource/Mode/Effect；
15. **Documentation**：参数、目标、时序与限制与实现一致。

`coverage count` 只能统计已经达到以上条件的行。

---

## 37. 实施顺序（Normative staging）

### Stage 0 — Freeze legacy evidence

- 保留当前 Runtime smoke/fuzz；
- 标记所有现有 completion 报告为 `LEGACY_EVIDENCE_NOT_PLAYER_PATH`；
- 禁止继续向旧 EffectSpec 增加新 public field/enum；
- 建 v6 feature branch。

### Stage 1 — Identity / Dependency / Save core

- Stable IDs；
- Resource/Mark/Mode/Entity declarations；
- typed refs；
- remove auto-binding；
- ClassBuild v6；
- canonical serializer；
- v5 migration skeleton；
- Test 10/11 的声明部分。

### Stage 2 — Typed Skill core + Resource

- Trigger/Condition/Effect/Delivery/Targeting/Modifier/Cost typed union；
- ResourceOperation transaction；
- HP BuiltinStat；
- BuilderCommand + NumberStepper；
- 完成 Test 1。

### Stage 3 — Mark / Mode

- dynamic RuleMark；
- ModeSpec/Ref；
- all reference points；
- 完成 Test 2、9、10。

### Stage 4 — Unified Payload / Entity / Timing

- EffectChain/Payload；
- EntitySpec/Instance；
- Trap/Device/Field/Carrier；
- Delay/Echo/Attachment chain；
- 完成 Test 3-7。

### Stage 5 — Transfer / Copy / Constraint / Budget

- Transfer matrix；
- Class Vow enforcement；
- BudgetLedger；
- 完成 Test 8、12。

### Stage 6 — Formatter / Localization / Migration

- typed formatter；
- en/zh sweep；
- v5→v6 full mapping；
- remove old Builder path；
- 完成 localization gate 与 full roundtrip。

### Stage 7 — New generic primitives

- Death/Corpse；
- Snapshot/Restore；
- Observation/Ability Pool；
- Property/Ingredient/Synthesis；
- 以最小 fixtures 完成 Archetype 11-14。

### Stage 8 — Final acceptance

- 12 mandatory tests；
- localization sweep；
- 15 recipe suite；
- Player-command Fuzz；
- real desktop/android build；
- manual Builder smoke；
- 生成新的 completion matrix。

---

## 38. 推荐代码保留与迁移边界

### MUST KEEP / ADAPT

- RuleHooks production call sites；
- RuleContext / RuleTrace / cause tracking；
- deterministic RuleRuntime ordering；
- recursion guard；
- real SPD effect helpers；
- WorldCapabilityValidator；
- RuleDelayedPayload actor/save concept；
- DirectableAlly/RuleOwnership command foundation；
- Class Action HUD；
- Resource HUD；
- Headless real game harness；
- Fuzz/Integrity auxiliary infrastructure。

### MUST REPLACE AS PUBLIC MODEL

- current `EffectSpec` field bag；
- current generic `RuleCondition(parameter, reference)`；
- current `RuleCost(amount, resourceId, reference)`；
- current `RuleModifier(type,magnitude)`；
- raw Mode strings；
- fixed player RuleMark enum；
- fixed carrier payload strings；
- mega `ClassGameplayComponentSpec` fields；
- PlayerBuildAssembler final-object interface；
- weak fingerprint；
- coverage booleans without evidence。

### MUST REMOVE

- `resolvePendingBindings()` and all call sites；
- invalid Mark fallback；
- first Resource/Mode fallback；
- AREA modifier after migration；
- no-op Constraint budget credit；
- Java class-name player cost schema；
- unsupported Transform variants from Player menus。

---

## Appendix A — Selector 基础类型

```java
enum SubjectSelector {
    CLASS_OWNER,
    EVENT_SOURCE,
    EVENT_TARGET,
    SELECTED_ACTOR,
    CURRENT_TARGET,
    SOURCE_ENTITY,
    OWNER_OF_SOURCE_ENTITY
}

interface ResourceHolderSelector {}
ClassOwnerResourceHolder
EventSourceResourceHolder
EventTargetResourceHolder
SelectedActorResourceHolder
SourceEntityResourceHolder
OwnedEntityResourceHolder { EntityFilterExpr filter; SelectionPolicy selection; }

interface CellSelectorRef {}
ClassOwnerCell
EventSourceCell
EventTargetCell
SelectedCell
SourceEntityCell

interface SubjectOrCellSelector {}
SubjectAsLocation { SubjectSelector subject; }
CellLocation { CellSelectorRef cell; }
```

所有 selector 必须有 compatibility validation。无法解析返回 NO_VALID_TARGET/BLOCKED，不得猜测 Hero。

## Appendix B — Reference Picker 显示规则

```text
Resource
  Rage                     [res_7e0f8240]
  Focus                    [res_91ab30f1]

Mark
  灼痕                     [mark_1ec54fb5]

Mode
  姿态 / 壁垒              [mode_2a674829]

Missing
  未解析：旧灼痕           [mark_aaaaaaaa]
```

短 ID 只在同名、debug、unresolved 或高级详情中显示。

## Appendix C — Resource Convert canonical example

```json
{
  "variant": "RESOURCE_CONVERT",
  "effect_id": "effect_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
  "operation": {
    "source_holder": { "type": "CLASS_OWNER" },
    "source": {
      "target_id": "res_11111111111111111111111111111111",
      "last_known_display_name": "Rage"
    },
    "source_amount": 2,
    "target": {
      "target_id": "res_22222222222222222222222222222222",
      "last_known_display_name": "Focus"
    },
    "target_amount": 5,
    "conversion_policy": "EXACT_ATOMIC",
    "target_overflow_policy": "FAIL"
  }
}
```

任何只保存一个 `strength/power=2` 的对象不符合本 Contract。

## Appendix D — Custom Mark canonical example

```json
{
  "id": "mark_33333333333333333333333333333333",
  "display_name": "灼痕",
  "kind": "STACK",
  "minimum": 0,
  "maximum": 10,
  "initial_value": 0,
  "duration_policy": "TURN_BASED",
  "default_duration_turns": 8,
  "refresh_policy": "KEEP_LONGER",
  "overflow_policy": "CLAMP",
  "provenance_policy": "TRACK_LAST_SOURCE"
}
```

## Appendix E — Trap canonical shape

```text
EntitySpec(TRAP)
├─ Trigger: ActorEnterTile(filter=Enemy)
├─ Payload
│  ├─ Targeting: EventTarget / Single
│  └─ EffectChain
│     ├─ Primary: Push(distance=2)
│     └─ Secondary: Poison / ImmediateOnPrimarySuccess
├─ oneShot: true
├─ ownership: ClassOwner
└─ persistence: UntilTriggered / RemoveOnLevelExit
```

## Appendix F — Evidence required in final implementation report

最终 Codex 实施报告必须返回：

1. 实际修改文件列表；
2. v5→v6 migration 文件与测试；
3. 12 mandatory test IDs 与结果；
4. 15 recipe test IDs 与结果；
5. localization sweep 结果；
6. canonical roundtrip diff=empty；
7. Budget Catalog/priceVersion；
8. 不再存在的旧路径搜索结果：
   - `resolvePendingBindings`；
   - carrier payload string enum；
   - player-facing `RuleMark.Type`；
   - `implemented(){return true;}` completion shortcut；
9. Headless、desktop、android 构建结果；
10. 明确列出仍为 DEFERRED/UNSUPPORTED 的 Variant。

报告中的数字不能替代测试证据链接。

---

# FINAL CONTRACT STATEMENT

符合本 Contract 的系统必须让玩家真正从空白 Builder 创建声明、绑定引用、编辑精确参数，并在真实 SPD Runtime 中得到同一语义。

> **Runtime Primitive 存在，不等于 Gameplay Component 完成。**  
> **Registry 暴露，不等于玩家表达能力完成。**  
> **无 crash，不等于行为正确。**  
> **Archetype 能被直接 new 出来，不等于玩家能构筑。**

在 12 项 Mandatory Adversarial Player-path Acceptance、Localization Gate、Deep Save/Load 与 15 Archetype Recipe Suite 全部通过前，不得再次宣布“全组件完成”或“coverage complete”。
