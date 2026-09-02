package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;

import java.util.ArrayList;

/** Slot-free class-level mechanisms which should not be simulated by ordinary Skills. */
/** MIGRATION_ONLY schema-4 container. New Builder and Runtime authority is ClassGameplayComponentSpec. */
@Deprecated
public class ClassGameplaySpec implements Bundlable {
	public BasicAttackProfile basicAttack = BasicAttackProfile.WEAK;
	public boolean ownership;
	public boolean command;
	public int entityCapacity;
	public int deviceCapacity;
	public boolean recycle;
	public String recycleResourceId = "";
	public int recycleAmount = 1;
	public final ArrayList<String> modes = new ArrayList<>();

	public ClassGameplaySpec copy() {
		ClassGameplaySpec result = new ClassGameplaySpec();
		result.basicAttack = basicAttack; result.ownership = ownership; result.command = command;
		result.entityCapacity = entityCapacity; result.deviceCapacity = deviceCapacity;
		result.recycle = recycle; result.recycleResourceId = recycleResourceId; result.recycleAmount = recycleAmount;
		result.modes.addAll(modes); return result;
	}
	public boolean hasModeEngine() { return modes.size() >= 2; }
	public boolean valid(ClassBuild build) {
		if (basicAttack == null || entityCapacity < 0 || entityCapacity > 6 || deviceCapacity < 0 || deviceCapacity > 6
				|| command && !ownership || recycle && !ownership || recycleAmount < 1) return false;
		if (recycle && (recycleResourceId == null || build == null || build.resource(recycleResourceId) == null)) return false;
		return !hasModeEngine() || modes.get(0) != null && !modes.get(0).isEmpty()
				&& modes.get(1) != null && !modes.get(1).isEmpty() && !modes.get(0).equals(modes.get(1));
	}
	public int nominalPowerCost(ClassBuild build) {
		int result = basicAttack == null ? 0 : basicAttack.budgetCost(build);
		if (ownership) result += 1;
		// Command is a reusable class action over every owned actor, not a cosmetic ownership
		// flag. Preserve the power formerly paid by the fake command Skill without reintroducing
		// that Skill into the player model.
		if (command) result += 4;
		result += Math.max(0, entityCapacity - 1);
		result += Math.max(0, deviceCapacity - 1);
		if (recycle) result += 1;
		// The Mode Engine creates a persistent, always-available operation and gates an entire
		// family of skills. Its former ACTIVE MODE_SHIFT Skill was not free; formalizing the
		// operation must not silently erase that class-budget value.
		if (hasModeEngine()) result += 10 + Math.max(0, modes.size() - 2);
		return result;
	}
	@Override public void storeInBundle(Bundle bundle) {
		bundle.put("basic_attack", basicAttack); bundle.put("ownership", ownership); bundle.put("command", command);
		bundle.put("entity_capacity", entityCapacity); bundle.put("device_capacity", deviceCapacity);
		bundle.put("recycle", recycle); bundle.put("recycle_resource", recycleResourceId);
		bundle.put("recycle_amount", recycleAmount); bundle.put("modes", modes.toArray(new String[0]));
	}
	@Override public void restoreFromBundle(Bundle bundle) {
		basicAttack = bundle.contains("basic_attack") ? bundle.getEnum("basic_attack", BasicAttackProfile.class) : BasicAttackProfile.FULL;
		ownership = bundle.getBoolean("ownership"); command = bundle.getBoolean("command");
		entityCapacity = Math.max(0, bundle.getInt("entity_capacity")); deviceCapacity = Math.max(0, bundle.getInt("device_capacity"));
		recycle = bundle.getBoolean("recycle"); recycleResourceId = bundle.getString("recycle_resource");
		recycleAmount = bundle.contains("recycle_amount") ? Math.max(1, bundle.getInt("recycle_amount")) : 1;
		modes.clear(); String[] saved = bundle.getStringArray("modes"); if (saved != null) for (String mode : saved) if (mode != null && !mode.isEmpty()) modes.add(mode);
	}
}
