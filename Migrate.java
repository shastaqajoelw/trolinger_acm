/** A sample player implemented in Java.  In this player, each pusher
    takes one of the red markers and tries to gradually move it to
    vertices that are on the boundary between red and non-red.

    ICPC Challenge
    Sturgill, NC State University */

import java.util.Scanner;
import java.util.Random;
import java.util.ArrayList;
import java.awt.geom.Point2D;

public class Migrate {
  /** Width and height of the world in game units. */
  public static final int FIELD_SIZE = 100;
  
  /** Number of pushers per side. */
  public static final int PCOUNT = 3;

  /** Radius of the pusher.  */
  public static final double PUSHER_RADIUS = 1;

  /** Mass of the pusher.  */
  public static final double PUSHER_MASS = 1;

  /** Maximum velocity for a pusher. */
  public static final double PUSHER_SPEED_LIMIT = 6.0;
  
  /** Maximum acceleration for a pusher. */
  public static final double PUSHER_ACCEL_LIMIT = 2.0;
  
  /** Total number of markers on the field. */
  public static final int MCOUNT = 22;

  /** Radius of the marker. */
  public static final double MARKER_RADIUS = 2;

  /** Mass of the marker. */
  public static final double MARKER_MASS = 3;

  /** Marker velocity lost per turn */
  public static final double MARKER_FRICTION = 0.35;

  /** Width and height of the home region. */
  public static final int HOME_SIZE = 20;

  /** Color value for the red player. */
  public static final int RED = 0;
  
  /** Color value for the blue player. */
  public static final int BLUE = 1;
  
  /** Color value for unclaimed pucks. */
  public static final int GREY = 2;

  /** Source of Randomness */
  private static Random rnd = new Random();

  /** Simple representation for a vertex of the map. */
  private static class Vertex3D {
    int x, y, z;
  };

  /** Simple representation for a pusher. */
  private static class Pusher {
    // Position of the pusher.
    Point2D pos;
    
    // Pusher velocity
    Point2D vel;
    
    // True if this pusher has a job.
    boolean busy;
    
    // How long we've been doing the current job.  If
    // this number gets to large, we'll pick a new job.
    int jobTime;
    
    // Target vertex for this pusher.
    int targetVertex;
    
    Pusher() {
      busy = false;
    }
  };

  /** Simple representation for a marker. */
  private static class Marker {
    // Position of the marker.
    Point2D pos;

    // Marker velocity
    Point2D vel;

    // Marker color
    int color;
  };

  /** Return the value of a, clamped to the [ b, c ] range */
  private static double clamp( double a, double b, double c ) {
    if ( a < b )
      return b;
    if ( a > c )
      return c;
    return a;
  }

  /** Return a new vector containing the sum of a and b. */
  static Point2D sum( Point2D a, Point2D b ) {
    return new Point2D.Double( a.getX() + b.getX(), 
                               a.getY() + b.getY() );
  }

  /** Return a new vector containing the difference between a and b. */
  static Point2D diff( Point2D a, Point2D b ) {
    return new Point2D.Double( a.getX() - b.getX(), 
                               a.getY() - b.getY() );
  }

  /** Return a new vector containing a scaled by scaling factor s. */
  static Point2D scale( Point2D a, double s ) {
    return new Point2D.Double( a.getX() * s, a.getY() * s );
  }

  /** Return a new vector thats the given vector rotated by r CCW. */
  static Point2D rotate( Point2D a, double r ) {
    double s = Math.sin( r );
    double c = Math.cos( r );
    return new Point2D.Double( a.getX() * c - a.getY() * s,
                               a.getX() * s + a.getY() * c );
  }

  /** Return the magnitude of vector a. */
  static double mag( Point2D a ) {
    return Math.sqrt( a.getX() * a.getX() + a.getY() * a.getY() );
  }

  /** Return a new vector containing normalized version of a. */
  static Point2D norm( Point2D a ) {
    double m = mag( a );
    return new Point2D.Double( a.getX() / m,
                               a.getY() / m );
  }

  /** Return a ccw perpendicular vector for a. */
  static Point2D perp( Point2D a ) {
    return new Point2D.Double( -a.getY(), a.getX() );
  }

  /** Return the dot product of a and b. */
  static double dot( Point2D a, Point2D b ) {
    return a.getX() * b.getX() + a.getY() * b.getY();
  }

  /** Return the cross product of a and b. */
  static double cross( Point2D a, Point2D b ) {
    return a.getX() * b.getY() - a.getY() * b.getX();
  }

