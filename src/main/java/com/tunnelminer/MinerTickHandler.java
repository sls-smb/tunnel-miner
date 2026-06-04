package com.tunnelminer;

import com.tunnelminer.mixin.KeyBindingAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public class MinerTickHandler {

    private static final Random RANDOM = new Random();

    // Break interval: ~10 minutes in ms, with ±30s variance
    private static final long BASE_BREAK_INTERVAL_MS = 10L * 60L * 1000L;
    private static final long BREAK_VARIANCE_MS = 30L * 1000L;

    // Pause duration: 15-30 seconds
    private static final long MIN_PAUSE_MS = 15L * 1000L;
    private static final long MAX_PAUSE_MS = 30L * 1000L;

    // Teleport wait timeout: 30 seconds in ticks (~20 ticks/s)
    private static final int TELEPORT_TIMEOUT_TICKS = 600;

    // Wait at point B: 2-5 seconds in ticks
    private static final int MIN_WAIT_B_TICKS = 40;
    private static final int MAX_WAIT_B_TICKS = 100;

    // Static: next break interval target (randomized each time)
    private static long nextBreakIntervalMs = randomBreakInterval();

    // Tick counter for teleport timeout
    private static int teleportTimeoutTicks = 0;

    public static void tick(MinecraftClient client) {
        MinerState state = MinerState.getInstance();

        if (!state.isRunning()) {
            return;
        }

        if (client.player == null || client.world == null) {
            state.stopMining("Player or world unavailable");
            return;
        }

        long currentTime = System.currentTimeMillis();

        // --- Stop condition 1: Duration elapsed ---
        if (currentTime - state.startTime > (long) state.durationMinutes * 60_000L) {
            state.stopMining("Duration elapsed");
            return;
        }

        // --- Stop condition 2: Held item changed ---
        ItemStack currentItem = client.player.getMainHandStack();
        if (!ItemStack.areItemsAndComponentsEqual(currentItem, state.heldItemAtStart)) {
            // Allow empty->empty (both empty is fine)
            boolean bothEmpty = currentItem.isEmpty() && state.heldItemAtStart.isEmpty();
            if (!bothEmpty) {
                state.stopMining("Held item changed");
                return;
            }
        }

        // --- Stop condition 3: Non-TunnelMiner screen is open ---
        if (client.currentScreen != null && !(client.currentScreen instanceof TunnelMinerScreen)) {
            state.stopMining("Screen opened");
            return;
        }

        // --- Stop condition 4: Window not focused ---
        if (!client.isWindowFocused()) {
            state.stopMining("Window lost focus");
            return;
        }

        // --- Stop condition 5: Player off corridor ---
        if (state.pointA != null && state.pointB != null) {
            Vec3d playerPos = client.player.getPos();
            double dist = distanceToSegment(playerPos, state.pointA, state.pointB);
            if (dist > 3.0) {
                state.stopMining("Player off corridor (dist=" + String.format("%.1f", dist) + ")");
                return;
            }
        }

        // --- Stop condition 6: Mouse moved ---
        if (MinerState.mouseMoved) {
            MinerState.mouseMoved = false;
            state.stopMining("Mouse moved");
            return;
        }

        // --- Stop condition 7: Keyboard input detected ---
        if (MinerState.keyPressedFlag) {
            MinerState.keyPressedFlag = false;
            state.stopMining("Keyboard input detected");
            return;
        }

        // --- Handle wait phases ---
        if (state.waitPhase == MinerState.WaitPhase.WAITING_AT_B) {
            state.waitTicks--;
            if (state.waitTicks <= 0) {
                // Send /home command
                if (client.player.networkHandler != null) {
                    client.player.networkHandler.sendChatCommand("home " + state.homeName);
                }
                state.waitPhase = MinerState.WaitPhase.WAITING_FOR_TELEPORT;
                teleportTimeoutTicks = TELEPORT_TIMEOUT_TICKS;
                state.setStatusMessage("Sent /home " + state.homeName + ", waiting for teleport...");
            }
            return;
        }

        if (state.waitPhase == MinerState.WaitPhase.WAITING_FOR_TELEPORT) {
            teleportTimeoutTicks--;

            Vec3d playerPos = client.player.getPos();
            // Check if player is near point A (within 5 blocks)
            if (state.pointA != null && playerPos.distanceTo(state.pointA) < 5.0) {
                state.waitPhase = MinerState.WaitPhase.NONE;
                state.setStatusMessage("Teleported back to A, resuming mining...");
                resumeInputs(client);
                return;
            }

            if (teleportTimeoutTicks <= 0) {
                state.stopMining("Teleport timeout");
                return;
            }
            return;
        }

        // --- Handle break pauses ---
        if (state.paused) {
            if (currentTime >= state.pauseUntil) {
                state.paused = false;
                state.lastBreakTime = currentTime;
                nextBreakIntervalMs = randomBreakInterval();
                state.setStatusMessage("Resuming after break...");
                resumeInputs(client);
            }
            // While paused, do nothing
            return;
        }

        // --- Check if it's time for a break ---
        if (currentTime - state.lastBreakTime > nextBreakIntervalMs) {
            long pauseDurationMs = MIN_PAUSE_MS + (long) (RANDOM.nextDouble() * (MAX_PAUSE_MS - MIN_PAUSE_MS));
            state.paused = true;
            state.pauseUntil = currentTime + pauseDurationMs;
            state.setStatusMessage("Taking a break for " + (pauseDurationMs / 1000) + "s...");
            releaseInputs(client);
            return;
        }

        // --- Normal mining logic ---
        if (state.pointA != null && state.pointB != null) {
            Vec3d playerPos = client.player.getPos();
            double distToB = playerPos.distanceTo(state.pointB);

            // Check if near point B
            if (distToB < 2.0) {
                releaseInputs(client);
                int waitTicks = MIN_WAIT_B_TICKS + RANDOM.nextInt(MAX_WAIT_B_TICKS - MIN_WAIT_B_TICKS + 1);
                state.waitTicks = waitTicks;
                state.waitPhase = MinerState.WaitPhase.WAITING_AT_B;
                state.setStatusMessage("Reached point B, waiting " + waitTicks + " ticks before teleporting...");
                return;
            }
        }

        // Press forward key and attack key to mine
        ((KeyBindingAccessor) client.options.forwardKey).setPressed(true);
        ((KeyBindingAccessor) client.options.attackKey).setPressed(true);
        state.setStatusMessage("Mining...");
    }

    private static void releaseInputs(MinecraftClient client) {
        ((KeyBindingAccessor) client.options.forwardKey).setPressed(false);
        ((KeyBindingAccessor) client.options.attackKey).setPressed(false);
    }

    private static void resumeInputs(MinecraftClient client) {
        ((KeyBindingAccessor) client.options.forwardKey).setPressed(true);
        ((KeyBindingAccessor) client.options.attackKey).setPressed(true);
    }

    /**
     * Computes the shortest distance from point P to the line segment AB.
     */
    private static double distanceToSegment(Vec3d p, Vec3d a, Vec3d b) {
        Vec3d ab = b.subtract(a);
        Vec3d ap = p.subtract(a);

        double abLenSq = ab.lengthSquared();
        if (abLenSq < 1e-10) {
            // A and B are the same point
            return p.distanceTo(a);
        }

        double t = ap.dotProduct(ab) / abLenSq;
        t = Math.max(0.0, Math.min(1.0, t));

        Vec3d closest = a.add(ab.multiply(t));
        return p.distanceTo(closest);
    }

    private static long randomBreakInterval() {
        long variance = (long) (RANDOM.nextDouble() * 2 * BREAK_VARIANCE_MS) - BREAK_VARIANCE_MS;
        return BASE_BREAK_INTERVAL_MS + variance;
    }
}
