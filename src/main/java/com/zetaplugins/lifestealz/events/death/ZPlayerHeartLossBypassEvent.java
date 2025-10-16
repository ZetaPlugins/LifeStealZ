package com.zetaplugins.lifestealz.events.death;

import com.zetaplugins.lifestealz.events.ZPlayerDeathEventBase;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Fired when a player's death would normally cause heart loss but is bypassed due to the bypass permission.
 * This event is not cancellable in terms of restoring heart loss; it is for observability and messaging.
 */
public class ZPlayerHeartLossBypassEvent extends ZPlayerDeathEventBase {
    @Getter
    private final Player killer; // Can be null

    @Getter @Setter
    private Component messageToVictim;

    @Getter @Setter
    private Component messageToKiller;

    @Getter @Setter
    private boolean notifyMessages = true;

    public ZPlayerHeartLossBypassEvent(PlayerDeathEvent originalEvent, Player killer,
                                       Component messageToVictim, Component messageToKiller) {
        super(originalEvent);
        this.killer = killer;
        this.messageToVictim = messageToVictim;
        this.messageToKiller = messageToKiller;
    }
}


