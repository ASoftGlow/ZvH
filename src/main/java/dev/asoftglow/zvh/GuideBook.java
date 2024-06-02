package dev.asoftglow.zvh;

import org.bukkit.entity.Player;

import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;

public abstract class GuideBook
{
  final static Style title_style = Style.style(TextDecoration.UNDERLINED);
  final static Style cmd_style = Style.style(NamedTextColor.GOLD, TextDecoration.BOLD);
  final static Style value_style = Style.style(NamedTextColor.GRAY);

  final static Book book = Book.book(Component.text("ZvH Guide"), Component.text("ASoftGlow"),
      Component.empty().append(Component.text("Zombies v.s. Humans\n", Style.style(TextDecoration.BOLD)))
          .append(title("Intro"))
          .append(Component.text("As a human, stay alive. As a zombie, infect the humans. Simple as that.\nThis minigame is based off of a the now-extinct minigame on the now extict `Full` server that bore the same name.\nCheck out ")
          .append(cmd("rules")).append(Component.text(" & join the "))
          .append(Component.text("Discord").decorate(TextDecoration.UNDERLINED).clickEvent(ClickEvent.openUrl(ZvH.discordLink)))),
      Component.empty().append(title("Commands"))
          .append(subtitle("Global"))
          .append(cmd("guide", "open this guide"))
          .append(cmd("join", "join game"))
          .append(cmd("leave", "leave game (or reset)"))
          .append(cmd("music", "control ZvH-original music"))
          .append(subtitle("Game"))
          .append(cmd("shop", "buy items using coins"))
          .append(cmd("die", "quick and painless")),
          cmd("teammsg", "communicate with your team"),
          Component.empty().append(title("Coins & Levels"))
          .append(Component.text("You can earn coins as a zombie or human various ways. Spend them in the ")
          .append(cmd("shop")).append(Component.text(", or on classes as a zombie")))
          .appendNewline()
          .append(row("Zombie kill", Rewards.COINS_ZOMBIE_KILL))
          .append(row("Human kill", Rewards.COINS_HUMAN_KILL))
          .append(row("Human assist kill", Rewards.COINS_HUMAN_ASSIST))
          .append(row("Zombie win", Rewards.COINS_ZOMBIE_WIN))
          .append(row("Human win", Rewards.COINS_HUMAN_WIN))
        );

  static Component row(String name, int value)
  {
    return Component.empty().append(Component.text("\n%2d ".formatted(value)).style(value_style)).append(Component.text(name));
  }

  static Component title(String text)
  {
    return Component.text("> ").append(Component.text(text + "\n", title_style));
  }

  static Component subtitle(String text)
  {
    return Component.text("  > ").append(Component.text(text + "\n", title_style));
  }

  static Component cmd(String cmd)
  {
    return Component.text("/" + cmd, cmd_style).clickEvent(ClickEvent.runCommand("/" + cmd)).hoverEvent(HoverEvent.showText(Component.text("Click to run")));
  }

  static Component cmd(String cmd, String description)
  {
    return Component.empty().append(cmd(cmd)).append(Component.text(" - " + description + "\n"));
  }

  static void showTo(Player player)
  {
    player.openBook(book);
  }
}
