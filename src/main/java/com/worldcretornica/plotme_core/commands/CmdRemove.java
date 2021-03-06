package com.worldcretornica.plotme_core.commands;

import com.worldcretornica.plotme_core.PermissionNames;
import com.worldcretornica.plotme_core.Plot;
import com.worldcretornica.plotme_core.PlotMapInfo;
import com.worldcretornica.plotme_core.PlotMe_Core;
import com.worldcretornica.plotme_core.api.ICommandSender;
import com.worldcretornica.plotme_core.api.IOfflinePlayer;
import com.worldcretornica.plotme_core.api.IPlayer;
import com.worldcretornica.plotme_core.api.IWorld;
import com.worldcretornica.plotme_core.api.event.PlotRemoveAllowedEvent;
import net.milkbowl.vault.economy.EconomyResponse;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class CmdRemove extends PlotCommand {

    public CmdRemove(PlotMe_Core instance) {
        super(instance);
    }

    public String getName() {
        return "remove";
    }

    public boolean execute(ICommandSender sender, String[] args) {
        if (args.length < 2 && args.length >= 3) {
            sender.sendMessage(getUsage());
            return true;
        }
        if (args[1].length() > 16 || !validUserPattern.matcher(args[1]).matches()) {
            throw new IllegalArgumentException(C("InvalidCommandInput"));
        }
        if ("*".equals(args[1]) && plugin.getConfig().getBoolean("disableWildCard")) {
            sender.sendMessage("Wildcards are disabled.");
            return true;
        }
        IPlayer player = (IPlayer) sender;
        if (player.hasPermission(PermissionNames.ADMIN_REMOVE) || player.hasPermission(PermissionNames.USER_REMOVE)) {
            IWorld world = player.getWorld();
            if (manager.isPlotWorld(world)) {
                PlotMapInfo pmi = manager.getMap(world);
                Plot plot = manager.getPlot(player);
                if (plot == null) {
                    player.sendMessage(C("MsgNoPlotFound"));
                    return true;
                } else {
                    UUID playerUniqueId = player.getUniqueId();
                    String allowed = args[1];

                    if (plot.getOwnerId().equals(playerUniqueId) || player.hasPermission(PermissionNames.ADMIN_REMOVE)) {
                        if (!allowed.equals("*")) {
                            IOfflinePlayer offlinePlayer = serverBridge.getOfflinePlayer(allowed);
                            if (offlinePlayer == null) {
                                player.sendMessage("An error occured while trying to remove " + allowed);
                                return true;
                            } else {
                                allowed = offlinePlayer.getUniqueId().toString();
                            }
                        }
                        if (plot.isAllowedConsulting(allowed)) {

                            double price = 0.0;

                            PlotRemoveAllowedEvent event = new PlotRemoveAllowedEvent(world, plot, player, allowed);
                            plugin.getEventBus().post(event);

                            if (manager.isEconomyEnabled(pmi) && !event.isCancelled()) {
                                price = pmi.getRemovePlayerPrice();

                                if (serverBridge.has(player, price)) {
                                    EconomyResponse er = serverBridge.withdrawPlayer(player, price);

                                    if (!er.transactionSuccess()) {
                                        player.sendMessage(er.errorMessage);
                                        serverBridge.getLogger().warning(er.errorMessage);
                                        return true;
                                    }
                                } else {
                                    player.sendMessage(C("MsgNotEnoughRemove") + " " + C("WordMissing") + " " + serverBridge.getEconomy().get()
                                            .format(
                                                    price));
                                    return true;
                                }
                            }

                            if (!event.isCancelled()) {
                                if ("*".equals(allowed)) {
                                    plot.removeAllAllowed();
                                } else {
                                    plot.removeAllowed(allowed);
                                }
                                player.sendMessage(
                                        C("WordPlayer") + " " + allowed + " " + C("WordRemoved") + ". " + serverBridge.getEconomy().get().format
                                                (price));

                                if (isAdvancedLogging()) {
                                    if (price == 0) {
                                        serverBridge.getLogger()
                                                .info(allowed + " " + C("MsgRemovedPlayer") + " " + allowed + " " + C("MsgFromPlot") + " " + plot
                                                        .getId());
                                    } else {
                                        serverBridge.getLogger()
                                                .info(allowed + " " + C("MsgRemovedPlayer") + " " + allowed + " " + C("MsgFromPlot") + " " + plot
                                                        .getId()
                                                        + (" " + C("WordFor") + " " + price));
                                    }
                                }
                            }
                        } else {
                            player.sendMessage(C("WordPlayer") + " " + args[1] + " " + C("MsgWasNotAllowed"));
                        }
                    } else {
                        player.sendMessage(C("MsgThisPlot") + "(" + plot.getId() + ") " + C("MsgNotYoursNotAllowedRemove"));
                    }
                }
            } else {
                player.sendMessage(C("MsgNotPlotWorld"));
            }
        } else {
            return false;
        }
        return true;
    }

    @Override
    public List getAliases() {
        return Collections.singletonList("-");
    }

    @Override
    public String getUsage() {
        return C("WordUsage") + ": /plotme remove <" + C("WordPlayer") + ">";
    }
}
