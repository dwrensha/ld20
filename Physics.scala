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



sealed abstract class PlayState
case object TitleScreen extends PlayState
case object MainGame extends PlayState
case object WinScreen extends PlayState


class Physics extends PApplet {

    var pointstring: String = ""
    var pointicks : Int = 0
    val POINTICKSMAX : Int = 500

    var playstate : PlayState = TitleScreen

   val  rand = new scala.util.Random(System.currentTimeMillis())
    val titlescreen : PImage = loadImage("assets/titlescreen.png")

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
  val MAXTORQUE = 0.5f;

  val BOTLONG = 0.5f;
  val BOTWIDE = 0.25f;
  val BOTDENSE = 25.0f;
  val BOTMASS = BOTLONG * BOTWIDE * BOTDENSE;

  // conservatively estimate the polling cycle as half the frame time.
  val EPS = 1f / (2f * targetFPS) 

  val minim : Minim = new Minim(this)
  val player : AudioPlayer = minim.loadFile("assets/title.mp3")



  val pittsburgh = ParseData.file("mydata/pittsburgh.dat")
  println("read pittsburgh data. length = " + pittsburgh.length)

  val manhattan = ParseData.file("mydata/manhattan.dat")
  println("read manhattan data. length = " + manhattan.length)

  val seattle = ParseData.file("mydata/seattle.dat")
  println("read seattle data. length = " + seattle.length)

  val boston = ParseData.file("mydata/boston.dat")
  println("read boston data. length = " + boston.length)

  val washington = ParseData.file("mydata/washington.dat")
  println("read washington data. length = " + washington.length)

  val sanfrancisco = ParseData.file("mydata/sanfrancisco.dat")
  println("read sanfrancisco data. length = " + sanfrancisco.length)

  val cities =  Array(pittsburgh, manhattan, seattle, boston, washington,  sanfrancisco)

  var controlledbots : List[BotID] = Nil

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


