package dev.asoftglow.zvh;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import dev.asoftglow.zvh.util.Logger;

public abstract class Database
{
  static private final Map<String, Map<Player, MutableInt>> caches = new HashMap<>(4);
  static private Connection con = null;
  static private boolean attempted = false;
  static
  {
    caches.put("coins", new HashMap<>());
    caches.put("xp", new HashMap<>());
    caches.put("lvl", new HashMap<>());
    caches.put("cos_blk", new HashMap<>());
    caches.put("cos_blk_own", new HashMap<>());
    caches.put("cos_par", new HashMap<>());
    caches.put("cos_par_own", new HashMap<>());
  }
  /**
   * Last known
   */
  static private String url, name, username, password;
  public static final Map<Player, Integer> cos_particles = new HashMap<>();

  public enum AsyncTarget
  {
    MainToMain, MainToAsync, AsyncToMain
  }

  public static boolean login()
  {
    if (attempted)
      return false;

    Logger.Get().info("Logging in to %s as %s".formatted(url, username));
    try
    {
      Class.forName("org.mariadb.jdbc.Driver");
      con = DriverManager.getConnection("jdbc:mariadb://" + url, username, password);
      con.createStatement().executeUpdate("USE " + name);

      Logger.Get().info("Connected to " + url);
      return true;

    } catch (SQLException e)
    {
      attempted = true;
      // allow trying again after a minute
      Bukkit.getScheduler().runTaskLater(ZvH.singleton, () -> attempted = false, 20 * 60);

      Logger.Get().warning("Failed to connect to db");
      handleSQLException(e);

    } catch (ClassNotFoundException e)
    {
      Logger.Get().warning("Failed to get driver");
      e.printStackTrace();
    }
    return false;
  }

  public static void login(String url, String name, String username, String password)
  {
    Database.url = url;
    Database.name = name;
    Database.username = username;
    Database.password = password;
    login();
  }

  public static void logout()
  {
    if (con == null)
      return;
    try
    {
      if (!con.isClosed())
      {
        con.close();
      }
    } catch (SQLException e)
    {
      e.printStackTrace();
    }
  }

  private static void handleSQLException(SQLException e)
  {
    Logger.Get().warning(e.getMessage() + " from " + e.getStackTrace()[0].toString());

    verifyConnection();
  }

  private static void handleSQLException(SQLException e, Player p)
  {
    handleSQLException(e);
    Bukkit.getScheduler().runTask(ZvH.singleton,
        () -> p.sendMessage("There was a database error (" + e.getErrorCode() + ')'));
  }

  private static void handleSQLException(SQLException e, Collection<Player> ps)
  {
    handleSQLException(e);
    Bukkit.getScheduler().runTask(ZvH.singleton, () -> {
      for (var p : ps)
      {
        p.sendMessage("There was a database error (" + e.getErrorCode() + ')');
      }
    });
  }

  private static void setupPreparedStatement(PreparedStatement statement) throws SQLException
  {
    if (ZvH.isDev)
    {
      Logger.Get().info("> Accessing db: " + statement.toString());
    }
  }

  public static void addPlayer(Player player)
  {
    if (!verifyConnection())
      return;

    try (var stmt = con.prepareStatement("INSERT INTO PlayerStats(uuid, coins) VALUES(?, ?)"))
    {
      setupPreparedStatement(stmt);

      stmt.setString(1, player.getUniqueId().toString());
      stmt.setInt(2, Rewards.COINS_JOIN);
      stmt.executeUpdate();

      Logger.Get().info("Added " + player.getName() + " to the db");

    } catch (SQLException e)
    {
      handleSQLException(e);
    }
  }

  public static void getIntStatLeaderboard(String stat, int limit, Consumer<Map<OfflinePlayer, Integer>> callback)
  {
    if (!verifyConnection())
      return;

    Bukkit.getScheduler().runTaskAsynchronously(ZvH.singleton, () -> {
      try (var stmt = con
          .prepareStatement("SELECT uuid, " + stat + " FROM PlayerStats ORDER BY " + stat + " DESC LIMIT " + limit))
      {
        setupPreparedStatement(stmt);

        var result = stmt.executeQuery();

        Bukkit.getScheduler().runTask(ZvH.singleton, () -> {
          var players = new LinkedHashMap<OfflinePlayer, Integer>(limit);
          try
          {
            while (result.next())
            {
              var player = Bukkit.getOfflinePlayer(UUID.fromString(result.getString("uuid")));
              players.put(player, result.getInt(2));
            }
          } catch (SQLException e)
          {
            handleSQLException(e);
            callback.accept(null);
          }
          callback.accept(players);
        });

      } catch (SQLException e)
      {
        handleSQLException(e);
        Bukkit.getScheduler().runTask(ZvH.singleton, () -> callback.accept(null));
      }
    });
  }

