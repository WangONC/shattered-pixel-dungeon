# SPD 自塑职业：基础 Gameplay Components 总规范

> **用途**：给代码 AI / Codex 作为职业系统实现与审查的设计基线。  
> **来源**：本规范不是重新设计。它整理的是此前从 10 类常见职业原型中逐项拆解、去重后得到的通用 Gameplay Components，并纳入后续已经确认的 `Basic Attack Model`、`ClassOperation` 等补充原则。  
> **重要**：本文件描述“组件语义与边界”，不是某一轮具体开发任务，也不是固定职业模板。

---

# 0. 最高原则

## 0.1 职业不是一个固定 Core / Domain

禁止实现：

```text
GUNNER_CORE
SUMMONER_CORE
ENGINEER_CORE
BLOOD_CORE
TERRAIN_DOMAIN
SUMMONING_DOMAIN
ENGINEERING_DOMAIN
```

也禁止让玩家先选择：

```text
职业核心：
- 枪手
- 法师
- 召唤师
- 工程师
```

**玩法核心是多个通用组件组合后的涌现结果，不是一个固定槽位。**

例如“枪手”应由：

```text
有限资源
+ 主动补充
+ Projectile Delivery
+ Damage
+ Range
+ Pierce
+ Resource Cost
```

自然形成，而不是由 `GUNNER_CORE` 产生。

---

## 0.2 Gameplay Component ≠ Skill

组件分布在多个层级。

### Class-level Components

描述：

> “这个角色整体怎样运作？”

例如：

- Resource / Cost Model
- Recovery / Refill
- Basic Attack Model
- Ownership / Relation
- Entity Capacity
- Persistence
- Mode Engine
- ClassOperation 来源
- 全局 Constraint / Tradeoff

### Skill-level Components

描述：

> “某一个具体能力怎样工作？”

例如：

- Activation / Trigger
- Condition
- Primary Core Effect
- Secondary Effect
- Delivery
- Targeting
- Modifier
- Cost
- Skill-local Constraint

**不能把所有职业机制都塞进 SkillSpec。**

---

## 0.3 组件应该是可添加、可移除、可参数化的对象

错误：

```text
hasOwnership = true
actorCapacity = 3
hasCommand = true
hasRecycle = false
hasModeSystem = false
```

并把这些字段永远固定显示在所有职业页面中。

正确思路：

```text
Gameplay Components
- [Entity Relation: Ownership]
- [Entity Capacity: Actor, 3]
- [Class Operation: Command, target=Owned Actor]
```

没有选择的机制不应该在玩家界面中显示：

```text
受控创建物：未配置
行动单位上限：0
装置上限：0
指挥能力：未配置
回收能力：未配置
模式系统：未配置
```

这种 UI 是开发者状态表，不是职业构筑器。

---

## 0.4 允许自由组合，但自由必须付费

职业使用统一 Power / Class Budget。

原则：

```text
强能力 → 消耗预算
真实限制 → 释放或减少预算需求
额外适应性 / 冗余战斗路径 → 增加预算
```

不能靠固定职业身份限制自由。

例如：

```text
近战 + 强远程 + 召唤
```

可以存在，但必须比单一战斗体系更昂贵。

---

## 0.5 Effective Constraint，而不是只看名义限制

一个 Constraint 的价值取决于整个 Build。

例如：

```text
“只对 Marked Target 生效”
```

如果职业可以无条件自动给所有敌人上 Mark，那么该限制实际几乎没有限制价值。

Ammo 也是如此：

```text
Ammo + Reload
```

如果玩家仍拥有完整、免费的普通攻击，那么 Ammo 的实际限制价值明显下降。

因此预算系统应评估：

```text
Nominal Constraint
→ Build Context
→ Effective Constraint Value
```

---

# 1. 原始 10 类职业拆解来源

这些职业只是**分析样本**，不是最终固定职业。

## 1.1 Martial Defender / 战士、防御者

拆出：

```text
Melee / Contact
Basic Attack
Shield / Barrier
Mitigation
On Damaged
Block
Counter
```

核心启示：

- 普通攻击本身是一条完整战斗路径。
- 防御和反击不应写死成 Warrior Domain。
- “被攻击后触发”是 Trigger。
- Barrier / Mitigation 属于 Recovery / Defense Effect。

---

## 1.2 Blood Berserker / 血系狂战

拆出：

```text
HP Cost
Low HP Condition
Damage Taken → Resource
Recovery / Lifesteal
Missing HP Scaling
Temporary HP
```

核心启示：

- “血系”不是固定职业标签。
- HP 可以是 Cost。
- 低血量是 Condition。
- 缺失生命可以成为 Scaling Source。
- Temporary HP 与 Shield / Barrier 语义必须区分。

---

## 1.3 Ranger / Gunner / 远程枪手

拆出：

```text
Finite Resource / Ammo
Reload / Active Refill
Ranged Delivery
Range Advantage
Pierce
Burst / Repeat
```

