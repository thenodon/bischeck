FROM java:8 

# Install maven
RUN apt-get update  
RUN apt-get install -y maven

RUN mkdir /app
WORKDIR /app

Add . /app
