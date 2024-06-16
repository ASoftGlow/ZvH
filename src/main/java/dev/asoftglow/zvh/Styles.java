package dev.asoftglow.zvh;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;

public abstract class Styles
{
  public static final Style zombie_style = Style.style(NamedTextColor.DARK_GREEN, TextDecoration.BOLD);
  public static final Style human_style = Style.style(NamedTextColor.GOLD, TextDecoration.BOLD);
  public static final Style too_expensive_style = Style.style(NamedTextColor.RED, TextDecoration.STRIKETHROUGH)
      .decoration(TextDecoration.ITALIC, false);
  public static final Style affordable_style = Style.style(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC,
      false);

  public static final Title zombie_spawn_title = Title.title(Component.text("Zombie", zombie_style),
      Component.text("Kill the humans!"));
  public static final Title human_spawn_title = Title.title(Component.text("Human", human_style),
      Component.text("Don't get killed!"));
  public static final Title zombies_win_title = Title.title(Component.text("Zombies", zombie_style),
      Component.text("have won!"));
  public static final Title humans_win_title = Title.title(Component.text("Humans", human_style),
      Component.text("have won!"));
}
