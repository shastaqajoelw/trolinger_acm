// A sample player implemented in C++.  The player uses the home
// region to try to convert markers to the player's color.  If the
// home region has enough of the player's markers touching it, the
// player tries to move them elsewhere on the field.
//
// ICPC Challenge
// Sturgill, NC State University

#include "Util.h"
#include <vector>
#include <iostream>
#include <list>
#include <cstdlib>

using namespace std;

/** Simple representation for a pusher. */
struct Pusher {
  // Position of the pusher
  Vector2D pos;

  // Pusher velocity
  Vector2D vel;

  // True if this pusher has a job.
  bool busy;

  // How long we've been doing the current job.  If
  // this number gets to large, we'll pick a new job.
  int jobTime;

  // Index of the marker each pusher is working with.
  int mdex;

  // Location the pusher is trying to move it's marker to.
  Vector2D targetPos;

  Pusher() {
    busy = false;
  }
};

/** Simple representation for a marker. */
struct Marker {
  // Position of the marker.
  Vector2D pos;

  // Marker velocity
  Vector2D vel;

  // Marker color
  int color;
};

/** Return a copy of x that's constrained to be between low and high. */
double clamp( double x, double low, double high ) {
  if ( x < low )
    x = low;
  if ( x > high )
    x = high;
  return x;
}

/** Compute a force vector that can be applied to a pusher to get it
    to run through the given target location.  Pos and vel are the
    pusher's current position and velocity.  Target is the position we
    want to run through and force is a returned vector that will move
    the pusher toward the target.  The function returns true until it
    looks like the next move will take us through the target
    location.  */
bool runTo( Vector2D const &pos,
            Vector2D const &vel, 
            Vector2D const &target, 
            Vector2D &force,
            double epsilon = 0.1 ) {
  // Get a unit vector in the direction we need to move.
  Vector2D direction = ( target - pos ).norm();

  // First, cancel out any movement that is perpendicular to the desired
  // movement direction.
  Vector2D perp = direction.perp();
  force = ( -(perp * vel ) * perp ).limit( PUSHER_ACCEL_LIMIT );

  // Use all the residual force to move toward the target.
  double resForce = PUSHER_ACCEL_LIMIT - force.mag();
  force = force + direction.norm() * resForce;

  // See if this move will cross close enough to the target location.
  Vector2D nvel = ( vel + force ).limit( PUSHER_SPEED_LIMIT );
  double t = clamp( ( target - pos ) * nvel / (nvel * nvel), 0, 1 );
  if ( ( pos + t * nvel - target ).mag() < epsilon )
    return true;

  return false;
}

/** Fill in the given force vector with a force intended to move the
    given pusher around behind the given marker so that it can be
    pushed toward the target destination.  Return true if the pusher
    is already behind the marker. */
bool getBehind( Pusher const &p, 
                Marker const &m, 
                Vector2D const &target, 
                Vector2D &force ) {
  // Make sure we're behind the target marker.
  Vector2D mToT = ( target - m.pos ).norm();
  Vector2D pToM = ( m.pos - p.pos ).norm();

  // See if we're already behind the marker.
  if ( mToT * pToM > 0.7 )
    return true;

  // We're not, decide which way to go around.
  if ( pToM.cross( mToT ) > 0 ) {
    // Try to go around to the right.
    force = pToM.perp() * -PUSHER_ACCEL_LIMIT;
  } else {
    // Try to go around to the left.
    force = pToM.perp() * PUSHER_ACCEL_LIMIT;
  }

  // Try to get closer to the marker if we're far away.
  const double maxDist = 8.0;
  const double minDist = 6.0;
  double dist = ( m.pos - p.pos ).mag();
  // Add a vector to help move in or out to get to the right distance.
  // from the marker.
  if ( dist > maxDist ) {
    force = force + pToM * ( dist - maxDist );
  } else if ( dist < minDist ) {
    force = force - pToM * ( minDist - dist );
  } else {
    // cancel out any inward/outward velocity if the distance is good.
    double inward = p.vel * pToM;
    force = force - pToM * inward;
  }

  return false;
}

/** Return true if the given marker is my color and is touching my home
    region. */
