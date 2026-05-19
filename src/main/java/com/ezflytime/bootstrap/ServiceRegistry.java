package com.ezflytime.bootstrap;

import com.ezflytime.flight.FlyTimeManager;
import com.ezflytime.flight.autoreward.AutoFlightTimeDistributor;
import com.ezflytime.particles.ParticlesManager;
import com.ezflytime.placeholder.PlaceholderIntegration;
import com.ezflytime.storage.FlyTimeStorage;
import com.ezflytime.storage.VoucherStorage;
import com.ezflytime.voucher.VoucherManager;
import com.ezflytime.gui.VoucherGUI;
import com.ezflytime.storage.UnlockedParticlesStorage;
import com.ezflytime.util.ServerUUID;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ServiceRegistry {
    private FlyTimeStorage flyTimeStorage;
    private VoucherStorage voucherStorage;
    private UnlockedParticlesStorage unlockedParticlesStorage;
    private FlyTimeManager flyTimeManager;
    private VoucherManager voucherManager;
    private ParticlesManager particlesManager;
    private VoucherGUI voucherGUI;
    private com.ezflytime.particles.UnlockedParticlesManager unlockedParticlesManager;
    private AutoFlightTimeDistributor autoFlightTimeDistributor;
    private PlaceholderIntegration placeholderIntegration;
    private ServerUUID serverUUID;
    private com.ezflytime.update.SpigotUpdateChecker updateChecker;
    private com.ezflytime.update.UpdateCheckResult updateCheckResult;
    private com.ezflytime.config.ConfigManager configManager;
    private net.milkbowl.vault.economy.Economy economy;
    private final Set<UUID> maxSingleBypassPlayers = new HashSet<>();
    private com.ezflytime.teams.TeamsIntegration teamsIntegration;

    public FlyTimeStorage getFlyTimeStorage() {
        return flyTimeStorage;
    }

    public void setFlyTimeStorage(FlyTimeStorage flyTimeStorage) {
        this.flyTimeStorage = flyTimeStorage;
    }

    public VoucherStorage getVoucherStorage() {
        return voucherStorage;
    }

    public void setVoucherStorage(VoucherStorage voucherStorage) {
        this.voucherStorage = voucherStorage;
    }

    public UnlockedParticlesStorage getUnlockedParticlesStorage() {
        return unlockedParticlesStorage;
    }

    public void setUnlockedParticlesStorage(UnlockedParticlesStorage unlockedParticlesStorage) {
        this.unlockedParticlesStorage = unlockedParticlesStorage;
    }

    public FlyTimeManager getFlyTimeManager() {
        return flyTimeManager;
    }

    public void setFlyTimeManager(FlyTimeManager flyTimeManager) {
        this.flyTimeManager = flyTimeManager;
    }

    public VoucherManager getVoucherManager() {
        return voucherManager;
    }

    public void setVoucherManager(VoucherManager voucherManager) {
        this.voucherManager = voucherManager;
    }

    public ParticlesManager getParticlesManager() {
        return particlesManager;
    }

    public void setParticlesManager(ParticlesManager particlesManager) {
        this.particlesManager = particlesManager;
    }

    public VoucherGUI getVoucherGUI() {
        return voucherGUI;
    }

    public void setVoucherGUI(VoucherGUI voucherGUI) {
        this.voucherGUI = voucherGUI;
    }

    private com.ezflytime.gui.ParticleShopGUI particleShopGUI;
    private com.ezflytime.gui.ParticleSelectGUI particleSelectGUI;

    public com.ezflytime.gui.ParticleShopGUI getParticleShopGUI() {
        return particleShopGUI;
    }

    public void setParticleShopGUI(com.ezflytime.gui.ParticleShopGUI particleShopGUI) {
        this.particleShopGUI = particleShopGUI;
    }

    public com.ezflytime.gui.ParticleSelectGUI getParticleSelectGUI() {
        return particleSelectGUI;
    }

    public void setParticleSelectGUI(com.ezflytime.gui.ParticleSelectGUI particleSelectGUI) {
        this.particleSelectGUI = particleSelectGUI;
    }

    public com.ezflytime.particles.UnlockedParticlesManager getUnlockedParticlesManager() {
        return unlockedParticlesManager;
    }

    public void setUnlockedParticlesManager(com.ezflytime.particles.UnlockedParticlesManager unlockedParticlesManager) {
        this.unlockedParticlesManager = unlockedParticlesManager;
    }

    public AutoFlightTimeDistributor getAutoFlightTimeDistributor() {
        return autoFlightTimeDistributor;
    }

    public void setAutoFlightTimeDistributor(AutoFlightTimeDistributor autoFlightTimeDistributor) {
        this.autoFlightTimeDistributor = autoFlightTimeDistributor;
    }

    public PlaceholderIntegration getPlaceholderIntegration() {
        return placeholderIntegration;
    }

    public void setPlaceholderIntegration(PlaceholderIntegration placeholderIntegration) {
        this.placeholderIntegration = placeholderIntegration;
    }

    public ServerUUID getServerUUID() {
        return serverUUID;
    }

    public void setServerUUID(ServerUUID serverUUID) {
        this.serverUUID = serverUUID;
    }

    public com.ezflytime.update.SpigotUpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    public void setUpdateChecker(com.ezflytime.update.SpigotUpdateChecker updateChecker) {
        this.updateChecker = updateChecker;
    }

    public com.ezflytime.update.UpdateCheckResult getUpdateCheckResult() {
        return updateCheckResult;
    }

    public void setUpdateCheckResult(com.ezflytime.update.UpdateCheckResult updateCheckResult) {
        this.updateCheckResult = updateCheckResult;
    }

    public com.ezflytime.config.ConfigManager getConfigManager() {
        return configManager;
    }

    public void setConfigManager(com.ezflytime.config.ConfigManager configManager) {
        this.configManager = configManager;
    }

    public net.milkbowl.vault.economy.Economy getEconomy() {
        return economy;
    }

    public void setEconomy(net.milkbowl.vault.economy.Economy economy) {
        this.economy = economy;
    }

    public Set<UUID> getMaxSingleBypassPlayers() {
        return maxSingleBypassPlayers;
    }

    public com.ezflytime.teams.TeamsIntegration getTeamsIntegration() {
        return teamsIntegration;
    }

    public void setTeamsIntegration(com.ezflytime.teams.TeamsIntegration teamsIntegration) {
        this.teamsIntegration = teamsIntegration;
    }
}