核心启示：

- 枪手不是 `GUNNER_CORE`。
- Ammo 是 Resource。
- Reload 是 Recovery / Refill，并可能生成 ClassOperation。
- Projectile 是 Delivery。
- Pierce / Repeat 是 Modifier。
- “保留完整普通攻击”是额外适应性，不能无成本白拿。

---

## 1.4 Mage / 法师

拆出：

```text
Renewable Resource
Ranged / Area Delivery
Charge / Timing
High Cost → High Power
Status
```

核心启示：

- “魔法”不是 Effect Family。
- 火球可以是：
  `Damage + Projectile + Radius + Resource Cost`
- 控制法术可以是：
  `Status + Area + Cost`
- Charge 属于 Timing / Commitment。

---

## 1.5 Assassin / Hunt / 刺客猎杀

拆出：

```text
Mark
Conditional Burst
Mobility
Specific / Isolated Target Advantage
Finisher
Kill Reset / Kill Acceleration
```

核心启示：

- Mark / Stack / Counter 是独立 Effect Family。
- Execute / Missing HP Damage 是 Damage Variant。
- “目标必须被标记”属于 Condition / Constraint。
- 击杀刷新属于 Trigger + Cooldown/Timing 修改。

---

## 1.6 Summon / 召唤

拆出：

```text
Create Actor
Ownership
Persistence
Quantity / Quality Tradeoff
Command
Capacity
```

核心启示：

召唤体系不是一个大开关，应由：

```text
Create Entity
+ Entity Type = Actor
+ Ownership
+ Persistence
+ Capacity
+ Command Operation
```

组合形成。

---

## 1.7 Engineer / Automation / 工程自动化

拆出：

```text
Create Device / Object
Trigger
Link
Production
Recycle
Persistence
Automation
Resource
```

核心启示：

工程体系应由通用 Primitive 组合：

```text
Create Entity(Device)
+ Persistence
+ Trigger
+ Relation(Link)
+ Resource Operation
+ Recycle Operation
```

不能实现成单独 `ENGINEERING_DOMAIN`。

---

## 1.8 Controller / 控场

拆出：

```text
Push
Pull
Slow
Field
Terrain
Area Denial
Position Manipulation
```

核心启示：

- Push/Pull 属于 Movement Effect。
- Slow 属于 Status。
- Field 属于 Persistent Carrier / Timing。
- Terrain 属于 World / Terrain Effect。
- “控场”是组合结果，不是 Effect Family。

---

## 1.9 Support / Defense / 支援防御

拆出：

```text
Heal
Shield
Cleanse
Transfer
Link
Buff
Redirect
```

核心启示：

- Heal / Barrier / Temporary HP / Mitigate / Cleanse 应有清晰语义边界。
- Transfer / Copy 是独立 Family。
- Link 属于 Relation。

---

## 1.10 Transform / Bio / 变形、生体

拆出：

```text
Body / Parts
Mode / Form Shift
Inheritance
Consume
Evolve
Capability Change
```

核心启示：

- 临时或局部形态变化归 Transform。
- 永久职业级规则改写不应该滥用 Transform，应归 Rule / Law / Core Gameplay Rule。
- 生物改造最终可以使用装备槽、器官 Slot、Consume/Evolve 等更高层系统，但不应先写死 Bio Domain。

---

# 2. 去重后的 11 个基础 Gameplay Component 维度

原始 10 类职业去重后，得到以下 11 个通用维度：

```text
1. Resource / Cost
2. Recovery / Refill
3. Activation / Trigger
4. Condition
5. Delivery / Form
6. Targeting
7. Core Effect
8. Modifier
9. Persistence / Timing
10. Relation / Ownership
11. Constraint / Tradeoff
```

这 11 个维度是职业构筑语法的基础。

它们不是都必须出现在每个职业中。

例如：

```text
纯幸运/装备型职业
```

理论上可以几乎没有 Skill。

```text
纯近战职业
```

可以没有自定义 Resource。

```text
反应型职业
```

甚至可以没有 Active Skill。

---

# 3. Resource / Cost

> 本节只定义“资源/支付”在组件体系中的语义。  
> 不规定 UI 必须存在六种固定资源，也不规定当前 Resource Preset 的具体改造方式。

## 3.1 Resource

Resource 表示：

> 可以被获得、保存、消耗、转换、衰减的一类职业状态量。

要求：

- 支持任意名称。
- 支持多个独立 Resource。
- 支持容量。
- 支持初始值。
- 支持当前值。
- 支持保存/读取。
- 支持 HUD 暴露。
- 不应把 Mana / Rage / Focus 等写死成唯一合法 Resource 类型。

预设可以存在，但预设必须最终展开为同一种 Generic ResourceSpec。

---

## 3.2 Cost

Cost 表示：

> 为执行某个能力真正支付的东西。

至少包括：

```text
Resource Cost
HP Cost
Action Cost
Cooldown
Consumable Cost
State / Stack Cost
```

