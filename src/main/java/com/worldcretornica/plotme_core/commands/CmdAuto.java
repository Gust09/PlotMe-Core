package com.worldcretornica.plotme_core.commands;

import com.worldcretornica.plotme_core.PermissionNames;
import com.worldcretornica.plotme_core.PlotId;
import com.worldcretornica.plotme_core.PlotMapInfo;
import com.worldcretornica.plotme_core.PlotMe_Core;
import com.worldcretornica.plotme_core.api.ICommandSender;
import com.worldcretornica.plotme_core.api.IPlayer;
import com.worldcretornica.plotme_core.api.IWorld;
import net.milkbowl.vault.economy.EconomyResponse;

import java.util.UUID;

public class CmdAuto extends PlotCommand {

    public CmdAuto(PlotMe_Core instance) {
        super(instance);
    }

    public String getName() {
        return "auto";
    }

    public boolean execute(ICommandSender sender, String[] args) {
        final IPlayer player = (IPlayer) sender;
        if (player.hasPermission(PermissionNames.USER_AUTO)) {
            if (manager.isPlotWorld(player) || plugin.getConfig().getBoolean("allowWorldTeleport")) {
                final IWorld world;
                if (!manager.isPlotWorld(player) && plugin.getConfig().getBoolean("allowWorldTeleport")) {
                    if (args.length == 2) {
                        world = manager.getWorld(args[1]);
                    } else {
                        world = manager.getFirstWorld();
                    }
                    if (world == null) {
                        player.sendMessage(C("MsgNotPlotWorld"));
                        return true;
                    }
                    if (!manager.isPlotWorld(world)) {
                        player.sendMessage(world + " " + C("MsgWorldNotPlot"));
                        return true;
                    }
                } else {
                    world = player.getWorld();
                }

                int playerLimit = getPlotLimit(player);

                int plotsOwned = manager.getOwnedPlotCount(player.getUniqueId(), world);

                if (playerLimit != -1 && plotsOwned >= playerLimit && !player.hasPermission("PlotMe.admin")) {
                    player.sendMessage(C("MsgAlreadyReachedMaxPlots") + " (" + plotsOwned + "/" + playerLimit + "). " + C("WordUse")
                            + " /plotme home " + C("MsgToGetToIt"));
                    return true;
                }
                final PlotMapInfo pmi = manager.getMap(world);
                serverBridge.runTaskAsynchronously(new Runnable() {
                    @Override public void run() {
                        loop:
                        for (int i = 0; i < 50000; i++) {
                            for (int x = -i; x <= i; x++) {
                                for (int z = -i; z <= i; z++) {
                                    PlotId id = new PlotId(x, z);

                                    if (manager.isPlotAvailable(id, world)) {
                                        String name = player.getName();
                                        UUID uuid = player.getUniqueId();


                                        if (manager.isEconomyEnabled(world)) {
                                            double price = pmi.getClaimPrice();

                                            if (serverBridge.has(player, price)) {
                                                EconomyResponse er = serverBridge.withdrawPlayer(player, price);

                                                if (!er.transactionSuccess()) {
                                                    player.sendMessage(er.errorMessage);
                                                }
                                            } else {
                                                player.sendMessage("You do not have enough money to buy this plot");
                                            }
                                        }

                                        manager.createPlot(id, world, name, uuid, pmi);

                                        player.teleport(manager.getPlotHome(id, world));

                                        player.sendMessage(C("MsgThisPlotYours") + " " + C("WordUse") + " /plotme " + C("CommandHome") + " " + C
                                                ("MsgToGetToIt"));
                                        break loop;
                                    }
                                }
                            }
                        }
                    }
                });
                player.sendMessage(C("MsgNoPlotFound"));
            } else {
                player.sendMessage(C("MsgNotPlotWorld"));
            }
        } else {
            return false;
        }
        return true;
    }

    @Override
    public String getUsage() {
        return C("CmdAutoUsage");
    }

}