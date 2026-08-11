package xrio.effectivelevel;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("effectivelevel")
public interface EffectiveLevelConfig extends Config
{
	@ConfigItem(
		keyName = "showPrayerBoost",
		name = "Show prayer boost",
		description = "Apply prayer boost multipliers to appropriate boosted skill levels.",
		position = 0
	)
	default boolean showPrayerBoost()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showStanceBonus",
		name = "Show stance bonus",
		description = "Add stance bonuses to appropriate boosted skill levels",
		position = 1
	)
	default boolean showStanceBonus()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showAdjustmentConstant",
		name = "Show adjustment constant",
		description = "Add the adjustment constant of +8.",
		position = 2
	)
	default boolean showAdjustmentConstant()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showVoidBonus",
		name = "Show void equipment bonus",
		description = "Apply void equipment boost multipliers to appropriate boosted skill levels.",
		position = 3
	)
	default boolean showVoidBonus()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showInvisibleBoost",
		name = "Show invisible skill boost",
		description = "Apply invisible boosts to appropriate boosted non-combat skill levels.",
		position = 4
	)
	default boolean showInvisibleBoost()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPrayerTooltipBoost",
		name = "Show prayer tooltip boost",
		description = "Show the effective level boost next to each stat when hovering a prayer, without needing to turn it on.",
		position = 5
	)
	default boolean showPrayerTooltipBoost()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showAttackStyleTooltipBoost",
		name = "Show attack style tooltip boost",
		description = "Show the hidden stance bonus when hovering an attack style, without needing to select it.",
		position = 6
	)
	default boolean showAttackStyleTooltipBoost()
	{
		return true;
	}
}
