package com.tunnelminer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class TunnelMinerScreen extends Screen {

    private TextFieldWidget homeNameField;
    private TextFieldWidget durationField;

    private static final int BUTTON_WIDTH  = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int FIELD_WIDTH   = 150;
    private static final int FIELD_HEIGHT  = 20;
    private static final int ROW_GAP       = 30;

    public TunnelMinerScreen() {
        super(Text.literal("TunnelMiner"));
    }

    @Override
    protected void init() {
        int cx   = this.width / 2;
        int left = cx - BUTTON_WIDTH - 5;
        int right = cx + 5;
        int y = 24;

        // Row 1 — Set point buttons
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Set Point A"), btn -> {
            if (this.client != null && this.client.player != null)
                MinerState.getInstance().pointA = this.client.player.getPos();
        }).dimensions(left, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Set Point B"), btn -> {
            if (this.client != null && this.client.player != null)
                MinerState.getInstance().pointB = this.client.player.getPos();
        }).dimensions(right, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        // Row 3 — text fields (row 2 = coordinate labels, drawn in render())
        int fieldY = y + BUTTON_HEIGHT + 22 + 12; // button + coord line + label

        homeNameField = new TextFieldWidget(this.textRenderer, left, fieldY, FIELD_WIDTH, FIELD_HEIGHT, Text.literal("Home Name"));
        homeNameField.setMaxLength(64);
        homeNameField.setText(MinerState.getInstance().homeName);
        homeNameField.setPlaceholder(Text.literal("home name"));
        this.addDrawableChild(homeNameField);

        durationField = new TextFieldWidget(this.textRenderer, right, fieldY, FIELD_WIDTH, FIELD_HEIGHT, Text.literal("Duration"));
        durationField.setMaxLength(6);
        durationField.setText(String.valueOf(MinerState.getInstance().durationMinutes));
        durationField.setPlaceholder(Text.literal("minutes"));
        this.addDrawableChild(durationField);

        // Row 4 — Start / Stop
        int actionY = fieldY + FIELD_HEIGHT + 10;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Start"), btn -> {
            MinerState state = MinerState.getInstance();
            if (state.pointA == null || state.pointB == null) {
                state.setStatusMessage("Error: Set both points first");
                return;
            }
            String name = homeNameField.getText().trim();
            if (name.isEmpty()) { state.setStatusMessage("Error: Home name empty"); return; }
            int dur;
            try {
                dur = Integer.parseInt(durationField.getText().trim());
                if (dur <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                state.setStatusMessage("Error: Invalid duration");
                return;
            }
            state.homeName = name;
            state.durationMinutes = dur;
            if (this.client != null) { state.startMining(this.client); this.close(); }
        }).dimensions(left, actionY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Stop"), btn ->
                MinerState.getInstance().stopMining("Manual stop")
        ).dimensions(right, actionY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        // Row 5 — Close
        int closeY = actionY + BUTTON_HEIGHT + 8;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), btn ->
                this.close()
        ).dimensions(cx - BUTTON_WIDTH / 2, closeY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        int cx    = this.width / 2;
        int left  = cx - BUTTON_WIDTH - 5;
        int right = cx + 5;
        int y     = 24;

        // Title
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, 8, 0xFFFFFF);

        // Coordinate lines under each button
        int coordY = y + BUTTON_HEIGHT + 4;
        MinerState state = MinerState.getInstance();

        String aText = state.pointA == null
                ? "Not set"
                : String.format("§a%.1f, %.1f, %.1f", state.pointA.x, state.pointA.y, state.pointA.z);
        String bText = state.pointB == null
                ? "Not set"
                : String.format("§a%.1f, %.1f, %.1f", state.pointB.x, state.pointB.y, state.pointB.z);

        ctx.drawTextWithShadow(this.textRenderer, Text.literal("A: " + aText), left, coordY, 0xAAAAAA);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("B: " + bText), right, coordY, 0xAAAAAA);

        // Field labels
        int fieldY = y + BUTTON_HEIGHT + 22 + 12;
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("Home Name:"),   left,  fieldY - 10, 0xFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("Duration (min):"), right, fieldY - 10, 0xFFFFFF);

        // Status
        int actionY = fieldY + FIELD_HEIGHT + 10;
        int statusY = actionY + BUTTON_HEIGHT + 8 + BUTTON_HEIGHT + 6;
        String status = state.getStatusMessage();
        int color = status.startsWith("Error") ? 0xFF5555 : state.isRunning() ? 0x55FF55 : 0xFFFF55;
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Status: " + status), cx, statusY, color);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
