FROM openjdk:11

COPY . /server

WORKDIR /server

RUN ./gradlew build

CMD ./gradlew run
