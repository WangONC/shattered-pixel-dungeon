package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.IconButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.ui.Component;

import java.util.HashMap;

/**
 * Builder-only selection window. Its header and navigation stay visible while an arbitrary
 * number of compact choices scroll in the middle. It contains no Rule Runtime knowledge.
 */
public abstract class WndBuilderStep extends Window {

	private static final int WIDTH_P = 136;
	private static final int WIDTH_L = 166;
	private static final int HEIGHT_P = 190;
	private static final int HEIGHT_L = 138;
	private static final int MARGIN = 2;
	private static final int BUTTON_HEIGHT = 16;
	private static final int INFO_WIDTH = 16;

	public static final class Option {
		public final String label;
		public final String detailTitle;
		public final String detail;
		public final boolean enabled;
		public final String disabledReason;

		public Option(String label, String detailTitle, String detail, boolean enabled) {
			this(label, detailTitle, detail, enabled, null);
		}

		public Option(String label, String detailTitle, String detail, boolean enabled, String disabledReason) {
			this.label = label;
			this.detailTitle = detailTitle;
			this.detail = !enabled && disabledReason != null && !disabledReason.isEmpty()
					? detail + "\n\n" + disabledReason : detail;
			this.enabled = enabled;
			this.disabledReason = disabledReason;
		}
	}

	private static final HashMap<String, Float> SCROLL_MEMORY = new HashMap<>();

	private final int windowWidth;
	private final int windowHeight;
	private final Option[] options;
	private final RedButton[] optionButtons;
	private final RenderedTextBlock title;
	private final RenderedTextBlock explanation;
	private final RenderedTextBlock buildStatus;
	private final RenderedTextBlock capacityStatus;
	private final ScrollPane pane;
	private final RedButton next;
	private final String scrollKey;
	private int selected;

	public WndBuilderStep(String titleText, String explanationText, String buildText,
			String capacityText, boolean capacityValid, Option[] options, int selected,
			String previousText, String nextText) {
		windowWidth = PixelScene.landscape() ? WIDTH_L : WIDTH_P;
		windowHeight = Math.min(PixelScene.landscape() ? HEIGHT_L : HEIGHT_P,
				(int) PixelScene.uiCamera.height - 12);
		this.options = options;
		this.scrollKey = titleText;
		this.selected = selected;
		this.optionButtons = new RedButton[options.length];
		resize(windowWidth, windowHeight);

		title = PixelScene.renderTextBlock(titleText, 9);
		title.hardlight(TITLE_COLOR);
		title.maxWidth(windowWidth - 2 * MARGIN);
		add(title);

		explanation = PixelScene.renderTextBlock(6);
		explanation.text(explanationText, windowWidth - 2 * MARGIN);
		add(explanation);

		buildStatus = PixelScene.renderTextBlock(5);
		buildStatus.text(buildText, windowWidth - 2 * MARGIN);
		add(buildStatus);

		capacityStatus = PixelScene.renderTextBlock(6);
		capacityStatus.text(capacityText, windowWidth - 2 * MARGIN);
		capacityStatus.hardlight(capacityValid ? TITLE_COLOR : 0xFF4444);
		add(capacityStatus);

		Component content = new Component();
		pane = new ScrollPane(content);
		add(pane);
		layoutOptions(content);

		RedButton previous = new RedButton(previousText, 7) {
			@Override protected void onClick() {
				rememberScroll();
				hide();
				onPrevious();
			}
		};
		add(previous);
		previous.setRect(MARGIN, windowHeight - BUTTON_HEIGHT,
				(windowWidth - 3 * MARGIN) / 2f, BUTTON_HEIGHT);

		next = new RedButton(nextText, 7) {
			@Override protected void onClick() {
				if (WndBuilderStep.this.selected < 0) return;
				rememberScroll();
				hide();
				onNext(WndBuilderStep.this.selected);
			}
		};
		add(next);
		next.setRect(previous.right() + MARGIN, windowHeight - BUTTON_HEIGHT,
				(windowWidth - 3 * MARGIN) / 2f, BUTTON_HEIGHT);

		layoutHeader(buildText, capacityText, capacityValid);
		refreshSelection();
	}