### Resource Cost

例如：

```text
穿甲射击
Cost = 1 Ammo
```

### HP Cost

例如：

```text
燃血术
Cost = 3 HP
```

必须定义：

- 是否允许致死。
- 最低保留 HP。
- 与 Shield / Temporary HP 是否交互。

### Action Cost

例如：

```text
Reload
Action Time = 1 turn
```

它不是 Resource Cost，而是行动经济成本。

### Cooldown

Cooldown 表示执行后的一段时间不可再次执行。

Cooldown 不是 Resource。

### State / Stack Cost

例如：

```text
Consume 3 Mark
Consume 2 Charge
```

---

# 4. Recovery / Refill

Recovery / Refill 表示：

> 一个职业如何重新获得继续使用能力所需的资源或状态。

它与 Resource 是分离组件。

同一种 Resource 可以拥有不同 Recovery。

例如 Ammo：

```text
主动 Reload
```

Mana：

```text
每回合恢复
```

Rage：

```text
受伤 / 攻击获得，脱战衰减
```

---

## 4.1 Passive Refill

例如：

```text
每回合 +1
每 3 回合 +1
脱战时 +1
```

---

## 4.2 Event-driven Gain

例如：

```text
On Hit +1
On Damaged +2
On Kill +3
On Move +1
On Wait +1
On Debuff Applied +1
On Entity Recycled +X
```

这些本质上是：

```text
Trigger + Resource Gain
```

但玩家可以通过高层 Component 配置，不需要理解底层 Rule graph。

---

## 4.3 Active Refill

例如：

```text
Ammo
→ 主动装填
→ 花费 1 回合
→ 补满
```

如果 Recovery 是主动行为，应产生：

```text
ClassOperation
```

例如：

```text
[装填]
```

玩家不应手工创建一个：

```text
ACTIVE
+ Resource Gain
+ Self
+ Action Cost
```

来假装 Reload。

---

## 4.4 Decay / Loss

例如：

```text
每回合 -1
脱战衰减
停止移动后衰减
WAIT 清空
```

---

## 4.5 Convert / Overflow

例如：

```text
Resource A → Resource B
超过容量部分 → Barrier
```

注意：

- Convert 是 Resource Operation。
- Overflow 可以是 Rule / Synergy。
- 不应默认所有资源都有 Overflow。

---

# 5. Activation / Trigger

Trigger 回答：

> **什么时候执行？**

已冻结的基础 Trigger 包括：

```text
ACTIVE
ON_TURN_START
ON_MOVE
ON_ATTACK
ON_HIT
ON_DAMAGED
ON_KILL
ON_ITEM_USE
ON_ENTER_TILE
ON_STATUS_APPLIED
ON_LOW_HP
ON_WAIT
```

要求：

- Active 和 Reactive 使用同一 SkillSpec/Rule Runtime。
- Trigger 不应决定 Effect 类型。
- Trigger 可以与 Condition 组合。
- 同一事件链必须保留 provenance / recursion guard。
- 不允许通过固定职业类硬编码触发行为。

---

## 5.1 Active

玩家主动点击。

如果是普通战斗能力：

```text
Active Skill
```

如果是职业机制天然产生的基础操作：

```text
ClassOperation
```

两者玩家身份必须区分。

---

## 5.2 Reactive Trigger

例如：

```text
ON_DAMAGED
→ Push attacker
```

这是反应能力，不应占主动 HUD Button。

---

# 6. Condition

Condition 回答：

> **在什么条件下允许执行 / 触发？**

基础条件包括：

```text
ALWAYS
TARGET_EXISTS
SELF_HP_BELOW
TARGET_HP_BELOW
TARGET_HAS_POISON
TARGET_IS_BURNING
SELF_IN_WATER
DISTANCE_AT_LEAST
ADJACENT_ENEMIES_AT_LEAST
RESOURCE_AT_LEAST
```

可继续扩展，但必须由真实玩法需求驱动。

---

## 6.1 Condition 与 Constraint 的区别

Condition：

> 当前这一刻是否满足执行条件？

例如：

```text
目标 HP < 30%
```

Constraint：

> 构筑为了获得能力长期承担的限制或承诺。

例如：

```text
只能对 Marked Target 使用
只能站在水中使用
每层只能使用 3 次
```

二者底层可能接近，但预算语义不同。

---

# 7. Delivery / Form

Delivery 回答：

> **效果怎样从来源抵达目标？**

冻结的 V0.1 Delivery：

```text
Self
Contact / Attack
Direct Target
Projectile
Trace / Beam
Ground Placement
Persistent Carrier
Action Attachment
```

---

## 7.1 Self

效果直接作用于自己。

例如：

```text
Heal Self
Mode Shift
Gain Resource
```

---

## 7.2 Contact / Attack

效果通过接触/攻击命中发生。

例如：

```text
近战命中附毒
```

---

## 7.3 Direct Target