    def makeBot(p: Vec2, v: Vec2,  theta:Float , omega: Float) : BotID = {
      val sd: PolygonDef = new PolygonDef()
      sd.setAsBox(0.5f * BOTLONG, 0.5f * BOTWIDE)
      sd.density = BOTDENSE 
      sd.restitution = 0.0f
      sd.friction = 0.5f

      val bd: BodyDef = new BodyDef()
      bd.position.set(p)
      bd.linearDamping = 0.03f
      bd.angularDamping = 0.5f
      bd.angle = theta
      val body: Body = world.createBody(bd)
      body.createShape(sd)
      body.setMassFromShapes()
      body.setLinearVelocity(v)
      body.setAngularVelocity( omega)
      
      val bid = nextBotID
      livebots.put(bid, new LiveBotInfo(body, 
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
        dd.setCamera(0.0f,0.0f, 2.0f);

        // add some stuff to the world.


/*
      for(x <- Range(0,1000)  ){ //about the upper limit of what we can efficiently simulate
        
        makeBot(new Vec2(rand.nextFloat() * 200.0f - 100f,rand.nextFloat()* 200f - 100f),
                new Vec2(rand.nextFloat(), rand.nextFloat()), 
                (new Vec2(80.0f, 0.0f), 5.0f),
                rand.nextFloat() * 10f, 0.0f)
      }
*/
/*
      makeObstacle(new Vec2(-102.0f,-102.0f), 98.0f)
      makeObstacle(new Vec2(-102.0f,102.0f), 98.0f)

      makeObstacle(new Vec2(102.0f,-102.0f), 98.0f)
      makeObstacle(new Vec2(102.0f,102.0f), 98.0f)
*/


      startbattle

      player.play()
      player.loop()
 
    }
    


  def startbattle : Unit = {
    println("starting battle")
    for((id,botinfo) <- livebots ){
      val b = botinfo.body
      world.destroyBody(b)
      intents.remove(id)
    }
    livebots = new HashMap[BotID,LiveBotInfo]()    

    val city1 = cities(rand.nextInt(cities.length)    )
    val city2 = cities(rand.nextInt(cities.length))




      for((c1,c2) <- city1  ){
        val (f1,f2) = (new java.lang.Float(7000d * c1.doubleValue+50d), new java.lang.Float(7000d * c2.doubleValue-150d))
        val id = makeBot(new Vec2(f1.floatValue, f2.floatValue),
                         new Vec2(0f, 0f), 
                         3f * pi / 4f, 0.0f)
        controlledbots = id :: controlledbots
        
      }

      for((c1,c2) <- city2  ){
        val (f1,f2) = (new java.lang.Float(7000d * c1.doubleValue - 200d), new java.lang.Float(7000d * c2.doubleValue+0d))
        makeBot(new Vec2(f1.floatValue, f2.floatValue),
                new Vec2(0f, 0f), 
                - pi / 4f, 0.0f)
      }

      println("bots done being made")


    
  }
    

    /**
     * This is the main looping function,
     * and is called targetFPS times per second.
     */
    override def draw() {
      playstate match {
        case TitleScreen => 
          background(0)
          image(titlescreen,0,0)
        case MainGame => 
          this.synchronized{
            trackFPS()



            for((id,botinfo) <- livebots ){
              val intent = intents.getOrElse(id,null)
              val b = botinfo.body
              val p = b.getPosition()
              val theta = b.getAngle()
              val v = b.getLinearVelocity()
              val vmag = v.length()
              val u = new Vec2(scala.math.cos(theta).asInstanceOf[Float], 
                               scala.math.sin(theta).asInstanceOf[Float])
              //          val _ = b.setLinearVelocity(u.mul(vmag))
              val (t,a) = intent
              
              t match {
                case Some(TurnLeft) => 
                  b.applyTorque(MAXTORQUE)
                case Some(TurnRight) => 
                  b.applyTorque(- MAXTORQUE)
                case None => 
              
              }
              a match {
                case Some(Accel) => 
                  b.applyForce(u.mul(MAXFORCE), b.getPosition())
                case Some(Brake) =>
                  if(Vec2.dot(v, u)  > 0){
                    b.applyForce(u.mul(- MAXBRAKE), b.getPosition())
                  } else {
                    b.setLinearVelocity(zerovec)
                  }
                case None => 
              }
            }


            // draw it and step.
            background(0)
            world.step(1.0f / targetFPS, 8)

            dd.drawString(370, 30, pointstring,new Color3f(255.0f,0f,0f))
            pointicks += 1
            if(pointicks > POINTICKSMAX){
              pointstring = ""
              pointicks = 0
            }
          }
        case WinScreen => 
          background(0)
      }
        return
    }
    


  val DTHETA = 0.25f

    /*
  */ 
    override def keyPressed(e: java.awt.event.KeyEvent) = {
      playstate match {
        case TitleScreen => 
          if( keyCode == ENTER || key == ' ') {
            playstate = MainGame
          }
        case MainGame =>
          if(keyCode == LEFT) {
            val ni =Some(TurnLeft) 
            for(id <- controlledbots){
//              val botinfo = livebots.getOrElse(id,null)
              val (t,a) = intents.getOrElse(id,null)
              intents.put(id, (ni,a))
            }
          } else if (keyCode == RIGHT) {
            val ni =  Some(TurnRight) 
            for(id <- controlledbots){
//              val botinfo = livebots.getOrElse(id,null)
              val (t,a) = intents.getOrElse(id,null)
              intents.put(id, (ni,a))
            } 
          }
            else if (keyCode == ENTER ) {
              this.synchronized{
                val points = rand.nextInt(20) * 100
                pointstring = " " + points + " POINTS!"
                startbattle
              }
            } 
        case _ => 
          
      }

    }



  override def stop() = {
    player.close()
    minim.stop()
    super.stop()
  }

}
