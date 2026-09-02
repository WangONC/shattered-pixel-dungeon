package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.watabou.utils.PointF;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TouchScrollGestureTest {
	@Test
	public void dragStartingOnOptionRowCancelsClickAtThreshold() {
		PointF start = new PointF(20, 20);
		assertFalse(TouchScrollGesture.isDrag(start, new PointF(22, 25), 8));
		assertTrue(TouchScrollGesture.isDrag(start, new PointF(20, 31), 8));
	}
}