没有投射路径表现，直接作用于选定目标。

---

## 7.4 Projectile

必须具有：

```text
起点
路径
碰撞
射程
可见投射表现
命中后 Effect
```

注意：

`Ballistica` 只证明逻辑路径存在，不等于 Projectile presentation 完成。

---

## 7.5 Trace / Beam

沿线即时或持续作用。

与 Projectile 的主要区别：

- Projectile 是移动实体/视觉弹丸。
- Beam 是路径/射线作用。

---

## 7.6 Ground Placement

玩家选择一个 Cell，效果部署在地面。

常用于：

```text
Trap
Device
Zone
Terrain Effect
```

---

## 7.7 Persistent Carrier

一个持续存在的 Carrier 定期或条件触发内部 Payload。

例如：

```text
毒雾区域
自动炮塔
持续火场
```

Carrier 不是 Core Effect。

它是 Effect 的持续承载形式。

---

## 7.8 Action Attachment

把效果附着到后续某类行动。

例如：

```text
下一次攻击附带额外效果
接下来 3 次移动触发某效果
```

---

# 8. Targeting

Targeting 必须拆成 3 个正交轴。

## 8.1 Selector

回答：

> 从哪里选目标？

```text
Self
Selected Actor
Selected Cell
Nearest
Random
All Matching
```

---

## 8.2 Coverage

回答：

> 选中后覆盖多少对象？

```text
Single
Adjacent
Radius
Line
Cone
Ring
Chain
```

---

## 8.3 Filter

回答：

> 哪些对象合法？

```text
Enemy
Ally
Self
Owned Entity
Marked
Has Status
HP Threshold
Compatible Entity Type
```

---

## 8.4 禁止把 Targeting 打成巨大枚举

错误：

```text
SELECTED_ENEMY_RADIUS_2
SELECTED_ALLY_RADIUS_2
NEAREST_MARKED_ENEMY
```

正确：

```text
Selector = Selected Actor
Coverage = Radius(2)
Filter = Enemy
```

---

# 9. Core Effect：11 个 Effect Families

每个 Skill 必须有一个 Primary Core Effect。

可以有：

```text
0..1 Secondary Effect
```

但不能退回任意 Effect Graph。

冻结的 11 个 Family：

```text
1. Damage
2. Status
3. Movement
4. Recovery / Defense
5. Resource Operation
6. Mark / Accumulation
7. Create Entity
8. World / Terrain
9. Relation / Control
10. Transfer / Copy
11. Transform
```

---

# 10. Damage

Damage 表示：

> 直接改变目标生命值的伤害。

可包括：

```text
Standard Damage
Scaling Damage
Percent Damage
Missing HP Damage
Execute
```

---

## 10.1 Scaling Source

Damage 可以从不同来源缩放，例如：

```text
固定基础值
角色等级
缺失生命
Resource 当前值
装备 Power
目标状态
```

Scaling Source 应是参数，不应制造新的 Effect Family。

---

## 10.2 Execute

Execute 是 Damage Variant，不是独立 Effect Family。

必须定义：

```text
阈值
Boss / Elite 兼容规则
是否允许无条件击杀
```

---

# 11. Status

Status 表示：

> 给目标施加持续状态或规则修改。

包括：

```text
DoT
Attribute Modifier
Action Control
Behavior Control
Special State
```

例如：

```text
Poison
Burn
Bleed
Slow
Haste
Vulnerable
Paralysis
Charm-like behavior
```

Poison / Burn / Bleed 是 Status Variant，不是独立 Family。

---

# 12. Movement

Movement 表示：

> 改变角色位置。

包括：

```text
Dash
Teleport
Forced Movement
Push
Pull
Throw
Swap
```

要求：

- 不直接复制地图内部位置。
- 必须遵守可达性、碰撞、Chasm 等 SPD 规则。
- Forced Movement 与 voluntary Move 的事件语义应区分，可通过 Event Remap Rule 修改。

---

# 13. Recovery / Defense

包括：

```text
Restore
Barrier / Shield
Temporary HP
Mitigate
Cleanse
Redirect / Absorb
Revive（后续）
```

---

## 13.1 Restore

恢复真实 HP。

---

## 13.2 Barrier / Shield

额外吸收伤害的独立防护层。

---

## 13.3 Temporary HP

临时提高可损失生命值。

**不得和 Shield/Barrier 简单视为同义词。**

---

## 13.4 Mitigate

减少即将受到的伤害。

---

## 13.5 Cleanse

移除符合条件的负面状态。

---

## 13.6 Redirect / Absorb

把伤害或效果转移到其它合法承载对象。

---

# 14. Resource Operation

Resource Operation 包括：

```text
Gain
Drain
Convert
Reserve
Suppress
```

注意：

```text
Spend
```

通常属于 Cost，而不是 Effect。

---

# 15. Mark / Accumulation

包括：

```text
Mark
Stack
Charge
Counter
Consume
Spread
```

这是实现：

