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


  val nodes : HashMap[ID, Coord] = new HashMap[Int,Coord]()
  val ways : HashMap[ID, List[ID]] = new HashMap[ID, List[ID]]()
  var citylimits : List[Coord] = Nil
//  val rels : HashMap[ID, List[ID]]


  def loadMap(filename: String, cityname: String) = {
    val data = XML.loadFile(filename)


    minlat = ((data \ "bounds") \ "@minlat").text.toDouble
    minlon = ((data \ "bounds") \ "@minlon").text.toDouble
    maxlat = ((data \ "bounds") \ "@maxlat").text.toDouble
    maxlon = ((data \ "bounds") \ "@maxlon").text.toDouble


    for( n <- data \ "node") {
      val id = (n \ "@id").text.toInt
      val lon = (n \ "@lon").text.toDouble
      val lat = (n \ "@lat").text.toDouble
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
            println(m)
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

    println("city limits = ")
    println(citylimits)
    println("length = " + citylimits.length)


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
    
  }

}
