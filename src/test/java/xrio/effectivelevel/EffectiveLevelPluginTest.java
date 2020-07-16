package xrio.effectivelevel;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class EffectiveLevelPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(EffectiveLevelPlugin.class);
        RuneLite.main(args);
    }
}