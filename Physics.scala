//package shvoid


import java.util.ArrayList

import org.jbox2d.common.Color3f
import org.jbox2d.common.Vec2
import org.jbox2d.collision.AABB
import org.jbox2d.collision.PolygonDef
import org.jbox2d.dynamics.Body
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.DebugDraw
import org.jbox2d.dynamics.World

import processing.core._
import processing.core.PConstants._


import ddf.minim.Minim
import ddf.minim.AudioPlayer
import ddf.minim.AudioInput
import ddf.minim.javasound._


import Types._
import PhysicsTypes._






class Physics extends PApplet {

    val pi = 3.14159265f
    val zerovec = new Vec2(0f,0f)

    var shiftKey = false
    
    /** Was the mouse down last frame? */
    var pmousePressed = false
    
    /** FPS that we want to achieve */
    val targetFPS = 60.0f
    
    var avgFPS = 0.0
    val numFrames = 100
    var frameNum = 0
    var startTime : Long = System.currentTimeMillis()

    /** Drawing handler to use. */
    var dd: DebugDraw = null

    var world: World = null



    
    var livebots : HashMap[BotID, LiveBotInfo]  = 
      new HashMap[BotID,LiveBotInfo]()    
    var donebots : HashMap[BotID, DoneBotInfo]  = 
      new HashMap[BotID,DoneBotInfo]()    
//    var positions : HashMap[BotID, Vec2]  = new HashMap[BotID,Vec2]()    
    var intents : SyncMap[BotID, Intent] = new SyncMap[BotID, Intent]()

    private var nextBotID = 0;

//    val maxspeed = 10.0f;
//    val maxomega = 5.0f;
  val MAXFORCE = 15.0f;
  val MAXBRAKE = 15.0f;
  val MAXTORQUE = 5.0f;

  val BOTLONG = 1.5f;
  val BOTWIDE = 1.0f;
  val BOTDENSE = 5.0f;
  val BOTMASS = BOTLONG * BOTWIDE * BOTDENSE;

  // conservatively estimate the polling cycle as half the frame time.
  val EPS = 1f / (2f * targetFPS) 



  def trackFPS() = {
    frameNum += 1
    if (frameNum == numFrames){
      val newTime = System.currentTimeMillis()
      avgFPS = numFrames * 1000 /  (newTime - startTime).asInstanceOf[Double];
//     avgFPS =  1.0 *  (newTime - startTime);
      frameNum = 0
      startTime = newTime
    }

  }


    def makeBot(p: Vec2, v: Vec2, gl: Goal, theta:Float , omega: Float) : Int = {
      val sd: PolygonDef = new PolygonDef()
      sd.setAsBox(0.5f * BOTLONG, 0.5f * BOTWIDE)
      sd.density = BOTDENSE
      sd.restitution = 0.0f
      sd.friction = 0.5f

      val bd: BodyDef = new BodyDef()
      bd.position.set(p)
      bd.linearDamping = 0.1f
      bd.angularDamping = 0.5f
      bd.angle = theta
      val body: Body = world.createBody(bd)
      body.createShape(sd)
      body.setMassFromShapes()
      body.setLinearVelocity(v)
      body.setAngularVelocity( omega)
      
      val bid = nextBotID
      livebots.put(bid, new LiveBotInfo(body, 
                                        gl,  
                                        System.currentTimeMillis()))
      intents.put(bid,(None,Some(Accel)))
       

      nextBotID+= 1

      return nextBotID - 1;

    }


    def makeObstacle(p: Vec2, sz: Float) = {
      val sd: PolygonDef = new PolygonDef()
      sd.setAsBox(sz, sz)
      sd.restitution = 0.0f
      sd.friction = 0.5f

      val bd: BodyDef = new BodyDef()
      bd.position.set(p)
      val body: Body = world.createBody(bd)
      body.createShape(sd)

    }