  /** Return a vector pointing in the same direction as v, but with
      magnitude no greater than d. */
  static Point2D limit( Point2D v, double d ) {
    double m = mag( v );
    if ( m > d )
      return new Point2D.Double( d * v.getX() / m, d * v.getY() / m );
    return v;
  }

  /** One dimensional function to help compute acceleration
      vectors. Return an acceleration that can be applied to a pusher
      at pos and moving with velocity vel to get it to target.  The
      alim parameter puts a limit on the acceleration available.  This
      function is used by the two-dimensional moveTo function to
      compute an acceleration vector toward the target after movement
      perp to the target direction has been cancelled out.  */
  private static double moveTo( double pos, double vel, double target,
                                double alim ) {
    // Compute how far pos has to go to hit target.
    double dist = target - pos;

    // Kill velocity if we are close enough.
    if ( Math.abs( dist ) < 0.01 )
      return clamp( -vel, -alim, alim );
    
    // How many steps, at minimum, would cover the remaining distance
    // and then stop.
    double steps = Math.ceil(( -1 + Math.sqrt(1 + 8.0 * Math.abs(dist) / alim)) 
                             / 2.0);
    if ( steps < 1 )
      steps = 1;
    
    // How much acceleration would we need to apply at each step to
    // cover dist.
    double accel = 2 * dist / ( ( steps + 1 ) * steps );
    
    // Ideally, how fast would we be going now
    double ivel = accel * steps;

    // Return the best change in velocity to get vel to ivel.
    return clamp( ivel - vel, -alim, alim );
  }

  /** Print out a force vector that will move the given pusher to
      the given target location. */
  private static void moveTo( Pusher p, Point2D target ) {
    // Compute a frame with axis a1 pointing at the target.
    Point2D a1, a2;

    // Build a frame (a trivial one if we're already too close).
    double dist = target.distance( p.pos );
    if ( dist < 0.0001 ) {
      a1 = new Point2D.Double( 1.0, 0.0 );
      a2 = new Point2D.Double( 0.0, 1.0 );
    } else {
      a1 = scale( diff( target, p.pos ), 1.0 / dist );
      a2 = perp( a1 );
    }
        
    // Represent the pusher velocity WRT that frame.
    double v1 = dot( a1, p.vel );
    double v2 = dot( a2, p.vel );

    // Compute a force vector in this frame, first cancel out velocity
    // perp to the target.
    double f1 = 0;
    double f2 = -v2;

    // If we have remaining force to spend, use it to move toward the target.
    if ( Math.abs( f2 ) < PUSHER_ACCEL_LIMIT ) {
      double raccel = Math.sqrt( PUSHER_ACCEL_LIMIT * PUSHER_ACCEL_LIMIT - 
                                 v2 * v2 );
      f1 = moveTo( -dist, v1, 0.0, raccel );
    }

    // Convert force 
    Point2D force = sum( scale( a1, f1 ), scale( a2, f2 ) );
    System.out.print( force.getX() + " " + force.getY() );
  }

  /** Print out a force vector that will move the given pusher around
      to the side of marker m that's opposite from target.  Return true
      if we're alreay behind the marker.  */
  private static boolean moveAround( Pusher p, Marker m, Point2D target ) {
    // Compute vectors pointing from marker-to-target and Marker-to-pusher
    Point2D mToT = norm( diff( target, m.pos ) );
    Point2D mToP = norm( diff( p.pos, m.pos ) );
    
    // See if we're already close to behind the marker.
    if ( dot( mToT, mToP ) < -0.8 )
      return true;

    // Figure out how far around the target we need to go, we're
    // going to move around a little bit at a time so we don't hit
    // the target.
    double moveAngle = Math.acos( dot( mToT, mToP ) );
    if ( moveAngle > Math.PI * 0.25 )
      moveAngle = Math.PI * 0.25;

    // We're not, decide which way to go around.
    if ( cross( mToT, mToP ) > 0 ) {
      // Try to go around to the right.
      moveTo( p, sum( m.pos, scale( rotate( mToP, moveAngle ), 4 ) ) );
    } else {
      // Try to go around to the left.
      moveTo( p, sum( m.pos, scale( rotate( mToP, -moveAngle ), 4 ) ) );
    }

    return false;
  }