- 连击
- 猎杀标记
- 充能
- 条件爆发
- Finisher

的通用基础。

---

## 15.1 Mark

给目标或自己附加可识别状态。

---

## 15.2 Stack

可累积层数。

---

## 15.3 Counter

记录行为次数或状态次数。

---

## 15.4 Consume

消耗 Mark / Stack / Charge 来换取效果。

---

## 15.5 Spread

把 Mark/Stack 转移/传播到其它合法目标。

必须有传播深度/递归保护。

---

# 16. Create Entity

Create Entity 表示：

> 在世界中生成一个真实可交互实体。

基础类型：

```text
Actor
Device / Object
Temporary Construct
Carrier / Trap
```

---

## 16.1 Actor

生成可进入 Actor turn loop 的实体。

如果它受玩家控制，需要结合：

```text
Relation / Ownership
Capacity
Persistence
Command
```

---

## 16.2 Device / Object

例如：

```text
炮塔
传感器
生产装置
障碍物
```

Device 本身不是 Engineer Domain。

---

## 16.3 Carrier / Trap

作为持续 Payload 的载体。

例如：

```text
陷阱
区域
周期触发装置
```

---

# 17. World / Terrain

World / Terrain 表示：

> 改变地牢世界状态。

必须基于 SPD 真实支持的地图能力。

建议 Capability：

```text
DESTRUCTIBLE
REPLACEABLE
TRAP_PLACEABLE
PLANTABLE
BLOB_SEEDABLE
HAZARD_CLEARABLE
```

禁止：

> 为了让某个自定义技能成立，随意跳过 SPD Level/Terrain 的真实规则。

---

# 18. Relation / Control

Relation 描述实体之间的游戏关系。

包括：

```text
Ownership
Link / Bind
Command
Inherit
Break
```

---

## 18.1 Ownership

表示：

> 这个 Entity 属于谁。

Ownership 本身不等于 Summon。

---

## 18.2 Link / Bind

建立实体间持续连接。

例如：

```text
装置 A → 装置 B
玩家 → 召唤物
两个目标共享伤害
```

---

## 18.3 Command

Command 是针对合法 Relation 的控制操作。

如果玩家拥有 Command Capability，应产生 ClassOperation，例如：

```text
[指挥]
```

Command 的目标不能写死为“召唤物”。

应该基于：

```text
Relation Filter
→ Owned Entity
```

---

## 18.4 Inherit

新实体继承来源实体的一部分属性 / Tag / Capability。

---

## 18.5 Break

解除关系。

---

# 19. Transfer / Copy

Transfer / Copy 包括：

```text
Transfer
Copy
Swap
```

它是通用效果，不只是资源行为。

可以应用于明确允许的数据类型，例如：

```text
Resource
Status
Mark
Position（若合法）
部分 Capability
```

必须通过安全 Capability 控制：

```text
TRANSFERABLE
COPYABLE
SWAPPABLE
```

禁止任意复制内部对象引用。

---

# 20. Transform

Transform 表示：

> 临时或局部改变实体工作模式、能力或行为。

至少包括：

```text
Mode Shift
Capability Override
Behavior Override
```

---

## 20.1 Mode Shift

例如：

```text
Assault
Guard
```

Mode Engine 应定义：

```text
可用 Mode
当前 Mode
Mode 切换规则
各 Mode 对组件的影响
```

如果切换是主动行为，应产生：

```text
ClassOperation: Mode Switch
```

错误：

```text
hasModeSystem = true
```

然后没有可配置的 Mode 内容。

---

## 20.2 Capability / Behavior Override

例如：

```text
某形态可以飞行
某形态无法普通攻击
某形态技能 Delivery 改为 Area
```

永久职业级规则改写不应滥用 Transform。

---

# 21. Modifier / Skill Affix

Modifier 表示：

> 改变已有 Effect / Delivery 的执行形式，而不是增加第二种主要效果。

基础 Modifier：

```text
Area
Repeat
Extend Duration
Intensity
Pierce
Bounce
Echo
Delay
Propagation
```

---

## 21.1 Area

扩展覆盖范围。

---

## 21.2 Repeat

重复同一个 Effect。

必须明确：

- Cost 是一次还是每次。
- Repeat 是否触发 Trigger。
- 防递归。

---

## 21.3 Extend Duration

延长持续效果。

---

## 21.4 Intensity

提高 Effect 强度。

UI 必须显示真实含义，而不是：

```text
Intensity 3
```

应显示：

```text
毒伤提高至 X
击退增加 2 格
```

---

## 21.5 Pierce

允许 Projectile / Line 继续通过额外目标。

---

## 21.6 Bounce

命中后跳转至其它合法目标。

---

## 21.7 Echo

延迟重复一次技能/效果。

---

## 21.8 Delay

延迟执行 Payload。

---

## 21.9 Propagation

把状态 / Mark / Effect 向其它目标传播。

---

# 22. Persistence / Timing

Persistence / Timing 回答：

