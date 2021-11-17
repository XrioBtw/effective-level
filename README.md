# Effective Level
A combat stats effective level is the boosted stat level together with invisible boosts like prayer boost, stance bonus, a +8 constant and void equipment boost. This plugin aims to display this invisible effective level in the skills tab. The effective level is a component in the max hit formula and accuracy and defence roll calculations, implemented according to Bitterkoekje's empirically derived combat formulas over at https://secure.runescape.com/m=forum/forums?317,318,461,66138854 (backup [1](https://docs.google.com/document/d/1hk7FxOAOFT4oxguC8411QQhE4kk-_GzqWcwkaPmaYns/edit), [2](https://archive.md/qNs1O), [3](https://web.archive.org/web/20190905124128/http://webcache.googleusercontent.com/search?q=cache:http://services.runescape.com/m=forum/forums.ws?317,318,712,65587452)).

This plugin will also display [invisible boosts](https://osrs.wiki/Temporary_skill_boost) for non-combat skills:

| Item / Location | Skill | Boost |
|-----------------|-------|:-----:|
| Celestial ring / signet | Mining | +4 |
| Crystal saw | Construction | +3 |
| Fishing Guild | Fishing | +7 |
| Mining Guild | Mining | +7 |
| Woodcutting Guild | Woodcutting | +7 |

![boosted and effective level side by side](https://i.imgur.com/8bJcdK8.png)

Config options:
* Show prayer boost
* Show stance bonus
* Show adjustment constant
* Show void equipment bonus
* Show invisible skill boost
