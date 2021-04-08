package com.google.errorprone.bugtrack.motion.trackers;

public final class NodeLocation {
  public final long startPos;
  public final long pos;
  public final long endPos;

  NodeLocation(final long startPos, final long pos, final long endPos) {
    this.startPos = startPos;
    this.pos = pos;
    this.endPos = endPos;
  }
}
