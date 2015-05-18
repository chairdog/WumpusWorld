package wumpusenv;

import java.awt.Point;
import java.util.Random;

/**
 * Handles the rules of the game but it does not own a board (the gridworld). If it needs the board, it
 * is passed as a parameter. I introduced a separation of action from percept. Therefore the game has to remember
 * the last percept so that we can return the same percept if the percept is asked again without doing an action.
 * 
 * It contains constants to communicate the actions, and a function to acquire the perceptions of the agent. There
 * is no visualisation of the grid world here. For that you use the WumpusCanvas.
*/
public class TheGame {
	// Following are Action numbers. These numbers are used to indicate which action is requested.
	public static final int FORWARD =1;
	public static final int GRAB = 2;
	public static final int SHOOT = 3;
	public static final int CLIMB = 4;
	public static final int TURN_RIGHT = 5;
	public static final int TURN_LEFT = 6;
	public static final int NO_ACTION = 7;	

	// Following are percept numbers.
	public static final int BREEZE = 1;
	public static final int STENCH = 2;
	public static final int BUMP = 3;
	public static final int SCREAM = 4;
	public static final int GLITTER = 5;
	public static final int NONE = 6;

    // Wumpus CLOCK
    int time = 0;
    Random wumpusSimpleBrain = new Random(); // used to decide how wumpus's going to move in the grid
    int wumpusLastMove = 0;
	
	/**
	 * Percept after the last action has been performed.
	 */
	private WumpusWorldPercept fPercept = new WumpusWorldPercept();
	private int lastscore;
	private int score;

	/**
	 * DOC
	 */
	public TheGame() {
		reset();
	}

	public void reset() {
		score = 0;
	}

	public int getScore() {
		return score;
	}
	
	public int getReward() {
		return score-lastscore;
	}

	/**
	 * Get last percept. A percept can only change by doing an action. If the
	 * agent steps into the Wumpus or a pit it is dead, and cannot perceive
	 * anything anymore.
	 * 
	 * TODO: According to Runner, the 'sense' method should
	 * return null also when the agent is still alive but has finished...
	 * 
	 * @return current percept, or null if the game has finished.
	 */
	public WumpusWorldPercept getPercept(WorldModel pWorld) {
		if (pWorld.gameFinished()) {
			return null;
		}
		return fPercept;
	}

	/**
	Action does the requested action and modifies given world accordingly.
	Also the current score is updated.
	This call originally was named consequence() and immediately returned the Percept. 
	But that was Prolog style stuff. 
	Now you need to call calcPercepts() to get the last percept.
	LastPercept is reset by an action, because part of the percepts is caused by actions
	e.g. hitting a wall gives a "bump" percept. 
	see calcPercepts(WorldModel).
*/

	public void Action(int pAction, WorldModel pWorld) {
	    fPercept.reset();
	    //System.out.println("Agent asked action "+action);

		// If the agent has finished, the agent cannot do anything anymore.
		if (pWorld.gameFinished()) {
			return;
		}

	    int lOrientation = pWorld.getAgentOrientation();
	    Point lAgent = pWorld.getAgentLocation();
	    Point lStepForward = locationAhead(lOrientation, lAgent);

	    // save the current score before updating
	    lastscore = score;
	    
	    // Every action costs one point.
		if (pAction != NO_ACTION) {
			score--;
		}
		
		//
		switch (pAction) {
		case NO_ACTION:
			break;
		case FORWARD:
	        if (pWorld.contains(lStepForward, WorldModel.WALL))
	        	fPercept.setBump(true);
	        else pWorld.setAgentLocation(lStepForward);
	        break;
	    case GRAB:
	        if (pWorld.contains(lAgent, WorldModel.GOLD)) {
	        	pWorld.setAgentHasGold(true);
				score=score+1000;
		        pWorld.removeItem(lAgent, WorldModel.GOLD);
		    }
		    break;
		case SHOOT:
		    if (pWorld.agentHasArrow()) {
				score-=10;
	        	Point p = lStepForward;
	            boolean wumpusHit = false;
	            while(!pWorld.contains(p, WorldModel.WALL)
	                    && !wumpusHit && pWorld.wumpusIsAlive()) {
	                wumpusHit = pWorld.contains(p, WorldModel.WUMPUS);
	                p = locationAhead(lOrientation, p);
	            }
	            pWorld.setAgentHasArrow(false);
	            if (wumpusHit) {
	                pWorld.setWumpusIsAlive(false);
	                fPercept.setScream(true);
	            }
	        }
	        break;
	    case CLIMB:
	        if (lAgent.equals(pWorld.getStartLocation())) {
	        	System.out.println("Agent succesfully climbed out Wumpus World.");
				pWorld.setAgentClimbedOut();
	            return;
	        }
	        break;
		case TURN_RIGHT:
	    	pWorld.setAgentOrientation((lOrientation + 270 ) % 360);
	    	break;
	    case TURN_LEFT:
	        pWorld.setAgentOrientation((lOrientation + 90 ) % 360);
			break;
		default:
			System.out.println("Warning: Ignoring unknown action "+pAction);
		}
		if (pWorld.agentKilled()) {
			score = score-1000; // just died...
		}
        time++;
		// modified by Yingzhi
		if(pWorld.wumpusIsAlive()) {
			// now is the wumpus's move
			// wumpus moves 1 time when every 5 moves the agent makes
			if(time - wumpusLastMove > 4) {
				System.out.print("[WUMPUS] IT IS TIME TO MOVE!!! ROARRRRRRR");
				Point wumpus = pWorld.getWumpusLocation();
				int diff = (wumpusSimpleBrain.nextBoolean() ? 1 : -1); // decide the direction
				if (wumpusSimpleBrain.nextBoolean()) {
					//move along x
					wumpus.x += diff;
				} else {
					// move along y
					wumpus.y += diff;
				}
				// check if the move is valid
				if (!pWorld.contains(wumpus, pWorld.WALL) && !pWorld.contains(wumpus, pWorld.PIT) && !pWorld.contains(wumpus, pWorld.GOLD)) {
                    pWorld.removeSmell();
                    pWorld.setWumpusLocation(wumpus);
                    pWorld.addSmell();
					wumpusLastMove=time;
				}
			}
		}

	    calcPercepts(pWorld);
	}

	/** 
	 * Compute the latest percept. This code is still messy, following original code.
	 * Probably it would be neater to put this stuff in Sense().
	 * IMPORTANT NOTE: you smell stench 
	 * @return the latest percept, or null if the game has finished.
	*/
    private void calcPercepts(WorldModel pWorld) {
        Point agent = pWorld.getAgentLocation();

        boolean stench = false;
        boolean breeze = false;
        for (int i = 0; i <= 270; i+= 90) {
            stench |= pWorld.contains(locationAhead(i, agent), WorldModel.WUMPUS);
            breeze |= pWorld.contains(locationAhead(i, agent), WorldModel.PIT);
        }
        fPercept.setStench(stench);
        fPercept.setBreeze(breeze);
        
        if (pWorld.contains(agent, WorldModel.GOLD)) {
        	fPercept.setGlitter(true);
        }
    }
    
    /**
     * locationAhead is a help function that might be better placed in WorldModel.
     */
	private static Point locationAhead(int pOrientation, Point p) {
        switch (pOrientation) {
            case 0:
                return new Point(p.x + 1, p.y);
            case 90:
                return new Point(p.x, p.y + 1);
            case 180:
                return new Point(p.x - 1, p.y);
            case 270:
                return new Point(p.x, p.y - 1);
            default:
                return p;
        }
    }
}
