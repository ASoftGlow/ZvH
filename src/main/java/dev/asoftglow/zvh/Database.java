package dev.asoftglow.zvh;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;

import dev.asoftglow.zvh.util.Util;

public abstract class Database
{
  static private final Map<String, Map<Player, MutableInt>> caches = new HashMap<>(4);
  static private Connection con = null;
  static
  {
    caches.put("coins", new HashMap<>());
    caches.put("xp", new HashMap<>());
    caches.put("lvl", new HashMap<>());
  }

  public enum AsyncTarget
  {
    MainToMain, MainToAsync, AsyncToMain
  }

  public static void login(String url, String name, String username, String password)
  {
    Logger.Get().info("Logging in to %s as %s".formatted(url, username));
    try
    {
      Class.forName("org.mariadb.jdbc.Driver");
      con = DriverManager.getConnection("jdbc:mariadb://" + url, username, password);
      con.createStatement().executeUpdate("USE " + name);

      Logger.Get().info("Connected to " + url);

    } catch (SQLException e)
    {
      Logger.Get().warning("Failed to connect to db");
      handleSQLException(e);

    } catch (ClassNotFoundException e)
    {
      Logger.Get().warning("Failed to get driver");
      e.printStackTrace();
    }
  }

  public static void addPlayer(Player player)
  {
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

  public static void logout()
  {
    if (con == null)
      return;
    try
    {
      con.close();
    } catch (SQLException e)
    {
      e.printStackTrace();
    }
  }

  private static void handleSQLException(SQLException e)
  {
    Logger.Get().warning(e.getMessage() + " from " + e.getStackTrace()[0].toString());
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

  public static Map<OfflinePlayer, Integer> getIntStatLeaderboard(String stat, int limit)
  {
    try (var stmt = con
        .prepareStatement("SELECT uuid, " + stat + " FROM PlayerStats ORDER BY " + stat + " DESC LIMIT " + limit))
    {
      setupPreparedStatement(stmt);

      var players = new HashMap<OfflinePlayer, Integer>();
      var result = stmt.executeQuery();

      while (result.next())
      {
        var player = Bukkit.getOfflinePlayer(UUID.fromString(result.getString("uuid")));
        players.put(player, result.getInt(2));
      }
      return players;

    } catch (SQLException e)
    {
      handleSQLException(e);
      return null;
    }
  }

  public static void getIntStat(Player player, String stat, Consumer<OptionalInt> callback)
  {
    getIntStat(player, stat, callback, AsyncTarget.MainToMain);
  }

  public static void getIntStat(Player player, String stat, Consumer<OptionalInt> callback, AsyncTarget target)
  {
    var cached_stat = caches.get(stat);
    if (cached_stat != null)
    {
      var cached_value = cached_stat.get(player);
      if (cached_value != null)
      {
        callback.accept(OptionalInt.of(cached_value.intValue()));
        return;
      }
    }

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

  // public static int[] getIntStats(Player player, String... stats)
  // {
  // var values = new int[stats.length];

  // for (int i = 0; i < values.length; i++)
  // {
  // var cached_stat = caches.get(stats[i]);
  // if (cached_stat != null)
  // {
  // var cached_value = cached_stat.get(player);
  // if (cached_value != null)
  // {
  // values[i] = cached_value.intValue();
  // }
  // }
  // }

  // try (var stmt = con.prepareStatement("SELECT " + stats + " FROM PlayerStats
  // WHERE uuid = ?"))
  // {
  // setupPreparedStatement(stmt);

  // stmt.setString(1, player.getUniqueId().toString());

  // var result = stmt.executeQuery();
  // if (result.next())
  // {
  // return OptionalInt.of(result.getInt(1));
  // }
  // return null;

  // } catch (SQLException e)
  // {
  // handleSQLException(e);
  // return null;
  // }
  // }

  public static void changeIntStat(Player player, String stat, int amount)
  {
    var cached_stat = caches.get(stat);
    if (cached_stat != null)
    {
      var cached_value = cached_stat.get(player);
      if (cached_value != null)
      {
        cached_value.add(amount);
      }
    }

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

  public static void changeIntStat(Collection<Player> players, String stat, int amount)
  {
    var cached_stat = caches.get(stat);
    if (cached_stat != null)
    {
      var cached_value = cached_stat.get(player);
      if (cached_value != null)
      {
        cached_value.add(amount);
      }
    }

    try (var stmt = con.prepareStatement("UPDATE PlayerStats SET " + stat + " = " + stat + " + ? WHERE uuid = ?"))
    {
      setupPreparedStatement(stmt);

      stmt.setArray(2, con.createArrayOf("VARCHAR", Util.getUUIDs(players)));
      stmt.setInt(1, amount);
      stmt.executeUpdate();

    } catch (SQLException e)
    {
      handleSQLException(e, players);
    }
  }

  public static void S_changeXp(Player player, int amount)
  {
    getIntStat(player, "xp", xp -> {
      if (!xp.isPresent())
        return;
      int old_xp = xp.getAsInt();

      getIntStat(player, "lvl", lvl -> {
        if (!lvl.isPresent())
          return;

        int old_lvl = lvl.getAsInt();
        int new_xp = old_xp + amount;
        int new_lvl = Rewards.calcLvl(new_xp);

        if (old_lvl < new_lvl)
        {
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
        }
        Rewards.displayExpBar(player, new_lvl, new_xp);

      }, AsyncTarget.AsyncToMain);
    }, AsyncTarget.MainToAsync);
  }

  public static void S_changeXp(Collection<Player> players, int amount)
  {
    getIntStat(players, "xp", xp -> {
      if (!xp.isPresent())
        return;
      int old_xp = xp.getAsInt();

      getIntStat(player, "lvl", lvl -> {
        if (!lvl.isPresent())
          return;

        int old_lvl = lvl.getAsInt();
        int new_xp = old_xp + amount;
        int new_lvl = Rewards.calcLvl(new_xp);

        if (old_lvl < new_lvl)
        {
          try (var stmt = con.prepareStatement("UPDATE PlayerStats SET lvl = ? WHERE uuid = ?"))
          {
            setupPreparedStatement(stmt);

            stmt.setString(2, player.getUniqueId().toString());
            stmt.setInt(1, new_lvl);
            stmt.executeUpdate();

          } catch (SQLException e)
          {
            handleSQLException(e, players);
          }
        }
        Rewards.displayExpBar(player, new_lvl, new_xp);

      }, AsyncTarget.AsyncToMain);
    }, AsyncTarget.MainToAsync);
  }

  public static void addIntStat(Collection<Player> players, String stat)
  {
    try (var stmt = con.prepareStatement("UPDATE PlayerStats SET " + stat + " = " + stat + " + 1 WHERE uuid IN ?"))
    {
      setupPreparedStatement(stmt);

      stmt.setArray(2, con.createArrayOf("VARCHAR", Util.getUUIDs(players)));
      stmt.executeUpdate();

    } catch (SQLException e)
    {
      handleSQLException(e, players);
    }
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
      sb.append("'+?, ");
    }
    sb.deleteCharAt(sb.length() - 2);
    sb.append("WHERE uuid IN ?");

    try (var stmt = con.prepareStatement(sb.toString()))
    {
      setupPreparedStatement(stmt);

      stmt.setArray(stats.length, con.createArrayOf("VARCHAR", Util.getUUIDs(players)));
      for (int i = 0; i < stats.length; i++)
      {
        stmt.setInt(i, stats[i].getRight().intValue());
      }
      stmt.executeUpdate();

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
      sb.append("'+?, ");

      var stat_cache = caches.get(s.getLeft());
      if (stat_cache != null)
      {
        var cache = stat_cache.get(player);
        if (cache == null)
        {
          stat_cache.put(player, new MutableInt())
        }
      }
      
    }
    sb.deleteCharAt(sb.length() - 2);
    sb.append("WHERE uuid IN ?");

    try (var stmt = con.prepareStatement(sb.toString()))
    {
      setupPreparedStatement(stmt);

      stmt.setString(stats.length, player.getUniqueId().toString());
      for (int i = 0; i < stats.length; i++)
      {
        stmt.setInt(i, stats[i].getRight().intValue());
      }
      stmt.executeUpdate();

    } catch (SQLException e)
    {
      handleSQLException(e);
    }
  }

  public static void fetchPlayer(Player player)
  {
    try (var stmt = con.prepareStatement("SELECT coins, lvl, xp FROM PlayerStats WHERE uuid = ?"))
    {
      setupPreparedStatement(stmt);

      stmt.setString(1, player.getUniqueId().toString());

      var result = stmt.executeQuery();
      if (result.next())
      {
        caches.get("coins").put(player, new MutableInt(result.getInt(1)));
        caches.get("lvl").put(player, new MutableInt(result.getInt(2)));
        caches.get("xp").put(player, new MutableInt(result.getInt(3)));
      } else
      {
        addPlayer(player);
      }

    } catch (SQLException e)
    {
      handleSQLException(e, player);
      return;
    }
  }

  public static void cleanCacheFor(Player player)
  {
    for (var c : caches.values())
    {
      c.remove(player);
    }
  }

  class UpdateBuilder
  {
    private HashMap<String, Integer> stats = new HashMap<>();

    public UpdateBuilder change(String stat, int amount)
    {
      stats.put(stat, amount);
      return this;
    }

    public UpdateBuilder(Player for_player)
    {

    }

    public UpdateBuilder(Collection<Player> for_players)
    {

    }

    public void execute()
    {
      var sb = new StringBuilder("UPDATE PlayerStats SET ");

      try (var stmt = con.prepareStatement("UPDATE PlayerStats SET WHERE uuid IN ?"))
      {
        setupPreparedStatement(stmt);

        // stmt.setArray(3, );

      } catch (SQLException e)
      {
        handleSQLException(e);
      }
    }
  }
}
