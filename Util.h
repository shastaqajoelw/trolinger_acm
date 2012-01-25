#ifndef __CHALLENGE_UTIL_H__
#define __CHALLENGE_UTIL_H__

// Supporting functions for a simple player.  Feel free to use this
// code or write better code for yourself.
//
// ICPC Challenge
// Sturgill, NC State University

#include <cmath>
#include <iostream>

/** Width and height of the world in game units. */
const double FIELD_SIZE = 100;
  
/** Number of pushers per side. */
const int PCOUNT = 3;

/** Radius of the pusher.  */
const double PUSHER_RADIUS = 1;

/** Mass of the pusher.  */
const double PUSHER_MASS = 1;

/** Maximum velocity for a pusher. */
const double PUSHER_SPEED_LIMIT = 6.0;
  
/** Maximum acceleration for a pusher. */
const double PUSHER_ACCEL_LIMIT = 2.0;
  
/** Radius of the marker. */
const double MARKER_RADIUS = 2;

/** Mass of the marker. */
const double MARKER_MASS = 3;

/** Marker velocity lost per turn */
const double MARKER_FRICTION = 0.35;

/** Width and height of the home region. */
const int HOME_SIZE = 20;

/** Color values for the two sides and for neutral.  The player can
    always think of itself as red. */
enum GameColor {
  RED = 0, BLUE = 1, GREY = 2
};

/** Simple 2D Point/Vector representation along with common utility
    functions. */
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

  /** Return the squared magnitude of this vector. */
  double squaredMag() const {
    return x * x + y * y;
  }

  /** Return the magnitude of this vector. */
  double mag() const {
    return std::sqrt( x * x + y * y );
  }

  /** Return a unit vector pointing in the same direction as this. */
  Vector2D norm() const {
    double m = mag();
    return Vector2D( x / m, y / m );
  }

  /** Return a CCW perpendicular to this vector.*/
  Vector2D perp() const {
    return Vector2D( -y, x );
  }

  /** Return a cross product of this and b. */
  double cross( Vector2D const &b ) const {
    return x * b.y - y * b.x;
  }

  /** Return a vector pointing in the same direction as this, but with
      magnitude no greater than d. */
  Vector2D limit( double d ) const {
    double m = mag();
    if ( m > d )
      return Vector2D( d * x / m, d * y / m );
    else
      return *this;
  }
};

/** Return a vector that's the sum of a and b. */
Vector2D operator+( Vector2D const &a, Vector2D const &b ) {
  return Vector2D( a.x + b.x, a.y + b.y );
}

/** Return a vector that's a minus b. */
Vector2D operator-( Vector2D const &a, Vector2D const &b ) {
  return Vector2D( a.x - b.x, a.y - b.y );
}

/** Return a copy of a that's a scaled by b. */
Vector2D operator*( Vector2D const &a, double b ) {
  return Vector2D( a.x * b, a.y * b );
}

/** Return a copy of a that's a scaled by b. */
Vector2D operator*( double b, Vector2D const &a ) {
  return Vector2D( b * a.x, b * a.y );
}

/** Return the dot product of a and b. */
double operator*( Vector2D const &a, Vector2D const &b ) {
  return a.x * b.x + a.y * b.y;
}

/** Print out the contents of a given vector */
std::ostream &operator<<( std::ostream &stream, Vector2D const &a ) {
  stream << "( " << a.x << ", " << a.y << " )"; 
  return stream;
}

/** Simple 3D Point/Vector representation. */
struct Vector3D {
  /** X coordinate of this point/vector. */
  double x;

  /** Y coordinate of this point/vector. */
  double y;

  /** Z coordinate of this point/vector. */
  double z;

  /** Initialize with given coordinates. */
  Vector3D( double xv = 0, double yv = 0, double zv = 0 ) {
    x = xv;
    y = yv;
    z = zv;
  }
};

#endif
