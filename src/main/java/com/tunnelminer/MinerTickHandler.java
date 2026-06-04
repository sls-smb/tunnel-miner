package com.tunnelminer;

import com.tunnelminer.mixin.KeyBindingAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/**
 * Called every client tick to drive the automated tunnel mining logic.
 * All delays are handled via tick counters rather than Thread.sleep.
 */
public class MinerTickHandler {

    private static final Random RANDOM = new Random();

    // Break interval: ~10 minutes (ms), ±30 s variance
    private static final long BASE_BREAK_INTERVAL_MS = 10L * 60L * 1_000L;
    private static final long BREAK_VARIANCE_MS       = 30L * 1_000L;

    // Break pause duration: 15–30 seconds
    private static final long MIN_PAUSE_MS = 15L * 1_000L;
    private static final long MAX_PAUSE_MS = 30L * 1_000L;

    // How many ticks to wait after teleport command before giving up (~30 s at 20 tps)
    private static final int TELEPORT_TIMEOUT_TICKS = 600;

    // Ticks to wait at point B before issuing /home (2–5 seconds at 20 tps)
    private static final int MIN_WAIT_B_TICKS = 40;
    private static final int MAX_WAIT_B_TICKS = 100;

    // Next randomised break interval (re-randomised after each break)
    private static long nextBreakIntervalMs = randomBreakInterval();

    // Countdown for teleport timeout
    private static int teleportTimeoutTicks = 0;

    // -------------------------------------------------------------------------

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

        // ------------------------------------------------------------------
        // Stop condition 1: Duration elapsed
        // ------------------------------------------------------------------
        if (currentTime - state.startTime > (long) state.durationMinutes * 60_000L) {
            state.stopMining("Duration elapsed");
            return;
        }

        // ------------------------------------------------------------------
        // Stop condition 2: Held item changed
        // ------------------------------------------------------------------
        ItemStack currentItem = client.player.getMainHandStack();
        boolean bothEmpty = currentItem.isEmpty() && state.heldItemAtStart.isEmpty();
        if (!bothEmpty && !ItemStack.areItemsAndComponentsEqual(currentItem, state.heldItemAtStart)) {
            state.stopMining("Held item changed");
            return;
        }

        // ------------------------------------------------------------------
        // Stop condition 3: A screen other than TunnelMinerScreen is open
        // ------------------------------------------------------------------
        if (client.currentScreen != null && !(client.currentScreen instanceof TunnelMinerScreen)) {
            state.stopMining("Screen opened");
            return;
        }

        // ------------------------------------------------------------------
        // Stop condition 4: Game window lost focus
        // ------------------------------------------------------------------
        if (!client.isWindowFocused()) {
            state.stopMining("Window lost focus");
            return;
        }

        // ------------------------------------------------------------------
        // Stop condition 5: Player has wandered off the A→B corridor
        // ------------------------------------------------------------------
        if (state.pointA != null && state.pointB != null) {
            Vec3d playerPos = client.player.getPos();
            double dist = distanceToSegment(playerPos, state.pointA, state.pointB);
            if (dist > 3.0) {
                state.stopMining("Player off corridor (dist=" + String.format("%.1f", dist) + ")");
                return;
            }
        }

        // Stop condition 6 (mouse movement) disabled — too easily triggered accidentally.
        MinerState.mouseMoved = false;

        // ------------------------------------------------------------------
        // Stop condition 7: Physical keyboard key pressed (set by KeyboardMixin)
        // ------------------------------------------------------------------
        if (MinerState.keyPressedFlag) {
            MinerState.keyPressedFlag = false;
            state.stopMining("Keyboard input detected");
            return;
        }

