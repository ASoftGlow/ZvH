package dev.asoftglow.zvh;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import dev.asoftglow.zvh.commands.ClassCommand;
import dev.asoftglow.zvh.commands.MapCommand;
import xyz.janboerman.guilib.GuiLibrary;
import xyz.janboerman.guilib.api.GuiListener;

public class ZvH extends JavaPlugin {

  public final ZClass[] zClasses = {
      new ZClass("Zombie", Material.ZOMBIE_HEAD, 0),
      new ZClass("Skeleton", Material.SKELETON_SKULL, 10)
  };

  private GuiListener guiListener;

  public GuiListener getGuiListener() {
    return guiListener;
  }

  @Override
  public void onEnable() {
    System.out.println("hi");
    getLogger().info("Hello");

    GuiLibrary guiLibrary = (GuiLibrary) getServer().getPluginManager().getPlugin("GuiLib");
    guiListener = guiLibrary.getGuiListener();

    getCommand("class").setExecutor(new ClassCommand(this));
    getCommand("resetMap").setExecutor(new MapCommand(this));

    var pm = getServer().getPluginManager();
    pm.registerEvents(new JoinLeaveListener(), this);
    pm.registerEvents(new ChatListener(), this);
    
    saveDefaultConfig();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (command.getLabel().equals("ex")) {
      sender.sendMessage("Hello, " + sender.getName() + "...");
      return true;
    }
    return super.onCommand(sender, command, label, args);
  }

}
