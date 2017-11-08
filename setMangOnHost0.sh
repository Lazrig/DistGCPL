#! /bin/sh

if [ "$1" -a "$2" ] ;
    then

	H=`sed '/^\#/d' config/jppf-node.properties | grep 'jppf.server.host'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'`

	if [  $H ]; 
		then sed -i 's/\(jppf.server.host =\).*/\1 '$1'/' config/jppf-node.properties; 
	else 
		sed -i '26i jppf.server.host = '$1' ' config/jppf-node.properties ;
	fi 


	P=`sed '/^\#/d' config/jppf-node.properties | grep 'jppf.server.port'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'`

	if [  $H ]; 
		then sed -i 's/\(jppf.server.port =\).*/\1 '$2'/' config/jppf-node.properties; 
	else 
		sed -i '29i jppf.server.port = '$2' ' config/jppf-node.properties ;
	fi 
fi

if [ "$3" ] ;
    then
    H=`sed '/^\#/d' config/jppf-node.properties | grep 'jppf.node.provisioning.startup.slaves'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'`

	if [  $H ]; 
		then sed -i 's/\(jppf.node.provisioning.startup.slaves =\).*/\1 '$3'/' config/jppf-node.properties;   
    fi
fi    


