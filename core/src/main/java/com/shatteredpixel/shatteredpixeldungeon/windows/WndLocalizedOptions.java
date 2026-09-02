package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.IconButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;

/**
 * A small WndOptions variant for localized rule UI. It keeps the existing SPD controls and
 * lets wrapped option labels increase their button height instead of escaping the button.
 */
public class WndLocalizedOptions extends WndOptions {

	public WndLocalizedOptions(String title, String message, String... options) {
		super(title, message, options);
	}

	@Override
	protected void layoutBody(float pos, String message, String... options) {
		int width = PixelScene.landscape() ? WIDTH_L : WIDTH_P;

		RenderedTextBlock body = PixelScene.renderTextBlock(6);
		body.text(message, width);
		body.setPos(0, pos);
		add(body);
		pos = body.bottom() + 2 * MARGIN;

		for (int i = 0; i < options.length; i++) {
			final int index = i;
			RedButton button = new RedButton(options[i]) {
				@Override
				protected void onClick() {
					hide();
					onSelect(index);
				}
			};
			button.multiline = true;
			if (hasIcon(i)) button.icon(getIcon(i));
			add(button);

			float buttonWidth = hasInfo(i) ? width - BUTTON_HEIGHT : width;
			button.setRect(0, pos, buttonWidth, BUTTON_HEIGHT);
			float buttonHeight = Math.max(BUTTON_HEIGHT, button.reqHeight());
			button.setRect(0, pos, buttonWidth, buttonHeight);

			if (hasInfo(i)) {
				IconButton info = new IconButton(Icons.get(Icons.INFO)) {
					@Override
					protected void onClick() {
						onInfo(index);
					}
				};
				info.setRect(width - BUTTON_HEIGHT, pos + (buttonHeight - BUTTON_HEIGHT) / 2f,
						BUTTON_HEIGHT, BUTTON_HEIGHT);
				add(info);
			}

			button.enable(enabled(i));
			pos += buttonHeight + MARGIN;
		}

		resize(width, (int)Math.ceil(pos - MARGIN));
	}
}
