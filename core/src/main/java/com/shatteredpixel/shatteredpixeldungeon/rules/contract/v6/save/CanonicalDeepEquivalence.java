package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ClassBuildSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.state.ClassRuntimeState;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Independent field-by-field oracle for canonical roundtrip tests. It does not
 * reuse canonical output as the equality definition, so a codec omission is
 * observable.
 */
public final class CanonicalDeepEquivalence {
	private final CanonicalBuildCodec builds = new CanonicalBuildCodec();
	private final CanonicalRuntimeCodec runtime = new CanonicalRuntimeCodec();

	public boolean equivalent(ClassBuildSpec first, ClassBuildSpec second) {
		return deeplyEquivalent(first, second);
	}

	public boolean equivalent(ClassRuntimeState first, ClassRuntimeState second) {
		return deeplyEquivalent(first, second);
	}

	public ClassBuildSpec copy(ClassBuildSpec source) {
		CanonicalLoadResult<ClassBuildSpec> result = builds.deserialize(builds.serialize(source));
		if (result.value() == null) throw new IllegalStateException(result.diagnostics().toString());
		return result.value();
	}

	public ClassRuntimeState copy(ClassRuntimeState source) {
		CanonicalLoadResult<ClassRuntimeState> result = runtime.deserialize(runtime.serialize(source));
		if (result.value() == null) throw new IllegalStateException(result.diagnostics().toString());
		return result.value();
	}

	private static boolean deeplyEquivalent(Object first, Object second) {
		if (first == second) return true;
		if (first == null || second == null || first.getClass() != second.getClass()) return false;
		if (first instanceof List) {
			List<?> left = (List<?>) first;
			List<?> right = (List<?>) second;
			if (left.size() != right.size()) return false;
			for (int i = 0; i < left.size(); i++) if (!deeplyEquivalent(left.get(i), right.get(i))) return false;
			return true;
		}
		if (first instanceof Set) return first.equals(second);
		if (first instanceof Map) {
			Map<?, ?> left = (Map<?, ?>) first;
			Map<?, ?> right = (Map<?, ?>) second;
			if (!left.keySet().equals(right.keySet())) return false;
			for (Object key : left.keySet()) if (!deeplyEquivalent(left.get(key), right.get(key))) return false;
			return true;
		}
		Package typePackage = first.getClass().getPackage();
		if (typePackage == null || !typePackage.getName().startsWith(
				"com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6")) return first.equals(second);
		try {
			for (Class<?> type = first.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
				for (Field field : type.getDeclaredFields()) {
					if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
					field.setAccessible(true);
					if (!deeplyEquivalent(field.get(first), field.get(second))) return false;
				}
			}
			return true;
		} catch (IllegalAccessException error) {
			throw new IllegalStateException("cannot inspect canonical value", error);
		}
	}
}