        // ------------------------------------------------------------------
        // Handle tick-based wait phases
        // ------------------------------------------------------------------
        if (state.waitPhase == MinerState.WaitPhase.WAITING_AT_B) {
            state.waitTicks--;
            if (state.waitTicks <= 0) {
                // Issue /home command via chat command API
                if (client.player.networkHandler != null) {
                    // sendChatCommand sends "/home <name>" as a command (no leading slash needed)
                    // Works on servers with Essentials/CMI. In solo it will fail silently.
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

            // If back near point A, resume mining
            Vec3d playerPos = client.player.getPos();
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

        // ------------------------------------------------------------------
        // Handle scheduled break pauses
        // ------------------------------------------------------------------
        if (state.paused) {
            if (currentTime >= state.pauseUntil) {
                state.paused = false;
                state.lastBreakTime = currentTime;
                nextBreakIntervalMs = randomBreakInterval();
                state.setStatusMessage("Resuming after break...");
                resumeInputs(client);
            }
            // While paused, hold all inputs released
            return;
        }

        // Check if it is time to take a break
        if (currentTime - state.lastBreakTime > nextBreakIntervalMs) {
            long pauseDurationMs = MIN_PAUSE_MS
                    + (long) (RANDOM.nextDouble() * (MAX_PAUSE_MS - MIN_PAUSE_MS));
            state.paused    = true;
            state.pauseUntil = currentTime + pauseDurationMs;
            state.setStatusMessage("Taking a break for " + (pauseDurationMs / 1_000) + "s...");
            releaseInputs(client);
            return;
        }

        // ------------------------------------------------------------------
        // Normal mining: hold forward + attack, check for point B arrival
        // ------------------------------------------------------------------
        if (state.pointA != null && state.pointB != null) {
            Vec3d playerPos = client.player.getPos();

            // Compute how far along the A→B corridor the player is (0.0 = at A, 1.0 = at B).
            // Stop when the player has reached or passed the block position of B.
            Vec3d ab = state.pointB.subtract(state.pointA);
            double abLenSq = ab.lengthSquared();
            double t = abLenSq < 1e-10 ? 0.0
                    : ab.dotProduct(playerPos.subtract(state.pointA)) / abLenSq;

            if (t >= 1.0) {
                // Player has reached the last block of point B
                releaseInputs(client);
                int ticks = MIN_WAIT_B_TICKS
                        + RANDOM.nextInt(MAX_WAIT_B_TICKS - MIN_WAIT_B_TICKS + 1);
                state.waitTicks  = ticks;
                state.waitPhase  = MinerState.WaitPhase.WAITING_AT_B;
                state.setStatusMessage("Reached point B, waiting " + ticks + " ticks...");
                return;
            }
        }

        // Hold forward + attack continuously (pressed=true every tick keeps the key held)
        ((KeyBindingAccessor) client.options.forwardKey).setPressed(true);
        ((KeyBindingAccessor) client.options.attackKey).setPressed(true);
        // Also directly trigger the attack interaction so block breaking is continuous
        if (client.interactionManager != null && client.crosshairTarget != null
                && client.crosshairTarget.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
            client.interactionManager.updateBlockBreakingProgress(
                    ((net.minecraft.util.hit.BlockHitResult) client.crosshairTarget).getBlockPos(),
                    ((net.minecraft.util.hit.BlockHitResult) client.crosshairTarget).getSide()
            );
        }
        state.setStatusMessage("Mining...");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void releaseInputs(MinecraftClient client) {
        ((KeyBindingAccessor) client.options.forwardKey).setPressed(false);
        ((KeyBindingAccessor) client.options.attackKey).setPressed(false);
    }

    private static void resumeInputs(MinecraftClient client) {
        ((KeyBindingAccessor) client.options.forwardKey).setPressed(true);
        ((KeyBindingAccessor) client.options.attackKey).setPressed(true);
    }

    /**
     * Returns the shortest distance from point {@code p} to the line segment {@code a}–{@code b}.
     */
    private static double distanceToSegment(Vec3d p, Vec3d a, Vec3d b) {
        Vec3d ab    = b.subtract(a);
        Vec3d ap    = p.subtract(a);
        double abLenSq = ab.lengthSquared();

        if (abLenSq < 1e-10) {
            return p.distanceTo(a);
        }

        double t = Math.max(0.0, Math.min(1.0, ap.dotProduct(ab) / abLenSq));
        Vec3d closest = a.add(ab.multiply(t));
        return p.distanceTo(closest);
    }

    private static long randomBreakInterval() {
        long variance = (long) (RANDOM.nextDouble() * 2 * BREAK_VARIANCE_MS) - BREAK_VARIANCE_MS;
        return BASE_BREAK_INTERVAL_MS + variance;
    }
}
