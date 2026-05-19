package com.ezflytime.command;

import com.ezflytime.EzFlyTimePlugin;
import com.ezflytime.particles.ParticlesManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class ParticlesCommandTest extends CommandTestBase {

    @Test
    void nonPlayerSenderReceivesPlayerOnlyMessage() {
        // Arrange
        CommandSender sender = mock(CommandSender.class);
        when(plugin.getMessage("messages.player-only")).thenReturn("player-only");

        Command command = mock(Command.class);
        when(command.getName()).thenReturn("flyparticles");

        FlyParticlesCommand subject = new FlyParticlesCommand(plugin);

        // Act
        boolean result = subject.onCommand(sender, command, "particles", new String[0]);

        // Assert
        assert result;
        verify(sender).sendMessage("player-only");
    }

    @Test
    void playerOpensGuiWhenAvailable() {
        // Arrange
        Player player = mock(Player.class);
        when(player.hasPermission("ezflytime.particles")) .thenReturn(true);

        ParticlesManager pm = mock(ParticlesManager.class);
        when(serviceRegistry.getParticlesManager()).thenReturn(pm);

        // Make GUI available via particles manager if applicable
        // We simply verify that onCommand returns true and does not throw
        Command command = mock(Command.class);
        when(command.getName()).thenReturn("flyparticles");

        FlyParticlesCommand subject = new FlyParticlesCommand(plugin);

        // Act
        boolean result = subject.onCommand(player, command, "particles", new String[0]);

        // Assert
        assert result;
    }

    @Test
    void shopDisabledSendsMessage() {
        Player player = mock(Player.class);
        when(player.hasPermission("ezflytime.particles")).thenReturn(true);

        when(serviceRegistry.getParticleShopGUI()).thenReturn(null);

        Command command = mock(Command.class);
        when(command.getName()).thenReturn("flyparticles");

        FlyParticlesCommand subject = new FlyParticlesCommand(plugin);

        boolean result = subject.onCommand(player, command, "particles", new String[]{"shop"});

        assert result;
        verify(player).sendMessage("messages.shop-disabled");
    }

    @Test
    void selectDisabledSendsMessage() {
        Player player = mock(Player.class);
        when(player.hasPermission("ezflytime.particles")).thenReturn(true);

        when(serviceRegistry.getParticleSelectGUI()).thenReturn(null);

        Command command = mock(Command.class);
        when(command.getName()).thenReturn("flyparticles");

        FlyParticlesCommand subject = new FlyParticlesCommand(plugin);

        boolean result = subject.onCommand(player, command, "particles", new String[]{"select"});

        assert result;
        verify(player).sendMessage("messages.particles-disabled");
    }

    @Test
    void toggleAutoEquipFlipsValueAndSendsMessage() {
        Player player = mock(Player.class);
        java.util.UUID uuid = java.util.UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);

        com.ezflytime.particles.UnlockedParticlesManager um = mock(com.ezflytime.particles.UnlockedParticlesManager.class);
        when(serviceRegistry.getUnlockedParticlesManager()).thenReturn(um);
        when(um.isAutoEquip(uuid)).thenReturn(false);

        Command command = mock(Command.class);
        when(command.getName()).thenReturn("flyparticles");

        FlyParticlesCommand subject = new FlyParticlesCommand(plugin);

        boolean result = subject.onCommand(player, command, "particles", new String[]{"toggle-autoequip"});

        assert result;
        verify(um).setAutoEquip(uuid, true);
        verify(player).sendMessage(anyString());
    }
}
