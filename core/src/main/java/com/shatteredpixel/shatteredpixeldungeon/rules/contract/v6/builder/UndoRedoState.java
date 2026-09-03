package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ClassBuildSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class UndoRedoState {
	private final List<ClassBuildSpec> undo;
	private final List<ClassBuildSpec> redo;
	private UndoRedoState(List<ClassBuildSpec> undo, List<ClassBuildSpec> redo) {
		this.undo = Collections.unmodifiableList(new ArrayList<>(undo));
		this.redo = Collections.unmodifiableList(new ArrayList<>(redo));
	}
	public static UndoRedoState empty() { return new UndoRedoState(Collections.<ClassBuildSpec>emptyList(), Collections.<ClassBuildSpec>emptyList()); }
	public List<ClassBuildSpec> undoStack() { return undo; }
	public List<ClassBuildSpec> redoStack() { return redo; }
	public boolean canUndo() { return !undo.isEmpty(); }
	public boolean canRedo() { return !redo.isEmpty(); }
	UndoRedoState push(ClassBuildSpec previous) {
		List<ClassBuildSpec> next = new ArrayList<>(undo); next.add(previous);
		return new UndoRedoState(next, Collections.<ClassBuildSpec>emptyList());
	}
	UndoResult undo(ClassBuildSpec current) {
		if (!canUndo()) return new UndoResult(current, this);
		List<ClassBuildSpec> nextUndo = new ArrayList<>(undo);
		ClassBuildSpec restored = nextUndo.remove(nextUndo.size() - 1);
		List<ClassBuildSpec> nextRedo = new ArrayList<>(redo); nextRedo.add(current);
		return new UndoResult(restored, new UndoRedoState(nextUndo, nextRedo));
	}
	UndoResult redo(ClassBuildSpec current) {
		if (!canRedo()) return new UndoResult(current, this);
		List<ClassBuildSpec> nextRedo = new ArrayList<>(redo);
		ClassBuildSpec restored = nextRedo.remove(nextRedo.size() - 1);
		List<ClassBuildSpec> nextUndo = new ArrayList<>(undo); nextUndo.add(current);
		return new UndoResult(restored, new UndoRedoState(nextUndo, nextRedo));
	}
	static final class UndoResult {
		final ClassBuildSpec draft; final UndoRedoState history;
		UndoResult(ClassBuildSpec draft, UndoRedoState history) { this.draft = draft; this.history = history; }
	}
}
