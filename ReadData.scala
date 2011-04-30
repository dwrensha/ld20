import scala.xml._

object XmlUtil {
  def getEssentials(filename: String, cityname: String) = {
    val data = XML.loadFile(filename)
    for (r <- (data \ "relation")){
      println(r.text)
    }
  }

}
