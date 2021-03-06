package com.worldcretornica.plotme_core.api.event;

import com.worldcretornica.plotme_core.Plot;
import com.worldcretornica.plotme_core.api.IPlayer;
import com.worldcretornica.plotme_core.api.IWorld;

public class PlotDisposeEvent extends PlotEvent implements ICancellable, Event {

    private final IPlayer disposer;
    private boolean canceled;

    public PlotDisposeEvent(IWorld world, Plot plot, IPlayer disposer) {
        super(plot, world);
        this.disposer = disposer;
    }

    @Override
    public boolean isCancelled() {
        return canceled;
    }

    @Override
    public void setCanceled(boolean cancel) {
        canceled = cancel;
    }

    public IPlayer getPlayer() {
        return disposer;
    }
}