  public static void getIntStat(Player player, String stat, Consumer<OptionalInt> callback)
  {
    getIntStat(player, stat, callback, AsyncTarget.MainToMain);
  }

  public static OptionalInt getCachedIntStat(Player player, String stat)
  {
    var cached_stat = caches.get(stat);
    if (cached_stat == null)
      return OptionalInt.empty();

    return OptionalInt.of(cached_stat.get(player).intValue());
  }

  public static void getIntStat(Player player, String stat, Consumer<OptionalInt> callback, AsyncTarget target)
  {
    if (!verifyConnection())
      return;

    switch (target)
    {
    case MainToAsync:
    case MainToMain:
      Bukkit.getScheduler().runTaskAsynchronously(ZvH.singleton, () -> {
        _getIntStat(player, stat, callback, target);
      });
      break;

    case AsyncToMain:
      _getIntStat(player, stat, callback, target);
      break;
    }
  }

  private static void _getIntStat(Player player, String stat, Consumer<OptionalInt> callback, AsyncTarget target)
  {
    try (var stmt = con.prepareStatement("SELECT " + stat + " FROM PlayerStats WHERE uuid = ?"))
    {
      setupPreparedStatement(stmt);

      stmt.setString(1, player.getUniqueId().toString());

      var result = stmt.executeQuery();
      if (result.next())
      {
        var val = result.getInt(1);
        switch (target)
        {
        case AsyncToMain:
        case MainToMain:
          Bukkit.getScheduler().runTask(ZvH.singleton, () -> callback.accept(OptionalInt.of(val)));
          return;

        case MainToAsync:
          callback.accept(OptionalInt.of(val));
          return;
        }
      }
    } catch (SQLException e)
    {
      handleSQLException(e, player);
      Bukkit.getScheduler().runTask(ZvH.singleton, () -> callback.accept(OptionalInt.empty()));
    }
  }

  public static void changeIntStat(Player player, String stat, int amount)
  {
    var cached_stat = caches.get(stat);
    if (cached_stat != null)
    {
      cached_stat.get(player).add(amount);
    }
    if (!verifyConnection())
      return;

    try (var stmt = con.prepareStatement("UPDATE PlayerStats SET " + stat + " = " + stat + " + ? WHERE uuid = ?"))
    {
      setupPreparedStatement(stmt);

      stmt.setString(2, player.getUniqueId().toString());
      stmt.setInt(1, amount);
      stmt.executeUpdate();

    } catch (SQLException e)
    {
      handleSQLException(e, player);
    }
  }

  public static void setIntStat(Player player, String stat, int amount)
  {
    var cached_stat = caches.get(stat);
    if (cached_stat != null)
    {
      cached_stat.get(player).setValue(amount);
    }
    if (!verifyConnection())
      return;

    try (var stmt = con.prepareStatement("UPDATE PlayerStats SET " + stat + " = ? WHERE uuid = ?"))
    {
      setupPreparedStatement(stmt);

      stmt.setString(2, player.getUniqueId().toString());
      stmt.setInt(1, amount);
      stmt.executeUpdate();

    } catch (SQLException e)
    {
      handleSQLException(e, player);
    }
  }

  public static void changeIntStat(Collection<Player> players, String stat, int amount)
  {
    var cached_stat = caches.get(stat);
    if (cached_stat != null)
    {
      for (var p : players)
      {
        cached_stat.get(p).add(amount);
      }
    }
    if (!verifyConnection())
      return;

    Bukkit.getScheduler().runTaskAsynchronously(ZvH.singleton, () -> {
      try (var stmt = con.prepareStatement("UPDATE PlayerStats SET " + stat + " = " + stat + " + ? WHERE uuid = ?"))
      {
        setupPreparedStatement(stmt);

        for (var p : players)
        {
          stmt.setString(2, p.getUniqueId().toString());
          stmt.setInt(1, amount);
          stmt.addBatch();
        }
        stmt.executeBatch();

      } catch (SQLException e)
      {
        handleSQLException(e, players);
      }
    });
  }

