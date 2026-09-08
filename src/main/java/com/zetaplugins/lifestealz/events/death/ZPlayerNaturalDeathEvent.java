package com.zetaplugins.lifestealz.events.death;

import com.zetaplugins.lifestealz.events.ZPlayerDeathEventBase;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.entity.PlayerDeathEvent;

public class ZPlayerNaturalDeathEvent extends ZPlayerDeathEventBase {
    @Getter @Setter
    private double healthToLose;

    @Getter @Setter
    private boolean shouldDropHearts;

    @Getter @Setter
    private String deathMessage;

    public ZPlayerNaturalDeathEvent(PlayerDeathEvent originalEvent, double healthToLose) {
        super(originalEvent);
        this.healthToLose = healthToLose;
        this.shouldDropHearts = false;
        this.deathMessage = originalEvent.getDeathMessage();
    }
}