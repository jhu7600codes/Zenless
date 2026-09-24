package com.zenless.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.zenless.game.enemy.EnemyType;
import com.zenless.game.enemy.EventManager;

import org.junit.Test;

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
    public void doorRollMatchesSpec() {
        // 51% of doors are empty
        for (int s = 0; s < 51; s++) for (int e = 0; e <= 100; e++) assertNull(EventManager.pick(s, e));
        assertEquals(EnemyType.RUSH, EventManager.pick(60, 25));
        for (int e = 26; e < 50; e++) assertEquals(EnemyType.A90B, EventManager.pick(60, e));
        for (int e = 0; e < 25; e++) assertEquals(EnemyType.FIGURE, EventManager.pick(99, e));
        for (int e = 50; e <= 100; e++) assertEquals(EnemyType.A90, EventManager.pick(51, e));
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
    }
}
