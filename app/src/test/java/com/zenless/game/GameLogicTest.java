package com.zenless.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.zenless.game.enemy.EnemyType;
import com.zenless.game.enemy.SpawnTable;

import org.junit.Test;

import java.util.Random;

public class GameLogicTest {

    @Test
    public void formatsHolos() {
        assertEquals("0", Fmt.holos(0));
        assertEquals("0.1", Fmt.holos(0.1));
        assertEquals("999", Fmt.holos(999));
        assertEquals("1.2K", Fmt.holos(1200));
        assertEquals("3.4M", Fmt.holos(3_400_000));
        assertEquals("1.0M", Fmt.holos(999_999));
        assertEquals("5.6B", Fmt.holos(5_600_000_000.0));
    }

    @Test
    public void scriptedFirstMeetings() {
        Random rng = new Random(1);
        Difficulty h = Difficulty.HARD;
        assertEquals(EnemyType.RUSH, SpawnTable.roll(10, h, true, rng));
        assertEquals(EnemyType.A90, SpawnTable.roll(30, h, false, rng));
        assertEquals(EnemyType.FIGURE, SpawnTable.roll(50, h, true, rng));
        assertEquals(EnemyType.A90B, SpawnTable.roll(70, h, false, rng));
        assertEquals(EnemyType.FIGURE, SpawnTable.roll(100, h, false, rng));
        // nothing random before the first meeting
        for (int door = 1; door < 10; door++) assertNull(SpawnTable.roll(door, h, false, rng));
        assertEquals(0, SpawnTable.chance(EnemyType.A90, h, 30), 0);
        assertEquals(0.06, SpawnTable.chance(EnemyType.A90, h, 31), 1e-9);
        assertEquals(0, SpawnTable.chance(EnemyType.FIGURE, h, 99), 0);
        assertEquals(0.02, SpawnTable.chance(EnemyType.FIGURE, h, 101), 1e-9);
        // ??? never shows up through the normal rolls
        for (Difficulty d : Difficulty.values()) assertEquals(0, SpawnTable.chance(EnemyType.SECRET, d, 500), 0);
        // quiet doors block random spawns
        for (int i = 0; i < 1000; i++) assertNull(SpawnTable.roll(200, h, true, rng));
    }

    @Test
    public void difficultiesDiffer() {
        Random rng = new Random(2);
        for (int door = 1; door <= 300; door++) {
            assertNull(SpawnTable.roll(door, Difficulty.EASY, false, rng));
            EnemyType t = SpawnTable.roll(door, Difficulty.NORMAL, false, rng);
            if (door % 30 == 0) assertEquals(EnemyType.FIGURE, t);
            else assertNull(t);
        }
        assertEquals(0.16, SpawnTable.chance(EnemyType.A90B, Difficulty.EXTREME, 80), 1e-9);
        assertEquals(0.16, SpawnTable.chance(EnemyType.RUSH, Difficulty.EXTREME, 80), 1e-9);
        assertEquals(0.08 * 6.47, SpawnTable.chance(EnemyType.RUSH, Difficulty.SUPER_HARD, 80), 1e-9);
        assertEquals(3.0, Difficulty.SUPER_HARD.doorSpeed, 0);

        GameState s = new GameState();
        Economy eco = new Economy(s);
        s.difficulty = Difficulty.EASY.ordinal();
        assertEquals(12, eco.buildingCost(0), 0);
        s.difficulty = Difficulty.SUPER_HARD.ordinal();
        assertEquals(23, eco.buildingCost(0), 0); // ceil(15 * 1.5)
        s.upgrades[Upgrade.CLICK_POWER] = 2;
        assertEquals(2.25, eco.clickPower(), 1e-9); // 1.5^2 instead of 2^2
        s.buildings[1] = 10;
        assertEquals(5, eco.hps(), 1e-9);
    }

    @Test
    public void riftCarriesOneThing() {
        GameState s = new GameState();
        s.buildings[2] = 25;
        s.upgrades[Upgrade.CLICK_POWER] = 4;
        s.riftKind = GameState.RIFT_BUILDING;
        s.riftIndex = 2;
        s.resetRun();
        assertEquals(25, s.buildings[2]);
        assertEquals(0, s.upgrades[Upgrade.CLICK_POWER]);
        assertEquals(GameState.RIFT_EMPTY, s.riftKind);

        s.upgrades[Upgrade.CLICK_POWER] = 4;
        s.riftKind = GameState.RIFT_UPGRADE;
        s.riftIndex = Upgrade.CLICK_POWER;
        s.adminEverUsed = true; // rift is closed in admin saves
        s.resetRun();
        assertEquals(0, s.upgrades[Upgrade.CLICK_POWER]);
    }

    @Test
    public void economyScales() {
        GameState s = new GameState();
        Economy eco = new Economy(s);
        assertEquals(15, eco.buildingCost(0), 0);
        s.buildings[0] = 1;
        assertEquals(18, eco.buildingCost(0), 0); // ceil(15 * 1.15)
        s.buildings[1] = 10;
        assertEquals(10.1, eco.hps(), 1e-9);
        s.upgrades[Upgrade.CLICK_POWER] = 3;
        assertEquals(8, eco.clickPower(), 1e-9);
        assertEquals(0, eco.rebirthReward());
        s.runEarned = 4_000_000;
        assertEquals(2, eco.rebirthReward());
        s.holos = 50;
        assertEquals(2, eco.rebirth());
        assertEquals(0, s.holos, 0);
        assertEquals(true, s.adminUnlocked);
        assertEquals(false, s.adminEnabled);
        assertEquals(-1, s.difficulty);
    }
}
