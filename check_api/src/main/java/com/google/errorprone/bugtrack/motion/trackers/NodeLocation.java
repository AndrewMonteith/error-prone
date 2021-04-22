package com.google.errorprone.bugtrack.motion.trackers;

public final class NodeLocation {
  public final long startPos;
  public final long endPos;

  NodeLocation(final long startPos, final long endPos) {
    this.startPos = startPos;
    this.endPos = endPos;
  }

  public static NodeLocation single(final long pos) {
    return new NodeLocation(pos, pos);
  }
}
