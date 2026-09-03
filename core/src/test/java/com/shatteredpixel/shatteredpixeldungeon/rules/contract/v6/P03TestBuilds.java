package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.BuilderCommand;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.PlayerBuildSession;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DeterministicIdGenerator;

final class P03TestBuilds {
	private P03TestBuilds() {}
	static PlayerBuildSession directDamage(String seed,int amount,boolean lineOfSight,boolean secondary) {
		PlayerBuildSession session=PlayerBuildSession.empty(new DeterministicIdGenerator(seed));
		session.dispatch(new BuilderCommand.CreateSkill("Arc Strike"));
		String id=session.state().draft().skills().get(0).id().value();
		session.dispatch(new BuilderCommand.SelectTriggerVariant(id,"ACTIVE"));
		session.dispatch(new BuilderCommand.SelectConditionVariant(id,"ALWAYS"));
		session.dispatch(new BuilderCommand.SelectEffectFamily(id,"PRIMARY","DAMAGE"));
		session.dispatch(new BuilderCommand.SelectEffectVariant(id,"PRIMARY","DIRECT_DAMAGE"));
		session.dispatch(new BuilderCommand.SetTypedSkillField(id,"effects.primary","DIRECT_DAMAGE","amount",Integer.toString(amount)));
		session.dispatch(new BuilderCommand.SetDelivery(id,"DIRECT"));
		if(lineOfSight)session.dispatch(new BuilderCommand.SetTypedSkillField(id,"delivery","DIRECT","requires_line_of_sight","true"));
		session.dispatch(new BuilderCommand.SetTargetingSelector(id,"SELECTED_ACTOR"));
		session.dispatch(new BuilderCommand.SetTargetingCoverage(id,"SINGLE"));
		session.dispatch(new BuilderCommand.SetTargetingFilter(id,"RELATION_ENEMY_EXCLUDE_SELF"));
		session.dispatch(new BuilderCommand.SetTypedSkillField(id,"targeting","TARGETING","range","5"));
		session.dispatch(new BuilderCommand.SetModifier(id,"NONE"));
		session.dispatch(new BuilderCommand.SetCost(id,"NO_COST"));
		session.dispatch(new BuilderCommand.SetSkillConstraint(id,"NONE"));
		if(secondary){
			session.dispatch(new BuilderCommand.SelectEffectFamily(id,"IMMEDIATE_SECONDARY","DAMAGE"));
			session.dispatch(new BuilderCommand.SelectEffectVariant(id,"IMMEDIATE_SECONDARY","DIRECT_DAMAGE"));
			session.dispatch(new BuilderCommand.SetTypedSkillField(id,"effects.secondary","DIRECT_DAMAGE","amount","3"));
		}
		if(!session.state().commandDiagnostics().isEmpty())throw new AssertionError(session.state().commandDiagnostics());
		return session;
	}
}
