Create a new bot named terminator that is better than all of the existing bots.

Don't read, or use in any way the source code or documentation for any of the existing bots. Start
completely from scratch. 

Think about the algorithms that can be used, and combined, to build a winning bot. This might be
graph algorithms, game playing algorithms, or other cs algorithms that can be used to create a high
performing botr. 

For quicker iteration you can run tests with 1000 rounds: ./gradlew :tournamentCli:run
--args='--bots terminator,rebel,turtle,bully,emperor,frontier-commander,max,optimus --rounds 1000 --parallel 8 --seed 5'

Don't stop improving the code until Terminator beats the other bots in a 10,000 round tournament.

Final validation is done with the command: 
./gradlew :tournamentCli:run --args='--bots
terminator,rebel,turtle,bully,emperor,frontier-commander,max,optimus --rounds 10000 --parallel 8 --seed 5'

Terminator must finish in 1st place.
