package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DisplayName;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable schema-6 declaration graph. Runtime values are never stored here. */
public final class ClassBuildSpec {
	public static final int SCHEMA_VERSION = 6;
	public static final String CONTRACT_VERSION = "0.2-final";

	private final int schemaVersion; private final String contractVersion; private final StableId buildId;
	private final DisplayName displayName;
	private final List<ResourceSpec> resources; private final List<MarkSpec> marks;
	private final List<ModeGroupSpec> modeGroups; private final List<ModeSpec> modes;
	private final List<EntityCapacitySpec> capacities; private final List<EntitySpec> entities;
	private final List<AbilityPoolSpec> abilityPools; private final List<PropertySpec> properties;
	private final List<SynthesisRecipeSpec> recipes;
	private final List<ContractNodeSpec> classComponents; private final List<ContractNodeSpec> classConstraints;
	private final List<ContractNodeSpec> classOperations; private final List<ContractNodeSpec> skills;
	private final DeferredSectionSpec startingKit; private final DeferredSectionSpec progression;
	private final BudgetMetadata budgetMetadata;

	private ClassBuildSpec(Builder value) {
		if(value.buildId==null||value.displayName==null||value.contractVersion==null||value.startingKit==null
				||value.progression==null||value.budgetMetadata==null)throw new IllegalArgumentException("class build fields are required");
		schemaVersion=value.schemaVersion;contractVersion=value.contractVersion;buildId=value.buildId;displayName=value.displayName;
		resources=copy(value.resources);marks=copy(value.marks);modeGroups=copy(value.modeGroups);modes=copy(value.modes);
		capacities=copy(value.capacities);entities=copy(value.entities);abilityPools=copy(value.abilityPools);
		properties=copy(value.properties);recipes=copy(value.recipes);classComponents=copy(value.classComponents);
		classConstraints=copy(value.classConstraints);classOperations=copy(value.classOperations);skills=copy(value.skills);
		startingKit=value.startingKit;progression=value.progression;budgetMetadata=value.budgetMetadata;
	}
	private static <T> List<T> copy(List<T> values){return Collections.unmodifiableList(new ArrayList<>(values));}

	public int schemaVersion(){return schemaVersion;} public String contractVersion(){return contractVersion;}
	public StableId buildId(){return buildId;} public DisplayName displayName(){return displayName;}
	public List<ResourceSpec> resources(){return resources;} public List<MarkSpec> marks(){return marks;}
	public List<ModeGroupSpec> modeGroups(){return modeGroups;} public List<ModeSpec> modes(){return modes;}
	public List<EntityCapacitySpec> capacities(){return capacities;} public List<EntitySpec> entities(){return entities;}
	public List<AbilityPoolSpec> abilityPools(){return abilityPools;} public List<PropertySpec> properties(){return properties;}
	public List<SynthesisRecipeSpec> recipes(){return recipes;} public List<ContractNodeSpec> classComponents(){return classComponents;}
	public List<ContractNodeSpec> classConstraints(){return classConstraints;} public List<ContractNodeSpec> classOperations(){return classOperations;}
	public List<ContractNodeSpec> skills(){return skills;} public DeferredSectionSpec startingKit(){return startingKit;}
	public DeferredSectionSpec progression(){return progression;} public BudgetMetadata budgetMetadata(){return budgetMetadata;}

	public List<StableTarget> allTargets(){
		ArrayList<StableTarget> result=new ArrayList<>();result.addAll(resources);result.addAll(marks);result.addAll(modeGroups);
		result.addAll(modes);result.addAll(capacities);result.addAll(entities);result.addAll(abilityPools);result.addAll(properties);
		result.addAll(recipes);result.addAll(classComponents);result.addAll(classConstraints);result.addAll(classOperations);result.addAll(skills);
		return Collections.unmodifiableList(result);
	}

	public Builder toBuilder(){return new Builder(this);}
	public static Builder builder(StableId buildId,DisplayName displayName){return new Builder().buildId(buildId).displayName(displayName);}