> **效果存在多久、多久执行一次、何时消失？**

包括：

```text
Lifetime
Periodic Tick
Delay
Cooldown
Charge Time
Duration
Limited Uses
Persistence Across Rooms/Floors（仅在明确安全时）
```

---

## 22.1 Lifetime

用于：

```text
Summon
Device
Carrier
Field
Trap
```

---

## 22.2 Periodic Tick

例如：

```text
每 2 回合触发一次
```

---

## 22.3 Charge Time

需要等待/蓄力后才能执行。

属于 Timing / Commitment，不是 Resource。

---

# 23. Constraint / Tradeoff

Constraint 表示：

> 职业为了换取 Power 长期承担的真实限制。

包括：

```text
Target Constraint
Self State Constraint
Position Constraint
Timing Constraint
Commitment
Frequency / Limited Use
Basic Attack Weakening
Resource Dependency
```

---

## 23.1 Target Constraint

例如：

```text
只能对 Marked Target
只能对中毒目标
```

---

## 23.2 Self State Constraint

例如：

```text
只有 HP < 30% 才可使用
```

---

## 23.3 Position Constraint

例如：

```text
必须站在水中
必须与目标保持至少 3 格
```

---

## 23.4 Commitment

例如：

```text
使用后无法移动 1 回合
需要先 Charge
Reload 消耗完整行动
```

---

## 23.5 Frequency

例如：

```text
每场战斗 1 次
每层 3 次
Cooldown
```

---

# 24. Basic Attack Model（后续补充并已确认）

> 这是在原始 10 类职业拆解之后补充确认的 Class-level Gameplay Component。

普通攻击不是所有构筑都应免费保留的完整战斗体系。

至少支持：

```text
WEAK
FULL
```

当前阶段不要求强行实现 DISABLED。

---

## 24.1 WEAK

目的：

> 给资源型/特殊攻击型职业保留最低安全 fallback，但不值得当主要输出。

要求：

- 只影响默认 Hero Basic Attack。
- 不影响 Skill Damage。
- 不影响 Projectile Skill。
- 不影响 Weapon Ability。
- 不影响 Throwing Weapon。
- 不影响 Vanilla Hero。

预算：

```text
低成本 / 0 成本基线
```

---

## 24.2 FULL

保留 SPD 原版完整普通攻击体系。

成本：

```text
Dynamic Cost
```

而不是固定价格。

纯近战职业：

```text
没有其它可靠 Damage Path
→ FULL 成本可以接近 0
```

枪手/法师/召唤等已经拥有独立攻击体系：

```text
FULL
→ 额外适应性
→ 额外预算
```

---

# 25. ClassOperation（后续补充并已确认）

ClassOperation 不是 Skill Family。

它是：

> **由 Class-level Gameplay Components 自动产生的基础职业操作。**

典型：

```text
Active Refill
→ Reload

Ownership + Command
→ Command

Mode Engine
→ Mode Switch

Device + Recycle Capability
→ Recycle
```

要求：

- 有独立 Runtime identity。
- 有独立 Save identity。
- 有独立 HUD identity。
- 不要求玩家自己制造伪 Skill。
- 可以复用 RuleRuntime 执行。
- Active Skill 与 ClassOperation 都进入 ClassActionBar。
- Reaction / Passive 不进入主动 Action Bar。

---

# 26. Capacity 不是“召唤师开关”

Capacity 是 Constraint / Class-level Parameter。

可以限定：

```text
Actor Capacity
Device Capacity
Carrier Capacity
Owned Entity Capacity
```

它必须绑定明确 Entity Filter。

错误：

```text
actorCapacity = 3
deviceCapacity = 2
```

永远固定挂在每个职业页面。

更合理：

```text
[Capacity]
Entity Filter = Owned Actor
Max = 3
```

或：

```text
[Capacity]
Entity Filter = Device
Max = 2
```

没有 Create Entity / Ownership 构筑时，该组件无需出现。

---

# 27. 典型职业如何由组件组合产生

这些只是验收 Recipe，不是固定职业。

---

## 27.1 Martial Defender

```text
Basic Attack = FULL
Contact Delivery
On Damaged Trigger
Barrier / Mitigate
Push
Counter-style reactive Skill
```

不需要：

```text
WARRIOR_DOMAIN
```

---

## 27.2 Blood Berserker

```text
HP Cost
Low HP Condition
On Damaged Trigger
Resource Gain
Missing HP Damage Scaling
Temporary HP / Recovery
```

---

## 27.3 Ammo Gunner

```text
Basic Attack = WEAK
Resource = Ammo
Capacity = 6
Recovery = Active Refill
→ ClassOperation [Reload]

Skill:
Damage
+ Projectile
+ Range
+ Pierce
+ Cost 1 Ammo
```

---

## 27.4 Area Caster

```text
Renewable Resource
Active Skill
Damage / Status
Projectile or Direct Target
Radius / Cone / Ring
High Cost
```

