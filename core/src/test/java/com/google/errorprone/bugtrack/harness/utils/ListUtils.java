package com.google.errorprone.bugtrack.harness.utils;

import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ListUtils {
    private ListUtils() {}

    public static <T> List<T> distinct(Iterable<T> collection) {
        Set<T> items = new HashSet<>();
        collection.forEach(items::add);
        return ImmutableList.copyOf(items);
    }

}
