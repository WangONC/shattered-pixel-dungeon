package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.ui.Component;

/** Compact final preview with a separate detailed-rule action. */
public abstract class WndBuilderSummary extends Window {

	private static final int WIDTH_P = 136;
	private static final int WIDTH_L = 166;
	private static final int HEIGHT_P = 190;
	private static final int HEIGHT_L = 138;
	private static final int MARGIN = 2;
	private static final int BUTTON_HEIGHT = 16;
	private final ScrollPane pane;

	public WndBuilderSummary(String titleText, String explanationText, String preview,
			String capacityText, boolean valid, String previousText, String modifyText,
			String detailsText, String createText) {
		int width = PixelScene.landscape() ? WIDTH_L : WIDTH_P;
		int height = Math.min(PixelScene.landscape() ? HEIGHT_L : HEIGHT_P,
				(int) PixelScene.uiCamera.height - 12);
		resize(width, height);

		RenderedTextBlock title = PixelScene.renderTextBlock(titleText, 9);
		title.hardlight(TITLE_COLOR);
		title.maxWidth(width - 2 * MARGIN);
		title.setPos(MARGIN, MARGIN);
		add(title);

		RenderedTextBlock explanation = PixelScene.renderTextBlock(6);
		explanation.text(explanationText, width - 2 * MARGIN);
		explanation.setPos(MARGIN, title.bottom() + MARGIN);
		add(explanation);

		RenderedTextBlock capacity = PixelScene.renderTextBlock(6);
		capacity.text(capacityText, width - 2 * MARGIN);
		capacity.hardlight(valid ? TITLE_COLOR : 0xFF4444);
		capacity.setPos(MARGIN, explanation.bottom() + MARGIN);
		add(capacity);

		Component content = new Component();
		RenderedTextBlock body = PixelScene.renderTextBlock(6);
		body.text(preview, width - 2 * MARGIN);
		body.setPos(MARGIN, 0);
		content.add(body);
		content.setRect(0, 0, width, body.bottom() + MARGIN);
		pane = new ScrollPane(content);
		add(pane);
		float paneTop = capacity.bottom() + MARGIN;
		pane.setRect(0, paneTop, width,
				Math.max(24, height - 2 * BUTTON_HEIGHT - 3 * MARGIN - paneTop));

		float rowOne = height - 2 * BUTTON_HEIGHT - MARGIN;
		float half = (width - 3 * MARGIN) / 2f;
		RedButton previous = button(previousText, new Runnable() {
			@Override public void run() { hide(); onPrevious(); }
		});
		previous.setRect(MARGIN, rowOne, half, BUTTON_HEIGHT);
		RedButton modify = button(modifyText, new Runnable() {
			@Override public void run() { hide(); onModify(); }
		});
		modify.setRect(previous.right() + MARGIN, rowOne, half, BUTTON_HEIGHT);

		RedButton details = button(detailsText, new Runnable() {
			@Override public void run() { onDetails(); }
		});
		details.setRect(MARGIN, rowOne + BUTTON_HEIGHT + MARGIN, half, BUTTON_HEIGHT);
		RedButton create = button(createText, new Runnable() {
			@Override public void run() { hide(); onCreate(); }
		});
		create.setRect(details.right() + MARGIN, rowOne + BUTTON_HEIGHT + MARGIN,
				half, BUTTON_HEIGHT);
		create.enable(valid);
	}

	private RedButton button(String text, final Runnable action) {
		RedButton button = new RedButton(text, 7) {
			@Override protected void onClick() { action.run(); }
		};
		button.multiline = true;
		add(button);
		return button;
	}

	@Override
	public void offset(int xOffset, int yOffset) {
		super.offset(xOffset, yOffset);
		pane.setRect(pane.left(), pane.top(), pane.width(), pane.height());
	}

	@Override
	public void onBackPressed() {
		hide();
		onPrevious();
	}

	protected abstract void onPrevious();
	protected abstract void onModify();
	protected abstract void onDetails();
	protected abstract void onCreate();
}