  public static void main( String[] args ) {
    // Scanner to parse input from the game engine.
    Scanner in = new Scanner( System.in );

    // current score for each player.
    int[] score = new int [ 2 ];

    // Read the list of vertex locations.
    int n = in.nextInt();
    // List of points in the map.
    Vertex3D[] vertexList = new Vertex3D [ n ];
    for ( int i = 0; i < n; i++ ) {
      vertexList[ i ] = new Vertex3D();
      vertexList[ i ].x = in.nextInt();
      vertexList[ i ].y = in.nextInt();
      vertexList[ i ].z = in.nextInt();
    }

    // Read the list of region outlines.
    n = in.nextInt();
    // List of regions in the map
    int[][] regionList = new int [ n ] [];
    for ( int i = 0; i < n; i++ ) {
      int m = in.nextInt();;
      regionList[ i ] = new int [ m ];
      for ( int j = 0; j < m; j++ )
        regionList[ i ][ j ] = in.nextInt();
    }

    // List of current region colors, pusher and marker locations.
    // These are updated on every turn snapshot from the game.
    int[] regionColors = new int [ regionList.length ];
    Pusher[] pList = new Pusher [ 2 * PCOUNT ];
    for ( int i = 0; i < pList.length; i++ )
      pList[ i ] = new Pusher();
    Marker[] mList = new Marker [ MCOUNT ];
    for ( int i = 0; i < mList.length; i++ )
      mList[ i ] = new Marker();

    int turnNum = in.nextInt();
    while ( turnNum >= 0 ) {
      score[ RED ] = in.nextInt();
      score[ BLUE ] = in.nextInt();

      // Read all the region colors.
      n = in.nextInt();
      for ( int i = 0; i < regionList.length; i++ )
        regionColors[ i ] = in.nextInt();

      // Read all the pusher locations.
      n = in.nextInt();
      for ( int i = 0; i < pList.length; i++ ) {
        double x = in.nextDouble();
        double y = in.nextDouble();
        pList[ i ].pos = new Point2D.Double( x, y );
        x = in.nextDouble();
        y = in.nextDouble();
        pList[ i ].vel = new Point2D.Double( x, y );
      }

      // Read all the marker locations.
      n = in.nextInt();
      for ( int i = 0; i < n; i++ ) {
        double x = in.nextDouble();
        double y = in.nextDouble();
        mList[ i ].pos = new Point2D.Double( x, y );
        x = in.nextDouble();
        y = in.nextDouble();
        mList[ i ].vel = new Point2D.Double( x, y );
        mList[ i ].color = in.nextInt();
      }
    
      // Compute a bit vector for the region colors incident on each
      // vertex.
      int[] vertexColors = new int [ vertexList.length ];
      for ( int i = 0; i < regionList.length; i++ )
        for ( int j = 0; j < regionList[ i ].length; j++ )
          vertexColors[ regionList[ i ][ j ] ] |= ( 1 << regionColors[ i ] );
      
      // Candidate vertices for putting a marker on, vertices that have
      // some red but are not all red.
      ArrayList< Integer > candidates = new ArrayList< Integer >();
      for ( int i = 0; i < vertexList.length; i++ )
        if ( ( vertexColors[ i ] & 0x1 ) == 1 &&
             vertexColors[ i ] != 1  )
          candidates.add( i );

      // Choose a next action for each pusher, each pusher is responsible
      // for the marker with the same index.
      for ( int pdex = 0; pdex < PCOUNT; pdex++ ) {
        Pusher p = pList[ pdex ];
      
        // See how long this pusher has been doing its job.
        if ( p.busy ) {
          // Go to idle if we work to long on the same job.
          p.jobTime++;
          if ( p.jobTime >= 60 )
            p.busy = false;
        }

        // If we lose our marker, then just sit idle.
        if ( mList[ pdex ].color != RED ) {
          p.busy = false;
        }
        
        // Otherwise, try to find a new place to push our marker.
        if ( mList[ pdex ].color == RED &&
             !p.busy ) {
          if ( candidates.size() > 0 ) {
            int choice = rnd.nextInt( candidates.size() );
            p.targetVertex = candidates.get( choice );
            candidates.remove( choice );
            p.busy = true;
          }
        }

        // Choose a move direction in support of our current goal.
        if ( p.busy ) {
          // Get behind our marker and push it toward its destination.
          Vertex3D v = vertexList[ p.targetVertex ];
          Point2D dest = new Point2D.Double( v.x, v.y );
          if ( moveAround( p, mList[ pdex ], dest ) ) {
            Point2D mToD = norm( diff( dest, mList[ pdex ].pos ) );
            moveTo( p, diff( mList[ pdex ].pos, mToD ) );
          }
        } else
          System.out.print( "0.0 0.0" );

        // Print a space or a newline depending on whether we're at
        // the last pusher.
        if ( pdex + 1 < PCOUNT )
          System.out.print( " " );
        else
          System.out.println();
      }

      turnNum = in.nextInt();
    }
  }
}
