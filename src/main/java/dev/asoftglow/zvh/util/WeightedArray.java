package dev.asoftglow.zvh.util;

import java.util.List;

public abstract class WeightedArray {
  public interface Item {
    public float weight();
  }

  public static <T extends WeightedArray.Item> T getRandomFrom(List<T> items) {
    // Compute the total weight of all items together.
    // This can be skipped of course if sum is already 1.
    float totalWeight = 0f;
    for (var i : items) {
      totalWeight += i.weight();
    }

    // Now choose a random item.
    int idx = 0;
    for (float r = (float) Math.random() * totalWeight; idx < items.size() - 1; ++idx) {
      r -= items.get(idx).weight();
      if (r <= 0.0)
        break;
    }
    return items.get(idx);
  }

  public static <T extends WeightedArray.Item> T getRandomFrom(T[] items) {
    // Compute the total weight of all items together.
    // This can be skipped of course if sum is already 1.
    float totalWeight = 0f;
    for (var i : items) {
      totalWeight += i.weight();
    }

    // Now choose a random item.
    int idx = 0;
    for (float r = (float) Math.random() * totalWeight; idx < items.length - 1; ++idx) {
      r -= items[idx].weight();
      if (r <= 0.0)
        break;
    }
    return items[idx];
  }
}
