package dev.asoftglow.zvh.menus;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import dev.asoftglow.zvh.Cosmetics;
import dev.asoftglow.zvh.Database;
import dev.asoftglow.zvh.ZvH;
import dev.asoftglow.zvh.util.guilib.CloseButton;
import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.janboerman.guilib.api.ItemBuilder;
import xyz.janboerman.guilib.api.menu.ItemButton;
import xyz.janboerman.guilib.api.menu.MenuButton;
import xyz.janboerman.guilib.api.menu.MenuHolder;
import xyz.janboerman.guilib.api.menu.RedirectItemButton;

public abstract class CosmeticsMenu
{
  private static final MenuHolder<ZvH> menu = new MenuHolder<>(ZvH.singleton, 9, "Cosmetics");
  static
  {
    final var blocks_item = new ItemBuilder(Material.PURPLE_WOOL).name("§r§fBlocks").flags(ItemFlag.HIDE_ATTRIBUTES)
        .build();
    final var particles_item = new ItemBuilder(Material.FIREWORK_STAR).name("§r§fParticle Trails") // TODO
        .flags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ITEM_SPECIFICS).build();
    final var kill_effect_item = new ItemBuilder(Material.IRON_SWORD).name("§r§fKill Effects")
        .flags(ItemFlag.HIDE_ATTRIBUTES).build();

