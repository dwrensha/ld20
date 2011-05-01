JARS = core.jar:jbox2d.jar:minimlibrary/minim.jar:minimlibrary/jl1.0.jar:minimlibrary/jsminim.jar:minimlibrary/minim-spi.jar:minimlibrary/mp3spi1.9.4.jar:minimlibrary/tritonus_aos.jar:minimlibrary/tritonus_share.jar
JAVAFILES = ProcessingDebugDraw.java 
SCALAFILES = Types.scala Physics.scala Main.scala 

SCALAFLAGS = -deprecation

game : $(JAVAFILES) $(SCALAFILES)
	javac -cp $(JARS):. -d .  $(JAVAFILES)
	fsc -cp $(JARS):. $(SCALAFLAGS) $(SCALAFILES)


data : ReadData.scala
	fsc ReadData.scala