  public static void S_changeXp(Player player, int amount)
  {
    var xp = getCachedIntStat(player, "xp");
    if (!xp.isPresent())
      return;
    int old_xp = xp.getAsInt();

    var lvl = getCachedIntStat(player, "lvl");
    if (!lvl.isPresent())
      return;

    int old_lvl = lvl.getAsInt();
    int new_xp = old_xp + amount;
    int new_lvl = Rewards.calcLvl(new_xp);

    caches.get("xp").get(player).setValue(new_xp);
    Rewards.displayExpBar(player, new_lvl, new_xp);

    if (old_lvl < new_lvl)
    {
      caches.get("lvl").get(player).setValue(new_lvl);
      Rewards.handleLvlUp(player, old_lvl, new_lvl);

      if (!verifyConnection())
        return;

      Bukkit.getScheduler().runTaskAsynchronously(ZvH.singleton, () -> {
        try (var stmt = con.prepareStatement("UPDATE PlayerStats SET lvl = ? WHERE uuid = ?"))
        {
          setupPreparedStatement(stmt);

          stmt.setString(2, player.getUniqueId().toString());
          stmt.setInt(1, new_lvl);
          stmt.executeUpdate();

        } catch (SQLException e)
        {
          handleSQLException(e, player);
        }
      });
    }
  }

  public static void S_changeXp(Collection<Player> players, int amount)
  {
    var to_update = new HashMap<Player, Integer>();

    for (var p : players)
    {
      var xp = getCachedIntStat(p, "xp");
      if (!xp.isPresent())
        continue;
      int old_xp = xp.getAsInt();

      var lvl = getCachedIntStat(p, "lvl");
      if (!lvl.isPresent())
        return;
      int old_lvl = lvl.getAsInt();

      int new_xp = old_xp + amount;
      int new_lvl = Rewards.calcLvl(new_xp);

      caches.get("xp").get(p).setValue(new_xp);
      Rewards.displayExpBar(p, new_lvl, new_xp);

      if (old_lvl < new_lvl)
      {
        caches.get("lvl").get(p).setValue(new_lvl);
        to_update.put(p, Integer.valueOf(new_lvl));
        Rewards.handleLvlUp(p, old_lvl, new_lvl);
      }

      if (to_update.size() == 0)
        return;
    }
    if (!verifyConnection())
      return;

    Bukkit.getScheduler().runTaskAsynchronously(ZvH.singleton, () -> {
      try (var stmt = con.prepareStatement("UPDATE PlayerStats SET lvl = ? WHERE uuid = ?"))
      {
        setupPreparedStatement(stmt);

        for (var e : to_update.entrySet())
        {
          stmt.setString(2, e.getKey().getUniqueId().toString());
          stmt.setInt(1, e.getValue().intValue());
          stmt.addBatch();
        }

        stmt.executeBatch();

      } catch (SQLException e)
      {
        handleSQLException(e, to_update.keySet());
      }
    });
  }

  public static void changeIntStats(Collection<Player> players,
      @SuppressWarnings("unchecked") Pair<String, Integer>... stats)
  {
    var sb = new StringBuilder("UPDATE PlayerStats SET ");
    for (var s : stats)
    {
      sb.append(s.getLeft());
      sb.append('=');
      sb.append(s.getLeft());
      sb.append("+?, ");

      var stat_cache = caches.get(s.getLeft());
      if (stat_cache != null)
      {
        for (var p : players)
        {
          var cache = stat_cache.get(p);
          if (cache != null)
          {
            cache.add(s.getRight());
          }
        }
      }
    }
    if (!verifyConnection())
      return;

    sb.deleteCharAt(sb.length() - 2);
    sb.append("WHERE uuid = ?");

    try (var stmt = con.prepareStatement(sb.toString()))
    {
      setupPreparedStatement(stmt);

      for (var p : players)
      {
        stmt.setString(stats.length + 1, p.getUniqueId().toString());
        for (int i = 0; i < stats.length; i++)
        {
          stmt.setInt(i + 1, stats[i].getRight().intValue());
        }
        stmt.addBatch();
      }
      stmt.executeBatch();

    } catch (SQLException e)
    {
      handleSQLException(e, players);
    }
  }

  public static void changeIntStats(Player player, @SuppressWarnings("unchecked") Pair<String, Integer>... stats)
  {
    var sb = new StringBuilder("UPDATE PlayerStats SET ");
    for (var s : stats)
    {
      sb.append(s.getLeft());
      sb.append('=');
      sb.append(s.getLeft());
      sb.append("+?, ");

      var stat_cache = caches.get(s.getLeft());
      if (stat_cache != null)
      {
        var cache = stat_cache.get(player);
        if (cache != null)
        {
          cache.add(s.getRight());
        }
      }
    }
    if (!verifyConnection())
      return;

    sb.deleteCharAt(sb.length() - 2);
    sb.append("WHERE uuid = ?");

    try (var stmt = con.prepareStatement(sb.toString()))
    {
      setupPreparedStatement(stmt);

      stmt.setString(stats.length + 1, player.getUniqueId().toString());
      for (int i = 0; i < stats.length; i++)
      {
        stmt.setInt(i + 1, stats[i].getRight().intValue());
      }
      stmt.executeUpdate();

    } catch (SQLException e)
    {
      handleSQLException(e);
    }
  }

