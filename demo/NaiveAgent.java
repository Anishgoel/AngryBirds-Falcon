/*****************************************************************************
 ** ANGRYBIRDS AI AGENT FRAMEWORK
 ** Copyright (c) 2014, XiaoYu (Gary) Ge, Stephen Gould, Jochen Renz
 **  Sahan Abeyasinghe,Jim Keys,  Andrew Wang, Peng Zhang
 ** All rights reserved.
 **This work is licensed under the Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License. 
 **To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-sa/3.0/ 
 *or send a letter to Creative Commons, 444 Castro Street, Suite 900, Mountain View, California, 94041, USA.
 *****************************************************************************/
package ab.demo;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ab.demo.other.ActionRobot;
import ab.demo.other.Shot;
import ab.planner.TrajectoryPlanner;
import ab.utils.StateUtil;
import ab.vision.ABObject;
import ab.vision.GameStateExtractor.GameState;
import ab.vision.Vision;
import ab.vision.VisionMBR;
import java.awt.image.*;

import javax.imageio.ImageIO;

public class NaiveAgent implements Runnable {

	private ActionRobot aRobot;
	private Random randomGenerator;
	public int currentLevel = 1;
	public static int time_limit = 12;
	private Map<Integer,Integer> scores = new LinkedHashMap<Integer,Integer>();
	public TrajectoryPlanner tp;
	private boolean firstShot;
	private Point prevTarget;
	// a standalone implementation of the Naive Agent
	public NaiveAgent() {
		
		aRobot = new ActionRobot();
		tp = new TrajectoryPlanner();
		prevTarget = null;
		firstShot = true;
		randomGenerator = new Random();
		// --- go to the Poached Eggs episode level selection page ---
		ActionRobot.GoFromMainMenuToLevelSelection();

	}

	
	// run the client
	public void run() {

		aRobot.loadLevel(currentLevel);
		while (true) {
			GameState state = solve();
			if (state == GameState.WON) {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				int score = StateUtil.getScore(ActionRobot.proxy);
				if(!scores.containsKey(currentLevel))
					scores.put(currentLevel, score);
				else
				{
					if(scores.get(currentLevel) < score)
						scores.put(currentLevel, score);
				}
				int totalScore = 0;
				for(Integer key: scores.keySet()){

					totalScore += scores.get(key);
					System.out.println(" Level " + key
							+ " Score: " + scores.get(key) + " ");
				}
				System.out.println("Total Score: " + totalScore);
				aRobot.loadLevel(++currentLevel);
				// make a new trajectory planner whenever a new level is entered
				tp = new TrajectoryPlanner();

				// first shot on this level, try high shot first
				firstShot = true;
			} else if (state == GameState.LOST) {
				System.out.println("Restart");
				aRobot.restartLevel();
			} else if (state == GameState.LEVEL_SELECTION) {
				System.out
				.println("Unexpected level selection page, go to the last current level : "
						+ currentLevel);
				aRobot.loadLevel(currentLevel);
			} else if (state == GameState.MAIN_MENU) {
				System.out
				.println("Unexpected main menu page, go to the last current level : "
						+ currentLevel);
				ActionRobot.GoFromMainMenuToLevelSelection();
				aRobot.loadLevel(currentLevel);
			} else if (state == GameState.EPISODE_MENU) {
				System.out
				.println("Unexpected episode menu page, go to the last current level : "
						+ currentLevel);
				ActionRobot.GoFromMainMenuToLevelSelection();
				aRobot.loadLevel(currentLevel);
			}

		}

	}

	private double distance(Point p1, Point p2) {
		return Math
				.sqrt((double) ((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y)
						* (p1.y - p2.y)));
	}

