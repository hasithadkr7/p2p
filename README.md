# Peer To Peer File Share and Ranking
To Execute Peer to Peer communication prototype,

For the execution of the application  you should be having java8 installed and updated.

First up the Bootstrap Server. To do this, find the "BootstrapServer-1.0-SNAPSHOT.jar" and execute it as follows in
a command line (or terminal) which has the jar file in the current path.

`$ java -jar BootstrapServer-1.0-SNAPSHOT.jar` (or the path to the jar file)


Now you have to compile and execute the Peer as follows.

To compile and build you should be having gradle on you path with the latest version.

`$ gradle fatJar`

this makes the application jar file to be saved in /dist/libs.

after that to, start the Peer in a node. Get this /dist/libs/PeerNode-1.0-SNAPSHOT.jar file to a comfortable place in a
desired computer and up the Peer Node. Then following command should be executed.

`$ java -jar PeerNode-1.0-SNAPSHOT.jar 10.10.10.10` (or the path to the jar file) where IP is the IP of the bootstrap server.


