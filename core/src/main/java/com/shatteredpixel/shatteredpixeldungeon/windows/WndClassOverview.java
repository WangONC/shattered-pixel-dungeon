package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.rules.CustomClassConfig;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassBuild;
import com.shatteredpixel.shatteredpixeldungeon.rules.PlayerFacingClassBuildFormatter;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleRuntime;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.ui.Component;

/** Scrollable, player-facing class build sheet. It deliberately contains no runtime debug data. */
public class WndClassOverview extends Window {

	private static final int WIDTH_P = 120;
	private static final int WIDTH_L = 160;
	private static final int HEIGHT = 170;
	private final ScrollPane pane;

	public WndClassOverview(RuleRuntime runtime) {
		this(runtime == null ? null : runtime.presentationBuild());
	}

	public WndClassOverview(CustomClassConfig config) {
		this(config == null ? null : config.toClassBuild());
	}

	public WndClassOverview(ClassBuild config) {
		int width = PixelScene.landscape() ? WIDTH_L : WIDTH_P;
		resize(width, HEIGHT);

		String titleText = config == null ? Messages.get(this, "title") : config.name;
		IconTitle title = new IconTitle(Icons.TALENT.get(), titleText);
		title.setRect(0, 0, width, 0);
		add(title);

		RenderedTextBlock type = PixelScene.renderTextBlock(Messages.get(this, "type_label"), 6);
		type.hardlight(TITLE_COLOR);
		type.maxWidth(width);
		type.setPos(0, title.bottom() + 2);
		add(type);

		Component content = new Component();
		pane = new ScrollPane(content);
		float paneTop = type.bottom() + 3;
		add(pane);
		pane.setRect(0, paneTop, width, HEIGHT - paneTop);

		RenderedTextBlock body = PixelScene.renderTextBlock(6);
		body.text(PlayerFacingClassBuildFormatter.buildSheet(config), width - 2);
		body.setPos(0, 0);
		content.add(body);
		content.setRect(0, 0, width, body.bottom() + 2);
	}

	@Override
	public void offset(int xOffset, int yOffset) {
		super.offset(xOffset, yOffset);
		// ScrollPane owns a screen-space camera, so it must be laid out again
		// whenever the containing window moves (see WndHero/WndJournal).
		pane.setRect(pane.left(), pane.top(), pane.width(), pane.height());
	}
}
