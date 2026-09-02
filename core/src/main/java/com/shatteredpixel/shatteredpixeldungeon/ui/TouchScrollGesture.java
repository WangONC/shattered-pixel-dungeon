package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.watabou.utils.PointF;

/** Shared drag threshold used by scroll panes and buttons embedded inside them. */
public final class TouchScrollGesture {
	private TouchScrollGesture() {}

	public static boolean isDrag(PointF start, PointF current, float threshold) {
		return start != null && current != null && PointF.distance(start, current) > threshold;
	}
}
