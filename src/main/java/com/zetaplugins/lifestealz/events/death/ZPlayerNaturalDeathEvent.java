package com.zetaplugins.lifestealz.events.death;

import com.zetaplugins.lifestealz.events.ZPlayerDeathEventBase;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.entity.PlayerDeathEvent;

public class ZPlayerNaturalDeathEvent extends ZPlayerDeathEventBase {
    @Getter @Setter
    private double heartsToLose;

    @Getter @Setter
    private boolean shouldDropHearts;

    @Getter @Setter
    private String deathMessage;
    
    @Getter @Setter
    private boolean heartSpawningBlocked;

    public ZPlayerNaturalDeathEvent(PlayerDeathEvent originalEvent, double heartsToLose) {
        super(originalEvent);
        this.heartsToLose = heartsToLose;
        this.shouldDropHearts = false;
        this.deathMessage = originalEvent.getDeathMessage();
        this.heartSpawningBlocked = false;
    }
}