	public GameState solve()
	{

		// capture Image
		BufferedImage screenshot = ActionRobot.doScreenShot();

		// process image
		Vision vision = new Vision(screenshot);

		// find the slingshot
		Rectangle sling = vision.findSlingshotMBR();

		// confirm the slingshot
		while (sling == null && aRobot.getState() == GameState.PLAYING) {
			System.out
			.println("No slingshot detected. Please remove pop up or zoom out");
			ActionRobot.fullyZoomOut();
			screenshot = ActionRobot.doScreenShot();
			vision = new Vision(screenshot);
			sling = vision.findSlingshotMBR();
		}
        // get all the pigs
	 		List<ABObject> pigs = vision.findPigsMBR();

		GameState state = aRobot.getState();

		// if there is a sling, then play, otherwise just skip.
		if (sling != null) {

			if (!pigs.isEmpty()) {

				Point releasePoint = null;
				Shot shot = new Shot();
				int dx,dy;
				{
					VisionMBR MBR = new VisionMBR(screenshot);					 
						
						
						

					int m_wood = 5;
					int m_ice = 4;
					int m_stone = 3;
					int m_pig = 11;
					
					List<Rectangle>  stones = MBR.findStonesMBR();
					List<Rectangle>  woods = MBR.findWoodMBR();
					List<Rectangle>  ice = MBR.findIceMBR();
					Point COM = new Point();
					int total_mass = 0;				
					

					for(Rectangle stone :stones)
					{
						COM.x += stone.width*stone.height*m_stone*(stone.getX());
						COM.y += stone.width*stone.height*m_stone*(stone.getY());
						total_mass +=  stone.width*stone.height*m_stone;
					}


					for(Rectangle wood :woods)
					{
						COM.x += wood.width*wood.height*m_wood*(wood.getX());
						COM.y += wood.width*wood.height*m_wood*(wood.getY());
						total_mass	+= 	wood.width*wood.height*m_wood;
								}

					for(Rectangle ICE :ice)
					{
						COM.x += ICE.width*ICE.height*m_ice*(ICE.getX()) ;
						COM.y += ICE.width*ICE.height*m_ice*(ICE.getY());
						total_mass += ICE.width*ICE.height*m_ice;
					}

					for(ABObject pig :pigs)
					{
						COM.x += pig.width*pig.height*m_pig*(pig.getX() );
						COM.y += pig.width*pig.height*m_pig*( pig.getY());
						total_mass += pig.width*pig.height*m_pig;
					}


					
					COM.x = (int)COM.getX()/total_mass;
					COM.y = (int)COM.getY()/total_mass;


					System.out.println("com : " + COM.x + "    --   " + COM.y);
					ABObject max_height_pig = new ABObject();
					int y = (randomGenerator.nextInt(pigs.size()));
					//System.out.println(colors[(int)pigs.get(y).getCenter().getY()][(int)pigs.get(y).getCenter().getX()]);
					Point  P = new Point();
						P.x = (int)pigs.get(y).getCenter().getX();
						P.y = (int)pigs.get(y).getCenter().getY();
						max_height_pig = pigs.get(y);


						// pick the max height pig
					double min = distance(COM, P);
					System.out.println("min" + min);
					Point  p = new Point();
					for(ABObject pig : pigs)
					{
						P.x = (int)pig.getCenter().getX();
						P.y = (int)pig.getCenter().getY();
						if(distance(COM, p) <= min)
						{
							max_height_pig = pig;
							min = distance(COM, p);
						}
					
					}
					

					Point _tpt = new Point();
					ABObject pig;
					if(min > 100)
					{
					  pig = max_height_pig;
					 _tpt = pig.getCenter();/// if the target is very close to before, randomly choose a
					}
					// point near it
					else
					{
					_tpt = COM;
					}
					List<ABObject> blocks = MBR.findBlocks();
					System.out.println("target :" + _tpt.x + "   " + _tpt.y);
					int flag = 0;
					for(ABObject block : blocks)
					{
						if(block.getCenter().getX() <= _tpt.x)
						{	
							if(Math.abs(block.getCenter().getY() - _tpt.y) > 70) 
							{
								System.out.println(block.getCenter().getX() + "   " + block.getCenter().getY());
								flag = 1;
								break;
							}
						}
					}
					if (prevTarget != null && distance(prevTarget, _tpt) < 10) {
						double _angle = randomGenerator.nextDouble() * Math.PI * 2;
						_tpt.x = _tpt.x + (int) (Math.cos(_angle) * 10);
						_tpt.y = _tpt.y + (int) (Math.sin(_angle) * 10);
						System.out.println("Randomly changing to " + _tpt);
					}

					prevTarget = new Point(_tpt.x, _tpt.y);
					//System.out.println(_tpt.x + "   " + _tpt.y);
					// estimate the trajectory
					ArrayList<Point> pts = tp.estimateLaunchPoint(sling, _tpt);
					//System.out.println("Size  "+pts.size());
					//System.out.println(pts.get(0).getX() + "  " + pts.get(0).getY());
					// do a high shot when entering a level to find an accurate velocity
					if(!pts.isEmpty())
					{
						if(flag == 0)
						releasePoint = pts.get(0);
						else	
						{	/*else*/
							 if (pts.size() == 1)
								releasePoint = pts.get(0);
							else if (pts.size() == 2)
							
										releasePoint = pts.get(1);
								}
									}
					else
					{
							System.out.println("No release point found for the target");
							System.out.println("Try a shot with 45 degree");
							releasePoint = tp.findReleasePoint(sling, Math.PI/4);
						}
					/*
					 List<Point> trajectory = tp.predictTrajectory(sling, releasePoint);

					for(int k= 0; k<trajectory.size(); k++)
					{
						System.out.println(trajectory.get(k).getX() + " " + trajectory.get(k).getY());
						System.out.println(screenshot.getRGB(trajectory.get(k).getY(), trajectory.get(k).getX()));
						}*/
					// Get the reference point
					Point refPoint = tp.getReferencePoint(sling);


					//Calculate the tapping time according the bird type 
					if (releasePoint != null) {
						double releaseAngle = tp.getReleaseAngle(sling,
								releasePoint);
						System.out.println("Release Point: " + releasePoint);
						System.out.println("Release Angle: "
								+ Math.toDegrees(releaseAngle));
						System.out.println("HI");
						int tapInterval = 0;
						switch (aRobot.getBirdTypeOnSling()) 
						{

						case RedBird:
							tapInterval = 0; break;               // start of trajectory
						case YellowBird:
							tapInterval = 65 + randomGenerator.nextInt(25);break; // 65-90% of the way
						case WhiteBird:
							tapInterval =  70 + randomGenerator.nextInt(20);break; // 70-90% of the way
						case BlackBird:
							tapInterval =  70 + randomGenerator.nextInt(20);break; // 70-90% of the way
						case BlueBird:
							tapInterval =  65 + randomGenerator.nextInt(20);break; // 65-85% of the way
						default:
							tapInterval =  60;
						}

						int tapTime = tp.getTapTime(sling, releasePoint, _tpt, tapInterval);
						dx = (int)releasePoint.getX() - refPoint.x;
						dy = (int)releasePoint.getY() - refPoint.y;
						shot = new Shot(refPoint.x, refPoint.y, dx, dy, 0, tapTime);
					}
					else
						{
							System.err.println("No Release Point Found");
							return state;
						}
				}

				// check whether the slingshot is changed. the change of the slingshot indicates a change in the scale.
				{
					ActionRobot.fullyZoomOut();
					screenshot = ActionRobot.doScreenShot();
					vision = new Vision(screenshot);
					Rectangle _sling = vision.findSlingshotMBR();
					if(_sling != null)
					{
						double scale_diff = Math.pow((sling.width - _sling.width),2) +  Math.pow((sling.height - _sling.height),2);
						if(scale_diff < 25)
						{
							if(dx < 0)
							{
								aRobot.cshoot(shot);
								state = aRobot.getState();
								if ( state == GameState.PLAYING )
								{
									screenshot = ActionRobot.doScreenShot();
									vision = new Vision(screenshot);
									List<Point> traj = vision.findTrajPoints();
									tp.adjustTrajectory(traj, sling, releasePoint);
									firstShot = false;
								}
							}
						}
						else
							System.out.println("Scale is changed, can not execute the shot, will re-segement the image");
					}
					else
						System.out.println("no sling detected, can not execute the shot, will re-segement the image");
				}

			}

		}
		return state;
	}

	public static void main(String args[]) {
		
		NaiveAgent na = new NaiveAgent();
		if (args.length > 0)
			na.currentLevel = Integer.parseInt(args[0]);
		na.run();

	}
}