---

## 27.5 Mark Assassin

```text
Mark / Stack
Specific Target Condition
Movement
Missing HP / Execute Damage
Kill Trigger
Cooldown Acceleration / Reset
```

---

## 27.6 Owned Summoner

```text
Create Entity(Actor)
Relation = Ownership
Persistence
Capacity(Owned Actor)
Command Capability
→ ClassOperation [Command]
```

---

## 27.7 Device Engineer

```text
Create Entity(Device)
Ground Placement
Persistence
Periodic / Trigger
Relation(Link)
Resource Operation
Recycle Capability
→ ClassOperation [Recycle]
```

---

## 27.8 Terrain Controller

```text
World / Terrain
Ground Placement
Persistent Carrier
Push / Pull
Slow
Area / Field
```

---

## 27.9 Support / Defense

```text
Heal
Barrier
Cleanse
Transfer
Link
Redirect
```

---

## 27.10 Mode / Transform Build

```text
Mode Definitions
Transform / Mode Shift
Mode-specific Rule Changes
→ ClassOperation [Mode Switch]
```

---

# 28. 组件之间的组合原则

## 28.1 允许跨原型混合

合法：

```text
Ammo
+ Summon
+ Terrain
```

只要预算允许。

---

## 28.2 不允许用职业标签阻止组合

禁止：

```text
if class == GUNNER:
    cannot summon
```

应该：

```text
Budget
Constraint
Dependency
Integrity
```

决定构筑是否合法。

---

## 28.3 Dependency 允许先选后绑定

依赖状态：

```text
RESOLVED
UNRESOLVED
HARD_CONFLICT
UNSUPPORTED
```

例如玩家先添加：

```text
Cost = 1 Resource
```

但还没创建 Resource：

```text
UNRESOLVED
尚需绑定资源
```

不能直接灰掉。

只有：

```text
HARD_CONFLICT
UNSUPPORTED
永久非法
超预算
```

才禁止选择。

---

# 29. Player-facing UI 要求

## 29.1 只显示已经选择的组件

枪手页面应该类似：

```text
玩法组件

弱化普通攻击
弹药：6
主动装填：1 回合补满

[+ 添加玩法组件]
```

而不是：

```text
Ownership：未配置
Actor Capacity：0
Device Capacity：0
Command：未配置
Recycle：未配置
Mode：未配置
```

---

## 29.2 组件选择器应按语义分类

例如：

```text
添加玩法组件

基础战斗
资源与循环
实体与关系
持续与容量
模式与状态
限制与代价
```

分类只是 UI 导航，不是固定 Domain。

---

## 29.3 Skill Editor 继续保留高级能力

Skill 本身应继续允许：

```text
Trigger
Condition
Primary Effect
Secondary Effect
Delivery
Targeting
Modifier
Cost
Constraint
```

不要为了高层组件 UI 降低 Skill Runtime 表达能力。

---

# 30. Rules / Synergy 与基础组件的边界

Law / Trait 旧系统不应冒充“职业核心”。

例如：

```text
Healing → Shield
Resource Overflow → Shield
Forced Move counts as Move
Echo
Propagation
```

这类属于：

```text
Rules / Synergy
```

它们可以改变组件之间的关系，但不是基础组件本身。

玩家层可以统一叫：

```text
规则与协同
```

底层为了兼容可以继续保留 LawSpec / TraitSpec。

---

# 31. 什么叫“真正实现了一个 Gameplay Component”

一个组件只有同时满足以下条件才能算完成。

## 31.1 Data

- 有明确数据结构。
- 参数不是散落的固定 boolean。
- 可独立存在。
- 可独立保存。

## 31.2 Player-facing

- 玩家可以在正常 Builder 找到。
- 玩家能理解它的语义。
- 没选择时不制造无关 UI 噪声。
- 参数使用玩家语言而不是内部枚举。

## 31.3 Runtime

- 真正改变游戏行为。
- 不只是 Builder Summary。
- 不只是 Registry Entry。
- 不只是 QA Metadata。

## 31.4 Budget

- 有统一预算成本或约束价值。
- 使用同一 ClassBudgetPolicy Authority。
- 不允许 Player Builder / Stress / Legacy 各有一套预算。

## 31.5 Save / Load

- 存档后保持。
- 旧存档迁移不崩。
- roundtrip 语义一致。

## 31.6 Dependency

- 缺依赖时可 UNRESOLVED。
- 不因为创建顺序不同而非法。

## 31.7 QA

- QA 必须从 Player Builder 路径构筑。
- 不允许直接 new 最终对象绕过 Player-facing 路径证明“玩家可构造”。

---

# 32. 明确禁止的“伪完成”

以下情况不能声称 Gameplay Component 已实现。

## 32.1 Skill 强行模拟

例如：

```text
Resource Gain + Action Cost
```

被报告成：

```text
Reload Component 已实现
```

如果玩家仍然必须自己创建这个 Skill：

**FAIL**

