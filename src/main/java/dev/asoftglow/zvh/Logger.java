package dev.asoftglow.zvh;

public abstract class Logger
{
  private static java.util.logging.Logger logger;

  public static java.util.logging.Logger Get()
  {
    return logger;
  }

  public static void Set(java.util.logging.Logger logger)
  {
    Logger.logger = logger;
  }
}
