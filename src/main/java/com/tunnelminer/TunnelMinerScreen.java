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

    // Prevents renderBackground from being triggered twice (once manually, once by super.render)
    private boolean bgRendered = false;

    public TunnelMinerScreen() {
        super(Text.literal("TunnelMiner"));
    }

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
        // Block double-blur: super.render() calls this automatically; we also call it
        // manually before drawing text. The flag ensures it only executes once per frame.
        if (bgRendered) return;
        bgRendered = true;
        super.renderBackground(context, mouseX, mouseY, delta);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        bgRendered = false;

        // 1. Draw background (blur + dark overlay)
        renderBackground(ctx, mouseX, mouseY, delta);

        int cx    = this.width / 2;
        int left  = cx - BUTTON_WIDTH - 5;
        int right = cx + 5;

        // 2. Draw custom text (behind widgets)
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, 8, 0xFFFFFF);

        MinerState state = MinerState.getInstance();

        String aCoord = state.pointA == null ? "§cNot set"
                : String.format("§a%.1f  %.1f  %.1f", state.pointA.x, state.pointA.y, state.pointA.z);
        String bCoord = state.pointB == null ? "§cNot set"
                : String.format("§a%.1f  %.1f  %.1f", state.pointB.x, state.pointB.y, state.pointB.z);

        ctx.drawTextWithShadow(this.textRenderer, Text.literal("A: ").append(Text.of(aCoord)), left,  rowCoords(), 0xFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("B: ").append(Text.of(bCoord)), right, rowCoords(), 0xFFFFFF);

        ctx.drawTextWithShadow(this.textRenderer, Text.literal("Home Name:"),      left,  rowLabels(), 0xFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("Duration (min):"), right, rowLabels(), 0xFFFFFF);

        String status = state.getStatusMessage();
        int color = status.startsWith("Error") ? 0xFF5555 : state.isRunning() ? 0x55FF55 : 0xFFFF55;
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Status: " + status), cx, rowStatus(), color);

        // 3. Render widgets on top (renderBackground inside is blocked by flag)
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
