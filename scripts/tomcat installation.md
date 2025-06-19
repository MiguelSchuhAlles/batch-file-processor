### Installing Tomcat 8 on Ubuntu 24.04

1. Install Java (Tomcat 8 requires Java 8+)

```  
sudo apt update
``` 

``` 
sudo apt install openjdk-11-jdk -y
``` 

Then check:
``` 
java -version 
```

2. Create a Tomcat user (optional but recommended)
```
sudo useradd -m -U -d /opt/tomcat -s /bin/false tomcat
```

3. Download Tomcat 8 from Apache archives
```
cd /tmp
```

```
wget https://archive.apache.org/dist/tomcat/tomcat-8/v8.5.100/bin/apache-tomcat-8.5.100.tar.gz
```
(You can check for newer Tomcat 8.x versions at: https://archive.apache.org/dist/tomcat/tomcat-8/ )

4. Install Tomcat
```
sudo mkdir /opt/tomcat-8.5.100
```

```
sudo tar xzvf apache-tomcat-8.5.100.tar.gz -C /opt/tomcat-8.5.100 --strip-components=1
```

5. Set Permissions
```
sudo chown -R tomcat: /opt/tomcat-8.5.100
```

```
sudo bash -c 'chmod +x /opt/tomcat-8.5.100/bin/*.sh'
```

6. (Optional) Create a systemd service for Tomcat
```
sudo nano /etc/systemd/system/tomcat.service
```

Paste:
```
[Unit]
Description=Apache Tomcat Web Application Container
After=network.target

[Service]
Type=forking

User=tomcat
Group=tomcat

Environment="JAVA_HOME=/usr/lib/jvm/java-1.11.0-openjdk-amd64"
Environment="CATALINA_PID=/opt/tomcat-8.5.100/temp/tomcat.pid"
Environment="CATALINA_HOME=/opt/tomcat-8.5.100"
Environment="CATALINA_BASE=/opt/tomcat-8.5.100"

ExecStart=/opt/tomcat-8.5.100/bin/startup.sh
ExecStop=/opt/tomcat-8.5.100/bin/shutdown.sh

[Install]
WantedBy=multi-user.target
```

7. Create a tomcat user

Edit users file:
```
sudo nano /opt/tomcat-8.5.100/conf/tomcat-users.xml
```

Add a user with manager-gui role:
```
<role rolename="manager-gui"/>
<user username="admin" password="someStrongPassword" roles="manager-gui"/>
```

Example:
```
<tomcat-users>
  <role rolename="manager-gui"/>
  <user username="admin" password="someStrongPassword!" roles="manager-gui"/>
</tomcat-users>
```

8. (optional and risky) Enable Manager access from external IP
```
sudo nano /opt/tomcat-8.5.100/webapps/manager/META-INF/context.xml
```

Comment-out this segment blocking external IPs:
```
<Valve className="org.apache.catalina.valves.RemoteAddrValve"
       allow="127\.\d+\.\d+\.\d+|::1|0:0:0:0:0:0:0:1" />
```


9. Start and Enable Tomcat
```
sudo systemctl daemon-reload
```

```
sudo systemctl start tomcat
```

```
sudo systemctl enable tomcat
```

Check status:
```
sudo systemctl status tomcat
```

Obs: the restart command is:
```
sudo systemctl restart tomcat
```


10. Allow Port 8080 in EC2 Security Group

Make sure inbound TCP port 8080 is allowed for your EC2 instance.

Then access:
http://<EC2-public-IP>:8080/