	public static final class Builder {
		private int schemaVersion=SCHEMA_VERSION;private String contractVersion=CONTRACT_VERSION;private StableId buildId;private DisplayName displayName;
		private List<ResourceSpec> resources=new ArrayList<>();private List<MarkSpec> marks=new ArrayList<>();
		private List<ModeGroupSpec> modeGroups=new ArrayList<>();private List<ModeSpec> modes=new ArrayList<>();
		private List<EntityCapacitySpec> capacities=new ArrayList<>();private List<EntitySpec> entities=new ArrayList<>();
		private List<AbilityPoolSpec> abilityPools=new ArrayList<>();private List<PropertySpec> properties=new ArrayList<>();
		private List<SynthesisRecipeSpec> recipes=new ArrayList<>();private List<ContractNodeSpec> classComponents=new ArrayList<>();
		private List<ContractNodeSpec> classConstraints=new ArrayList<>();private List<ContractNodeSpec> classOperations=new ArrayList<>();
		private List<ContractNodeSpec> skills=new ArrayList<>();
		private DeferredSectionSpec startingKit=new DeferredSectionSpec("starting_kit",ImplementationState.DEFERRED);
		private DeferredSectionSpec progression=new DeferredSectionSpec("progression",ImplementationState.DEFERRED);
		private BudgetMetadata budgetMetadata=new BudgetMetadata("",0);
		public Builder(){}
		private Builder(ClassBuildSpec source){schemaVersion=source.schemaVersion;contractVersion=source.contractVersion;buildId=source.buildId;displayName=source.displayName;
			resources=new ArrayList<>(source.resources);marks=new ArrayList<>(source.marks);modeGroups=new ArrayList<>(source.modeGroups);modes=new ArrayList<>(source.modes);
			capacities=new ArrayList<>(source.capacities);entities=new ArrayList<>(source.entities);abilityPools=new ArrayList<>(source.abilityPools);
			properties=new ArrayList<>(source.properties);recipes=new ArrayList<>(source.recipes);classComponents=new ArrayList<>(source.classComponents);
			classConstraints=new ArrayList<>(source.classConstraints);classOperations=new ArrayList<>(source.classOperations);skills=new ArrayList<>(source.skills);
			startingKit=source.startingKit;progression=source.progression;budgetMetadata=source.budgetMetadata;}
		public Builder schemaVersion(int value){schemaVersion=value;return this;}public Builder contractVersion(String value){contractVersion=value;return this;}
		public Builder buildId(StableId value){buildId=value;return this;}public Builder displayName(DisplayName value){displayName=value;return this;}
		public Builder resources(List<ResourceSpec> value){resources=new ArrayList<>(value);return this;}public Builder addResource(ResourceSpec value){resources.add(value);return this;}
		public Builder marks(List<MarkSpec> value){marks=new ArrayList<>(value);return this;}public Builder addMark(MarkSpec value){marks.add(value);return this;}
		public Builder modeGroups(List<ModeGroupSpec> value){modeGroups=new ArrayList<>(value);return this;}public Builder addModeGroup(ModeGroupSpec value){modeGroups.add(value);return this;}
		public Builder modes(List<ModeSpec> value){modes=new ArrayList<>(value);return this;}public Builder addMode(ModeSpec value){modes.add(value);return this;}
		public Builder capacities(List<EntityCapacitySpec> value){capacities=new ArrayList<>(value);return this;}public Builder addCapacity(EntityCapacitySpec value){capacities.add(value);return this;}
		public Builder entities(List<EntitySpec> value){entities=new ArrayList<>(value);return this;}public Builder addEntity(EntitySpec value){entities.add(value);return this;}
		public Builder abilityPools(List<AbilityPoolSpec> value){abilityPools=new ArrayList<>(value);return this;}public Builder addAbilityPool(AbilityPoolSpec value){abilityPools.add(value);return this;}
		public Builder properties(List<PropertySpec> value){properties=new ArrayList<>(value);return this;}public Builder addProperty(PropertySpec value){properties.add(value);return this;}
		public Builder recipes(List<SynthesisRecipeSpec> value){recipes=new ArrayList<>(value);return this;}public Builder addRecipe(SynthesisRecipeSpec value){recipes.add(value);return this;}
		public Builder classComponents(List<ContractNodeSpec> value){classComponents=new ArrayList<>(value);return this;}public Builder addClassComponent(ContractNodeSpec value){classComponents.add(value);return this;}
		public Builder classConstraints(List<ContractNodeSpec> value){classConstraints=new ArrayList<>(value);return this;}public Builder addClassConstraint(ContractNodeSpec value){classConstraints.add(value);return this;}
		public Builder classOperations(List<ContractNodeSpec> value){classOperations=new ArrayList<>(value);return this;}public Builder addClassOperation(ContractNodeSpec value){classOperations.add(value);return this;}
		public Builder skills(List<ContractNodeSpec> value){skills=new ArrayList<>(value);return this;}public Builder addSkill(ContractNodeSpec value){skills.add(value);return this;}
		public Builder startingKit(DeferredSectionSpec value){startingKit=value;return this;}public Builder progression(DeferredSectionSpec value){progression=value;return this;}
		public Builder budgetMetadata(BudgetMetadata value){budgetMetadata=value;return this;}
		public ClassBuildSpec build(){return new ClassBuildSpec(this);}
	}
}