  public static void fetchPlayerStats(Player player, Consumer<Map<String, String>> callback)
  {
    if (!verifyConnection())
      return;

    Bukkit.getScheduler().runTaskAsynchronously(ZvH.singleton, () -> {
      try (var stmt = con.prepareStatement(
          "SELECT coins, lvl, xp, z_kills, h_kills, z_wins, h_wins, z_losses, h_losses, play_time FROM PlayerStats WHERE uuid = ?"))
      {
        setupPreparedStatement(stmt);

        stmt.setString(1, player.getUniqueId().toString());
        var result = stmt.executeQuery();
        var stats = new LinkedHashMap<String, String>();

        if (result.next())
        {
          stats.put("coins", Integer.toString(result.getInt(1)));
          stats.put("level", Integer.toString(result.getInt(2)));
          stats.put("xp", Integer.toString(result.getInt(3)));
          stats.put("zombie kills", Integer.toString(result.getInt(4)));
          stats.put("human kills", Integer.toString(result.getInt(5)));
          stats.put("zombie wins", Integer.toString(result.getInt(6)));
          stats.put("human wins", Integer.toString(result.getInt(7)));
          stats.put("zombie losses", Integer.toString(result.getInt(8)));
          stats.put("human losses", Integer.toString(result.getInt(9)));
          stats.put("playtime (min)", Integer.toString(result.getInt(10)));
        }

        Bukkit.getScheduler().runTask(ZvH.singleton, () -> callback.accept(stats));

      } catch (SQLException e)
      {
        handleSQLException(e);
      }
    });
  }

  public static void fetchPlayer(Player player)
  {
    if (!verifyConnection())
    {
      putDefaultsFor(player);
      player.sendMessage("There was an issue with fetching your stats. It has been assumed that you new.");
      return;
    }

    try (var stmt = con.prepareStatement("SELECT coins, lvl, xp, cos_blk, cos_blk_own, cos_par, cos_par_own FROM PlayerStats WHERE uuid = ?"))
    {
      setupPreparedStatement(stmt);

      stmt.setString(1, player.getUniqueId().toString());

      var result = stmt.executeQuery();
      if (result.next())
      {
        caches.get("coins").put(player, new MutableInt(result.getInt(1)));
        caches.get("lvl").put(player, new MutableInt(result.getInt(2)));
        caches.get("xp").put(player, new MutableInt(result.getInt(3)));
        caches.get("cos_blk").put(player, new MutableInt(result.getInt(4)));
        caches.get("cos_blk_own").put(player, new MutableInt(result.getInt(5)));
        caches.get("cos_par").put(player, new MutableInt(result.getInt(6)));
        caches.get("cos_par_own").put(player, new MutableInt(result.getInt(7)));

        if (result.getInt(6) > 0)
        {
          cos_particles.put(player, result.getInt(6));
        }

      } else
      {
        addPlayer(player);
        putDefaultsFor(player);
      }

    } catch (SQLException e)
    {
      handleSQLException(e, player);
      return;
    }
  }

  private static void putDefaultsFor(Player player)
  {
    caches.get("coins").put(player, new MutableInt(Rewards.COINS_JOIN));
    caches.get("lvl").put(player, new MutableInt(0));
    caches.get("xp").put(player, new MutableInt(0));
    caches.get("cos_blk").put(player, new MutableInt(Cosmetics.Blocks.DEFAULT));
    caches.get("cos_blk_own").put(player, new MutableInt(Cosmetics.Blocks.PackMasks.DEFAULT));
    caches.get("cos_par").put(player, new MutableInt(Cosmetics.Particles.DEFAULT));
    caches.get("cos_par_own").put(player, new MutableInt(0));
  }

  public static void cleanCacheFor(Player player)
  {
    for (var c : caches.values())
    {
      c.remove(player);
    }
    cos_particles.remove(player);
  }

  private static boolean verifyConnection()
  {
    try
    {
      if (con == null || !con.isValid(3))
      {
        // Attempt to reconnect
        return login();
      }
      return true;

    } catch (SQLException e1)
    {
      e1.printStackTrace();
      return false;
    }
  }
}