    override def setup() {
    	size(800,600,P3D)
    	frameRate(targetFPS)
    	dd = new ProcessingDebugDraw(this)
        this.requestFocus()


        val worldAABB:AABB = new AABB()
        worldAABB.lowerBound = new Vec2(-200.0f, -200.0f)
        worldAABB.upperBound = new Vec2(200.0f, 200.0f)
        val gravity:Vec2 = new Vec2(0.0f, 0.0f)
        val doSleep = true
        world = new World(worldAABB, gravity, doSleep)
        world.setDebugDraw(dd)
    	
        dd.appendFlags(DebugDraw.e_shapeBit);
        dd.setCamera(0.0f,0.0f, 3.0f);

        // add some stuff to the world.

        makeBot(new Vec2(0.0f,0.0f),
                new Vec2(0.0f, 0.0f), 
                (new Vec2(80.0f, 0.0f), 5.0f),
                0.0f, 0.0f)



      makeObstacle(new Vec2(-102.0f,-102.0f), 98.0f)
      makeObstacle(new Vec2(-102.0f,102.0f), 98.0f)

      makeObstacle(new Vec2(102.0f,-102.0f), 98.0f)
      makeObstacle(new Vec2(102.0f,102.0f), 98.0f)



      val minim : Minim = new Minim(this)
      val player : AudioPlayer = minim.loadFile("assets/title.mp3")
      player.play()
      
 
    }
    
    /**
     * This is the main looping function,
     * and is called targetFPS times per second.
     */
    override def draw() {
        trackFPS()
        perhapsCreateBots()

        var toRemove: List[BotID] = Nil
      // draw it and step.
      background(0)
      world.step(1.0f / targetFPS, 8)


      dd.drawString(5, 30, "FPS: " + avgFPS ,new Color3f(255.0f,255.0f,255.0f))
      return
    }
    

  /****2 ****
   ****  ****
   ****  ****
            3
   1
   ****  ****
   ****  ****
   **** 0****
  */
    val spawnpoints = 
      Array(new Vec2(2f,-90f),
            new Vec2(-90f,-2f),
            new Vec2(-2f,90f),
            new Vec2(90f,2f))

    val spawnvels = 
      Array(new Vec2(0f,5f),
            new Vec2(5f,0f),
            new Vec2(0f,-5f),
            new Vec2(-5f,0f))

    val spawnangles = 
      Array(pi / 2.0f,
            0f,
            3f * pi / 2f,
            pi
      )

    val goals = 
      Array((new Vec2(2f,90f), 5.0f),
            (new Vec2(90f,-2f), 5f),
            (new Vec2(-2f,-90f), 5f),
            (new Vec2(-90f,2f), 5f))

   val spawndelay = 120

   val ticks = Array(spawndelay, spawndelay, spawndelay, spawndelay)


          

   val  rand = new scala.util.Random(System.currentTimeMillis())

    /**
     *  perhaps add bots to the world.
     */ 
    def perhapsCreateBots() : Unit = {

      for(i <- ticks.indices){
        if(ticks(i) > 0 ) {
          ticks.update(i,ticks(i) - 1)
        } else { 
          if(rand.nextDouble < 0.01){
            val spawn = spawnpoints(i)
            val v = spawnvels(i)
            val angle = spawnangles(i)
            val gl = goals(i)
            val omega = 0f;
            val id = makeBot(spawn, v, gl, angle, omega )
            ticks.update(i,spawndelay)
          }
        }
      }


      return();
    }





    /*
     * Do I have to worry about thread safety here?
  */ 
    override def keyPressed(e: java.awt.event.KeyEvent) = {
      var (t,a) = intents.getOrElse(0,(None,None))
      if(keyCode == UP) {
           a = Some(Accel)
      } else if(keyCode == DOWN) {
           a = Some(Brake)
      } else {
        a = None
      }

      if(keyCode == LEFT) {
           t = Some(TurnLeft)
      } else if(keyCode == RIGHT) {
           t = Some(TurnRight)
      } else {
        t = None
      }


      intents.put(0,(t,a))

    }



}