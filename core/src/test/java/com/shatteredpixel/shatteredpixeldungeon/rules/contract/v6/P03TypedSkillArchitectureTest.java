package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class P03TypedSkillArchitectureTest {
	@Test public void v6SkillModelHasNoLegacyRegistryOrGenericEffectBag() throws Exception {
		Path root=repoRoot().resolve("core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6");
		try(Stream<Path> files=Files.walk(root)){
			for(Path file:(Iterable<Path>)files.filter(v->v.toString().endsWith(".java"))::iterator){String code=new String(Files.readAllBytes(file),StandardCharsets.UTF_8);
				assertFalse(file.toString(),code.contains("rules.EffectVocabularyRegistry"));assertFalse(file.toString(),code.contains("rules.PlayerBuildAssembler"));assertFalse(file.toString(),code.contains("rules.EffectSpec"));assertFalse(file.toString(),code.contains("DAMAGE_STANDARD"));
			}
		}
		Path skillRoot=root.resolve("spec/skill");try(Stream<Path> files=Files.walk(skillRoot)){for(Path file:(Iterable<Path>)files.filter(v->v.toString().endsWith(".java"))::iterator){String code=new String(Files.readAllBytes(file),StandardCharsets.UTF_8);assertFalse(file.toString(),code.matches("(?s).*\\b(?:int|String)\\s+(?:power|duration|stateId|templateId)\\b.*"));}}
		String registry=new String(Files.readAllBytes(root.resolve("runtime/EffectExecutorRegistry.java")),StandardCharsets.UTF_8);assertFalse(registry.contains("V6FormSchemas"));
		for(String relative:new String[]{"compile/CompiledSkill.java","compile/ClassCompilePlan.java","runtime/V6RuleRuntime.java","runtime/EffectExecutor.java","runtime/EffectExecutorRegistry.java","runtime/DirectDamageExecutor.java"}){
			String code=new String(Files.readAllBytes(root.resolve(relative)),StandardCharsets.UTF_8);assertFalse(relative+" must not depend on authoring SkillSpec",code.contains("spec.skill"));assertFalse(relative+" must not retain SkillSpec",code.contains("SkillSpec"));
		}
		String event=new String(Files.readAllBytes(root.resolve("runtime/GameplayEventContext.java")),StandardCharsets.UTF_8);assertFalse(event.contains("actors.Char"));assertFalse(event.matches("(?s).*\bChar\b.*"));
		try(Stream<Path> files=Files.walk(root)){for(Path file:(Iterable<Path>)files.filter(v->v.toString().endsWith(".java"))::iterator){String code=new String(Files.readAllBytes(file),StandardCharsets.UTF_8);assertFalse(file+" contains production completion evidence",code.contains("P03CompletionMatrix")||code.contains("ComponentCompletionRow"));}}
	}
	private static Path repoRoot(){Path current=Paths.get("").toAbsolutePath().normalize();for(int i=0;i<10&&current!=null;i++,current=current.getParent())if(Files.isRegularFile(current.resolve("settings.gradle")))return current;throw new AssertionError("repository root not found");}
}
