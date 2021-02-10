Student: Peetu Kaan - Student Number: 2307280 - Email: aripeetu.kaan@gmail.com 

to compile:

> mvn assembly:assembly -DdescriptorId=jar-with-dependencies
> java -cp target/chatserver-1.0-SNAPSHOT-jar-with-dependencies.jar com.peetukaan.chatserver.ChatServer


Exercise 1: done.

Exercise 2: done.

Exercise 3: done.

Exercise 4: done.
--refactored
--database support
--fixed jsonArray when returning(GET) the messages
--Implemented some missing requirements
--/register, /get & sending the messages working using the chatClient