    menu.setButton(1,
        new RedirectItemButton<>(blocks_item, (hm, e) -> new BlocksMenu((Player) e.getWhoClicked()).getInventory()));
    //menu.setButton(3, new ItemButton<>(particles_item));
    //menu.setButton(5, new ItemButton<>(kill_effect_item));
    menu.setButton(8, new CloseButton<>());
  }

  private static class BlocksMenu extends MenuHolder<ZvH>
  {
    private static final Sound click = Sound.sound(Key.key("ui.button.click"), Sound.Source.MASTER, 0.5f, 1f);
    private int selected;

    private class BlockButton<MH extends MenuHolder<?>> implements MenuButton<MH>
    {
      @Getter
      private ItemStack icon;
      private boolean isOwned;

      public BlockButton(ItemStack icon, boolean isOwned)
      {
        this.icon = icon;
        this.isOwned = isOwned;
      }

      public void onClick(MH holder, InventoryClickEvent event)
      {
        var slot = event.getRawSlot();
        if (selected != slot)
        {
          if (isOwned)
          {
            event.getView().getItem(selected).removeEnchantment(Enchantment.LUCK);
            event.getCurrentItem().addUnsafeEnchantment(Enchantment.LUCK, 1);
            selected = slot;
            Database.setIntStat((Player) event.getWhoClicked(), "cos_blk", slot < 9 ? slot : slot - 1);

          } else
          {
            event.getView().close();
            event.getWhoClicked().sendMessage(
                Component.text("\nClick to open store\n").decorate(TextDecoration.UNDERLINED, TextDecoration.BOLD)
                    .clickEvent(ClickEvent.openUrl(ZvH.singleton.getConfig().getString("store.link"))));
          }
          event.getWhoClicked().playSound(click, Sound.Emitter.self());
        }
      }
    }

    private void setButton(int i, ItemStack item, boolean isOwned)
    {
      var meta = item.getItemMeta();
      meta.displayName(meta.displayName().decoration(TextDecoration.ITALIC, false)
          .decoration(TextDecoration.STRIKETHROUGH, !isOwned));
      meta.lore(List.of(
          isOwned ? Component.text("You own this") : Component.text("You don't have this. Click to go to the store.")));
      meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
      item.setItemMeta(meta);
      if (i == selected)
      {
        item.addUnsafeEnchantment(Enchantment.LUCK, 1);
      }
      setButton(i, new BlockButton<>(item, isOwned));
    }

    private void setButton(int i, String name, Material material, boolean isOwned)
    {
      var item = new ItemStack(material);
      var meta = item.getItemMeta();
      meta.displayName(Component.text(name));
      item.setItemMeta(meta);
      setButton(i, item, isOwned);
    }

    public BlocksMenu(Player player)
    {
      super(ZvH.singleton, 18, "Block Colors");
      selected = Database.getCachedIntStat(player, "cos_blk").orElse(0);
      int owned = Database.getCachedIntStat(player, "cos_blk_own").orElse(0);

      for (int i = 0; i < Cosmetics.Blocks.solid_materials.length; i++)
      {
        boolean isOwned = Cosmetics.Blocks.doesOwnPack(owned,
            i == 0 ? Cosmetics.Blocks.PackMasks.DEFAULT : Cosmetics.Blocks.PackMasks.COLORFUL);
        String name = StringUtils.capitalize(Cosmetics.Blocks.solid_materials[i].name().split("_")[0]);

        var item = new ItemStack(Cosmetics.Blocks.solid_materials[i]);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name).color(Cosmetics.Blocks.text_colors[i]));
        item.setItemMeta(meta);
        setButton(i, item, isOwned);
      }

      boolean isOwned = Cosmetics.Blocks.doesOwnPack(owned, Cosmetics.Blocks.PackMasks.RAINBOW);
      setButton(10, "Rainbow", Material.PARROT_SPAWN_EGG, isOwned);

      setButton(9,
          new RedirectItemButton<>(new ItemBuilder(Material.ARROW).name("§r§fBack").build(), menu::getInventory));
      setButton(17, new CloseButton<>());
    }

    public void onClick(InventoryClickEvent event)
    {
      Inventory clickedInventory = getClickedInventory(event);
      if (clickedInventory != null && clickedInventory == this.getInventory())
      {
        var slot = event.getRawSlot();
        var btn = (MenuButton<BlocksMenu>) getButton(slot);
        if (btn == null)
          return;
        btn.onClick(this, event);
      }
    }
  }

  private static class ParticlesMenu extends MenuHolder<ZvH>
  {
    private static final Sound click = Sound.sound(Key.key("ui.button.click"), Sound.Source.MASTER, 0.5f, 1f);
    private int selected;

    private class ParticleButton<MH extends MenuHolder<?>> implements MenuButton<MH>
    {
      @Getter
      private ItemStack icon;
      private boolean isOwned;

      public ParticleButton(ItemStack icon, boolean isOwned)
      {
        this.icon = icon;
        this.isOwned = isOwned;
      }

      public void onClick(MH holder, InventoryClickEvent event)
      {
        var slot = event.getRawSlot();
        if (selected != slot)
        {
          if (isOwned)
          {
            event.getView().getItem(selected).removeEnchantment(Enchantment.LUCK);
            event.getCurrentItem().addUnsafeEnchantment(Enchantment.LUCK, 1);
            selected = slot;
            Database.setIntStat((Player) event.getWhoClicked(), "cos_par", slot < 9 ? slot : slot - 1);

          } else
          {
            event.getWhoClicked().sendMessage(
                Component.text("\nClick to open store\n").decorate(TextDecoration.UNDERLINED, TextDecoration.BOLD)
                    .clickEvent(ClickEvent.openUrl(ZvH.singleton.getConfig().getString("discord.link"))));
          }
          event.getWhoClicked().playSound(click, Sound.Emitter.self());
        }
      }
    }

    private void setButton(int i, String name, Material material, boolean isOwned)
    {
      var item = new ItemStack(material);
      var meta = item.getItemMeta();
      meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false)
          .color(Cosmetics.Blocks.text_colors[i]).decoration(TextDecoration.STRIKETHROUGH, !isOwned));
      meta.lore(List.of(
          isOwned ? Component.text("You own this") : Component.text("You don't have this. Click to go to the store.")));
      item.setItemMeta(meta);
      if (i == selected)
      {
        item.addUnsafeEnchantment(Enchantment.LUCK, 1);
      }
      setButton(i, new ParticleButton<>(item, isOwned));
    }

    public ParticlesMenu(Player player)
    {
      super(ZvH.singleton, 18, "Particle Trails");
      selected = Database.getCachedIntStat(player, "cos_par").orElse(0);
      int owned = Database.getCachedIntStat(player, "cos_par_own").orElse(0);

      setButton(9,
          new RedirectItemButton<>(new ItemBuilder(Material.ARROW).name("§r§fBack").build(), menu::getInventory));
      setButton(17, new CloseButton<>());
    }

    public void onClick(InventoryClickEvent event)
    {
      Inventory clickedInventory = getClickedInventory(event);
      if (clickedInventory != null && clickedInventory == this.getInventory())
      {
        var slot = event.getRawSlot();
        var btn = (MenuButton<ParticlesMenu>) getButton(slot);
        if (btn == null)
          return;
        btn.onClick(this, event);
      }
    }
  }

  public static void showTo(Player player)
  {
    player.openInventory(menu.getInventory());
  }
}
