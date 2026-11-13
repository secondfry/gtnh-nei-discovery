package com.secondfry.mc.neidiscovery;

import org.junit.Test;

import cpw.mods.fml.common.eventhandler.ASMEventHandler;
import cpw.mods.fml.common.gameevent.TickEvent;

public class DiscoveryEventHandlerTest {

    @Test
    public void forgeGeneratedHandlerCanInvokeClientTicks() throws Exception {
        ASMEventHandler handler = new ASMEventHandler(
            new DiscoveryClient(),
            DiscoveryClient.class.getMethod("tick", TickEvent.ClientTickEvent.class),
            null);
        // Before NEI initialization the real callback returns without needing a running Minecraft client.
        handler.invoke(new TickEvent.ClientTickEvent(TickEvent.Phase.START));
        handler.invoke(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
    }
}
