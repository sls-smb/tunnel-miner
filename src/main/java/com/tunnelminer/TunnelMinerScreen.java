package com.tunnelminer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class TunnelMinerScreen extends Screen {

    private TextFieldWidget homeNameField;
    private TextFieldWidget durationField;
    private ButtonWidget btnA;
    private ButtonWidget btnB;
    private ButtonWidget btnStatus;

    private static final int BUTTON_WIDTH  = 180;
    private static final int BUTTON_HEIGHT = 20;
    private static final int FIELD_WIDTH   = 180;
    private static final int FIELD_HEIGHT  = 20;
    private static final int GAP           = 8;

    private boolean skipBg = false;

    public TunnelMinerScreen() {
        super(Text.literal("TunnelMiner"));
    }

    private int left()  { return this.width / 2 - BUTTON_WIDTH - 5; }
    private int right() { return this.width / 2 + 5; }

    @Override
    protected void init() {
        int left  = left();
        int right = right();
        int y = 30;

        // Row 1 — Set point buttons (label updates to show coords after click)
        btnA = ButtonWidget.builder(labelA(), btn -> {
            if (this.client != null && this.client.player != null) {
                MinerState.getInstance().pointA = this.client.player.getPos();
                btn.setMessage(labelA());
            }
        }).dimensions(left, y, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        this.addDrawableChild(btnA);

        btnB = ButtonWidget.builder(labelB(), btn -> {
            if (this.client != null && this.client.player != null) {
                MinerState.getInstance().pointB = this.client.player.getPos();
                btn.setMessage(labelB());
            }
        }).dimensions(right, y, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        this.addDrawableChild(btnB);

        y += BUTTON_HEIGHT + GAP;

        // Row 2 — text fields with inline labels as placeholder
        homeNameField = new TextFieldWidget(this.textRenderer, left, y, FIELD_WIDTH, FIELD_HEIGHT, Text.literal("Home Name"));
        homeNameField.setMaxLength(64);
        homeNameField.setText(MinerState.getInstance().homeName);
        homeNameField.setPlaceholder(Text.literal("Home name (ex: home)"));
        this.addDrawableChild(homeNameField);

        durationField = new TextFieldWidget(this.textRenderer, right, y, FIELD_WIDTH, FIELD_HEIGHT, Text.literal("Duration"));
        durationField.setMaxLength(6);
        durationField.setText(String.valueOf(MinerState.getInstance().durationMinutes));
        durationField.setPlaceholder(Text.literal("Duration in minutes"));
        this.addDrawableChild(durationField);

        y += FIELD_HEIGHT + GAP;

        // Row 3 — Start / Stop
        int cx = this.width / 2;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Start"), btn -> {
            MinerState state = MinerState.getInstance();
            if (state.pointA == null || state.pointB == null) { state.setStatusMessage("Error: Set both points first"); btnStatus.setMessage(statusLabel()); return; }
            String name = homeNameField.getText().trim();
            if (name.isEmpty()) { state.setStatusMessage("Error: Home name empty"); btnStatus.setMessage(statusLabel()); return; }
            int dur;
            try { dur = Integer.parseInt(durationField.getText().trim()); if (dur <= 0) throw new NumberFormatException(); }
            catch (NumberFormatException e) { state.setStatusMessage("Error: Invalid duration"); btnStatus.setMessage(statusLabel()); return; }
            state.homeName = name;
            state.durationMinutes = dur;
            if (this.client != null) { state.startMining(this.client); this.close(); }
        }).dimensions(left, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Stop"), btn -> {
            MinerState.getInstance().stopMining("Manual stop");
            if (btnStatus != null) btnStatus.setMessage(statusLabel());
        }).dimensions(right, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        y += BUTTON_HEIGHT + GAP;

        // Row 4 — Status (read-only button used as a label so it's always rendered)
        btnStatus = ButtonWidget.builder(statusLabel(), btn -> {}).dimensions(cx - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        btnStatus.active = false;
        this.addDrawableChild(btnStatus);

        y += BUTTON_HEIGHT + GAP;

        // Row 5 — Close
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), btn ->
                this.close()
        ).dimensions(cx - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    private Text labelA() {
        MinerState s = MinerState.getInstance();
        return s.pointA == null
                ? Text.literal("Set Point A")
                : Text.literal(String.format("A: %.0f / %.0f / %.0f", s.pointA.x, s.pointA.y, s.pointA.z));
    }

    private Text labelB() {
        MinerState s = MinerState.getInstance();
        return s.pointB == null
                ? Text.literal("Set Point B")
                : Text.literal(String.format("B: %.0f / %.0f / %.0f", s.pointB.x, s.pointB.y, s.pointB.z));
    }

    private Text statusLabel() {
        return Text.literal("Status: " + MinerState.getInstance().getStatusMessage());
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        if (skipBg) return;
        context.fill(0, 0, this.width, this.height, 0xB0000000);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        skipBg = true;
        super.render(ctx, mouseX, mouseY, delta);
        skipBg = false;

        // Keep status button text fresh every frame
        if (btnStatus != null) btnStatus.setMessage(statusLabel());
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
