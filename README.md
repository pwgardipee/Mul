Mul is a native anonymous hotspot sharing application for Android. 

By interfacing with a phone's bluetooth and wifi modules, a Mul "client" searches the local area for another 
Mul user who has declared themselves as a "provider". Upon finding a provider(who has their hotspot
turned on and has provided their hotspot credentials to the application), hotspot credentials will
be passed to the client via bluetooth and the client will autonomously connect to the provider's hotspot.

While connected, data usage is monitored and recorded using a REST api hosted by AWS. The data stored there
can be used as the accepted truth to how much data has been consumed by a client to implement some sort of payment
to the provider. Client phones will automatically request 10 MB "chunks" of data as needed instead of using data in 
more of a continuous manner. This will allow for the future implementation of prepayment for each chunk. This way, 
the provider is protected from providing more data than they set their data limit to be before starting their 
providing session. That eliminates situations like race conditions between multiple clients using data from the same provider.    

Upon a client or provider ending a session, any disconnected client will autonomously forget any connected wifi networks.
This prevents clients from connecting to providers' hotspots outside of the Mul application after ending a session( avoiding payment).
