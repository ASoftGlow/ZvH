package dev.asoftglow.zvh.util;

import java.util.ArrayDeque;

public class LimitedStack<T> extends ArrayDeque<T> {
  private int maxLength;

  public LimitedStack(int maxLength) {
    super();
    this.maxLength = maxLength;
  }

  @Override
  public void push(T item) {
    super.push(item);
    if (size() > maxLength) {
      super.removeLast();
    }
  }
}