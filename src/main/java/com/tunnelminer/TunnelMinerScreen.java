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


    public TunnelMinerScreen() {
        super(Text.literal("TunnelMiner"));
    }

    private boolean skipBg = false;

    // Row Y positions — computed once so render() and init() agree
    private int rowButtons() { return 24; }
    private int rowCoords()  { return rowButtons() + BUTTON_HEIGHT + 4; }
    private int rowLabels()  { return rowCoords()  + 12; }
    private int rowFields()  { return rowLabels()  + 11; }
    private int rowAction()  { return rowFields()  + FIELD_HEIGHT + 8; }
    private int rowClose()   { return rowAction()  + BUTTON_HEIGHT + 6; }
    private int rowStatus()  { return rowClose()   + BUTTON_HEIGHT + 8; }

    @Override
    protected void init() {
        int cx    = this.width / 2;
        int left  = cx - BUTTON_WIDTH - 5;
        int right = cx + 5;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Set Point A"), btn -> {
            if (this.client != null && this.client.player != null)
                MinerState.getInstance().pointA = this.client.player.getPos();
        }).dimensions(left, rowButtons(), BUTTON_WIDTH, BUTTON_HEIGHT).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Set Point B"), btn -> {
            if (this.client != null && this.client.player != null)
                MinerState.getInstance().pointB = this.client.player.getPos();
        }).dimensions(right, rowButtons(), BUTTON_WIDTH, BUTTON_HEIGHT).build());

        homeNameField = new TextFieldWidget(this.textRenderer, left, rowFields(), FIELD_WIDTH, FIELD_HEIGHT, Text.literal("Home Name"));
        homeNameField.setMaxLength(64);
        homeNameField.setText(MinerState.getInstance().homeName);
        homeNameField.setPlaceholder(Text.literal("home name"));
        this.addDrawableChild(homeNameField);

        durationField = new TextFieldWidget(this.textRenderer, right, rowFields(), FIELD_WIDTH, FIELD_HEIGHT, Text.literal("Duration"));
        durationField.setMaxLength(6);
        durationField.setText(String.valueOf(MinerState.getInstance().durationMinutes));
        durationField.setPlaceholder(Text.literal("minutes"));
        this.addDrawableChild(durationField);

        int cx2 = this.width / 2;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Start"), btn -> {
            MinerState state = MinerState.getInstance();
            if (state.pointA == null || state.pointB == null) { state.setStatusMessage("Error: Set both points first"); return; }
            String name = homeNameField.getText().trim();
            if (name.isEmpty()) { state.setStatusMessage("Error: Home name empty"); return; }
            int dur;
            try { dur = Integer.parseInt(durationField.getText().trim()); if (dur <= 0) throw new NumberFormatException(); }
            catch (NumberFormatException e) { state.setStatusMessage("Error: Invalid duration"); return; }
            state.homeName = name;
            state.durationMinutes = dur;
            if (this.client != null) { state.startMining(this.client); this.close(); }
        }).dimensions(cx2 - BUTTON_WIDTH - 5, rowAction(), BUTTON_WIDTH, BUTTON_HEIGHT).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Stop"), btn ->
                MinerState.getInstance().stopMining("Manual stop")
        ).dimensions(cx2 + 5, rowAction(), BUTTON_WIDTH, BUTTON_HEIGHT).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), btn ->
                this.close()
        ).dimensions(cx2 - BUTTON_WIDTH / 2, rowClose(), BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        if (skipBg) return;
        context.fill(0, 0, this.width, this.height, 0xB0000000);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Draw background once, then block the second call from super.render()
        renderBackground(ctx, mouseX, mouseY, delta);
        skipBg = true;
        super.render(ctx, mouseX, mouseY, delta);
        skipBg = false;

        // Draw text AFTER super.render() so it appears on top of the dark overlay
        // (widgets are already drawn, text renders above them — acceptable since
        //  labels sit in rows that don't overlap with any button or field)
        int cx    = this.width / 2;
        int left  = cx - BUTTON_WIDTH - 5;
        int right = cx + 5;

        MinerState state = MinerState.getInstance();

        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, 8, 0xFFFFFF);

        // Coordinate display — plain color integers, no § codes
        if (state.pointA == null) {
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("A: Not set"), left, rowCoords(), 0xFF5555);
        } else {
            ctx.drawTextWithShadow(this.textRenderer,
                Text.literal(String.format("A: %.1f / %.1f / %.1f", state.pointA.x, state.pointA.y, state.pointA.z)),
                left, rowCoords(), 0x55FF55);
        }
        if (state.pointB == null) {
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("B: Not set"), right, rowCoords(), 0xFF5555);
        } else {
            ctx.drawTextWithShadow(this.textRenderer,
                Text.literal(String.format("B: %.1f / %.1f / %.1f", state.pointB.x, state.pointB.y, state.pointB.z)),
                right, rowCoords(), 0x55FF55);
        }

        ctx.drawTextWithShadow(this.textRenderer, Text.literal("Home Name:"),      left,  rowLabels(), 0xFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("Duration (min):"), right, rowLabels(), 0xFFFFFF);

        String status = state.getStatusMessage();
        int color = status.startsWith("Error") ? 0xFF5555 : state.isRunning() ? 0x55FF55 : 0xFFFF55;
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Status: " + status), cx, rowStatus(), color);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