	private void layoutOptions(Component content) {
		float pos = 0;
		for (int i = 0; i < options.length; i++) {
			final int index = i;
			RedButton button = new RedButton(options[i].label, 6) {
				@Override protected void onClick() {
					if (!WndBuilderStep.this.options[index].enabled) return;
					WndBuilderStep.this.selected = index;
					onSelect(index);
					refreshSelection();
				}
			};
			button.multiline = true;
			button.leftJustify = true;
			content.add(button);
			button.setRect(0, pos, windowWidth - INFO_WIDTH - MARGIN, BUTTON_HEIGHT);
			float height = Math.max(BUTTON_HEIGHT, button.reqHeight());
			button.setRect(0, pos, windowWidth - INFO_WIDTH - MARGIN, height);
			button.enable(options[i].enabled);
			button.passScrollGestures();
			optionButtons[i] = button;

			IconButton info = new IconButton(Icons.get(Icons.INFO)) {
				@Override protected void onClick() {
					ShatteredPixelDungeon.scene().addToFront(new WndTitledMessage(
							new IconTitle(Icons.INFO.get(), WndBuilderStep.this.options[index].detailTitle),
							WndBuilderStep.this.options[index].detail));
				}
			};
			content.add(info);
			info.passScrollGestures();
			info.setRect(windowWidth - INFO_WIDTH, pos + (height - INFO_WIDTH) / 2f,
					INFO_WIDTH, INFO_WIDTH);
			pos += height + MARGIN;
		}
		content.setRect(0, 0, windowWidth, Math.max(1, pos - MARGIN));
	}

	private void refreshSelection() {
		for (int i = 0; i < optionButtons.length; i++) {
			optionButtons[i].textColor(i == selected ? TITLE_COLOR : WHITE);
		}
		next.enable(selected >= 0 && selected < options.length && options[selected].enabled);
	}

	public final void refreshHeader(String buildText, String capacityText, boolean capacityValid) {
		layoutHeader(buildText, capacityText, capacityValid);
	}

	private void layoutHeader(String buildText, String capacityText, boolean capacityValid) {
		buildStatus.text(buildText, windowWidth - 2 * MARGIN);
		capacityStatus.text(capacityText, windowWidth - 2 * MARGIN);
		capacityStatus.hardlight(capacityValid ? TITLE_COLOR : 0xFF4444);

		float pos = MARGIN;
		title.setPos(MARGIN, pos);
		pos = title.bottom() + MARGIN;
		explanation.setPos(MARGIN, pos);
		pos = explanation.bottom() + MARGIN;
		buildStatus.setPos(MARGIN, pos);
		pos = buildStatus.bottom() + MARGIN;
		capacityStatus.setPos(MARGIN, pos);
		pos = capacityStatus.bottom() + MARGIN;
		float paneHeight = Math.max(24, windowHeight - BUTTON_HEIGHT - 2 * MARGIN - pos);
		pane.setRect(0, pos, windowWidth, paneHeight);
		Float restored = SCROLL_MEMORY.get(scrollKey);
		if (restored != null) pane.scrollTo(0, restored);
	}

	private void rememberScroll() {
		SCROLL_MEMORY.put(scrollKey, pane.scrollY());
	}

	@Override
	public void offset(int xOffset, int yOffset) {
		super.offset(xOffset, yOffset);
		pane.setRect(pane.left(), pane.top(), pane.width(), pane.height());
	}

	@Override
	public void onBackPressed() {
		rememberScroll();
		hide();
		onPrevious();
	}

	protected abstract void onSelect(int index);
	protected abstract void onNext(int index);
	protected abstract void onPrevious();
}
