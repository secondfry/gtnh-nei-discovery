package com.secondfry.mc.neidiscovery;

import codechicken.nei.ItemList;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.config.OptionToggleButton;

public final class NEIDiscoveryConfig implements IConfigureNEI {

    static final String ENABLED = "inventory.discovery.enabled";
    static final String CHAINS = "inventory.discovery.chains";

    @Override
    public void loadConfig() {
        NEIClientConfig.getSetting(ENABLED)
            .getBooleanValue(true);
        NEIClientConfig.getSetting(CHAINS)
            .getBooleanValue(false);
        for (String setting : new String[] { ENABLED, CHAINS }) {
            API.addOption(new OptionToggleButton(setting, true) {

                @Override
                public boolean onClick(int button) {
                    super.onClick(button);
                    DiscoveryClient.INSTANCE.invalidate();
                    ItemList.updateFilter.restart();
                    return true;
                }
            });
        }
        API.addItemFilter(() -> {
            // NEI runs item filtering on worker threads: capture an immutable snapshot.
            final java.util.Set<StackKey> visible = DiscoveryClient.INSTANCE.visible;
            final boolean enabled = NEIClientConfig.getBooleanSetting(ENABLED);
            return stack -> !enabled || visible.contains(StackKey.of(stack));
        });
        DiscoveryClient.INSTANCE.ready = true;
    }

    @Override
    public String getName() {
        return "NEI Discovery";
    }

    @Override
    public String getVersion() {
        return "0.1.2";
    }
}
