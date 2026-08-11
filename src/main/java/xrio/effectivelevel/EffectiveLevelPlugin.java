package xrio.effectivelevel;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Provides;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.FontTypeFace;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
	name = "Effective Levels",
	description = "Shows the invisible effective boosted skill levels in the skills tab.<br>" +
		"These are the levels that go into the max hit, accuracy roll and <br>" +
		"defence roll formulas before accounting for equipment bonuses.",
	tags = {"skill", "effective", "boosted", "invisible", "levels"}
)
public class EffectiveLevelPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private EffectiveLevelConfig config;

	private final Skill[] combatSkills = new Skill[] { Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC };
	private final Skill[] nonCombatSkills = new Skill[] { Skill.MINING, Skill.CONSTRUCTION, Skill.FISHING, Skill.WOODCUTTING };
	private final Skill[] skills = concat(combatSkills, nonCombatSkills);

	private final int[] miningRings = new int[]
	{
		ItemID.CELESTIAL_RING, ItemID.CELESTIAL_RING_CHARGED, ItemID.CELESTIAL_SIGNET, ItemID.CELESTIAL_SIGNET_CHARGED
	};

	private static Skill[] concat(Skill[] a, Skill[] b)
	{
		Skill[] combined = Arrays.copyOf(a, a.length + b.length);
		System.arraycopy(b, 0, combined, a.length, b.length);
		return combined;
	}

	@Provides
	EffectiveLevelConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(EffectiveLevelConfig.class);
	}

	@Override
	protected void shutDown() throws Exception
	{
		resetLevels();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("effectivelevel"))
		{
			resetLevels();
		}
	}

	@Subscribe
	public void onClientTick(ClientTick tick)
	{
		if (!GameState.LOGGED_IN.equals(client.getGameState()))
		{
			return;
		}

		updatePrayerTooltip();
		updateAttackStyleTooltip();
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (!GameState.LOGGED_IN.equals(client.getGameState()))
		{
			return;
		}

		for (Skill skill : combatSkills)
		{
			double prayerBoost = getPrayerBoost(skill);
			int stanceBonus = getStanceBonus(skill);
			double voidBonus = getVoidBonus(skill);

			int effectiveLevel = client.getBoostedSkillLevel(skill);

			if (config.showPrayerBoost())
			{
				effectiveLevel *= prayerBoost;
			}

			if (config.showStanceBonus())
			{
				effectiveLevel += stanceBonus;
			}

			if (config.showAdjustmentConstant())
			{
				effectiveLevel += 8;
			}

			if (config.showVoidBonus())
			{
				effectiveLevel *= voidBonus;
			}

			updateSkillLevel(skill, effectiveLevel);
		}

		if (config.showInvisibleBoost())
		{
			int miningLevel = client.getBoostedSkillLevel(Skill.MINING);
			int constructionLevel = client.getBoostedSkillLevel(Skill.CONSTRUCTION);
			int fishingLevel = client.getBoostedSkillLevel(Skill.FISHING);
			int woodcuttingLevel = client.getBoostedSkillLevel(Skill.WOODCUTTING);

			Set<Integer> equipment = getItemIDs(InventoryID.WORN);
			for (int id : miningRings)
			{
				if (equipment.contains(id))
				{
					miningLevel += 4;
					break;
				}
			}

			Set<Integer> inventory = getItemIDs(InventoryID.INV);
			if (inventory.contains(ItemID.EYEGLO_CRYSTAL_SAW))
			{
				constructionLevel += 3;
			}

			if (client.getLocalPlayer() != null)
			{
				WorldPoint worldPoint = client.getLocalPlayer().getWorldLocation();
				int regionId = worldPoint.getRegionID();
				int x = worldPoint.getX();
				int y = worldPoint.getY();

				if (regionId == 11927 || regionId == 12183)
				{
					miningLevel += 7;
				}
				else if (x >= 2579 && y >= 3394 && x <= 2623 && y <= 3425)
				{
					fishingLevel += 7;
				}
				else if (regionId == 6198 || regionId == 6454)
				{
					woodcuttingLevel += 7;
				}
			}

			updateSkillLevel(Skill.MINING, miningLevel);
			updateSkillLevel(Skill.CONSTRUCTION, constructionLevel);
			updateSkillLevel(Skill.FISHING, fishingLevel);
			updateSkillLevel(Skill.WOODCUTTING, woodcuttingLevel);
		}
	}

	private void resetLevels()
	{
		for (Skill skill : skills)
		{
			updateSkillLevel(skill, client.getBoostedSkillLevel(skill));
		}
	}

	private void updateSkillLevel(Skill skill, int effectiveLevel)
	{
		int childId;
		switch (skill)
		{
			case ATTACK:
				childId = InterfaceID.Stats.ATTACK;
				break;
			case STRENGTH:
				childId = InterfaceID.Stats.STRENGTH;
				break;
			case DEFENCE:
				childId = InterfaceID.Stats.DEFENCE;
				break;
			case RANGED:
				childId = InterfaceID.Stats.RANGED;
				break;
			case MAGIC:
				childId = InterfaceID.Stats.MAGIC;
				break;
			case MINING:
				childId = InterfaceID.Stats.MINING;
				break;
			case CONSTRUCTION:
				childId = InterfaceID.Stats.CONSTRUCTION;
				break;
			case FISHING:
				childId = InterfaceID.Stats.FISHING;
				break;
			case WOODCUTTING:
				childId = InterfaceID.Stats.WOODCUTTING;
				break;
			default:
				return;
		}
		Widget skillWidget = client.getWidget(childId);
		if (skillWidget == null)
		{
			return;
		}

		Widget[] skillWidgetComponents = skillWidget.getDynamicChildren();
		if (skillWidgetComponents.length >= 5)
		{
			skillWidgetComponents[4].setText("" + effectiveLevel);
		}
	}

	private static final Map<String, Prayer> PRAYER_NAMES_BY_DISPLAY_NAME = new ImmutableMap.Builder<String, Prayer>()
		.put("Clarity of Thought", Prayer.CLARITY_OF_THOUGHT)
		.put("Improved Reflexes", Prayer.IMPROVED_REFLEXES)
		.put("Incredible Reflexes", Prayer.INCREDIBLE_REFLEXES)
		.put("Burst of Strength", Prayer.BURST_OF_STRENGTH)
		.put("Superhuman Strength", Prayer.SUPERHUMAN_STRENGTH)
		.put("Ultimate Strength", Prayer.ULTIMATE_STRENGTH)
		.put("Thick Skin", Prayer.THICK_SKIN)
		.put("Rock Skin", Prayer.ROCK_SKIN)
		.put("Steel Skin", Prayer.STEEL_SKIN)
		.put("Sharp Eye", Prayer.SHARP_EYE)
		.put("Hawk Eye", Prayer.HAWK_EYE)
		.put("Eagle Eye", Prayer.EAGLE_EYE)
		.put("Mystic Will", Prayer.MYSTIC_WILL)
		.put("Mystic Lore", Prayer.MYSTIC_LORE)
		.put("Mystic Might", Prayer.MYSTIC_MIGHT)
		.put("Chivalry", Prayer.CHIVALRY)
		.put("Piety", Prayer.PIETY)
		.put("Rigour", Prayer.RIGOUR)
		.put("Augury", Prayer.AUGURY)
		.build();

	private static final String TOOLTIP_BOOST_COLOR = "<col=ff0000>";
	private static final Pattern PERCENT_PATTERN = Pattern.compile("(\\d+)%");

	private void updatePrayerTooltip()
	{
		if (!config.showPrayerTooltipBoost())
		{
			return;
		}

		Widget prayerTooltip = client.getWidget(InterfaceID.Prayerbook.TOOLTIP);
		if (prayerTooltip == null || prayerTooltip.isHidden())
		{
			return;
		}

		Widget[] children = prayerTooltip.getDynamicChildren();
		if (children.length < 3)
		{
			return;
		}

		Widget descriptionWidget = children[2];
		String text = descriptionWidget.getText();
		if (text == null || text.contains(TOOLTIP_BOOST_COLOR))
		{
			return;
		}

		Prayer hoveredPrayer = null;
		for (Map.Entry<String, Prayer> entry : PRAYER_NAMES_BY_DISPLAY_NAME.entrySet())
		{
			if (text.contains(entry.getKey()))
			{
				hoveredPrayer = entry.getValue();
				break;
			}
		}

		if (hoveredPrayer == null)
		{
			return;
		}

		SkillBoost[] boosts = getPrayerSkillBoosts(hoveredPrayer);
		if (boosts.length == 0)
		{
			return;
		}

		Matcher matcher = PERCENT_PATTERN.matcher(text);
		StringBuilder result = new StringBuilder();
		int lastEnd = 0;
		boolean foundAny = false;
		while (matcher.find())
		{
			int percent = Integer.parseInt(matcher.group(1));
			result.append(text, lastEnd, matcher.end());
			lastEnd = matcher.end();

			for (SkillBoost boost : boosts)
			{
				if (Math.round((boost.multiplier - 1) * 100) == percent)
				{
					int currentLevel = client.getBoostedSkillLevel(boost.skill);
					int delta = (int) (currentLevel * boost.multiplier) - currentLevel;
					String annotation = " (" + (delta >= 0 ? "+" : "") + delta + ")";
					result.append(TOOLTIP_BOOST_COLOR).append(annotation).append("</col>");
					foundAny = true;
					break;
				}
			}
		}

		if (!foundAny)
		{
			return;
		}

		result.append(text, lastEnd, text.length());
		resizeTooltip(prayerTooltip, children, descriptionWidget, text, result.toString());
	}

	private int countWrappedLines(String markupText, int maxWidth, FontTypeFace font)
	{
		int totalLines = 0;
		for (String paragraph : markupText.split("<br>", -1))
		{
			String plain = paragraph.replaceAll("<[^>]*>", "").trim();
			String[] words = plain.isEmpty() ? new String[0] : plain.split("\\s+");

			int linesInParagraph = 1;
			StringBuilder line = new StringBuilder();
			for (String word : words)
			{
				String candidate = line.length() == 0 ? word : line + " " + word;
				if (line.length() > 0 && font.getTextWidth(candidate) > maxWidth)
				{
					linesInParagraph++;
					line = new StringBuilder(word);
				}
				else
				{
					line = new StringBuilder(candidate);
				}
			}
			totalLines += linesInParagraph;
		}
		return totalLines;
	}

	private void updateAttackStyleTooltip()
	{
		if (!config.showAttackStyleTooltipBoost())
		{
			return;
		}

		Widget combatTooltip = client.getWidget(InterfaceID.CombatInterface.TOOLTIP);
		if (combatTooltip == null || combatTooltip.isHidden())
		{
			return;
		}

		Widget[] children = combatTooltip.getDynamicChildren();
		if (children.length < 3)
		{
			return;
		}

		Widget descriptionWidget = children[2];
		String text = descriptionWidget.getText();
		if (text == null || text.contains(TOOLTIP_BOOST_COLOR))
		{
			return;
		}

		int nameStart = text.indexOf('(');
		int nameEnd = text.indexOf(')', nameStart);
		if (nameStart == -1 || nameEnd == -1)
		{
			return;
		}
		String hoveredStyle = text.substring(nameStart + 1, nameEnd);

		// The tooltip shows a short style name (e.g. "Accurate"), but the same short name is reused
		// across weapon types for different skills (melee's "Accurate" vs ranged's "Accurate ranging").
		// Resolve it against this weapon's own style list so the right skill gets credited.
		int weaponCategory = client.getVarbitValue(VarbitID.COMBAT_WEAPON_CATEGORY);
		for (int i = 0; i < 4; i++)
		{
			String candidate = CombatStyle.getAttackStyleText(weaponCategory, i);
			if (candidate != null && (candidate.equals(hoveredStyle) || candidate.startsWith(hoveredStyle + " ")))
			{
				hoveredStyle = candidate;
				break;
			}
		}

		StringBuilder extra = new StringBuilder();
		boolean anyBonus = false;
		for (Skill skill : combatSkills)
		{
			int bonus = getStanceBonusForStyle(skill, hoveredStyle);
			if (bonus != 0)
			{
				extra.append("<br>").append(TOOLTIP_BOOST_COLOR).append('+').append(bonus).append(' ').append(skill.getName()).append("</col>");
				anyBonus = true;
			}
		}

		if (!anyBonus)
		{
			return;
		}

		resizeTooltip(combatTooltip, children, descriptionWidget, text, text + extra);
	}

	private void resizeTooltip(Widget tooltip, Widget[] children, Widget descriptionWidget, String oldText, String newText)
	{
		// OSRS text widgets have no auto-size-to-wrapped-text mode (WidgetSizeMode is only
		// ABSOLUTE/MINUS/ABSOLUTE_16384THS) and getScrollHeight() is only populated for
		// scrollable LAYER widgets, not plain TEXT ones — so there's no way to ask the client
		// for the wrapped height directly. We have to work it out from the same font metrics
		// the client itself renders with.
		FontTypeFace font = descriptionWidget.getFont();
		if (font != null)
		{
			int maxWidth = descriptionWidget.getWidth();
			int extraLineCount = countWrappedLines(newText, maxWidth, font) - countWrappedLines(oldText, maxWidth, font);
			if (extraLineCount > 0)
			{
				int lineHeight = descriptionWidget.getLineHeight();
				int resolvedLineHeight = lineHeight > 0 ? lineHeight : font.getBaseline() + 3;
				tooltip.setOriginalHeight(tooltip.getHeight() + extraLineCount * resolvedLineHeight);
			}
		}

		descriptionWidget.setText(newText);
		tooltip.revalidate();
		for (Widget child : children)
		{
			child.revalidate();
		}
	}

	private static final class SkillBoost
	{
		private final Skill skill;
		private final double multiplier;

		private SkillBoost(Skill skill, double multiplier)
		{
			this.skill = skill;
			this.multiplier = multiplier;
		}
	}

	private SkillBoost[] getPrayerSkillBoosts(Prayer prayer)
	{
		switch (prayer)
		{
			case CLARITY_OF_THOUGHT:
				return new SkillBoost[] { new SkillBoost(Skill.ATTACK, 1.05) };
			case IMPROVED_REFLEXES:
				return new SkillBoost[] { new SkillBoost(Skill.ATTACK, 1.10) };
			case INCREDIBLE_REFLEXES:
				return new SkillBoost[] { new SkillBoost(Skill.ATTACK, 1.15) };
			case BURST_OF_STRENGTH:
				return new SkillBoost[] { new SkillBoost(Skill.STRENGTH, 1.05) };
			case SUPERHUMAN_STRENGTH:
				return new SkillBoost[] { new SkillBoost(Skill.STRENGTH, 1.10) };
			case ULTIMATE_STRENGTH:
				return new SkillBoost[] { new SkillBoost(Skill.STRENGTH, 1.15) };
			case THICK_SKIN:
				return new SkillBoost[] { new SkillBoost(Skill.DEFENCE, 1.05) };
			case ROCK_SKIN:
				return new SkillBoost[] { new SkillBoost(Skill.DEFENCE, 1.10) };
			case STEEL_SKIN:
				return new SkillBoost[] { new SkillBoost(Skill.DEFENCE, 1.15) };
			case SHARP_EYE:
				return new SkillBoost[] { new SkillBoost(Skill.RANGED, 1.05) };
			case HAWK_EYE:
				return new SkillBoost[] { new SkillBoost(Skill.RANGED, 1.10) };
			case EAGLE_EYE:
				return new SkillBoost[] { new SkillBoost(Skill.RANGED, 1.15) };
			case MYSTIC_WILL:
				return new SkillBoost[] { new SkillBoost(Skill.MAGIC, 1.05) };
			case MYSTIC_LORE:
				return new SkillBoost[] { new SkillBoost(Skill.MAGIC, 1.10) };
			case MYSTIC_MIGHT:
				return new SkillBoost[] { new SkillBoost(Skill.MAGIC, 1.15) };
			case CHIVALRY:
				return new SkillBoost[] { new SkillBoost(Skill.ATTACK, 1.15), new SkillBoost(Skill.STRENGTH, 1.18), new SkillBoost(Skill.DEFENCE, 1.20) };
			case PIETY:
				return new SkillBoost[] { new SkillBoost(Skill.ATTACK, 1.20), new SkillBoost(Skill.STRENGTH, 1.23), new SkillBoost(Skill.DEFENCE, 1.25) };
			case RIGOUR:
				return new SkillBoost[] { new SkillBoost(Skill.RANGED, 1.20), new SkillBoost(Skill.DEFENCE, 1.25) };
			case AUGURY:
				return new SkillBoost[] { new SkillBoost(Skill.MAGIC, 1.25), new SkillBoost(Skill.DEFENCE, 1.25) };
			default:
				return new SkillBoost[0];
		}
	}

	private double getPrayerBoost(Skill skill)
	{
		double multiplier = 1;

		switch (skill)
		{
			case ATTACK:
				multiplier = client.isPrayerActive(Prayer.CLARITY_OF_THOUGHT) ? 1.05 : multiplier;
				multiplier = client.isPrayerActive(Prayer.IMPROVED_REFLEXES) ? 1.10 : multiplier;
				multiplier = client.isPrayerActive(Prayer.INCREDIBLE_REFLEXES) ? 1.15 : multiplier;
				multiplier = client.isPrayerActive(Prayer.CHIVALRY) ? 1.15 : multiplier;
				multiplier = client.isPrayerActive(Prayer.PIETY) ? 1.20 : multiplier;
				break;
			case STRENGTH:
				multiplier = client.isPrayerActive(Prayer.BURST_OF_STRENGTH) ? 1.05 : multiplier;
				multiplier = client.isPrayerActive(Prayer.SUPERHUMAN_STRENGTH) ? 1.10 : multiplier;
				multiplier = client.isPrayerActive(Prayer.ULTIMATE_STRENGTH) ? 1.15 : multiplier;
				multiplier = client.isPrayerActive(Prayer.CHIVALRY) ? 1.18 : multiplier;
				multiplier = client.isPrayerActive(Prayer.PIETY) ? 1.23 : multiplier;
				break;
			case DEFENCE:
				multiplier = client.isPrayerActive(Prayer.THICK_SKIN) ? 1.05 : multiplier;
				multiplier = client.isPrayerActive(Prayer.ROCK_SKIN) ? 1.10 : multiplier;
				multiplier = client.isPrayerActive(Prayer.STEEL_SKIN) ? 1.15 : multiplier;
				multiplier = client.isPrayerActive(Prayer.CHIVALRY) ? 1.20 : multiplier;
				multiplier = client.isPrayerActive(Prayer.PIETY) ? 1.25 : multiplier;
				multiplier = client.isPrayerActive(Prayer.RIGOUR) ? 1.25 : multiplier;
				multiplier = client.isPrayerActive(Prayer.AUGURY) ? 1.25 : multiplier;
				break;
			case RANGED:
				multiplier = client.isPrayerActive(Prayer.SHARP_EYE) ? 1.05 : multiplier;
				multiplier = client.isPrayerActive(Prayer.HAWK_EYE) ? 1.10 : multiplier;
				multiplier = client.isPrayerActive(Prayer.EAGLE_EYE) ? 1.15 : multiplier;
				multiplier = client.isPrayerActive(Prayer.RIGOUR) ? 1.20 : multiplier;
				break;
			case MAGIC:
				multiplier = client.isPrayerActive(Prayer.MYSTIC_WILL) ? 1.05 : multiplier;
				multiplier = client.isPrayerActive(Prayer.MYSTIC_LORE) ? 1.10 : multiplier;
				multiplier = client.isPrayerActive(Prayer.MYSTIC_MIGHT) ? 1.15 : multiplier;
				multiplier = client.isPrayerActive(Prayer.AUGURY) ? 1.25 : multiplier;
				break;
			default:
				break;
		}
		return multiplier;
	}

	private int getStanceBonus(Skill skill)
	{
		int attackStyleVarbit = client.getVarpValue(VarPlayerID.COM_MODE);
		int combatStyleVarbit = client.getVarbitValue(VarbitID.COMBAT_WEAPON_CATEGORY);

		String attackStyle = CombatStyle.getAttackStyleText(combatStyleVarbit, attackStyleVarbit);

		return getStanceBonusForStyle(skill, attackStyle);
	}

	private int getStanceBonusForStyle(Skill skill, String attackStyle)
	{
		int bonus = 0;

		switch (skill)
		{
			case ATTACK:
				bonus = "Accurate".equals(attackStyle) ? 3 : bonus;
				bonus = "Controlled".equals(attackStyle) ? 1 : bonus;
				break;
			case STRENGTH:
				bonus = "Aggressive".equals(attackStyle) ? 3 : bonus;
				bonus = "Controlled".equals(attackStyle) ? 1 : bonus;
				break;
			case DEFENCE:
				bonus = "Controlled".equals(attackStyle) ? 1 : bonus;
				bonus = "Defensive".equals(attackStyle) ? 3 : bonus;
				bonus = "Longrange".equals(attackStyle) ? 3 : bonus;
				break;
			case RANGED:
				bonus = "Accurate ranging".equals(attackStyle) ? 3 : bonus;
				break;
			case MAGIC:
				bonus = "Accurate casting".equals(attackStyle) ? 3 : bonus;
				bonus = "Longrange casting".equals(attackStyle) ? 1 : bonus;
				break;
			default:
				break;
		}
		return bonus;
	}

	private Set<Integer> getItemIDs(final int inventoryID)
	{
		final ItemContainer container = client.getItemContainer(inventoryID);
		Set<Integer> itemIDs = new HashSet<>();
		if (container != null)
		{
			for (Item item : container.getItems())
			{
				itemIDs.add(item.getId());
			}
		}
		return itemIDs;
	}

	private double getVoidBonus(Skill skill)
	{
		double multiplier = 1;

		if (Skill.DEFENCE.equals(skill))
		{
			return multiplier;
		}

		Set<Integer> itemIDs = getItemIDs(InventoryID.WORN);

		boolean voidGloves = (itemIDs.contains(ItemID.PEST_VOID_KNIGHT_GLOVES) ||
			itemIDs.contains(ItemID.PEST_VOID_KNIGHT_GLOVES_TROUVER) || //24182
			itemIDs.contains(ItemID.LEAGUE_3_VOID_KNIGHT_GLOVES) ||
			itemIDs.contains(ItemID.LEAGUE_3_VOID_KNIGHT_GLOVES_TROUVER));
		boolean voidTop = (itemIDs.contains(ItemID.PEST_VOID_KNIGHT_TOP) ||
			itemIDs.contains(ItemID.PEST_VOID_KNIGHT_TOP_TROUVER) ||
			itemIDs.contains(ItemID.LEAGUE_3_VOID_KNIGHT_TOP) ||
			itemIDs.contains(ItemID.LEAGUE_3_VOID_KNIGHT_TOP_TROUVER));
		boolean voidBottom = (itemIDs.contains(ItemID.PEST_VOID_KNIGHT_ROBES) ||
			itemIDs.contains(ItemID.PEST_VOID_KNIGHT_ROBES_TROUVER) ||
			itemIDs.contains(ItemID.LEAGUE_3_VOID_KNIGHT_ROBES) ||
			itemIDs.contains(ItemID.LEAGUE_3_VOID_KNIGHT_ROBES_TROUVER));

		boolean eliteVoidTop = (itemIDs.contains(ItemID.ELITE_VOID_KNIGHT_TOP) ||
			itemIDs.contains(ItemID.ELITE_VOID_KNIGHT_TOP_TROUVER) ||
			itemIDs.contains(ItemID.LEAGUE_3_VOID_KNIGHT_TOP_ELITE) ||
			itemIDs.contains(ItemID.LEAGUE_3_VOID_KNIGHT_TOP_ELITE_TROUVER));
		boolean eliteVoidBottom = (itemIDs.contains(ItemID.ELITE_VOID_KNIGHT_ROBES) ||
			itemIDs.contains(ItemID.ELITE_VOID_KNIGHT_ROBES_TROUVER) ||
			itemIDs.contains(ItemID.LEAGUE_3_VOID_KNIGHT_ROBES_ELITE) ||
			itemIDs.contains(ItemID.LEAGUE_3_VOID_KNIGHT_ROBES_ELITE_TROUVER));

		boolean voidHelmMelee = (itemIDs.contains(ItemID.GAME_PEST_MELEE_HELM) ||
			itemIDs.contains(ItemID.GAME_PEST_MELEE_HELM_TROUVER) ||
			itemIDs.contains(ItemID.LEAGUE_3_VOID_MELEE_HELM) ||
			itemIDs.contains(ItemID.LEAGUE_3_VOID_MELEE_HELM_TROUVER));
		boolean voidHelmRanged = (itemIDs.contains(ItemID.GAME_PEST_ARCHER_HELM) ||
			itemIDs.contains(ItemID.GAME_PEST_ARCHER_HELM_TROUVER) ||
			itemIDs.contains(ItemID.LEAGUE_3_VOID_RANGE_HELM) ||
			itemIDs.contains(ItemID.LEAGUE_3_VOID_RANGE_HELM_TROUVER));
		boolean voidHelmMagic = (itemIDs.contains(ItemID.GAME_PEST_MAGE_HELM) ||
			itemIDs.contains(ItemID.GAME_PEST_MAGE_HELM_TROUVER) ||
			itemIDs.contains(ItemID.LEAGUE_3_VOID_MAGE_HELM) ||
			itemIDs.contains(ItemID.LEAGUE_3_VOID_MAGE_HELM_TROUVER));

		if (!(voidGloves && (voidTop || eliteVoidTop) && (voidBottom || eliteVoidBottom)))
		{
			return multiplier;
		}

		if ((Skill.ATTACK.equals(skill) || Skill.STRENGTH.equals(skill)) && voidHelmMelee)
		{
			multiplier = 1.10;
		}
		else if (Skill.RANGED.equals(skill) && voidHelmRanged)
		{
			if (eliteVoidTop && eliteVoidBottom)
			{
				multiplier = 1.125;
			}
			else
			{
				multiplier = 1.10;
			}
		}
		else if (Skill.MAGIC.equals(skill) && voidHelmMagic)
		{
			if (eliteVoidTop && eliteVoidBottom)
			{
				multiplier = 1.475;
			}
			else
			{
				multiplier = 1.45;
			}
		}
		return multiplier;
	}

	private enum CombatStyle
	{
		TYPE_0("Accurate", "Aggressive", null, "Defensive"),
		TYPE_1("Accurate", "Aggressive", "Aggressive", "Defensive"),
		TYPE_2("Accurate", "Aggressive", null, "Defensive"),
		TYPE_3("Accurate ranging", "Rapid", null, "Longrange"),
		TYPE_4("Accurate", "Aggressive", "Controlled", "Defensive"),
		TYPE_5("Accurate ranging", "Rapid", null, "Longrange"),
		TYPE_6("Aggressive", "Rapid", "Casting", null),
		TYPE_7("Accurate ranging", "Rapid", null, "Longrange"),
		TYPE_8("Other", "Aggressive", null, null),
		TYPE_9("Accurate", "Aggressive", "Controlled", "Defensive"),
		TYPE_10("Accurate", "Aggressive", "Aggressive", "Defensive"),
		TYPE_11("Accurate", "Aggressive", "Aggressive", "Defensive"),
		TYPE_12("Controlled", "Aggressive", null, "Defensive"),
		TYPE_13("Accurate", "Aggressive", null, "Defensive"),
		TYPE_14("Accurate", "Aggressive", "Aggressive", "Defensive"),
		TYPE_15("Controlled", "Controlled", "Controlled", "Defensive"),
		TYPE_16("Accurate", "Aggressive", "Controlled", "Defensive"),
		TYPE_17("Accurate", "Aggressive", "Aggressive", "Defensive"),
		TYPE_18("Accurate", "Aggressive", null, "Defensive", "Casting", "Defensive casting"),
		TYPE_19("Accurate ranging", "Rapid", null, "Longrange"),
		TYPE_20("Accurate", "Controlled", null, "Defensive"),
		TYPE_21("Accurate", "Aggressive", null, "Defensive", "Casting", "Defensive casting"),
		TYPE_22("Accurate", "Aggressive", "Aggressive", "Defensive"),
		TYPE_23("Accurate casting", "Accurate casting", null, "Longrange casting"),
		TYPE_24("Accurate", "Aggressive", "Controlled", "Defensive"),
		TYPE_25("Controlled", "Aggressive", null, "Defensive"),
		TYPE_26("Aggressive", "Aggressive", null, "Aggressive"),
		TYPE_27("Accurate", null, null, "Other"),
		TYPE_28("Accurate casting", "Accurate casting", null, "Longrange casting"),
		TYPE_29("Accurate", "Aggressive", "Aggressive", "Defensive");

		private final String[] attackStyles;

		private static final Map<Integer, CombatStyle> combatStyles;

		static
		{
			ImmutableMap.Builder<Integer, CombatStyle> builder = new ImmutableMap.Builder<>();

			for (CombatStyle combatStyle : values())
			{
				builder.put(combatStyle.ordinal(), combatStyle);
			}

			combatStyles = builder.build();
		}

		CombatStyle(final String... attackStyles)
		{
			this.attackStyles = attackStyles;
		}

		public static String getAttackStyleText(int combatStyleId, int attackStyleId)
		{
			return combatStyles.get(combatStyleId).attackStyles[attackStyleId];
		}
	}
}