bool atHome( Marker &m ) {
  return m.color == RED &&
    m.pos.x < HOME_SIZE + MARKER_RADIUS &&
    m.pos.y < HOME_SIZE + MARKER_RADIUS;
}

/** Return a random field location where we could move a marker. */
Vector2D randomFieldPosition() {
  int range = FIELD_SIZE - MARKER_RADIUS * 2 + 1;
  return Vector2D( MARKER_RADIUS + rand() % range,
                  MARKER_RADIUS + rand() % range );
}

int main() {
  // current score for each player.
  int score[ 2 ];

  // Read the static parts of the map.
  int n, m;

  // Read the list of vertex locations.
  cin >> n;
  // List of points in the map.
  vector< Vector3D > vertexList( n );
  for ( int i = 0; i < n; i++ )
    cin >> vertexList[ i ].x >> vertexList[ i ].y >> vertexList[ i ].z;

  // Read the list of region outlines.
  cin >> n;
  // List of regions in the map
  vector< vector< int > > regionList( n );
  for ( int i = 0; i < n; i++ ) {
    cin >> m;
    regionList[ i ] = vector< int >( m );
    for ( int j = 0; j < m; j++ )
      cin >> regionList[ i ][ j ];
  }

  // List of current region colors, pusher and marker locations.
  // These are updated on every turn snapshot from the game.
  vector< int > regionColors( regionList.size() );
  vector< Pusher > pList( 2 * PCOUNT );
  vector< Marker > mList;

  int turnNum;
  cin >> turnNum;
  while ( turnNum >= 0 ) {
    cin >> score[ RED ] >> score[ BLUE ];

    // Read all the region colors.
    cin >> n;
    for ( unsigned int i = 0; i < regionList.size(); i++ )
      cin >> regionColors[ i ];

    // Read all the pusher locations.
    cin >> n;
    for ( unsigned int i = 0; i < pList.size(); i++ ) {
      Pusher &pusher = pList[ i ];
      cin >> pusher.pos.x >> pusher.pos.y >> pusher.vel.x >> pusher.vel.y;
    }

    // Read all the marker locations.
    cin >> n;
    mList.resize( n );
    for ( int i = 0; i < n; i++ ) {
      Marker &marker = mList[ i ];
      cin >> marker.pos.x >> marker.pos.y >> marker.vel.x >> marker.vel.y
          >> marker.color;
    }
    
    // Choose a next action for each pusher.
    for ( int pdex = 0; pdex < PCOUNT; pdex++ ) {
      Pusher &p = pList[ pdex ];
      
      // See how long this pusher has been doing its job.
      if ( p.busy ) {
        // Go to idle if we work to long on the same job.
        p.jobTime++;
        if ( p.jobTime >= 35 )
          p.busy = false;

        // Go back to idle if we finish our job.
        if ( ( mList[ p.mdex ].pos - p.targetPos ).mag() < 5 ) {
          p.busy = false;
        }
      }

      if ( !p.busy ) {
        // Choose a random marker.
        int mdex = rand() % mList.size();

        // Make sure we don't have a teammate working on this marker.
        bool available = true;
        for ( int j = 0; j < PCOUNT; j++ )
          if ( j != pdex && 
               pList[ j ].busy && 
               pList[ j ].mdex == mdex )
            available = false;

        if ( available ) {
          if ( mList[ mdex ].color == RED ) {
            // Move it to a random spot on the field.
            p.mdex = mdex;
            p.targetPos = randomFieldPosition();
            p.busy = true;
            p.jobTime = 0;
          } else {
            // This marker isn't our color, try to move it to our
            // home and convert it.
            p.mdex = mdex;
            p.targetPos = Vector2D( 10, 10 );
            p.busy = true;
            p.jobTime = 0;
          }
        }
      }

      // Choose a move direction in support of our current goal.
      Vector2D force( 0, 0 );
      if ( p.busy ) {
        Marker &marker = mList[ p.mdex ];
        
        if ( getBehind( p, marker, p.targetPos, force ) ) {
          runTo( p.pos, p.vel, 
                 marker.pos - ( p.targetPos - marker.pos ).norm(),
                 force );
        }
      }

      cout << force.x << " " << force.y << " ";
    }
    cout << endl;

    cin >> turnNum;
  }
}
