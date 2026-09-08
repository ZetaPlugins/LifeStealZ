package com.zetaplugins.lifestealz.events.death;

import com.zetaplugins.lifestealz.events.ZPlayerDeathEventBase;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;

public class ZPlayerPvPDeathEvent extends ZPlayerDeathEventBase {
    @Getter
    private final Player killer;
    
    @Getter @Setter
    private double healthToLose;
    
    @Getter @Setter
    private double healthKillerGains;
    
    @Getter @Setter
    private boolean shouldDropHearts;
    
    @Getter @Setter
    private boolean killerShouldGainHearts;
    
    @Getter @Setter
    private String deathMessage;

    public ZPlayerPvPDeathEvent(PlayerDeathEvent originalEvent, Player killer, double healthToLose, double healthKillerGains) {
        super(originalEvent);
        this.killer = killer;
        this.healthToLose = healthToLose;
        this.healthKillerGains = healthKillerGains;
        this.shouldDropHearts = false;
        this.killerShouldGainHearts = true;
        this.deathMessage = originalEvent.getDeathMessage();
    }
}
