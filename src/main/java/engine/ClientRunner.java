/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine;

import lwjgui.geometry.Insets;
import lwjgui.geometry.Pos;
import lwjgui.paint.Color;
import lwjgui.scene.control.Label;
import lwjgui.scene.layout.StackPane;

public abstract class ClientRunner extends ClientEngine {

	public ClientRunner(String... args) {
		super(args);
	}

	private Label fps;

	@Override
	public void setupEngine() {
		StackPane displayPane = renderThread.getPipeline().getDisplayPane();
		renderThread.getWindow().getScene().setRoot(displayPane);
		displayPane.getChildren().add(renderThread.getClientUI());

		fps = new Label("fps");
		fps.setTextFill(Color.WHITE);
		fps.setMouseTransparent(true);

		displayPane.getChildren().add(fps);
		displayPane.setAlignment(Pos.TOP_LEFT);
		displayPane.setPadding(new Insets(2, 2, 2, 2));

		// Tell the game to run
		InternalGameThread.runLater(() -> {
			loadScene(args);
			Game.load();
			Game.getGame().gameUpdate(true);
			Game.setRunning(true);
		});
		renderThread.getPipeline().setRenderableWorld(Game.workspace());
	}

	@Override
	public void render() {
		fps.setText(InternalRenderThread.fps + " fps");
	}

	@Override
	public void update() {
		// TODO Auto-generated method stub

	}

	public abstract void loadScene(String[] args);

}
