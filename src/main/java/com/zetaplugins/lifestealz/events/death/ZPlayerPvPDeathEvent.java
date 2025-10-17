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
    private double heartsToLose;
    
    @Getter @Setter
    private double heartsKillerGains;
    
    @Getter @Setter
    private boolean shouldDropHearts;
    
    @Getter @Setter
    private boolean killerShouldGainHearts;
    
    @Getter @Setter
    private String deathMessage;
    
    @Getter @Setter
    private boolean heartSpawningBlocked;

    public ZPlayerPvPDeathEvent(PlayerDeathEvent originalEvent, Player killer, double heartsToLose, double heartsKillerGains) {
        super(originalEvent);
        this.killer = killer;
        this.heartsToLose = heartsToLose;
        this.heartsKillerGains = heartsKillerGains;
        this.shouldDropHearts = false;
        this.killerShouldGainHearts = true;
        this.deathMessage = originalEvent.getDeathMessage();
        this.heartSpawningBlocked = false;
    }
}
