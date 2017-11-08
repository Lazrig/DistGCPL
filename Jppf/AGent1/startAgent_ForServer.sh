#! /bin/sh

if [ "$1" -a "$2" ] ;
    then
    if [ "$3" ] ;
        then
        ./setMangOnHost.sh  "$1"  "$2" "$3"
		./startAgent.sh
     else   
		./setMangOnHost.sh  "$1"  "$2"
		./startAgent.sh
	fi
else
	echo -e '\n' "!!!  YOU SHOULD SPECIFY THE IP and PORT # of the DRIVER I Will Work For" '\n' 
fi
