package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.BuilderCommand;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.TypedRef;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.Assert.*;

public class BuilderCommandArchitectureTest {
	@Test public void commandPayloadsContainOnlyPrimitivesStringsAndTypedRefs() {
		for(Class<?> command:BuilderCommand.class.getDeclaredClasses()){
			if(!BuilderCommand.class.isAssignableFrom(command)||Modifier.isAbstract(command.getModifiers()))continue;
			for(Field field:command.getDeclaredFields()){
				if(field.isSynthetic()||Modifier.isStatic(field.getModifiers()))continue;
				Class<?> type=field.getType();
				assertTrue(command.getName()+"."+field.getName()+" has forbidden payload "+type,
						type.isPrimitive()||type==String.class||TypedRef.class.isAssignableFrom(type));
			}
		}
	}
}
