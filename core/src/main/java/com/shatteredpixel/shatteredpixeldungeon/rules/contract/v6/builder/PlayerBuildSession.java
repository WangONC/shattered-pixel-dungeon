package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DisplayName;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.IdGenerator;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.BudgetMetadata;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ClassBuildSpec;

import java.util.ArrayList;
import java.util.List;

/** Headless-capable player assembler. Its only mutation entrypoint is dispatch(command). */
public final class PlayerBuildSession {
	private final BuilderReducer reducer;
	private final List<BuilderCommand> trace = new ArrayList<>();
	private BuilderState state;

	private PlayerBuildSession(BuilderReducer reducer, BuilderState state) { this.reducer = reducer; this.state = state; }
	public static PlayerBuildSession empty(IdGenerator ids) {
		if (ids == null) throw new IllegalArgumentException("id generator is required");
		ClassBuildSpec draft=ClassBuildSpec.builder(ids.nextId("build"),DisplayName.of("New Build"))
				.budgetMetadata(new BudgetMetadata(BuilderBudgetPolicy.P03TypedSkill.PRICE_VERSION,20)).build();
		BuilderReducer reducer=new BuilderReducer(ids,new BuilderBudgetPolicy.P03TypedSkill());
		return new PlayerBuildSession(reducer,reducer.initial(draft));
	}
	public static PlayerBuildSession fromDraft(IdGenerator ids,ClassBuildSpec draft){BuilderReducer reducer=new BuilderReducer(ids);return new PlayerBuildSession(reducer,reducer.initial(draft));}
	public static PlayerBuildSession fromDraft(IdGenerator ids,ClassBuildSpec draft,BuilderBudgetPolicy budgetPolicy){BuilderReducer reducer=new BuilderReducer(ids,budgetPolicy);return new PlayerBuildSession(reducer,reducer.initial(draft));}
	public BuilderState dispatch(BuilderCommand command){trace.add(command);state=reducer.apply(state,command);return state;}
	public BuilderState state(){return state;}
	public BuilderCommandTrace trace(){return new BuilderCommandTrace(trace);}
	public String saveTrace(){return trace().serialize();}
	public ClassBuildSpec finalizeOrThrow(){dispatch(new BuilderCommand.FinalizeBuild());if(state.finalization()==null||!state.finalization().allowed())throw new IllegalStateException("build cannot be finalized: "+(state.finalization()==null?"missing finalization":state.finalization().diagnostics()));return state.finalization().build();}
}
