#!/bin/bash

##################################################################################################
# Licensed Materials - Property of IBM
#
# IBM MAKES NO WARRANTY REGARDING THE OWNERSHIP, NON-INFRINGEMENT, ACCURACY OR VIRUS-FREE
# NATURE OF THIS PROGRAM. ALL WARRANTIES, INCLUDING EXPRESS AND IMPLIED WARRANTIES, ARE
# DISCLAIMED. IBM (AND ITS AFFILIATES) ARE NOT LIABLE FOR (i) ANY SPECIAL, DIRECT,
# INDIRECT, PUNITIVE, INCIDENTAL OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
# DAMAGES FOR LOSS OF BUSINESS PROFITS, BUSINESS INTERRUPTION, LOSS OF PROGRAMS OR
# INFORMATION, AND THE LIKE) OR ANY OTHER DAMAGES ARISING IN ANY WAY FROM OR IN CONNECTION
# WITH THE AVAILABILITY, USE, RELIANCE ON, PERFORMANCE OF THIS PROGRAM, OR LOSS OF DATA,
# INCLUDING VIRUSES ALLEGED TO HAVE BEEN OBTAINED, OR INVASION OF PRIVACY FROM OR THROUGH
# THE PROGRAM, EVEN IF IBM HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES AND
# REGARDLESS OF THE FORM OF ACTION, WHETHER IN CONTRACT, TORT OR OTHERWISE; OR (ii) ANY
# CLAIM ATTRIBUTABLE TO ERRORS, OMISSIONS, OR OTHER DYSFUNCTION IN, OR DESTRUCTIVE
# PROPERTIES OF, ARISING OUT OF OR IN CONNECTION WITH THE USE THIS PROGRAM. SOME STATES
# OR JURISDICTIONS DO NOT ALLOW THE EXCLUSION OR THE LIMITATION OF LIABILITY FOR
# CONSEQUENTIAL OR INCIDENTAL DAMAGES. IN SUCH STATES OR JURISDICTIONS, IBM'S LIABILITY
# SHALL BE LIMITED TO THE MAXIMUM EXTENT PERMITTED BY LAW. 
#
# (C) COPYRIGHT International Business Machines Corp. 2014
# All Rights Reserved.
##################################################################################################


user=""
password=""
filePath=""
remotePath=""
host=""
port="8080"
httpfsPort="14000"

function usage
{
	echo "Usage: httpfsUpload -u user -p password -h hostip/dnsname -f file [-lp loginport -hp httpfsport -rp remotepath --help]"
}

function helpPage 
{
	usage
	echo "-u/--user		BigInsights user name"
	echo "-p/--password		password"
	echo "-h/--host		BigInsights host name/ip"
	echo "-f/--filePath		Path to local file to be uploaded"
	echo "-lp/--loginport		Port number to BigInsights login page. Default 8080"
	echo "-hp/--httpfsport	Port number to Httpfs Rest api. Default 14000"
	echo "-rp/--remotePath	Location of directory in DFS where file is to be uploaded. By default would upload to /user/<username>"
	echo "--help			Describe command usage and options"
}

#extract values from command line parameters
if [ $# -eq 0 ]
then usage; exit 1
fi
while [ "$1" != "" ]; do
	case $1 in 
		-u | --user )		shift
					user=$1
					;;
		-p | --password )	shift
					password=$1
					;;
		-h | --host )		shift
					host=$1
					;;
		-lp | --loginport )	shift
					port=$1		
					;;
		-hp | --httpfsPort)	shift
					httpfsPort=$1
					;;
		-f | --filePath )	shift
					filePath=$1
					;;
		-rp | --remotePath )	shift
					remotePath=$1
					;;
		--help )		helpPage
					exit 0
					;;			
		* )			usage
					exit 1
	esac
	shift
done

if [ "$user" == "" ]
then exit 1
fi
if [ "$password" == "" ]
then exit 1
fi
if [ "$filePath" == "" ]
then exit 1
fi
if [ "$remotePath" == "" ]
then 
	remotePath="/user/$user"
elif [ ${remotePath:0:1} == "/" ]
then 
	remotePath=$remotePath
else 
	remotePath="/user/$user/$remotePath"
fi

#extract file name
fileName=$(basename "$filePath")

#Get LTPA Token
hostLoginForm="http://$host:$port/j_security_check"
echo "Connecting to $hostLoginForm to acquire LTPA token"
curl -i -s -S -c httpfsDemoCookieFile -d "j_username=$user" -d "j_password=$password" $hostLoginForm>NUL

#HTTPFS url
httpfsUrl="http://$host:$httpfsPort/webhdfs/v1"
echo "HTTPFS url : $httpfsUrl"
#List directory
#curl -i -b httpfsDemoCookieFile "$httpfsUrl/tmp?op=LISTSTATUS"

#Upload specified file to specified location via HTTPFS
echo "Uploading file $filePath to $remotePath in HDFS via HTTPFS"
curl -X PUT -L -b httpfsDemoCookieFile "$httpfsUrl$remotePath/$fileName?op=CREATE" --header "Content-Type:application/octet-stream" --header "Transfer-Encoding:chunked" -T "$filePath"
