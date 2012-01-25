#ifndef __PLAYER_H__
#define __PLAYER_H__

/** Simple 2D Point/Vector representation. */
struct Vector2D {
  /** X coordinate of this point/vector. */
  double x;

  /** Y coordinate of this point/vector. */
  double y;

  /** Initialize with given coordinates. */
  Vector2D( double xv = 0, double yv = 0 ) {
    x = xv;
    y = yv;
  }

#endif
