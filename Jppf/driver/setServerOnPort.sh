#! /bin/sh

if [ "$1" ] ;
    then

	H=`sed '/^\#/d' config/jppf-driver.properties | grep 'jppf.server.port'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'`
#jppf.server.port = 11110
	if [  $H ]; 
		then sed -i 's/\(jppf.server.port =\).*/\1 '$1'/' config/jppf-driver.properties; 
	else 
		sed -i '27i jppf.server.port = '$1' ' config/jppf-driver.properties ;
	fi 

else
	H=`sed '/^\#/d' config/jppf-driver.properties | grep 'jppf.server.port'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'`
	if [  $H ]; 
		then sed -i 's/\(jppf.server.port =\).*/\1 11110/' config/jppf-driver.properties; 
	else 
		sed -i '27i jppf.server.port = 11110 ' config/jppf-driver.properties ;
	fi 
	
fi

