import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.InputStream
import java.io.FileInputStream
import java.lang.Double


object ParseData {

  def line(ln: String) : (Double, Double) = {
    val ds = ln.split(",")
    val d0 = Double.parseDouble(ds(0))
    val d1 = Double.parseDouble(ds(1))
    (d0,d1)
  }

  def file(filename: String) : List[(Double,Double)] = {
    var res : List[(Double, Double)] = Nil
    val in = new FileInputStream(filename)
    val br = new BufferedReader(new InputStreamReader(in))
    var ins1 = ""
    var ln = br.readLine()
    while (ln != null){
      ln = br.readLine()
      res = line(ln) :: res
    }

    res
    
  }

}
