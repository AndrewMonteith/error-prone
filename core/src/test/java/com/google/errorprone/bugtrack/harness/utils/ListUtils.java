package com.google.errorprone.bugtrack.harness.utils;

import com.google.common.collect.ImmutableList;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

public class ListUtils {
  private ListUtils() {}

  public static <T> List<T> distinct(Iterable<T> collection) {
    Set<T> items = new HashSet<>();
    collection.forEach(items::add);
    return ImmutableList.copyOf(items);
  }

  public static <T> void consecutivePairs(Iterable<T> items, BiConsumer<T, T> callback) {
    Iterator<T> iter = items.iterator();

    T current = iter.next();
    while (iter.hasNext()) {
      T next = iter.next();
      callback.accept(current, next);
      current = next;
    }
  }

  public static <T> void enumerate(Iterable<T> items, BiConsumer<Integer, T> callback) {
    int i = 0;
    for (T item : items) {
      callback.accept(i, item);
      ++i;
    }
  }
}
