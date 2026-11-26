/** Copyright (c) 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.tray;

import cn.harryh.arkpets.animations.PetAction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BattleMenuPanelTest {
    @Test
    void actionMenuShowsFourDailyAndThreePerformanceActions() {
        assertEquals(List.of(PetAction.WALK, PetAction.SIT, PetAction.SLEEP, PetAction.INTERACT,
                PetAction.ANGRY, PetAction.HOP, PetAction.SPECIAL), BattleMenuPanel.MENU_ACTIONS);
        assertFalse(BattleMenuPanel.MENU_ACTIONS.contains(PetAction.RELAX));
    }
}
