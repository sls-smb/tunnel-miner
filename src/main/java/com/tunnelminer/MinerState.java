package com.tunnelminer;

import com.tunnelminer.mixin.KeyBindingAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

public class MinerState {

    public enum WaitPhase {
        NONE,
        WAITING_AT_B,
        WAITING_FOR_TELEPORT
    }

    private static final MinerState INSTANCE = new MinerState();

    // Mining points
    public Vec3d pointA = null;
    public Vec3d pointB = null;

    // Config
    public String homeName = "";
    public int durationMinutes = 60;

    // Runtime state
    public boolean running = false;
    public long startTime = 0L;
    public ItemStack heldItemAtStart = ItemStack.EMPTY;

    // Break management
    public long lastBreakTime = 0L;
    public boolean paused = false;
    public long pauseUntil = 0L;

    // Wait phase (tick-based delays)
    public int waitTicks = 0;
    public WaitPhase waitPhase = WaitPhase.NONE;

    // Input detection flags (set by mixins)
    public static volatile boolean keyPressedFlag = false;
    public static volatile boolean mouseMoved = false;

    // Status
    private String statusMessage = "Idle";

    private MinerState() {}

    public static MinerState getInstance() {
        return INSTANCE;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String msg) {
        this.statusMessage = msg;
    }

    public boolean isRunning() {
        return running;
    }

    public void reset() {
        running = false;
        startTime = 0L;
        heldItemAtStart = ItemStack.EMPTY;
        lastBreakTime = 0L;
        paused = false;
        pauseUntil = 0L;
        waitTicks = 0;
        waitPhase = WaitPhase.NONE;
        keyPressedFlag = false;
        mouseMoved = false;
        statusMessage = "Idle";
    }

    public void startMining(MinecraftClient client) {
        if (client.player == null) {
            statusMessage = "No player found";
            return;
        }
        reset();
        running = true;
        startTime = System.currentTimeMillis();
        lastBreakTime = startTime;

        // Copy current held item
        ItemStack current = client.player.getMainHandStack();
        heldItemAtStart = current.isEmpty() ? ItemStack.EMPTY : current.copy();

        statusMessage = "Mining...";
    }

    public void stopMining(String reason) {
        running = false;
        paused = false;
        waitTicks = 0;
        waitPhase = WaitPhase.NONE;
        statusMessage = "Stopped: " + reason;

        // Release simulated inputs
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options != null) {
            ((KeyBindingAccessor) client.options.forwardKey).setPressed(false);
            ((KeyBindingAccessor) client.options.attackKey).setPressed(false);
        }
    }
}
