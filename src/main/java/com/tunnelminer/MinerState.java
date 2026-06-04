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

    // Mining corridor endpoints
    public Vec3d pointA = null;
    public Vec3d pointB = null;

    // Configuration
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

    // Tick-based wait phase (used for delays without Thread.sleep)
    public int waitTicks = 0;
    public WaitPhase waitPhase = WaitPhase.NONE;

    // Mixin-set flags for detecting external input
    public static volatile boolean keyPressedFlag = false;
    public static volatile boolean mouseMoved = false;

    // Status displayed in the screen
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

    /**
     * Resets all runtime state back to defaults without clearing pointA/B or config.
     */
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

    /**
     * Starts the mining session. Snapshots the currently held item for change detection.
     */
    public void startMining(MinecraftClient client) {
        if (client.player == null) {
            statusMessage = "No player found";
            return;
        }
        reset();
        running = true;
        startTime = System.currentTimeMillis();
        lastBreakTime = startTime;

        ItemStack current = client.player.getMainHandStack();
        heldItemAtStart = current.isEmpty() ? ItemStack.EMPTY : current.copy();

        statusMessage = "Mining...";
    }

    /**
     * Stops the mining session, releases all simulated key presses, and records the stop reason.
     */
    public void stopMining(String reason) {
        running = false;
        paused = false;
        waitTicks = 0;
        waitPhase = WaitPhase.NONE;
        statusMessage = "Stopped: " + reason;

        // Release simulated inputs
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.options != null) {
            ((KeyBindingAccessor) client.options.forwardKey).setPressed(false);
            ((KeyBindingAccessor) client.options.attackKey).setPressed(false);
        }
    }
}
