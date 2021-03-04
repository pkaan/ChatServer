Programming 3 course assignment

Student: Peetu Kaan - Student Number: 2307280 - Email: aripeetu.kaan@gmail.com 


Compile and start with 3 parameters:
	1. database file (database will be created if does not exist)
	2. certificate file (has to exist)
	3. certificate password

	>mvn assembly:assembly -DdescriptorId=jar-with-dependencies
	>java -cp target/chatserver-1.0-SNAPSHOT-jar-with-dependencies.jar com.peetukaan.chatserver.ChatServer chatDatabase.db keystore.jks c1wsFeoBtGNffM0BPWOZ

	or 

	compile_and_start_the_server.bat (WINDOWS) 


API:
	Username needs to be at least 2 characters long
	Password needs to be at least 4 characters long
	(commented code at the moment because the tests will fail) Email needs to have '@' in it and at least 6 characters long
	// email.contains("@"))
	For example:

	{
		"username": "vesa",
		"password": "salis22",
		"email": "vesapekka@gmail.com"
	}



ADVANCED API: (WEATHER)

	If the user wants to attach weather information (from open data, fmi.fi) to the message:
	 	- he needs to add a new element called "location" to JSON
		- add value to that element (city in Finland)
		- For example:
		
		{
			"message": "moi",
			"user": "vesa",
			"sent": "2021-03-02T10:18:00.123Z",
			"location": "oulu"
		}

	Information will be added ONLY if:
		-"location" and city have been written correctly
		- city is in Finland and can be found in FMI data
		- there is at least temperature data available (pressure not always available)
		-"sent" value cant be older than 168.000000 hours (from now)

	Information will consist:
		-city(measure time):temperatureC/AND(if available)pressurehPA 
		-pressure not always available (for example: Tampere)
		-measure time is the most recent one available between LOCALTIME 2021-03-02T00:00:00 - 2021-03-02T10:18:00 (between start of the day to value in "sent") 
	
	If the information cannot be found -> the message will be stored without location and weather information 

Problems:

	Registered user can send a message with a different username than his own (as long as the other username exists in the database)
	For example: Vesa can send a message as 'spede' if 'spede' can be found in the database:
	
	curl -k -u "vesa:salis22" -d "@message.json" https://localhost:8001/chat -H "Content-Type: application/json"

	{
		"message": "moi",
		"user": "spede",
		"sent": "2021-03-02T10:18:00.123Z",
	}


