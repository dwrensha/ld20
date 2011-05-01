import scala.xml._
import scala.collection.mutable.HashMap


case class Coord(lat: Double, lon: Double) 


case class Relation()

object XmlUtil {
  type ID = Int


   //bounds
  var minlat: Double = 0D
  var minlon: Double = 0D
  var maxlat: Double = 0D
  var maxlon: Double = 0D

  var latscale = 1D
  var lonscale = 1D


  val nodes : HashMap[ID, Coord] = new HashMap[Int,Coord]()
  val ways : HashMap[ID, List[ID]] = new HashMap[ID, List[ID]]()
  var citylimits : List[Coord] = Nil
//  val rels : HashMap[ID, List[ID]]

  var riverbanks : List[Coord] = Nil
  
//  var highways : List[List[Coord]] = Nil
  var highways : List[Coord] = Nil

  def printcoordlist(lst: List[Coord]) : Unit = {
    for(Coord(lat,lon) <- lst) {
      println(lon.toString + ",  " + lat.toString)
    }
  }

  def printlistdelimited[A](printer : A => Unit, del : String, lst: List[A]) : Unit = lst match {
    case Nil => ()
    case x :: Nil => printer(x)
    case x :: xs => 
      printer(x)
      print(del)
      printlistdelimited(printer, del, xs)
  }

  def printwaylist(lsts: List[List[Coord]]) : Unit = {
    for(lst <- lsts) {
      printlistdelimited[Coord]( {case Coord(lat,lon)  => print("{" + lon + "," + lat + "}"  )  }   , ",", lst)
      println()
    }
  }
    

  def loadMap(filename: String, cityname: String) = {
    val data = XML.loadFile(filename)


    minlat = ((data \ "bounds") \ "@minlat").text.toDouble
    minlon = ((data \ "bounds") \ "@minlon").text.toDouble
    maxlat = ((data \ "bounds") \ "@maxlat").text.toDouble
    maxlon = ((data \ "bounds") \ "@maxlon").text.toDouble


    for( n <- data \ "node") {
      val id = (n \ "@id").text.toInt
      val lat = (n \ "@lat").text.toDouble - minlat
      val lon = (n \ "@lon").text.toDouble - minlon
      nodes.put(id, Coord(lat,lon))
    }

    println("done loading nodes. this many: " + nodes.size)

    for( w <- data \ "way") {
      val id = (w \ "@id").text.toInt
      val nds = (w \ "nd").toList.map(nd => (nd \ "@ref").text.toInt)
      ways.put(id, nds)
    }

    println("done loading ways. this many: " + ways.size)


    for (r <- (data \ "relation")){
      for (t <- (r \ "tag")){
        if((t \ "@k").text == "name" && (t \ "@v").text == cityname){
          println("this is the one")
          for(m <- ( r \ "member")){
            val memid = (m \ "@ref").text.toInt
            ways.get(memid) match {
              case None => ()
              case Some(nds) => 
                for(nd <- nds) {
                  nodes.get(nd) match {
                    case None => ()
                    case Some(crd) => citylimits = crd::citylimits
                  }
                }
                  
            }
            
            
          }
        }
        
      }
    }

    println("citylimits length = " + citylimits.length)

    for (w <- (data \ "way")){
      for (t <- (w \ "tag")){
        if ((t \ "@k").text == "waterway" && (t \ "@v").text == "riverbank"){
          for(nd <- ( w \ "nd")){
            val ndid = (nd \ "@ref").text.toInt
            nodes.get(ndid) match {
              case None => ()
              case Some(crd) => riverbanks = crd::riverbanks
            }
            
            
          }
        }
        
      }
    }

    println("riverbanks length = " + citylimits.length)



    for (w <- (data \ "way")){
      for (t <- (w \ "tag")){
        if ((t \ "@k").text == "highway" && 
            (  (t \ "@v").text == "primary" || (t \ "@v").text == "secondary"  || (t \ "@v").text == "tertiary"  )
          ) {
//          var thisway: List[Coord] = Nil
          for(nd <- ( w \ "nd")){
            val ndid = (nd \ "@ref").text.toInt
            nodes.get(ndid) match {
              case None => ()
//              case Some(crd) => thisway = crd::thisway 
              case Some(crd) => highways = crd::highways 
            }
//            highways = thisway::highways
            
            
          }
        }
        
      }
    }

    println("highways length = " + highways.length)


  }


  def getEssentials(filename: String, cityname: String) = {
    val data = XML.loadFile(filename)
    for (r <- (data \ "relation")){
      for (t <- (r \ "tag")){
        if((t \ "@k").text == "name" && (t \ "@v").text == cityname){
          println("this is the one")
          println(r)
        }

      }
    }
  }

  def main(args : Array[String]) = {
    println("loading data.")
    loadMap(args(0), args(1))
    
//    printcoordlist(citylimits)
//    printcoordlist(riverbanks)
//    printcoordlist(nodes.values.toList)
     printcoordlist(highways)

  }

}
