package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6;

import org.junit.Test;import java.nio.charset.StandardCharsets;import java.nio.file.*;import java.util.ArrayList;import java.util.List;import java.util.stream.Stream;
import static org.junit.Assert.*;

/** Static P01 gate: the formal v6 tree cannot revive legacy auto-binding or first-item fallbacks. */
public class V6NoFallbackArchitectureTest {
	@Test public void v6ProductionContainsNoAutoBindingOrFirstItemFallback()throws Exception{Path root=repoRoot().resolve("core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6");List<String> violations=new ArrayList<>();try(Stream<Path> files=Files.walk(root)){files.filter(p->p.toString().endsWith(".java")).forEach(p->{try{String source=new String(Files.readAllBytes(p),StandardCharsets.UTF_8);for(String banned:new String[]{"resolvePendingBindings","primaryResource()","resources.get(0)","modes.get(0)","RuleMark.Type.HUNTED","DAMAGE_STANDARD"})if(source.contains(banned))violations.add(p+": "+banned);}catch(Exception error){throw new RuntimeException(error);}});}assertTrue(violations.toString(),violations.isEmpty());}
	private static Path repoRoot(){Path current=Paths.get("").toAbsolutePath().normalize();for(int i=0;i<10&&current!=null;i++,current=current.getParent())if(Files.isRegularFile(current.resolve("settings.gradle")))return current;throw new AssertionError("repository root not found");}
}
