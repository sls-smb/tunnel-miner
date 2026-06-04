package com.tunnelminer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class TunnelMinerScreen extends Screen {

    private TextFieldWidget homeNameField;
    private TextFieldWidget durationField;

    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;
    private static final int FIELD_WIDTH = 120;
    private static final int FIELD_HEIGHT = 20;
    private static final int PADDING = 6;

    public TunnelMinerScreen() {
        super(Text.literal("TunnelMiner"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = 30;
        int col1X = centerX - 130;
        int col2X = centerX + 10;

        // --- Point A button ---
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Set Point A"), button -> {
            if (this.client != null && this.client.player != null) {
                MinerState.getInstance().pointA = this.client.player.getPos();
            }
        }).dimensions(col1X, startY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        // --- Point B button ---
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Set Point B"), button -> {
            if (this.client != null && this.client.player != null) {
                MinerState.getInstance().pointB = this.client.player.getPos();
            }
        }).dimensions(col2X, startY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        int fieldY = startY + BUTTON_HEIGHT + PADDING + 14;

        // --- Home Name field ---
        homeNameField = new TextFieldWidget(
                this.textRenderer,
                col1X, fieldY,
                FIELD_WIDTH, FIELD_HEIGHT,
                Text.literal("Home Name")
        );
        homeNameField.setMaxLength(64);
        homeNameField.setText(MinerState.getInstance().homeName);
        homeNameField.setPlaceholder(Text.literal("home name"));
        this.addDrawableChild(homeNameField);

        // --- Duration field ---
        durationField = new TextFieldWidget(
                this.textRenderer,
                col2X, fieldY,
                FIELD_WIDTH, FIELD_HEIGHT,
                Text.literal("Duration (min)")
        );
        durationField.setMaxLength(6);
        durationField.setText(String.valueOf(MinerState.getInstance().durationMinutes));
        durationField.setPlaceholder(Text.literal("minutes"));
        this.addDrawableChild(durationField);

        int actionY = fieldY + FIELD_HEIGHT + PADDING + 14;

        // --- Start button ---
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Start"), button -> {
            MinerState state = MinerState.getInstance();

            if (state.pointA == null || state.pointB == null) {
                state.setStatusMessage("Error: Set both Point A and Point B first");
                return;
            }

            String homeName = homeNameField.getText().trim();
            if (homeName.isEmpty()) {
                state.setStatusMessage("Error: Home name cannot be empty");
                return;
            }

            int duration;
            try {
                duration = Integer.parseInt(durationField.getText().trim());
                if (duration <= 0) throw new NumberFormatException("Non-positive");
            } catch (NumberFormatException e) {
                state.setStatusMessage("Error: Invalid duration");
                return;
            }

            state.homeName = homeName;
            state.durationMinutes = duration;

            if (this.client != null) {
                state.startMining(this.client);
                this.close();
            }
        }).dimensions(col1X, actionY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        // --- Stop button ---
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Stop"), button -> {
            MinerState.getInstance().stopMining("Manual stop");
        }).dimensions(col2X, actionY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        int closeY = actionY + BUTTON_HEIGHT + PADDING;

        // --- Close button ---
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> {
            this.close();
        }).dimensions(centerX - BUTTON_WIDTH / 2, closeY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        MinerState state = MinerState.getInstance();

        // Title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, 10, 0xFFFFFF);

        int col1X = centerX - 130;
        int col2X = centerX + 10;
        int startY = 30;

        // Point A coordinate label
        String pointAText = state.pointA == null
                ? "Point A: Not set"
                : String.format("Point A: %.1f, %.1f, %.1f", state.pointA.x, state.pointA.y, state.pointA.z);
        context.drawTextWithShadow(this.textRenderer, Text.literal(pointAText), col1X, startY + BUTTON_HEIGHT + 2, 0xAAAAAA);

        // Point B coordinate label
        String pointBText = state.pointB == null
                ? "Point B: Not set"
                : String.format("Point B: %.1f, %.1f, %.1f", state.pointB.x, state.pointB.y, state.pointB.z);
        context.drawTextWithShadow(this.textRenderer, Text.literal(pointBText), col2X, startY + BUTTON_HEIGHT + 2, 0xAAAAAA);

        int fieldY = startY + BUTTON_HEIGHT + PADDING + 14;

        // Home Name label
        context.drawTextWithShadow(this.textRenderer, Text.literal("Home Name:"), col1X, fieldY - 10, 0xFFFFFF);

        // Duration label
        context.drawTextWithShadow(this.textRenderer, Text.literal("Duration (min):"), col2X, fieldY - 10, 0xFFFFFF);

        // Status message
        int statusY = fieldY + FIELD_HEIGHT + PADDING + 14 + BUTTON_HEIGHT + PADDING + BUTTON_HEIGHT + PADDING + 14;
        String status = state.getStatusMessage();
        int statusColor = status.startsWith("Error") ? 0xFF4444 : (state.isRunning() ? 0x44FF44 : 0xFFFFAA);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Status: " + status), centerX, statusY, statusColor);

        // Draw text fields
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