---

## 32.2 固定 Archetype 字段

例如：

```text
hasCommand
hasRecycle
hasModeEngine
actorCapacity
deviceCapacity
```

然后称为“通用 Gameplay Component System”。

如果这些字段只是为了通过几个参考职业：

**PARTIAL / FAIL**

---

## 32.3 Summary 冒充组件

系统分析已有 Skill 后显示：

```text
核心玩法：远程弹药
```

这只是 Summary。

不是 Component。

---

## 32.4 Registry Coverage 冒充玩家实现

```text
210/210 registered
SUPPORTED_BUT_NOT_EXPOSED = 0
```

不能证明玩家抽象正确。

---

## 32.5 Toggle 冒充完整机制

例如：

```text
Mode Engine：ON
```

但不能定义 Mode：

**FAIL**

---

# 33. 构筑质量要求

## 33.1 有效组合密度

目标不是最大理论组合数量。

目标是：

> 大量组合能够形成有意义、有反馈、有玩法差异的 Build。

---

## 33.2 避免“税收选项”

一个玩家永远不可能理性选择的选项没有价值。

例如：

```text
弱化普通攻击
```

如果它只是单纯削弱且不给 Build 带来任何预算/体系价值，就没有意义。

当前确认的设计是：

```text
WEAK = 低/0成本基线
FULL = 根据其它攻击能力动态收费
```

从而形成真实选择。

---

## 33.3 全能职业允许存在

不要禁止：

```text
近战 + 法术 + 召唤
```

但其多套可靠战斗路径必须反映到预算。

---

# 34. 当前暂缓，不应混入本规范实现

以下方向存在，但不应因为本组件规范而擅自实现：

```text
Luck / 幸运
Talent
Subclass
Specialization
Progression
正式枪械 Item 系统
Weapon Access Restriction
Weapon Enchantment → Skill 继承
完整器官 Slot
Learn / 悟道完整系统
动态炼成完整系统
尸体遗产
赏金 / Nemesis
```

其中有些未来会使用本组件系统作为基础。

---

# 35. Weapon 暂定边界

当前阶段：

- SPD 原版武器正常拾取。
- 正常装备。
- 正常升级。
- 正常附魔。
- 不新增武器类别限制。
- 不为了枪手擅自增加 Gun Item System。

Basic Attack Model 只控制：

> 默认 Hero 普通攻击路径。

未来武器是否作为：

```text
Skill Scaling Source
```

以及 Enchantment 是否传递，另行设计。

---

# 36. 实现时的推荐内部结构（非强制类名）

可采用类似：

```text
ClassBuild
├─ gameplayComponents: List<ClassGameplayComponentSpec>
├─ resources: List<ResourceSpec>
├─ skills: List<SkillSpec>
├─ rules/synergies
├─ startingKit
└─ budget/integrity
```

其中：

```text
ClassGameplayComponentSpec
```

应该是可扩展、可参数化的组件集合。

不要继续增长一个巨大：

```text
ClassGameplaySpec
```

然后里面无限新增：

```text
boolean ownership
boolean command
boolean recycle
boolean mode
int actorCapacity
int deviceCapacity
...
```

否则最终会退化成“固定职业功能字段集合”。

---

# 37. 组件层级速查

```text
CLASS-LEVEL
│
├─ Basic Attack Model
├─ Resource Model
├─ Recovery / Refill Model
├─ Entity Relation / Ownership
├─ Capacity
├─ Persistence
├─ Mode Definitions
├─ ClassOperation Source
└─ Global Constraint / Tradeoff


SKILL-LEVEL
│
├─ Activation / Trigger
├─ Condition
├─ Primary Core Effect
├─ Secondary Effect [0..1]
├─ Delivery
├─ Targeting
├─ Modifier
├─ Cost
└─ Constraint


CORE EFFECT FAMILIES
│
├─ Damage
├─ Status
├─ Movement
├─ Recovery / Defense
├─ Resource Operation
├─ Mark / Accumulation
├─ Create Entity
├─ World / Terrain
├─ Relation / Control
├─ Transfer / Copy
└─ Transform


RULES / SYNERGY
│
├─ Global semantic rewrite
├─ Cross-component bridge
├─ Event remap
├─ Overflow behavior
├─ Echo / propagation
└─ Other systemic synergy
```

---

# 38. 最终判定原则

不要问：

> “这 10 个职业能不能被 Runtime 拼出来？”

必须问：

> “玩家是否真的拥有一套通用 Gameplay Component 语言，可以在不选择固定职业、不理解底层 Rule Graph 的情况下，自由构造这些玩法？”

如果：

```text
枪手只能靠手工做 Reload Skill
召唤只能靠几个 hasOwnership/hasCommand 开关
工程只能靠 hasRecycle
模式只有 hasModeEngine=true
```

那么：

> **Runtime Primitive 可能已完成，但 Gameplay Component System 仍未完成。**

本规范的目标就是解决这个层级问题。
