# TitanMessenger
An attempt at creating a WhatsApp competetor in high-school, using GPG encryption and allowing sign-in without needing a phone.

This was my first serious programming project.

I believe this is not the final version, but this is one of many attempts to set up git correctly and it seems to have a somewhat recent version of the code.

This was started when WhatsApp started to become popular in Germany and was lacking two features that, to this day, are very important to me:

Cross platform accessability and encryption.

Both features that I was planning to include in this software.

I built this together with a friend of mine, who did the webinterface in PHP.

Since we didn't have an idea of how to communicate between my Java Server and his PHP code, we ended up using the database itself as the connection, which surprisingly worked out quite well.

We were planning to go to Jugend Forscht with this project and get actual funding to realize it, but then WhatsApp came out with the update that brought encryption to the messenger.

We ended up making this our IT high-school project and scored the maximum available point in IT.

Afterwards we lost motivation, mostly due to the overwhelming growth of WhatsApp and through gradual loss of contact.

Feel free to use any and all code from this project if you like.

I'm still a bit proud on the NioServer, which uses Socket channels to enable networking to be handled in one thread (instead of having one thread per connection).

And I actually still use variations of the Config class, since it is just too simple to store a JSON config this way...

Hope you enjoyed my little summary :)
