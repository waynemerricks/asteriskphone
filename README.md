Asteriskphone
=============

Multi User Asterisk interface for radio use.

**Requires:**
* Asterisk
* Openfire (for XMPP Chat)
* MySQL (caller id lookups, record storage)

See install docs for more info

![Interface Example](http://www.thevoiceasia.com/phone.png)

The system has two components, Server which hooks into Asterisk using the Manager API.  This listens for call events and passes
them on all clients via XMPP messaging.

The clients respond to these messages by showing calls as ringing etc.  

###The system can currently do the following:
* Direct calls to compatible phones (if they implement SIP Auto Answer messages they will work, I've tried it with Grandstream, Cisco and BareSIP).
* Transfer calls between queues and phones
* Transfer calls between phones
* Keep track of data associated with a call and sync between all clients
* Keep track of data associated with numbers so you don't have to keep asking people what their name is every time they call (uses Caller ID lookups)
* Play ringing noises to wake up call answerers (turn on/off via db setting)
* Has short cut buttons for "No Calls", "Going for a Break", "Wake Up" and "Help" usable from studio clients as quick fire messages to all Clients
* Calls are colour coded depending on state (blue ringing, grey answered by someone else, orange answered by me, red answered by studio, green in queue for studio)
* Icons are changeable depending upon call type e.g. song request, general chat
* Work across multiple Asterisk servers as long as you have appropriate trunk and routing rules in place
* Work across multiple slave databases for remote offices (speeds up record lookups immensely)

###Things it can't do yet (apart from make coffee etc):
* Customisable database fields that follow the call.  At the moment although the GUI is dynamic and read from the DB.  The underlying records aren't.  This is on the TODO list.
* If you have an incoming DID trunk on a remote server, I'm not sure how the system will react to the incoming call.  It should work but it needs testing.
* Need to make a web interface for managing settings and getting stats.  At the moment I just do this directly on the MySQL server.

###Does this work?

Yes but you will have to tweak the settings and I've only tested fully on Elastix PBX 2.4 and 2.5.  There are some quirks with the way calls are shown by some systems that might not be the way the program expects.  For the most part it should work fine but your mileage may vary.

I use this in production every day at a radio station with a remote office 6,000 miles away.  At its peak, we ran 3 studios and 8 call handlers with up to 1,500 calls a day.
