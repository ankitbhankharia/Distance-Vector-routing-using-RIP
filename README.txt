README:

How to Compile?
command - javac *.java

How to run?

Neighbor network address are taken as the command line argument.
Once form the network topology by passing command line arguments for all the networks.
Then enter the cost for the links.

e.g. Consider following topology:

Glados ----------- Rhea
  |		    |		    |
  |		    |
  |		    |
  |		    |
Comet-------------Queeg

Run following commands:
For Glados - java Connection 129.21.37.49
For Rhea - java Connection 129.21.30.37
For Queeg - java Connection 129.21.34.80
For Comet - java Connection 129.21.22.196

Then for every link enter the cost as you wish

Once you enter cost for any link it starts exchanging routing tables in every 1 second.


OUTPUT:
It will display destination network address, subnet mask, cost and next hop.